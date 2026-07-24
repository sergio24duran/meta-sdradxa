SUMMARY = "Full SDK for Radxa Dragon boards — complete cross-compilation toolchain"
DESCRIPTION = "Extends the minimal SDK with development libraries and headers \
for every subsystem in the full image. Used with populate_sdk to produce the \
sdradxa cross-compilation SDK. Not intended to be flashed to the board."

require recipes-core/images/sdradxa-image-minimal-sdk.bb

# Full image packages (same additions as sdradxa-image-full.bb)
# mavsdk: its -dev goes to the toolchain below; the runtime must be in the
# image so bitbake can resolve the recipe's packages (RPROVIDES).
IMAGE_INSTALL:append = " \
    packagegroup-sdradxa-connectivity \
    libgpiod \
    libgpiod-tools \
    mavsdk \
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
# mavsdk-dev: sw_bpk (bpk-mavlink) links libmavsdk3 from the SDK, retiring
# the out-of-tree manual cross-build (Rung B, yocto-bpk#56). Use the
# bitbake package name (mavsdk-dev); the debian rename to libmavsdk-dev is
# a rootfs-level PKG name and does not resolve in TOOLCHAIN_TARGET_TASK.
# Target-only: MAVSDK runs on the drone, never on the host.
TOOLCHAIN_TARGET_TASK:append = " \
    iproute2 \
    mavsdk-dev \
"
