SUMMARY = "Full SDK for Radxa Dragon boards — complete cross-compilation toolchain"
DESCRIPTION = "Extends the minimal SDK with development libraries and headers \
for every subsystem in the full image. Used with populate_sdk to produce the \
sdradxa cross-compilation SDK. Not intended to be flashed to the board."

require recipes-core/images/sdradxa-image-minimal-sdk.bb

# Full image packages (same additions as sdradxa-image-full.bb)
IMAGE_INSTALL:append = " \
    packagegroup-sdradxa-connectivity \
    libgpiod \
    libgpiod-tools \
"

# ---------------------------------------------------------------------------
# COMMON LIBRARIES (target + host)
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
