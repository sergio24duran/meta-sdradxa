SUMMARY = "Persistent product data store for BPK (/opt/bpk)"
DESCRIPTION = "Binds /opt/bpk to the UFS persistent store so the signed \
mission, the mission public key, the AI model, the camera calibration and the \
flight logs survive both a reboot and a full SD card reflash. The rootfs \
writable layer is a tmpfs overlay, so without this package everything written \
under /opt/bpk lives in RAM only. Also ships bpk-storage-provision, the \
explicit one-shot tool that creates the UFS filesystem on a fresh board."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://bpk-storage-init \
    file://bpk-storage-provision \
    file://bpk-storage-init.service \
    file://opt-bpk.mount \
    file://README \
"

S = "${WORKDIR}"

inherit allarch systemd

SYSTEMD_SERVICE:${PN} = "bpk-storage-init.service opt-bpk.mount"
SYSTEMD_AUTO_ENABLE = "enable"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/bpk-storage-init ${D}${bindir}/bpk-storage-init
    install -m 0755 ${S}/bpk-storage-provision ${D}${bindir}/bpk-storage-provision

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/bpk-storage-init.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${S}/opt-bpk.mount ${D}${systemd_system_unitdir}/

    # Not ${docdir}: ${PN}-doc precedes ${PN} in PACKAGES and would split the
    # README out of the image package.
    install -d ${D}${datadir}/bpk-storage
    install -m 0644 ${S}/README ${D}${datadir}/bpk-storage/README

    # Mount point only. The rootfs is read-only, so an empty /opt/bpk is what
    # product mode sees when the UFS store is missing -- bpk-mission then
    # Fatals on the absent mission and camera.json instead of arming (DEC-011).
    install -d ${D}/opt/bpk
}

FILES:${PN} = " \
    ${bindir}/bpk-storage-init \
    ${bindir}/bpk-storage-provision \
    ${systemd_system_unitdir}/bpk-storage-init.service \
    ${systemd_system_unitdir}/opt-bpk.mount \
    ${datadir}/bpk-storage \
    /opt/bpk \
"

# mountpoint/lsblk/findmnt guard the scripts; mke2fs creates the store.
RDEPENDS:${PN} = " \
    util-linux-mountpoint \
    util-linux-lsblk \
    util-linux-findmnt \
    e2fsprogs-mke2fs \
"
