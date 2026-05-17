SUMMARY = "tmpfs overlay module for initramfs-framework"
DESCRIPTION = "Mounts a tmpfs-backed overlayfs on top of the read-only \
rootfs so that runtime writes go to RAM and the base image is never \
modified. Disable at boot with the 'nooverlay' kernel parameter."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://overlayroot"

inherit allarch

# Numbered 92 so it runs after 90-rootfs (which mounts the real root)
# but before 99-finish (which does switch_root). In practice 99-finish
# is never reached because this module does exec chroot itself.
do_install() {
    install -d ${D}/init.d
    install -m 0755 ${WORKDIR}/overlayroot ${D}/init.d/92-overlayroot
}

FILES:${PN} = "/init.d/92-overlayroot"

RDEPENDS:${PN} = "initramfs-framework-base"
