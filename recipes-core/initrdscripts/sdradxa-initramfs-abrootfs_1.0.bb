SUMMARY = "A/B root filesystem selection module for initramfs-framework"
DESCRIPTION = "Reads slot.conf from the ESP partition to select rootfs_a or \
rootfs_b, with automatic rollback after exhausting boot attempts."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://abrootfs"

inherit allarch

do_install() {
    install -d ${D}/init.d
    install -m 0755 ${WORKDIR}/abrootfs ${D}/init.d/85-abrootfs
}

FILES:${PN} = "/init.d/85-abrootfs"
RDEPENDS:${PN} = "initramfs-framework-base"
