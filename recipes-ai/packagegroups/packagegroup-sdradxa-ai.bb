SUMMARY = "AI/ML packages for the Radxa Dragon Q6A (QCS6490 Hexagon NPU)"
DESCRIPTION = "Runtime libraries and tools for AI inference on the 12 TOPS \
Hexagon V68 DSP via QNN SDK. Includes the QNN runtime, DSP firmware, FastRPC \
compatibility shim, and the cdsprpcd daemon (cDSP RPC listener)."

LICENSE = "MIT"

inherit packagegroup

RDEPENDS:${PN} = " \
    qnn-sdk \
    fastrpc-systemd \
"
