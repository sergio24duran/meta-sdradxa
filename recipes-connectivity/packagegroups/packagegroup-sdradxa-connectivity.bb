SUMMARY = "Network and connectivity packages for sdradxa boards"
DESCRIPTION = "Wired Ethernet support for the RTL8168h NIC behind PCIe: \
firmware blob, systemd-networkd configuration and diagnostic tools."

LICENSE = "MIT"

inherit packagegroup

RDEPENDS:${PN} = " \
    linux-firmware-rtl8168 \
    sdradxa-network-config \
    iproute2 \
"
