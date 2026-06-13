# BPK camera stack: CAMSS (CSIPHY/CSID/VFE) + IMX214 raw sensor on QCM6490.
# The Spectra ISP is deferred (yocto-bpk#12), so frames come out as raw
# bayer and libcamera's software ISP does debayer/3A: that combination is
# the "simple" pipeline + simple IPA.
LIBCAMERA_PIPELINES = "simple"
EXTRA_OEMESON += "-Dipas=simple"

# libcamerasrc element for the sw_bpk GStreamer capture path (sw_bpk#16).
PACKAGECONFIG:append = " gst"
