SUMMARY = "MAVLink libevents C++ library, MAVSDK dependency"
DESCRIPTION = "MAVLink events interface (libs/cpp) consumed by MAVSDK's \
SUPERBUILD=OFF build (find_package(libevents)). Built static + PIC so it can \
link into the MAVSDK shared library."
HOMEPAGE = "https://github.com/mavlink/libevents"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE.md;md5=92eb10e2bbc58e0d704de5f23dd755ef"

SRC_URI = "git://github.com/mavlink/libevents;protocol=https;nobranch=1"
SRCREV = "7c1720749dfe555ec2e71d5f9f753e6ac1244e1c"

S = "${WORKDIR}/git"
OECMAKE_SOURCEPATH = "${S}/libs/cpp"

inherit cmake

EXTRA_OECMAKE = "-DBUILD_SHARED_LIBS=OFF -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DENABLE_TESTING=OFF"
