# SoC include for Qualcomm QCM6490 - reusable for all Radxa boards with this SoC

SOC_FAMILY = "qcm6490"

require conf/machine/include/qcom-common.inc

DEFAULTTUNE = "armv8-2a-crypto"
require conf/machine/include/arm/arch-armv8-2a.inc

SERIAL_CONSOLES ?= "115200;ttyMSM0"

# Standard Qualcomm userspace services: peripheral manager, IPC router,
# remote filesystem and TFTP server
MACHINE_ESSENTIAL_EXTRA_RRECOMMENDS += " \
    pd-mapper \
    qrtr \
    rmtfs \
    tqftpserv \
"

# FastRPC support for Hexagon DSP offload
MACHINE_EXTRA_RRECOMMENDS += " \
    fastrpc \
"
