SUMMARY = "A/B boot management tools for sdradxa boards"
DESCRIPTION = "Runtime tools for A/B root filesystem updates and boot-counting \
kernel updates: ab-status shows the active slot, ab-update writes a new image \
to the inactive slot, ab-mark-good resets the boot attempt counter after a \
successful boot, and ab-kernel stages/promotes kernel+DTB candidates behind \
systemd-boot's automatic boot assessment."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://ab-status \
    file://ab-update \
    file://ab-mark-good \
    file://ab-kernel \
    file://ab-mark-good.service \
    file://mnt-sdcard.mount \
    file://mnt-ufscard.mount \
    file://efi.mount \
    file://efi.automount \
    file://slot.conf \
    file://loader.conf \
    file://bpk-good.conf \
"

S = "${WORKDIR}"

inherit allarch systemd deploy

# efi.mount is activated on demand through efi.automount, so only the
# automount unit is enabled. systemd-bless-boot.service needs no enabling:
# systemd's bless-boot-generator pulls it in whenever the boot loader
# reports a counted entry (LoaderBootCountPath EFI variable).
SYSTEMD_SERVICE:${PN} = "ab-mark-good.service mnt-sdcard.mount mnt-ufscard.mount efi.automount"
SYSTEMD_AUTO_ENABLE = "enable"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/ab-status ${D}${bindir}/ab-status
    install -m 0755 ${S}/ab-update ${D}${bindir}/ab-update
    install -m 0755 ${S}/ab-mark-good ${D}${bindir}/ab-mark-good
    install -m 0755 ${S}/ab-kernel ${D}${bindir}/ab-kernel

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/ab-mark-good.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${S}/mnt-sdcard.mount ${D}${systemd_system_unitdir}/
    install -m 0644 ${S}/mnt-ufscard.mount ${D}${systemd_system_unitdir}/
    install -m 0644 ${S}/efi.mount ${D}${systemd_system_unitdir}/
    install -m 0644 ${S}/efi.automount ${D}${systemd_system_unitdir}/

    install -d ${D}/mnt/sdcard
    install -d ${D}/mnt/ufscard
    install -d ${D}/efi
}

# slot.conf, loader.conf and bpk-good.conf are picked up from the deploy dir
# by IMAGE_BOOT_FILES (wic bootimg-partition) in the image recipes.
do_deploy() {
    install -m 0644 ${S}/slot.conf ${DEPLOYDIR}/slot.conf
    install -m 0644 ${S}/loader.conf ${DEPLOYDIR}/loader.conf
    install -m 0644 ${S}/bpk-good.conf ${DEPLOYDIR}/bpk-good.conf
}
addtask deploy before do_build after do_install

FILES:${PN} = " \
    ${bindir}/ab-status \
    ${bindir}/ab-update \
    ${bindir}/ab-mark-good \
    ${bindir}/ab-kernel \
    ${systemd_system_unitdir}/ab-mark-good.service \
    ${systemd_system_unitdir}/mnt-sdcard.mount \
    ${systemd_system_unitdir}/mnt-ufscard.mount \
    ${systemd_system_unitdir}/efi.mount \
    ${systemd_system_unitdir}/efi.automount \
    /mnt/sdcard \
    /mnt/ufscard \
    /efi \
"

RDEPENDS:${PN} = "util-linux-mount"
