SUMMARY = "libmavlike - runtime-defined MAVLink message set, MAVSDK dependency"
DESCRIPTION = "julianoes' libmavlike, providing the 'mav' CMake package \
consumed by MAVSDK's SUPERBUILD=OFF build (find_package(mav)). Static + PIC \
so it links into the MAVSDK shared library."
HOMEPAGE = "https://github.com/julianoes/libmavlike"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=8e5974086ac4189f96d6afe4dc36ddbe"

SRC_URI = "git://github.com/julianoes/libmavlike;protocol=https;nobranch=1"
SRCREV = "90498b14262137ae10b633705810e81bdb85de9c"

S = "${WORKDIR}/git"

DEPENDS = "libtinyxml2 picosha2"

inherit cmake

EXTRA_OECMAKE = "-DBUILD_SHARED_LIBS=OFF -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DBUILD_TESTING=OFF"

# The CMake package config is installed under ${datadir}/mav/cmake, not the
# default ${libdir}/cmake; ship it (and the headers/static lib) in -dev.
FILES:${PN}-dev += "${datadir}/mav"
