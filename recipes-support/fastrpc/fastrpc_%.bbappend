# Enable the FastRPC DSP daemon services at boot.
#
# The upstream fastrpc recipe disables these by default. We need cdsprpcd
# (cDSP RPC daemon) running to create the unsigned protection domain (PD)
# on the Hexagon compute DSP, which is required before QNN can load skel
# libraries for NPU inference.
#
# This also enables adsprpcd and sdsprpcd (audio/sensor DSP daemons).
# They are harmless no-ops if the corresponding hardware is unused.
SYSTEMD_AUTO_ENABLE:${PN}-systemd = "enable"
