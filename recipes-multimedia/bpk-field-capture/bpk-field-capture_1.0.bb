SUMMARY = "BPK field data-capture tool (button-triggered libcamera stills)"
DESCRIPTION = "On-board dev tool for collecting real IMX214 training frames: a \
systemd service waits for a GPIO button press, grabs one libcamera ISP frame \
(the same path the drone uses) and saves it as ABGR8888 to the UFS card, \
blinking a status LED per shot. Frames are pulled to a PC and converted with \
models_bpk/data_collection/abgr_to_png.py. Not part of the production image; \
add it to a dev image or IMAGE_INSTALL:append when collecting a dataset."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://bpk-field-capture \
    file://bpk-field-capture.service \
"

S = "${WORKDIR}"

inherit allarch systemd

SYSTEMD_SERVICE:${PN} = "bpk-field-capture.service"
SYSTEMD_AUTO_ENABLE = "enable"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/bpk-field-capture ${D}${bindir}/bpk-field-capture

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/bpk-field-capture.service ${D}${systemd_system_unitdir}/
}

FILES:${PN} = " \
    ${bindir}/bpk-field-capture \
    ${systemd_system_unitdir}/bpk-field-capture.service \
"

# gpiomon/gpioset (libgpiod), the `cam` tool (libcamera), and the ufscard mount
# unit (sdradxa-ab-tools) are needed at runtime.
RDEPENDS:${PN} = "libgpiod-tools libcamera sdradxa-ab-tools"

COMPATIBLE_MACHINE = "^(sdradxa-dragon-q6a)$"
