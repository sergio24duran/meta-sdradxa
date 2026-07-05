SUMMARY = "Minimal bring-up image for Radxa Dragon boards with Qualcomm SoCs"
DESCRIPTION = "Minimal bootable image: UART console, SSH (dropbear) and \
base utilities. Intended for board bring-up only; for a fully featured \
image use sdradxa-image-full."

LICENSE = "MIT"

# To add support for new machines, extend the COMPATIBLE_MACHINE regex
COMPATIBLE_MACHINE = "^(sdradxa-dragon-q6a)$"

# Disable ext4 orphan_file feature: Linux 6.6 does not support the checksum
# variant written by e2fsprogs 1.47.0's resize2fs, causing mount failures.
EXTRA_IMAGECMD:ext4:append = " -O ^orphan_file"

# wic SD card image (A/B rootfs layout, boot-counting kernel fallback)
# UEFI loads EFI/BOOT/BOOTAA64.EFI = systemd-boot, which picks a loader entry:
# bpk-good.conf (known-good kernel+DTB pair in bank EFI/bpk/0 or /1) or, when
# a candidate is staged by ab-kernel, bpk-new+N.conf (the other bank). sd-boot
# decrements the +N filename counter by FAT rename on every attempt (works
# without NVRAM Boot#### support); at +0 the entry is "bad" and bpk-good
# boots — a broken kernel/DTB can no longer brick the board (LES-010).
# systemd-bless-boot removes the counter once boot-complete.target is reached;
# ab-kernel promote then rewrites bpk-good.conf to the new bank (single atomic
# rename is the commit point).
# The initramfs reads slot.conf from the ESP to select rootfs_a or rootfs_b.
# bootimg-partition only copies files listed in IMAGE_BOOT_FILES; it does
# not install any bootloader binaries, which avoids name collisions on
# case-insensitive FAT32 (e.g. BOOTAA64.EFI vs bootaa64.efi from bootimg-efi).
IMAGE_FSTYPES += "wic wic.gz wic.bmap"
SDCARD_SIZE ?= "64g"
WKS_FILE = "sdradxa-dragon-q6a-${SDCARD_SIZE}.wks"
IMAGE_BOOT_FILES = " \
    systemd-bootaa64.efi;EFI/BOOT/BOOTAA64.EFI \
    loader.conf;loader/loader.conf \
    bpk-good.conf;loader/entries/bpk-good.conf \
    Image-initramfs-${MACHINE}.bin;EFI/bpk/0/Image.efi \
    sdradxa-dragon-q6a-bpk.dtb;EFI/bpk/0/bpk.dtb \
    slot.conf \
"
do_image_wic[depends] += "virtual/kernel:do_deploy sdradxa-ab-tools:do_deploy sdradxa-dtb-overlays:do_deploy systemd-boot:do_deploy"

inherit core-image extrausers

# Write image identity after rootfs assembly. ROOTFS_POSTPROCESS_COMMAND is the
# correct hook for shell code (do_rootfs is a Python task in image.bbclass).
# ${PN} = image recipe name (sdradxa-image-minimal or -full).
# Runs fresh on every image build so DATETIME is image build time, not the
# stale package-level timestamp frozen in sstate cache.
ROOTFS_POSTPROCESS_COMMAND += "write_image_release;"

write_image_release() {
    BUILD_DATE=$(echo "${DATETIME}" | \
        sed 's/\(....\)\(..\)\(..\)\(..\)\(..\).*/\1-\2-\3 \4:\5 UTC/')
    echo "IMAGE_ID=${PN}"               > ${IMAGE_ROOTFS}/etc/image-release
    echo "IMAGE_BUILDDATE=$BUILD_DATE" >> ${IMAGE_ROOTFS}/etc/image-release
}

# ssh-server-dropbear: installs and enables dropbear sshd
IMAGE_FEATURES += "ssh-server-dropbear"

IMAGE_INSTALL:append = " \
    packagegroup-sdradxa-minimal \
    sdradxa-ab-tools \
    sdradxa-ssh-dev-keys \
"

# Users and passwords
# Hashes generated with: openssl passwd -6 -salt 'sdradxasalt' '<password>'
# root / root
# sdradxa / sdradxa
EXTRA_USERS_PARAMS = " \
    usermod  -p '\$6\$sdradxasalt\$ic5ocTvowNeOlrdEohMZpZjtQNzJlGcI6ZD41fNXtlQ6w9cOrx0ahMoHz3LL7ivPrtH2OpSsPdrzrF4dIIGgS1' root; \
    useradd  -m -s /bin/bash sdradxa; \
    usermod  -p '\$6\$sdradxasalt\$vsYc.aUkryFNXAWHDOIMTgc.m73Ok81V/WIgDamq/kj3L0iaeNMNQuAA18tJb50REPmzac7xANHctuW3igkbp0' sdradxa; \
"
