SUMMARY = "A/B boot initramfs for sdradxa boards"
DESCRIPTION = "Minimal initramfs that selects rootfs_a or rootfs_b based on \
slot.conf stored in the ESP. Used with INITRAMFS_IMAGE_BUNDLE to produce a \
single kernel+initramfs EFI binary."

PACKAGE_INSTALL = " \
    initramfs-framework-base \
    initramfs-module-e2fs \
    initramfs-module-rootfs \
    sdradxa-initramfs-abrootfs \
    busybox \
    base-passwd \
"

IMAGE_FEATURES = ""
PACKAGE_EXCLUDE = "kernel-image-*"
IMAGE_NAME_SUFFIX ?= ""
IMAGE_LINGUAS = ""
LICENSE = "MIT"
IMAGE_FSTYPES = "${INITRAMFS_FSTYPES}"

inherit core-image

IMAGE_ROOTFS_SIZE = "8192"
IMAGE_ROOTFS_EXTRA_SPACE = "0"

COMPATIBLE_MACHINE = "^(sdradxa-dragon-q6a)$"
