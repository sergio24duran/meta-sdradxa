SUMMARY = "Firmware packages for the Radxa Dragon Q6A (QCM6490)"
DESCRIPTION = "Board-specific firmware: QUP v3 serial engine, GPU, audio DSP, \
compute DSP, video codec, HDMI bridge. WiFi and Bluetooth firmware are guarded \
by DISTRO_FEATURES since those subsystems are not yet enabled."

LICENSE = "MIT"

inherit packagegroup

# SoC firmware from firmware-qcom-rb3gen2 (same QCM6490 silicon).
# The subpackages use generic qcs6490-* names and contain SoC-level
# blobs (ADSP, CDSP, Adreno zap shader) that are not board-specific.
RRECOMMENDS:${PN} = " \
    sdradxa-firmware-qupv3 \
    linux-firmware-qcom-qcs6490-audio \
    linux-firmware-qcom-qcs6490-compute \
    linux-firmware-qcom-qcs6490-adreno \
    sdradxa-firmware-symlinks \
    ${@bb.utils.contains('DISTRO_FEATURES', 'opengl', 'linux-firmware-qcom-adreno-a660', '', d)} \
    linux-firmware-qcom-vpu-2.0 \
    linux-firmware-lt9611uxc \
    ${@bb.utils.contains('DISTRO_FEATURES', 'wifi', 'linux-firmware-ath11k linux-firmware-qcom-qcs6490-wifi', '', d)} \
    ${@bb.utils.contains('DISTRO_FEATURES', 'bluetooth', 'linux-firmware-qca', '', d)} \
"
