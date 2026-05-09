SECTION = "kernel"

DESCRIPTION = "Qualcomm mainline-tracking kernel for QCOM devices"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

inherit kernel cml1

COMPATIBLE_MACHINE = "(qcom)"

LINUX_VERSION ?= "7.0"

PV = "${LINUX_VERSION}+git"

# tag: qcom-next-7.0-20260507
SRCREV ?= "6e159a33b007b46e30fe0f4e4b5167e2f4462ac3"

SRC_URI = "git://github.com/qualcomm-linux/kernel.git;nobranch=1;protocol=https"

S = "${WORKDIR}/git"

KBUILD_DEFCONFIG ?= "defconfig"

FILESEXTRAPATHS:prepend:sdradxa-dragon-q6a := "${THISDIR}/linux-qcom-next:"
SRC_URI:append:sdradxa-dragon-q6a = " file://configs/sdradxa-dragon-q6a.cfg"

do_configure:prepend() {
    cp ${S}/arch/${ARCH}/configs/${KBUILD_DEFCONFIG} ${B}/.config

    EXTRA_CONFIGS="${S}/arch/arm64/configs/qcom.config"
    if [ -f "${S}/arch/arm64/configs/prune.config" ]; then
        EXTRA_CONFIGS="${EXTRA_CONFIGS} ${S}/arch/arm64/configs/prune.config"
    fi

    ${S}/scripts/kconfig/merge_config.sh -m -O ${B} ${B}/.config \
        ${EXTRA_CONFIGS} \
        ${@" ".join(find_cfgs(d))}
}

# Kernel image is deployed to ESP via WIC — not needed in rootfs /boot
RDEPENDS:${KERNEL_PACKAGE_NAME}-base = ""

# qcom-common.inc appends "Image" to KERNEL_IMAGETYPES. When KERNEL_IMAGETYPE
# is already "Image" (required for UEFI direct boot), this creates a duplicate
# that breaks kernel-image packaging. Deduplicate both variables.
python () {
    types = (d.getVar('KERNEL_IMAGETYPES') or '').split()
    deduped = list(dict.fromkeys(types))
    if len(deduped) != len(types):
        d.setVar('KERNEL_IMAGETYPES', ' '.join(deduped))
        pkgs = (d.getVar('PACKAGES') or '').split()
        d.setVar('PACKAGES', ' '.join(dict.fromkeys(pkgs)))
}
