SUMMARY = "Multimedia packages for sdradxa boards"
DESCRIPTION = "GStreamer pipeline framework and V4L2 utilities for \
hardware video codec acceleration (Qualcomm Iris encoder/decoder)."

LICENSE = "MIT"

inherit packagegroup

RDEPENDS:${PN} = " \
    gstreamer1.0 \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-bad \
    v4l-utils \
"
