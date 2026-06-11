SUMMARY = "BPK boot device tree: firmware DT snapshot + camera/UART overlays"
DESCRIPTION = "Builds sdradxa-dragon-q6a-bpk.dtb: the SPI NOR UEFI firmware \
device tree snapshot with __symbols__/phandles injected, plus the Radxa \
overlays BPK needs (CAM1 IMX214 camera, UART6 with flow control for the \
flight controller). The EFI stub loads it via dtb= from the ESP; if the \
file is missing the stub falls back to the firmware DT."
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9"

SRC_URI = " \
    file://firmware-fdt-dragon-q6a.dts \
    file://inject_symbols.py \
    file://qcs6490-radxa-dragon-q6a-cam1-radxa-camera-13m-214-bpk.dtso \
    file://qcs6490-radxa-dragon-q6a-uart6-flowctl.dtso \
"

COMPATIBLE_MACHINE = "^(sdradxa-dragon-q6a)$"

DEPENDS = "dtc-native"
# .dtso files include dt-bindings headers from the kernel source
do_compile[depends] += "virtual/kernel:do_shared_workdir"

inherit deploy nopackages
INHIBIT_DEFAULT_DEPS = "1"

S = "${WORKDIR}"
B = "${WORKDIR}/build"

BPK_DTB = "sdradxa-dragon-q6a-bpk.dtb"

do_configure[noexec] = "1"

do_compile() {
    install -d ${B}

    # Base: firmware snapshot + injected __symbols__ and phandles so the
    # label-targeted overlays can resolve.
    python3 ${S}/inject_symbols.py \
        ${S}/firmware-fdt-dragon-q6a.dts ${B}/base-prepared.dts
    dtc -I dts -O dtb -o ${B}/base.dtb ${B}/base-prepared.dts

    # Overlays: preprocess (dt-bindings includes) then compile with -@.
    for ovl in qcs6490-radxa-dragon-q6a-cam1-radxa-camera-13m-214-bpk \
               qcs6490-radxa-dragon-q6a-uart6-flowctl; do
        ${BUILD_CPP} -nostdinc -undef -x assembler-with-cpp \
            -I ${STAGING_KERNEL_DIR}/include \
            ${S}/${ovl}.dtso -o ${B}/${ovl}.pp.dts
        dtc -@ -I dts -O dtb -o ${B}/${ovl}.dtbo ${B}/${ovl}.pp.dts
    done

    fdtoverlay -i ${B}/base.dtb -o ${B}/${BPK_DTB} \
        ${B}/qcs6490-radxa-dragon-q6a-cam1-radxa-camera-13m-214-bpk.dtbo \
        ${B}/qcs6490-radxa-dragon-q6a-uart6-flowctl.dtbo

    # The merge silently no-ops if a node went missing; assert the result.
    fdtget ${B}/${BPK_DTB} /soc@0/isp@acb3000 status | grep -qx okay
    fdtget ${B}/${BPK_DTB} /soc@0/cci@ac4a000/i2c-bus@0/camera@10 compatible \
        | grep -qx "sony,imx214"
    fdtget ${B}/${BPK_DTB} /soc@0/geniqup@9c0000/serial@998000 status \
        | grep -qx okay
}

do_deploy() {
    install -d ${DEPLOYDIR}
    install -m 0644 ${B}/${BPK_DTB} ${DEPLOYDIR}/${BPK_DTB}
}
addtask deploy after do_compile
