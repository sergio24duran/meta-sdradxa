SUMMARY = "Minimal image for Radxa Dragon boards with Qualcomm SoCs"
DESCRIPTION = "Minimal bootable image: terminal shell, UART console, \
40-pin GPIO (libgpiod) and SSH (dropbear). Base for all sdradxa images."

LICENSE = "MIT"

# To add support for new machines, extend the COMPATIBLE_MACHINE regex
COMPATIBLE_MACHINE = "^(sdradxa-dragon-q6a)$"

# Disable ext4 orphan_file feature: Linux 6.6 does not support the checksum
# variant written by e2fsprogs 1.47.0's resize2fs, causing mount failures.
EXTRA_IMAGECMD:ext4:append = " -O ^orphan_file"

# wic SD card image (A/B rootfs layout)
# UEFI loads EFI/BOOT/BOOTAA64.EFI which is the kernel+initramfs bundle.
# The initramfs reads slot.conf from the ESP to select rootfs_a or rootfs_b.
# bootimg-partition only copies files listed in IMAGE_BOOT_FILES; it does
# not install any bootloader binaries, which avoids name collisions on
# case-insensitive FAT32 (e.g. BOOTAA64.EFI vs bootaa64.efi from bootimg-efi).
IMAGE_FSTYPES += "wic wic.gz wic.bmap"
SDCARD_SIZE ?= "64g"
WKS_FILE = "sdradxa-dragon-q6a-${SDCARD_SIZE}.wks"
IMAGE_BOOT_FILES = "Image-initramfs-${MACHINE}.bin;EFI/BOOT/BOOTAA64.EFI slot.conf"
do_image_wic[depends] += "virtual/kernel:do_deploy sdradxa-ab-tools:do_deploy"

inherit core-image extrausers

# ssh-server-dropbear: installs and enables dropbear sshd
IMAGE_FEATURES += "ssh-server-dropbear"

# libgpiod is listed here instead of in the packagegroup because it is
# dynamically renamed to libgpiod3 at packaging time, which causes an
# allarch packagegroup QA error. Image recipes are not allarch.
IMAGE_INSTALL:append = " \
    packagegroup-sdradxa-minimal \
    packagegroup-sdradxa-connectivity \
    sdradxa-ab-tools \
    sdradxa-ssh-dev-keys \
    libgpiod \
    libgpiod-tools \
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
