# QNN SDK environment setup
# Sourced automatically by login shells via /etc/profile.d/
#
# ADSP_LIBRARY_PATH: semicolon-separated search path used by the FastRPC
# apps_std file-serving layer when the cDSP requests to load a shared object
# (e.g., libQnnHtpV68Skel.so). The cDSP's dynamic linker (_rtld) calls back
# to the ARM side via apps_std_fopen_with_env("ADSP_LIBRARY_PATH", ";", ...).
#
# LD_PRELOAD: loads the FastRPC compatibility shim (libfastrpc_compat.so)
# into any process started from a login shell. The shim provides symbols
# (fastrpc_mmap, rpcmem_alloc2, etc.) that the QNN HTP Stub library expects
# but the open-source Linaro fastrpc does not export. The shim also forces
# libcdsprpc.so to be loaded (via its NEEDED entry), which is required for
# rpcmem and remote_handle functions to be available to the QNN Stub.
#
# Note: this only affects login shell processes, not boot-time services.
# The cdsprpcd daemon (FastRPC cDSP listener) runs independently via systemd.

export ADSP_LIBRARY_PATH="/usr/lib/rfsa/adsp;/usr/lib"
export LD_PRELOAD="${LD_PRELOAD:+$LD_PRELOAD:}/usr/lib/libfastrpc_compat.so"
