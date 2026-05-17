SUMMARY = "Minimal SDK for Radxa Dragon boards — base cross-compilation toolchain"
DESCRIPTION = "Extends the minimal image with the base host tools (CMake, \
pkgconfig, Python3). Used with populate_sdk to produce a lightweight \
cross-compilation SDK. Not intended to be flashed to the board."

require recipes-core/images/sdradxa-image-minimal.bb

TOOLCHAIN_HOST_TASK:append = " \
    nativesdk-cmake \
    nativesdk-pkgconfig \
    nativesdk-python3 \
    nativesdk-python3-pip \
"
