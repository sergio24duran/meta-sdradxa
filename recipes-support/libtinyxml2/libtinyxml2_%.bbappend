FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# meta-oe's meson build ships no CMake package config; add one so consumers
# using find_package(tinyxml2) in CONFIG mode (MAVSDK, libmavlike) resolve.
SRC_URI += "file://tinyxml2Config.cmake"

do_install:append() {
    install -d ${D}${libdir}/cmake/tinyxml2
    install -m 0644 ${WORKDIR}/tinyxml2Config.cmake ${D}${libdir}/cmake/tinyxml2/tinyxml2Config.cmake
}

FILES:${PN}-dev += "${libdir}/cmake/tinyxml2"
