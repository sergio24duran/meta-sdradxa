# linux-linaro-qcomlt does not carry a QCM6490 DTB.
# The board receives its DTB from UEFI firmware at boot time.
#
# Set KERNEL_DEVICETREE to an explicit empty string so that
# linux-qcom-bootimg.bbclass does not crash: d.getVar() returns None
# when the variable is unset, and None.split() raises AttributeError.
# An empty string makes .split() return [] and the loop is skipped cleanly.
# Compiled-in fallback cmdline so EFI-stub auto-boot works without GRUB/startup.nsh.
# The bootloader cmdline (e.g. from a UEFI boot entry) still takes priority.

# qcom-common.inc does KERNEL_IMAGETYPES:append = " Image" (for UKI generation).
# When KERNEL_IMAGETYPE is already "Image" (required for UEFI direct boot),
# this creates "Image Image" → duplicate kernel-image-image package.
# kernel.bbclass anonymous python runs before ours and already appended the
# duplicate package name to PACKAGES, so we must fix both variables.
python () {
    types = (d.getVar('KERNEL_IMAGETYPES') or '').split()
    deduped = list(dict.fromkeys(types))
    if len(deduped) != len(types):
        d.setVar('KERNEL_IMAGETYPES', ' '.join(deduped))
        pkgs = (d.getVar('PACKAGES') or '').split()
        d.setVar('PACKAGES', ' '.join(dict.fromkeys(pkgs)))
}
FILESEXTRAPATHS:prepend:sdradxa-dragon-q6a := "${THISDIR}/linux-linaro-qcomlt:"
SRC_URI:append:sdradxa-dragon-q6a = " file://sdradxa-dragon-q6a.cfg"
# linux-linaro-qcom.inc applies fragments via KERNEL_CONFIG_FRAGMENTS (not kernel-yocto auto-merge)
KERNEL_CONFIG_FRAGMENTS:append:sdradxa-dragon-q6a = " ${WORKDIR}/sdradxa-dragon-q6a.cfg"

KERNEL_DEVICETREE:sdradxa-dragon-q6a = ""

# qcom-common.inc sets KERNEL_DTBDEST="dtb", which causes do_install to
# create /dtb even with no DTBs, triggering an installed-vs-shipped QA error.
do_install:append:sdradxa-dragon-q6a() {
    rm -rf "${D}/dtb" 2>/dev/null || true
}
