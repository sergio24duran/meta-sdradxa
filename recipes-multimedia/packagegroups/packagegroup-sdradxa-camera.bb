SUMMARY = "Camera stack for BPK perception"
DESCRIPTION = "libcamera (simple pipeline + software ISP for IMX214 over \
CAMSS), GStreamer with the plugin sets the capture path needs, and V4L2 \
debugging tools (v4l2-ctl, media-ctl) for bench bringup."
LICENSE = "MIT"

inherit packagegroup

RDEPENDS:${PN} = " \
    libcamera \
    libcamera-gst \
    gstreamer1.0 \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-bad \
    v4l-utils \
"
