SUMMARY = "A/B boot management tools for sdradxa boards"
DESCRIPTION = "Runtime tools for A/B root filesystem updates: ab-status shows \
the active slot, ab-update writes a new image to the inactive slot, and \
ab-mark-good resets the boot attempt counter after a successful boot."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://ab-status \
    file://ab-update \
    file://ab-mark-good \
    file://ab-mark-good.service \
    file://mnt-sdcard.mount \
    file://mnt-ufscard.mount \
    file://slot.conf \
"

S = "${WORKDIR}"

inherit allarch systemd deploy

SYSTEMD_SERVICE:${PN} = "ab-mark-good.service mnt-sdcard.mount mnt-ufscard.mount"
SYSTEMD_AUTO_ENABLE = "enable"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/ab-status ${D}${bindir}/ab-status
    install -m 0755 ${S}/ab-update ${D}${bindir}/ab-update
    install -m 0755 ${S}/ab-mark-good ${D}${bindir}/ab-mark-good

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/ab-mark-good.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${S}/mnt-sdcard.mount ${D}${systemd_system_unitdir}/
    install -m 0644 ${S}/mnt-ufscard.mount ${D}${systemd_system_unitdir}/

    install -d ${D}/mnt/sdcard
    install -d ${D}/mnt/ufscard
}

do_deploy() {
    install -m 0644 ${S}/slot.conf ${DEPLOYDIR}/slot.conf
}
addtask deploy before do_build after do_install

FILES:${PN} = " \
    ${bindir}/ab-status \
    ${bindir}/ab-update \
    ${bindir}/ab-mark-good \
    ${systemd_system_unitdir}/ab-mark-good.service \
    ${systemd_system_unitdir}/mnt-sdcard.mount \
    ${systemd_system_unitdir}/mnt-ufscard.mount \
    /mnt/sdcard \
    /mnt/ufscard \
"

RDEPENDS:${PN} = "util-linux-mount"
