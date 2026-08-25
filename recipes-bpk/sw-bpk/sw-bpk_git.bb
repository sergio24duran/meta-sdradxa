SUMMARY = "BPK flight software: mission FSM, perception, MAVLink, telemetry, HMI"
DESCRIPTION = "The five product-mode services that make the drone autonomous, \
plus the on-board dev tools. Until this recipe existed the binaries were \
hand-copied to the UFS every session, so the systemd units, the sd_notify \
watchdog supervision and the gold-core pinning were never exercised on the \
board (yocto-bpk#74, sw_bpk#59)."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=${SW_BPK_LIC_MD5}"

SW_BPK_LIC_MD5 = "c068bc574523d3d523002211fa03811d"

# Private repo: the builder needs an SSH key with read access to
# sergio24duran/sw_bpk (the same key used to push it).
SRC_URI = " \
    git://git@github.com/sergio24duran/sw_bpk.git;protocol=ssh;branch=develop;name=sw \
    https://github.com/nanomsg/nng/archive/refs/tags/v1.10.2.tar.gz;name=nng;subdir=fetchcontent \
    https://github.com/nlohmann/json/releases/download/v3.11.3/json.tar.xz;name=json;subdir=fetchcontent \
"
SRCREV_sw = "057a2703ac32dbf628be9bb9e362e14ffffa1e3d"
SRCREV_FORMAT = "sw"

# Same pins the CMake FetchContent declarations carry -- if they drift, the
# build stops rather than quietly compiling a different nng.
SRC_URI[nng.sha256sum] = "c8947b2398f9c5fc6234805c96f4fd0c51442b14fadd14df25e6bb129848736f"
SRC_URI[json.sha256sum] = "d6c65aca6b1ed68e7a182f4757257b107ae403032760ed6ef121c9d55e81757d"

S = "${WORKDIR}/git"

DEPENDS = "qnn-sdk mavsdk libcamera"
RDEPENDS:${PN} = "libgpiod-tools"

inherit cmake systemd pkgconfig

# do_compile has no network, so FetchContent must never reach out: bitbake
# fetches the pinned tarballs above and these point CMake at the extracted
# trees. Without this the configure step dies trying to download nng.
FETCHCONTENT_DIR = "${WORKDIR}/fetchcontent"

EXTRA_OECMAKE = " \
    -DBPK_BUILD_TESTS=OFF \
    -DQNN_SDK_ROOT=${STAGING_DIR_TARGET}${prefix} \
    -DBPK_LIBCAMERA_ROOT=${STAGING_DIR_TARGET} \
    -DFETCHCONTENT_SOURCE_DIR_NNG=${FETCHCONTENT_DIR}/nng-1.10.2 \
    -DFETCHCONTENT_SOURCE_DIR_NLOHMANN_JSON=${FETCHCONTENT_DIR}/json \
    -DFETCHCONTENT_FULLY_DISCONNECTED=ON \
"

SERVICE_BINS = "bpk-mission bpk-mavlink bpk-perception bpk-telemetry bpk-hmi"
# Dev tools that earn their place on the board: the benchmark of record
# (LES-001), the bus spy and the MAVLink monitor used during bench sessions.
TOOL_BINS = "bpk-model-bench bpk-ipc-spy bpk-ipc-pub bpk-mav-monitor bpk-qnn-smoke"

# bpk-mission-sign is deliberately NOT shipped: the signing key never leaves
# the host, so a signer on the drone would be dead weight (DEC-020).

SYSTEMD_SERVICE:${PN} = " \
    bpk-mission.service \
    bpk-mavlink.service \
    bpk-perception.service \
    bpk-telemetry.service \
    bpk-hmi.service \
"
# Nothing auto-starts yet: the services are installed and inspectable, but
# arming still needs the physical START button (DEC-017) and product mode fails
# closed without a signed mission. Enabling them is a separate, deliberate step
# once the bench has run them by hand.
SYSTEMD_AUTO_ENABLE = "disable"

do_install() {
    install -d ${D}${bindir}
    for b in ${SERVICE_BINS}; do
        src=$(find ${B}/services -type f -name "$b" -perm -111 | head -n 1)
        if [ -z "$src" ]; then
            bbfatal "$b was not built -- a dependency was silently skipped " \
                    "(QNN, MAVSDK and libcamera drop out with a WARNING)"
        fi
        install -m 0755 "$src" ${D}${bindir}/$b
    done
    for b in ${TOOL_BINS}; do
        src=$(find ${B}/tools -type f -name "$b" -perm -111 | head -n 1)
        if [ -z "$src" ]; then
            bbfatal "$b was not built -- see the note above"
        fi
        install -m 0755 "$src" ${D}${bindir}/$b
    done

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/deploy/systemd/*.service ${D}${systemd_system_unitdir}/

    install -d ${D}${nonarch_libdir}/tmpfiles.d
    install -m 0644 ${S}/deploy/tmpfiles.d/bpk.conf \
        ${D}${nonarch_libdir}/tmpfiles.d/bpk.conf
}

FILES:${PN} += "${nonarch_libdir}/tmpfiles.d/bpk.conf"
