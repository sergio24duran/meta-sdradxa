SUMMARY = "Qualcomm AI Runtime (QNN) SDK for QCS6490 Hexagon NPU"
DESCRIPTION = "Packages the QNN v2.43 runtime for AI inference on the QCS6490 \
Hexagon V68 NPU (12 TOPS). Includes: \
  - ARM64 runtime libraries (HTP, CPU, GPU, HTA backends) \
  - Hexagon DSP skel libraries (loaded into cDSP via FastRPC) \
  - CLI tools (qnn-net-run, qnn-platform-validator, etc.) \
  - DSP user-space runtime from QCM6490 firmware (fastrpc shells, libc++, sysmon) \
  - FastRPC compatibility shim (bridges Hexagon SDK 5.5.5 API to open-source fastrpc) \
  - Environment setup for ADSP_LIBRARY_PATH and LD_PRELOAD"
HOMEPAGE = "https://developer.qualcomm.com/software/qualcomm-neural-network-sdk"
LICENSE = "CLOSED"

# ---------------------------------------------------------------------------
# Source: Qualcomm AI Runtime (QAIRT) Community Edition
#
# Hosted on GitHub Releases for automated download by bitbake.
# Original source: softwarecenter.qualcomm.com (requires free Qualcomm account).
# The SDK was downloaded via QPM3, then repacked as ZIP for Yocto consumption.
# See the release notes at the URL below for reproduction steps.
# ---------------------------------------------------------------------------
QAIRT_VER = "2.43.0.260128"
QAIRT_BLOB_BASE = "https://github.com/sergio24duran/meta-sdradxa/releases/download/vendor-blobs-v1"

# ---------------------------------------------------------------------------
# Source: QCM6490 DSP runtime from Qualcomm Linux SPF (CodeLinaro artifacts)
#
# Contains Hexagon ELF binaries that run *inside* the cDSP protection domain:
#   - fastrpc_shell_unsigned_3 : creates the unsigned PD for user skel libs
#   - fastrpc_shell_3          : creates the signed PD (fallback)
#   - libsysmondomain_skel.so : DSP system monitor (required for PD init)
#   - libc++.so.1, libc++abi.so.1 : C++ runtime for Hexagon skel libraries
#
# Without these, the cDSP refuses to load QNN skel libraries (errno 69).
# This URL is publicly accessible without authentication.
# ---------------------------------------------------------------------------
DSPFW_VER = "00039.2"
DSPFW_BASE = "https://artifacts.codelinaro.org/artifactory/qli-ci/software/chip/qualcomm_linux-spf-1-0/qualcomm-linux-spf-1-0_test_device_public/r1.0_${DSPFW_VER}/QCM6490.LE.1.0/common/build/ufs/bin"

SRC_URI = " \
    ${QAIRT_BLOB_BASE}/v${QAIRT_VER}.zip;name=qairt \
    ${DSPFW_BASE}/QCM6490_dspso.zip;name=dspso \
    file://fastrpc_compat_shim.c \
    file://qnn-env.sh \
"
SRC_URI[qairt.sha256sum] = "abc704e79ab3e6f556907436422fe23870cd564f2957a5c0df9d97788a1ee415"
SRC_URI[dspso.sha256sum] = "56d719772c60ceb237b0d46cf2cb1fafd35c2fd25a8f816e4426665876ad5934"

S = "${WORKDIR}/qairt/${QAIRT_VER}"

# QCS6490 has Hexagon V68 (Hexagon 698 core, part of the ADRENO_6XX_GEN4 family)
HEXAGON_VERSION = "v68"
HEXAGON_DIR = "hexagon-${HEXAGON_VERSION}"

# QNN SDK ships pre-built binaries for this OE toolchain variant
PLATFORM_DIR = "aarch64-oe-linux-gcc11.2"

# Build-time dependency: the compatibility shim links against libcdsprpc
DEPENDS = "fastrpc"

# The SDK ships pre-stripped proprietary binaries and Hexagon ELFs (not ARM).
# Both trigger QA false positives that we must suppress.
do_package_qa[noexec] = "1"

INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"

INSANE_SKIP:${PN} += "arch already-stripped ldflags"
INSANE_SKIP:${PN}-dev += "arch already-stripped"

# ---------------------------------------------------------------------------
# Compile: build the FastRPC compatibility shim
#
# The QNN SDK's libQnnHtpV68Stub.so was built against Qualcomm's proprietary
# Hexagon SDK 5.5.5, which exports symbols that the open-source Linaro
# fastrpc (libcdsprpc.so) does not provide:
#
#   fastrpc_mmap   - map DMA buffer for DSP access (kernel ioctl 10)
#   fastrpc_munmap - unmap DMA buffer (kernel ioctl 11)
#   rpcmem_alloc2  - extended rpcmem allocation with 64-bit flags
#   remote_system_request - DSP system request (stub, unused in practice)
#
# The shim implements these using kernel ioctls directly and dlsym fallback.
# It MUST link against libcdsprpc.so (--no-as-needed) to ensure the fastrpc
# library is loaded into the process, which is required for rpcmem and
# remote_handle functions to work.
# ---------------------------------------------------------------------------
do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -shared -fPIC \
        -o ${B}/libfastrpc_compat.so \
        ${WORKDIR}/fastrpc_compat_shim.c \
        -Wl,--no-as-needed -lcdsprpc -ldl
}

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${includedir}/QNN
    install -d ${D}${libdir}
    install -d ${D}${libdir}/rfsa/adsp
    install -d ${D}${sysconfdir}/profile.d

    # --- ARM64 runtime libraries ---
    # Core QNN backend libraries: HTP (NPU), CPU, GPU, HTA, GenAI, etc.
    for f in ${S}/lib/${PLATFORM_DIR}/libQnn*.so; do
        [ -f "$f" ] && install -m 0755 "$f" ${D}${libdir}/
    done
    install -m 0755 ${S}/lib/${PLATFORM_DIR}/libhta_hexagon_runtime_qnn.so ${D}${libdir}/
    install -m 0755 ${S}/lib/${PLATFORM_DIR}/libPlatformValidatorShared.so ${D}${libdir}/
    install -m 0755 ${S}/lib/${PLATFORM_DIR}/libcalculator.so ${D}${libdir}/

    # --- FastRPC compatibility shim ---
    install -m 0755 ${B}/libfastrpc_compat.so ${D}${libdir}/

    # --- CLI tools ---
    # qnn-net-run:       run inference with a model + backend
    # qnn-platform-validator: check DSP hardware/library availability
    # qnn-context-binary-generator: serialize model graphs for deployment
    # snpe-net-run:      legacy SNPE inference runner
    for f in ${S}/bin/${PLATFORM_DIR}/qnn-*; do
        [ -f "$f" ] && install -m 0755 "$f" ${D}${bindir}/
    done
    install -m 0755 ${S}/bin/${PLATFORM_DIR}/qtld-net-run ${D}${bindir}/
    install -m 0755 ${S}/bin/${PLATFORM_DIR}/snpe-net-run ${D}${bindir}/

    # --- Hexagon DSP skel libraries ---
    # These are Hexagon V68 ELFs (NOT ARM) loaded into the cDSP via FastRPC.
    # libQnnHtpV68Skel.so is the main NPU inference skel.
    # Installed to BOTH paths because:
    #   - /usr/lib/rfsa/adsp/ : canonical ADSP_LIBRARY_PATH search location
    #   - /usr/lib/           : the cDSP dynamic linker (_rtld) also resolves
    #                           transitive dependencies from this path
    for f in ${S}/lib/${HEXAGON_DIR}/unsigned/*.so; do
        [ -f "$f" ] && install -m 0755 "$f" ${D}${libdir}/rfsa/adsp/
        [ -f "$f" ] && install -m 0755 "$f" ${D}${libdir}/
    done

    # --- DSP runtime from QCM6490 firmware ---
    # Hexagon ELF binaries required by the cDSP to create protection domains
    # and provide C++ runtime for skel libraries.
    #
    # fastrpc_shell_unsigned_3 : the unsigned PD shell for cDSP (domain 3).
    #   The open-source fastrpc library searches /usr/lib/ first (hardcoded),
    #   then /vendor/dsp/, then ADSP_LIBRARY_PATH. Must be in /usr/lib/.
    #
    # libc++.so.1, libsysmondomain_skel.so, etc. : DSP-side runtime deps.
    #   Without these the cDSP dynamic linker fails to load skel libraries
    #   with "_rtld_map_object_ex: cannot open ..., errno 69".
    install -m 0755 ${WORKDIR}/QCM6490_dspso/usr/lib/dsp/cdsp/fastrpc_shell_3 ${D}${libdir}/fastrpc_shell_3
    install -m 0755 ${WORKDIR}/QCM6490_dspso/usr/lib/dsp/cdsp/fastrpc_shell_unsigned_3 ${D}${libdir}/fastrpc_shell_unsigned_3
    for f in ${WORKDIR}/QCM6490_dspso/usr/lib/dsp/cdsp/lib*.so ${WORKDIR}/QCM6490_dspso/usr/lib/dsp/cdsp/lib*.so.1; do
        [ -f "$f" ] && install -m 0755 "$f" ${D}${libdir}/
    done

    # --- Environment setup ---
    # Sourced by login shells via /etc/profile.d/. Sets:
    #   ADSP_LIBRARY_PATH : tells fastrpc where to find DSP skel libraries
    #   LD_PRELOAD        : loads the compatibility shim before QNN binaries
    install -m 0644 ${WORKDIR}/qnn-env.sh ${D}${sysconfdir}/profile.d/qnn-env.sh

    # --- Development headers ---
    cp -r ${S}/include/QNN/* ${D}${includedir}/QNN/
    chmod -R 0644 ${D}${includedir}/QNN/
}

# QNN libraries use bare .so names (no .so.N versioning)
SOLIBS = ".so"
FILES_SOLIBSDEV = ""

FILES:${PN} = " \
    ${libdir}/*.so \
    ${libdir}/*.so.1 \
    ${libdir}/fastrpc_shell_3 \
    ${libdir}/fastrpc_shell_unsigned_3 \
    ${libdir}/rfsa/adsp/* \
    ${bindir}/* \
    ${sysconfdir}/profile.d/qnn-env.sh \
"
FILES:${PN}-dev = "${includedir}/QNN/*"

RDEPENDS:${PN} = "fastrpc"
