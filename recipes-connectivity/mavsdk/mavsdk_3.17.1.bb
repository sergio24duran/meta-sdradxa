SUMMARY = "MAVSDK - MAVLink C++ SDK, shared library"
DESCRIPTION = "MAVSDK v3 built SUPERBUILD=OFF against system dependencies (no \
build-time source fetches): shared libmavsdk.so + headers + CMake package for \
sw_bpk to link against via the Yocto SDK. Server/gRPC backend and curl are off \
(no camera-definition download in product mode)."
HOMEPAGE = "https://mavsdk.mavlink.io"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE.md;md5=84b641454775df91a2bae8fdd450e2e9"

SRCREV = "23fd285341dd066d5459a9c822e54c420e1299cd"
SRC_URI = "gitsm://github.com/mavlink/MAVSDK;protocol=https;nobranch=1 \
           file://0001-version-str-override.patch \
           file://0002-liblzma-module-mode.patch"

S = "${WORKDIR}/git"

DEPENDS = "jsoncpp libtinyxml2 libevents picosha2 libmavlike mavlink xz"

inherit cmake pkgconfig

# SUPERBUILD=OFF: all third_party deps come from the sysroot recipes above.
# git describe is unavailable after do_unpack, so pin the version explicitly.
EXTRA_OECMAKE = " \
    -DSUPERBUILD=OFF \
    -DBUILD_SHARED_LIBS=ON \
    -DBUILD_MAVSDK_SERVER=OFF \
    -DBUILD_WITHOUT_CURL=ON \
    -DBUILD_TESTING=OFF \
    -DMAVLINK_DIALECT=ardupilotmega \
    -DVERSION_STR=v3.17.1 \
"
