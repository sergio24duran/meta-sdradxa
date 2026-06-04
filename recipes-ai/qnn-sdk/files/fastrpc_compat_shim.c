/*
 * FastRPC Compatibility Shim for QNN SDK on open-source Yocto/Linux
 *
 * Problem:
 *   The QNN SDK's HTP Stub library (libQnnHtpV68Stub.so) was built against
 *   Qualcomm's proprietary Hexagon SDK 5.5.5 fastrpc, which exports symbols
 *   not present in the open-source Linaro fastrpc (libcdsprpc.so):
 *
 *     - fastrpc_mmap()          : map a DMA buffer for DSP access
 *     - fastrpc_munmap()        : unmap a DMA buffer
 *     - rpcmem_alloc2()         : allocate RPC-shared memory (64-bit flags)
 *     - remote_system_request() : send system-level request to DSP
 *
 *   Without these symbols, dlopen() of the Stub library fails:
 *     "undefined symbol: fastrpc_mmap"
 *
 * Solution:
 *   This shim implements the missing functions using:
 *     - Kernel ioctls (FASTRPC_IOCTL_MEM_MAP/MEM_UNMAP) for memory mapping
 *     - dlsym(RTLD_NEXT) to delegate rpcmem_alloc2 to the real rpcmem_alloc
 *     - A no-op stub for remote_system_request (not used in practice)
 *
 *   The shim finds the fastrpc device fd by scanning /proc/self/fd, which
 *   avoids depending on internal state of libcdsprpc.so.
 *
 * Usage:
 *   Loaded via LD_PRELOAD before any QNN binary that uses the HTP backend.
 *   The qnn-env.sh profile script sets this up automatically.
 *
 * Note on --no-as-needed:
 *   This shim is compiled with -Wl,--no-as-needed -lcdsprpc. Even though no
 *   symbols from libcdsprpc are referenced directly, the NEEDED entry is
 *   critical: it forces the dynamic linker to load libcdsprpc.so into the
 *   process when the shim is LD_PRELOAD'ed. Without it, rpcmem_alloc and
 *   remote_handle functions (called by the QNN Stub via PLT) are never
 *   resolved, causing "Failed to allocate memory" at runtime.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

#define _GNU_SOURCE
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <dlfcn.h>
#include <dirent.h>
#include <sys/ioctl.h>
#include <linux/types.h>

/* ---- Kernel uAPI structures (from <misc/fastrpc.h>) ---- */

struct fastrpc_mem_map {
    __s32 version;
    __s32 fd;
    __s32 offset;
    __u32 flags;
    __u64 vaddrin;
    __u64 length;
    __u64 vaddrout;
    __s32 attrs;
    __s32 reserved[4];
};

struct fastrpc_mem_unmap {
    __s32 vesion;       /* typo in kernel uapi, kept for ABI compat */
    __s32 fd;
    __u64 vaddr;
    __u64 length;
    __s32 reserved[5];
};

#define FASTRPC_IOCTL_MEM_MAP   _IOWR('R', 10, struct fastrpc_mem_map)
#define FASTRPC_IOCTL_MEM_UNMAP _IOWR('R', 11, struct fastrpc_mem_unmap)

/* ---- FastRPC domain ID to device node mapping ---- */

static const char *domain_devnames[] = {
    [0] = "fastrpc-adsp",   /* Audio DSP */
    [1] = "fastrpc-mdsp",   /* Modem DSP */
    [2] = "fastrpc-sdsp",   /* Sensor DSP */
    [3] = "fastrpc-cdsp",   /* Compute DSP (NPU) */
};
#define NUM_DOMAINS 4

/*
 * find_session_fd - locate the fastrpc device fd for a given domain
 *
 * The open-source libcdsprpc opens /dev/fastrpc-cdsp internally and stores
 * the fd in a static struct (hlist[domain].dev). Since we can't access that
 * internal state, we scan /proc/self/fd to find which fd points to the
 * fastrpc device for the requested domain.
 *
 * This must use the SAME fd that libcdsprpc opened, because the kernel
 * fastrpc driver associates PD sessions with specific file descriptors.
 * Opening a new fd would create a separate session and break memory mapping.
 */
static int find_session_fd(int domain) {
    DIR *d;
    struct dirent *e;
    char link[256], target[256];
    int fd_num;
    ssize_t len;

    if (domain < 0 || domain >= NUM_DOMAINS)
        return -1;

    d = opendir("/proc/self/fd");
    if (!d)
        return -1;

    while ((e = readdir(d)) != NULL) {
        if (e->d_name[0] == '.')
            continue;
        fd_num = atoi(e->d_name);
        snprintf(link, sizeof(link), "/proc/self/fd/%d", fd_num);
        len = readlink(link, target, sizeof(target) - 1);
        if (len <= 0)
            continue;
        target[len] = '\0';
        if (strstr(target, domain_devnames[domain])) {
            closedir(d);
            return fd_num;
        }
    }
    closedir(d);
    return -1;
}

/*
 * fastrpc_mmap - map a DMA buffer for DSP access
 *
 * Called by libQnnHtpV68Stub.so to register shared memory buffers with the
 * DSP. Uses the newer FASTRPC_IOCTL_MEM_MAP (ioctl 10) which supports
 * offset and extended flags, unlike the older FASTRPC_IOCTL_MMAP (ioctl 6)
 * used by the open-source remote_mmap64().
 */
int fastrpc_mmap(int domain, int fd, void *addr, int offset,
                 size_t length, int flags) {
    struct fastrpc_mem_map map;
    int dev = find_session_fd(domain);
    if (dev < 0)
        return -1;

    memset(&map, 0, sizeof(map));
    map.fd = fd;
    map.offset = offset;
    map.flags = (__u32)flags;
    map.vaddrin = (__u64)(uintptr_t)addr;
    map.length = (__u64)length;

    return ioctl(dev, FASTRPC_IOCTL_MEM_MAP, &map);
}

/*
 * fastrpc_munmap - unmap a previously mapped DMA buffer
 *
 * Note: the munmap errors seen at QNN shutdown ("fastrpc memory failed to
 * unmap") are cosmetic — inference results are already written. The kernel
 * driver cleans up mappings when the fd is closed.
 */
int fastrpc_munmap(int domain, int fd, void *addr, size_t length) {
    struct fastrpc_mem_unmap unmap;
    int dev = find_session_fd(domain);
    if (dev < 0)
        return -1;

    memset(&unmap, 0, sizeof(unmap));
    unmap.fd = fd;
    unmap.vaddr = (__u64)(uintptr_t)addr;
    unmap.length = (__u64)length;

    return ioctl(dev, FASTRPC_IOCTL_MEM_UNMAP, &unmap);
}

/*
 * rpcmem_alloc2 - allocate RPC-shared memory (64-bit flags variant)
 *
 * The QNN Stub calls this instead of rpcmem_alloc() when it needs extended
 * allocation flags. We delegate to the real rpcmem_alloc from libcdsprpc.so
 * via dlsym(RTLD_NEXT), truncating the flags to 32-bit (upper bits unused
 * in practice on this platform).
 */
void *rpcmem_alloc2(int heapid, uint64_t flags, int size) {
    void *(*real_alloc)(int, unsigned int, int);
    real_alloc = dlsym(RTLD_NEXT, "rpcmem_alloc");
    if (!real_alloc)
        return NULL;
    return real_alloc(heapid, (unsigned int)flags, size);
}

/*
 * remote_system_request - send a system request to the DSP
 *
 * Not used by the QNN HTP inference path. Provided as a no-op to satisfy
 * the Stub library's symbol dependency.
 */
int remote_system_request(int domain, int request) {
    (void)domain;
    (void)request;
    return 0;
}
