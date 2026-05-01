SUMMARY = "Minimal package group for sdradxa images"
DESCRIPTION = "Core packages present in every sdradxa image: \
terminal shell, GPIO userspace tools and base utilities."

LICENSE = "MIT"

inherit packagegroup

# libgpiod is dynamically renamed (libgpiod → libgpiod3), which is not
# allowed in allarch packagegroups. Force machine-specific arch.
PACKAGE_ARCH = "${MACHINE_ARCH}"

RDEPENDS:${PN} = " \
    bash \
    util-linux \
    libgpiod \
    libgpiod-tools \
"
