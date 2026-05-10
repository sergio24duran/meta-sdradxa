SUMMARY = "Firmware path symlinks for Radxa Dragon Q6A"
DESCRIPTION = "The UEFI-provided DTB references board-specific firmware \
paths under qcom/qcs6490/radxa/dragon-q6a/ but the SoC firmware blobs \
from firmware-qcom-rb3gen2 are installed at qcom/qcs6490/. This recipe \
creates symlinks so the kernel can find the firmware at the expected paths."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit allarch

RDEPENDS:${PN} = " \
    linux-firmware-qcom-qcs6490-audio \
    linux-firmware-qcom-qcs6490-compute \
"

do_install() {
    install -d ${D}${nonarch_base_libdir}/firmware/qcom/qcs6490/radxa/dragon-q6a

    # ADSP firmware (audio DSP)
    ln -sf ../../adsp.mbn ${D}${nonarch_base_libdir}/firmware/qcom/qcs6490/radxa/dragon-q6a/adsp.mbn
    ln -sf ../../adspr.jsn ${D}${nonarch_base_libdir}/firmware/qcom/qcs6490/radxa/dragon-q6a/adspr.jsn
    ln -sf ../../adsps.jsn ${D}${nonarch_base_libdir}/firmware/qcom/qcs6490/radxa/dragon-q6a/adsps.jsn
    ln -sf ../../adspua.jsn ${D}${nonarch_base_libdir}/firmware/qcom/qcs6490/radxa/dragon-q6a/adspua.jsn

    # CDSP firmware (compute DSP / NPU)
    ln -sf ../../cdsp.mbn ${D}${nonarch_base_libdir}/firmware/qcom/qcs6490/radxa/dragon-q6a/cdsp.mbn
    ln -sf ../../cdspr.jsn ${D}${nonarch_base_libdir}/firmware/qcom/qcs6490/radxa/dragon-q6a/cdspr.jsn
}

FILES:${PN} = "${nonarch_base_libdir}/firmware/qcom/qcs6490/radxa/dragon-q6a"
