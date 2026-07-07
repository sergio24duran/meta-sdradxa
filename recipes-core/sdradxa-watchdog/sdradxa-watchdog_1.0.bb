SUMMARY = "Hardware watchdog policy: systemd pets the Gunyah/qcom WDT"
DESCRIPTION = "Arms the QCM6490 hardware watchdog ('Gunyah Watchdog', \
/dev/watchdog0) via systemd RuntimeWatchdogSec so a hung kernel resets the \
SoC without a manual power cycle, and sets kernel.panic so panics reboot on \
their own even before systemd has armed the watchdog. Together with the \
systemd-boot boot-counting fallback (DEC-024) this makes recovery from a bad \
kernel fully hands-off: hang -> WDT reset -> boot attempt burned -> \
auto-fallback to the last good kernel after 3 attempts. \
Drill evidence 2026-07-07 (yocto-bpk#60): sysrq-c panic with kernel.panic=0 \
and RuntimeWatchdogSec=10 -> bite -> full SoC reset -> multi-user in <30 s, \
reproduced twice, observed over the read-only debug UART."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://10-watchdog.conf \
    file://10-kernel-panic.conf \
"

S = "${WORKDIR}"

inherit allarch

do_install() {
    install -d ${D}${systemd_unitdir}/system.conf.d
    install -m 0644 ${S}/10-watchdog.conf \
        ${D}${systemd_unitdir}/system.conf.d/10-watchdog.conf

    install -d ${D}${libdir}/sysctl.d
    install -m 0644 ${S}/10-kernel-panic.conf \
        ${D}${libdir}/sysctl.d/10-kernel-panic.conf
}

FILES:${PN} = " \
    ${systemd_unitdir}/system.conf.d/10-watchdog.conf \
    ${libdir}/sysctl.d/10-kernel-panic.conf \
"

RDEPENDS:${PN} = "systemd"
