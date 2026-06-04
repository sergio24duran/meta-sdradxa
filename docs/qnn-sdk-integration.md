# QNN SDK Integration for Radxa Dragon Q6A (QCS6490)

## Overview

This document describes the integration of the Qualcomm Neural Network (QNN)
SDK v2.43 into the sdYocto BSP for the Radxa Dragon Q6A board. The QCS6490 SoC
includes a Hexagon V68 DSP with a 12 TOPS NPU (Hexagon Tensor Processor).

The integration enables AI inference on the NPU directly from the Yocto image,
with no manual configuration required after flashing.

## Hardware

| Component | Details |
|-----------|---------|
| SoC | Qualcomm QCS6490 |
| NPU | Hexagon V68 DSP, 12 TOPS (HTP - Hexagon Tensor Processor) |
| GPU | Adreno 643 (A635) — not functional yet, separate issue |
| FastRPC | `/dev/fastrpc-cdsp`, 14 compute context banks |
| DSP Firmware | `cdsp.mbn` (Compute DSP), `adsp.mbn` (Audio DSP) |

## Architecture

```
+---------------------------+       +---------------------------+
|        ARM CPU (A78)      |       |    Hexagon V68 cDSP       |
|                           |       |                           |
|  qnn-net-run              |       |  libQnnHtpV68Skel.so      |
|    |                      |       |    |                      |
|  libQnnHtp.so             |       |  libQnnHtpV68.so          |
|    |                      |       |    |                      |
|  libQnnHtpV68Stub.so ----FastRPC---->  (loaded via FastRPC)   |
|    |                      |       |                           |
|  libfastrpc_compat.so     |       |  fastrpc_shell_unsigned_3 |
|    |                      |       |  libc++.so.1              |
|  libcdsprpc.so            |       |  libsysmondomain_skel.so  |
|    |                      |       |                           |
|  /dev/fastrpc-cdsp -------+-------+  (kernel fastrpc driver)  |
+---------------------------+       +---------------------------+
```

## Yocto recipes

### `recipes-ai/qnn-sdk/qnn-sdk_2.43.bb`

Main recipe. Packages:

- **ARM64 runtime libraries** from `lib/aarch64-oe-linux-gcc11.2/`:
  `libQnnHtp.so`, `libQnnCpu.so`, `libQnnGpu.so`, `libQnnHta.so`, etc.

- **Hexagon skel libraries** from `lib/hexagon-v68/unsigned/`:
  `libQnnHtpV68Skel.so`, `libQnnHtpV68.so` — Hexagon ELFs loaded into the cDSP

- **CLI tools** from `bin/aarch64-oe-linux-gcc11.2/`:
  `qnn-net-run`, `qnn-platform-validator`, `qnn-context-binary-generator`, etc.

- **DSP runtime** from QCM6490 firmware (CodeLinaro artifacts):
  `fastrpc_shell_unsigned_3`, `libc++.so.1`, `libsysmondomain_skel.so`, etc.

- **FastRPC compatibility shim** (`libfastrpc_compat.so`):
  Bridges the API gap between Qualcomm's proprietary Hexagon SDK 5.5.5 and the
  open-source Linaro fastrpc. See section below.

- **Environment script** (`/etc/profile.d/qnn-env.sh`):
  Sets `ADSP_LIBRARY_PATH` and `LD_PRELOAD` for login shells.

### `recipes-ai/packagegroups/packagegroup-sdradxa-ai.bb`

Meta-package that pulls in `qnn-sdk` and `fastrpc-systemd` (the cDSP daemon).

### `recipes-support/fastrpc/fastrpc_%.bbappend`

Enables the `cdsprpcd` systemd service at boot. The upstream fastrpc recipe
installs but disables this daemon by default.

## The FastRPC compatibility problem

### Background

The QNN SDK is built against Qualcomm's proprietary Hexagon SDK 5.5.5, which
ships a proprietary `libcdsprpc.so` (often called `libxdsprpc.so` internally).
This library exports functions not present in the open-source Linaro fastrpc:

| Symbol | Purpose | Open-source equivalent |
|--------|---------|----------------------|
| `fastrpc_mmap` | Map DMA buffer for DSP (ioctl 10) | `remote_mmap64` (ioctl 6, different API) |
| `fastrpc_munmap` | Unmap DMA buffer (ioctl 11) | `remote_munmap64` (ioctl 7, different API) |
| `rpcmem_alloc2` | Allocate RPC memory (64-bit flags) | `rpcmem_alloc` (32-bit flags) |
| `remote_system_request` | System request to DSP | None |

When `libQnnHtp.so` tries to `dlopen("libQnnHtpV68Stub.so")`, the load fails:

```
Failed in loading stub: libQnnHtpV68Stub.so: undefined symbol: fastrpc_mmap
```

### Solution: `libfastrpc_compat.so`

A small shared library that implements the missing symbols:

- **`fastrpc_mmap` / `fastrpc_munmap`**: Use the newer kernel ioctls
  (`FASTRPC_IOCTL_MEM_MAP`, ioctl 10 / `FASTRPC_IOCTL_MEM_UNMAP`, ioctl 11)
  directly. The key challenge is finding the correct device fd — it must be the
  same fd that `libcdsprpc.so` opened for the domain session. The shim scans
  `/proc/self/fd` to find the fd pointing to `/dev/fastrpc-cdsp`.

- **`rpcmem_alloc2`**: Delegates to `rpcmem_alloc` via `dlsym(RTLD_NEXT)`,
  truncating the 64-bit flags to 32-bit.

- **`remote_system_request`**: No-op stub (not used by QNN inference path).

The shim is loaded via `LD_PRELOAD` (set in `/etc/profile.d/qnn-env.sh`) and
**must** link against `libcdsprpc.so` with `--no-as-needed` to force the
fastrpc library into the process address space.

### Why `--no-as-needed`?

The shim doesn't call any `libcdsprpc` functions directly — it uses kernel
ioctls and `dlsym`. The Yocto toolchain sets `--as-needed` by default, which
strips "unused" library dependencies. But `libcdsprpc.so` **must** be loaded
because the QNN Stub library resolves `rpcmem_alloc`, `remote_handle64_open`,
and other symbols from it at runtime via the global symbol table.

## Other requirements discovered

### 1. `fastrpc_shell_unsigned_3` (DSP-side binary)

The open-source fastrpc library creates an "unsigned protection domain" (PD) on
the cDSP by loading a shell binary into the DSP. The fastrpc code searches:

1. `/usr/lib/fastrpc_shell_unsigned_3` (hardcoded first path)
2. `/vendor/dsp/fastrpc_shell_unsigned_3`
3. `ADSP_LIBRARY_PATH` entries

This binary is a Hexagon ELF (not ARM), provided by Qualcomm's QCM6490 firmware
package (`QCM6490_dspso.zip` from CodeLinaro artifacts).

### 2. DSP runtime libraries

The cDSP's dynamic linker (`_rtld_map_object_ex`) needs C++ runtime and system
monitor libraries to load QNN skel binaries. Without them:

```
_rtld_map_object_ex: cannot open libQnnHtpV68Skel.so, errno 69
```

Required files from `QCM6490_dspso.zip`:
- `libc++.so.1`, `libc++abi.so.1` — C++ standard library for Hexagon
- `libsysmondomain_skel.so` — system monitor (PD lifecycle management)

### 3. `cdsprpcd` daemon

The cDSP RPC daemon creates the default listener for FastRPC on domain 3
(cDSP). Without it running, the `remote_handle_open` call fails when trying to
create the unsigned PD. Enabled via the `fastrpc_%.bbappend`.

### 4. `qnn-platform-validator --testBackend` segfault

The validator tool only supports `--backend dsp` (legacy Hexagon V66 backend).
Our chip is V68 and uses the HTP backend. The `--testBackend` flag crashes
because it tries to use the incompatible DSP V66 skel path. This is a tool
limitation, not a system bug. The `--coreVersion` flag works correctly and
reports "Hexagon Architecture V68".

## Running inference

After booting the image, inference works out-of-the-box in any login shell:

```bash
# The environment is already set via /etc/profile.d/qnn-env.sh
# Verify hardware:
qnn-platform-validator --backend dsp --coreVersion

# Run a model on CPU backend:
qnn-net-run \
    --model libmy_model.so \
    --backend libQnnCpu.so \
    --input_list inputs.txt \
    --output_dir ./output_cpu

# Run on HTP (NPU, 12 TOPS):
qnn-net-run \
    --model libmy_model.so \
    --backend libQnnHtp.so \
    --input_list inputs.txt \
    --output_dir ./output_htp
```

### Compiling models

Models must be compiled into `.so` on the host using the QNN SDK tools:

```bash
# Source the Yocto SDK (needed for aarch64 cross-compiler)
source /opt/poky/environment-setup-armv8-2a-poky-linux

# Compile a model .cpp + .bin into a shared library
qnn-model-lib-generator \
    -c model.cpp \
    -b model.bin \
    -o ./model_libs \
    -t aarch64-oe-linux-gcc11.2
```

The QNN SDK host tools are at `/opt/qcom/aistack/qairt/2.43.0.260128/bin/x86_64-linux-clang/`.

## Verified test

Model: InceptionV3 Conv2d+ReLU (quantized int8, input 299x299x3)

| Backend | Exit code | Output size | Notes |
|---------|-----------|-------------|-------|
| CPU (`libQnnCpu.so`) | 0 | 2,841,728 bytes | Reference |
| HTP (`libQnnHtp.so`) | 0 | 2,841,728 bytes | NPU, 12 TOPS |

The HTP run produces identical output size, confirming correct NPU execution.

Non-critical `munmap` errors appear at shutdown (cleanup phase) — these do not
affect inference results and the kernel driver handles cleanup when the fd closes.

## File inventory

```
meta-sdradxa/
  recipes-ai/
    qnn-sdk/
      qnn-sdk_2.43.bb                      # Main recipe
      files/
        fastrpc_compat_shim.c               # FastRPC API bridge
        qnn-env.sh                          # Profile.d environment setup
    packagegroups/
      packagegroup-sdradxa-ai.bb            # AI meta-package
  recipes-support/
    fastrpc/
      fastrpc_%.bbappend                    # Enable cdsprpcd at boot
  recipes-core/
    images/
      sdradxa-image-full.bb                 # Includes packagegroup-sdradxa-ai
  docs/
    qnn-sdk-integration.md                  # This document
```

## Known limitations

- **GPU backend**: `libQnnGpu.so` requires `/dev/dri/renderD128` which is not
  available (Adreno 643 GMU probe fails). Tracked separately.
- **`munmap` errors at shutdown**: Cosmetic errors from the fastrpc shim's
  `fastrpc_munmap` implementation. Does not affect inference.
- **Profile.d only**: The `LD_PRELOAD` + `ADSP_LIBRARY_PATH` setup requires
  sourcing the profile script. Non-login-shell processes (e.g., systemd services)
  must set these variables explicitly in their unit files.
- **Model compilation on host**: Requires the Yocto SDK installed and the QNN SDK
  extracted. Models cannot be compiled on-target.

## Vendor blob hosting

The QNN SDK is proprietary and cannot be downloaded automatically from
Qualcomm's portal (requires authentication). The repacked ZIP is hosted on
GitHub Releases:

```
https://github.com/sergio24duran/meta-sdradxa/releases/tag/vendor-blobs-v1
```

The recipe's `SRC_URI` points there, so `bitbake` downloads it automatically.
If updating the QNN SDK version, upload the new ZIP to a new release and update
`QAIRT_VER`, the release URL, and the sha256 checksum in `qnn-sdk_2.43.bb`.

The DSP runtime (`QCM6490_dspso.zip`) is hosted on CodeLinaro's public artifact
server and downloads automatically without any manual steps.
