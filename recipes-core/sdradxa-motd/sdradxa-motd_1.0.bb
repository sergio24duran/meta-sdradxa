SUMMARY = "Login banner with runtime system info for sdradxa boards"
DESCRIPTION = "Installs /etc/profile.d/sdradxa-motd.sh which prints host, kernel, \
memory, disk, UFS mount status, active A/B slot and IP addresses on every \
interactive login. Slot info is read from /run/sdradxa/ (written at boot by \
ab-mark-good), so no ESP partition mount is needed at login time."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://sdradxa-motd.sh"

S = "${WORKDIR}"

inherit allarch

do_install() {
    install -d ${D}${sysconfdir}/profile.d
    install -m 0755 ${S}/sdradxa-motd.sh \
        ${D}${sysconfdir}/profile.d/sdradxa-motd.sh
}

FILES:${PN} = "${sysconfdir}/profile.d/sdradxa-motd.sh"

# ab-mark-good.service (from sdradxa-ab-tools) must be present to populate
# /run/sdradxa/slot and /run/sdradxa/tries at boot
RDEPENDS:${PN} = "sdradxa-ab-tools"
