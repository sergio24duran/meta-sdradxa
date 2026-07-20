SUMMARY = "MAVLink C headers (ardupilotmega dialect), MAVSDK dependency"
DESCRIPTION = "Generated MAVLink v2 C headers plus the MAVLink CMake package \
consumed by MAVSDK's SUPERBUILD=OFF build (find_package(MAVLink)). Headers are \
generated offline at build time by the bundled pymavlink (mavgen) using the \
native Python stdlib only -- the upstream pip-install step is patched out."
HOMEPAGE = "https://github.com/mavlink/mavlink"
LICENSE = "LGPL-3.0-only & MIT"
LIC_FILES_CHKSUM = "file://COPYING;md5=54ad3cbe91bebcf6b1823970ff1fb97f"

# Pinned to the commit MAVSDK v3.17.1 uses; gitsm pulls the pymavlink submodule.
SRCREV = "d6a7eeaf43319ce6da19a1973ca40180a4210643"
SRC_URI = "gitsm://github.com/mavlink/mavlink;protocol=https;nobranch=1 \
           file://0001-offline-codegen-no-pip.patch"

S = "${WORKDIR}/git"

inherit cmake python3native

# mavgen runs on the build host: force CMake to the native interpreter.
EXTRA_OECMAKE = "-DMAVLINK_DIALECT=ardupilotmega -DPython_EXECUTABLE=${PYTHON}"

# MAVSDK embeds the raw XML dialect definitions at build time (they are not
# installed by upstream's CMake), so ship them alongside the generated headers.
do_install:append() {
    install -d ${D}${includedir}/mavlink/message_definitions/v1.0
    for x in minimal standard common ardupilotmega; do
        install -m 0644 ${S}/message_definitions/v1.0/$x.xml \
            ${D}${includedir}/mavlink/message_definitions/v1.0/
    done
}

# Header-only INTERFACE library: only generated headers + the CMake package.
ALLOW_EMPTY:${PN} = "1"
FILES:${PN}-dev += "${includedir}/mavlink"
