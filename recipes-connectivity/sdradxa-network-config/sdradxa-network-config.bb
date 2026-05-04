SUMMARY = "Network configuration for sdradxa boards"
DESCRIPTION = "Systemd-networkd configuration: enables the service and \
installs a DHCP network file for wired Ethernet interfaces."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://80-ethernet.network"

S = "${WORKDIR}"

inherit allarch

do_install() {
    # Network file: DHCP on all physical Ethernet ports
    install -d ${D}${sysconfdir}/systemd/network
    install -m 0644 ${WORKDIR}/80-ethernet.network \
        ${D}${sysconfdir}/systemd/network/

    # Enable systemd-networkd at boot
    install -d ${D}${sysconfdir}/systemd/system/multi-user.target.wants
    ln -sf /usr/lib/systemd/system/systemd-networkd.service \
        ${D}${sysconfdir}/systemd/system/multi-user.target.wants/systemd-networkd.service
}

FILES:${PN} = " \
    ${sysconfdir}/systemd/network/80-ethernet.network \
    ${sysconfdir}/systemd/system/multi-user.target.wants/systemd-networkd.service \
"

RDEPENDS:${PN} = "systemd"
