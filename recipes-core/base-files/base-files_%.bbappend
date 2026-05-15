# Override the Poky reference-distro warning in /etc/motd with our own banner.
# Our files/ directory has higher layer priority (6 vs 5) so our motd wins.
# The do_install:append injects the build timestamp from bitbake at image creation
# time so the banner always shows when the rootfs was built.

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

do_install:append() {
    # Format DATETIME (YYYYMMDDHHMMSS) → "YYYY-MM-DD HH:MM UTC"
    BUILD_DATE=$(echo "${DATETIME}" | \
        sed 's/\(....\)\(..\)\(..\)\(..\)\(..\).*/\1-\2-\3 \4:\5 UTC/')
    printf "  Built  : %s\n\n" "$BUILD_DATE" >> ${D}${sysconfdir}/motd
}
