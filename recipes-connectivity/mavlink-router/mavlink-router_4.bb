SUMMARY = "MAVLink router: bridges the flight-controller UART to UDP/TCP endpoints"
DESCRIPTION = "mavlink-routerd routes MAVLink between a serial FC link and \
UDP/TCP endpoints. On BPK it bridges the FC on /dev/ttyHS1 to udp 14540, the \
single endpoint bpk-mavlink (the sole MAVSDK owner) dials into."
HOMEPAGE = "https://github.com/mavlink-router/mavlink-router"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=93888867ace35ffec2c845ea90b2e16b"

# v4 tag; gitsm pulls the pinned c_library_v2 submodule (generated MAVLink headers).
SRCREV = "42529d55b665e0a9a29e424e186f514c56c2e5b5"
SRC_URI = "gitsm://github.com/mavlink-router/mavlink-router.git;protocol=https;nobranch=1 \
           file://main.conf"

S = "${WORKDIR}/git"
PV = "4+git${SRCPV}"

DEPENDS = "systemd"

inherit meson pkgconfig systemd

SYSTEMD_SERVICE:${PN} = "mavlink-router.service"
SYSTEMD_AUTO_ENABLE = "enable"

do_install:append() {
    install -d ${D}${sysconfdir}/mavlink-router
    install -m 0644 ${WORKDIR}/main.conf ${D}${sysconfdir}/mavlink-router/main.conf
}

CONFFILES:${PN} = "${sysconfdir}/mavlink-router/main.conf"
FILES:${PN} += "${sysconfdir}/mavlink-router/main.conf"
