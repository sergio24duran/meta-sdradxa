SUMMARY = "Minimal package group for sdradxa images"
DESCRIPTION = "Core packages present in every sdradxa image: \
terminal shell, GPIO userspace tools and base utilities."

LICENSE = "MIT"

inherit packagegroup

RDEPENDS:${PN} = " \
    bash \
    util-linux \
"
