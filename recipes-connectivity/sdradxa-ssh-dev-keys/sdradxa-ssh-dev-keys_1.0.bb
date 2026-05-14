SUMMARY = "Static SSH host key and developer authorized_keys for sdradxa boards"
DESCRIPTION = "Ships a fixed Dropbear RSA host key so the board keeps \
the same SSH fingerprint across reflashes, and deploys a developer \
public key for passwordless login. Development use only."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://dropbear_rsa_host_key \
    file://authorized_keys \
"

S = "${WORKDIR}"

inherit allarch

do_install() {
    install -d -m 0700 ${D}${sysconfdir}/dropbear
    install -m 0600 ${S}/dropbear_rsa_host_key ${D}${sysconfdir}/dropbear/

    install -d -m 0700 ${D}/home/root/.ssh
    install -m 0600 ${S}/authorized_keys ${D}/home/root/.ssh/authorized_keys
}

FILES:${PN} = " \
    ${sysconfdir}/dropbear/dropbear_rsa_host_key \
    /home/root/.ssh/authorized_keys \
"

RDEPENDS:${PN} = "dropbear"
CONFFILES:${PN} = "${sysconfdir}/dropbear/dropbear_rsa_host_key"
