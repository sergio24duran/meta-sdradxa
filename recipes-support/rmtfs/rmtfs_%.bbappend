# The Dragon Q6A boots with no modem: only the adsp and cdsp remoteprocs exist
# (`/sys/class/remoteproc/*`), and rmtfs serves the MODEM's remote filesystem.
# It starts, fails with "Failed to get rprocfd", retries five times and gives
# up, which has left `systemctl is-system-running` at "degraded" since at least
# July -- so every boot report has carried a red herring (yocto-bpk#76).
#
# Keep the package (harmless, and a future modem-enabled variant would want it)
# but stop enabling a service that cannot work on this hardware. Remove this
# bbappend the day the modem is brought up in the device tree.
SYSTEMD_AUTO_ENABLE = "disable"
