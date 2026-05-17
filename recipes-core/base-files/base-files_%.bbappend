# Override the Poky reference-distro warning in /etc/motd with our own banner.
# Our files/ directory has higher layer priority (6 vs 5) so our motd wins.
# Build date and image identity are written by the image recipe into
# /etc/image-release so the timestamp is always fresh (image build time,
# not package build time which is stale in sstate cache).

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
