SUMMARY = "QUP v3 serial engine firmware for QCM6490"
DESCRIPTION = "Qualcomm QUPv3 firmware required for I2C and SPI bus operation \
on QCM6490/QCS6490 SoCs. Without this firmware all GENI-based I2C and SPI \
controllers fail to probe. Backported from linux-firmware commit 1d986802 \
(2025-05-19), which is newer than Scarthgap's linux-firmware 20240909."

LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://LICENSE.qcom;md5=164e3362a538eb11d3ac51e8e134294b"

LINUX_FW_COMMIT = "1d986802"
LINUX_FW_BASE = "https://gitlab.com/kernel-firmware/linux-firmware/-/raw/${LINUX_FW_COMMIT}"

SRC_URI = " \
    ${LINUX_FW_BASE}/qcom/qcm6490/qupv3fw.elf;name=fw \
    ${LINUX_FW_BASE}/LICENSE.qcom;name=license \
"
SRC_URI[fw.sha256sum] = "9fc1f136858ee878c38b05aff63621b6bf5fbb5dd91a9ab98bbf46057a9c963d"
SRC_URI[license.sha256sum] = "be904cd28cb292b80cdb6cf412ab0d9159d431671e987ad433c1f62e0988a9bc"

S = "${WORKDIR}"

INHIBIT_DEFAULT_DEPS = "1"
INSANE_SKIP:${PN} += "arch"

do_install() {
    install -d ${D}${nonarch_base_libdir}/firmware/qcom/qcm6490
    install -m 0644 ${S}/qupv3fw.elf \
        ${D}${nonarch_base_libdir}/firmware/qcom/qcm6490/qupv3fw.elf
}

FILES:${PN} = "${nonarch_base_libdir}/firmware/qcom/qcm6490/qupv3fw.elf"
