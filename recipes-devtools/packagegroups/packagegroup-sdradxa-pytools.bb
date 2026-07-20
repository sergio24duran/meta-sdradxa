SUMMARY = "On-device Python image tooling for BPK"
DESCRIPTION = "Python 3 with numpy and OpenCV so frame conversion and image \
tooling (e.g. ABGR capture post-processing) can run on the board instead of \
only host-side. OpenCV pulls python3-numpy and python3-core transitively."
LICENSE = "MIT"

inherit packagegroup

RDEPENDS:${PN} = " \
    python3-core \
    python3-numpy \
    python3-opencv \
"
