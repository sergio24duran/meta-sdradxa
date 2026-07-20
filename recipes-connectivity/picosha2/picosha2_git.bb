SUMMARY = "PicoSHA2 - header-only SHA-256, MAVSDK dependency"
DESCRIPTION = "julianoes' fork of PicoSHA2 with CMake install support, as \
consumed by MAVSDK's SUPERBUILD=OFF build (find_package(picosha2))."
HOMEPAGE = "https://github.com/julianoes/PicoSHA2"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=277ac5bc231af240c801ade17211246a"

SRC_URI = "git://github.com/julianoes/PicoSHA2;protocol=https;branch=cmake-install-support"
SRCREV = "1bf940d8a03bb752604fbb366d47b97b50b9e6ce"

S = "${WORKDIR}/git"

inherit cmake

# Header-only: only picosha2.h + the CMake package config land, in -dev.
ALLOW_EMPTY:${PN} = "1"
