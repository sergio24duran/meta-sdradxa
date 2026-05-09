SUMMARY = "SDK image for Radxa Dragon boards — generates the cross-compilation toolchain"
DESCRIPTION = "Extends the minimal image with development libraries and headers. \
Used exclusively with populate_sdk to produce the sdradxa cross-compilation SDK. \
Not intended to be flashed to the board."

require recipes-core/images/sdradxa-image-minimal.bb

# ---------------------------------------------------------------------------
# COMMON LIBRARIES
# Same libraries available on both sides of the SDK:
#   - target: cross-compiled for aarch64 (link against in cross-compilation)
#   - host:   compiled for x86_64 (local development and testing)
# To add a new common library: add <lib>-dev to TARGET and
# nativesdk-<lib>-dev to HOST. The lib's recipe needs
# BBCLASSEXTEND += "nativesdk" (bbappend in meta-sdradxa if missing).
# ---------------------------------------------------------------------------
TOOLCHAIN_TARGET_TASK:append = " \
    libgpiod-dev \
"

TOOLCHAIN_HOST_TASK:append = " \
    nativesdk-libgpiod-dev \
"

# ---------------------------------------------------------------------------
# TARGET-ONLY
# ---------------------------------------------------------------------------
TOOLCHAIN_TARGET_TASK:append = " \
    iproute2 \
"

# ---------------------------------------------------------------------------
# HOST-ONLY
# ---------------------------------------------------------------------------
TOOLCHAIN_HOST_TASK:append = " \
    nativesdk-cmake \
    nativesdk-pkgconfig \
    nativesdk-python3 \
    nativesdk-python3-pip \
"
