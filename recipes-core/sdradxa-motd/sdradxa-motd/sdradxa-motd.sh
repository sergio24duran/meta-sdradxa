#!/bin/sh
# Runtime system info printed on every interactive login (SSH or console).
# The static header (banner + build date) lives in /etc/motd.
# The active A/B slot is cached at /run/sdradxa/ by ab-mark-good.service
# at boot, so this script never needs to mount the ESP partition.

# --- Image identity (written by image recipe at build time) ---
IMAGE_ID=$(grep '^IMAGE_ID=' /etc/image-release 2>/dev/null | cut -d= -f2)
IMAGE_BUILDDATE=$(grep '^IMAGE_BUILDDATE=' /etc/image-release 2>/dev/null | cut -d= -f2)

# --- A/B slot (cached by ab-mark-good at boot) ---
SLOT=$(cat /run/sdradxa/slot  2>/dev/null || echo "?")
TRIES=$(cat /run/sdradxa/tries 2>/dev/null || echo "?")

# --- Kernel ---
KERNEL=$(uname -r)

# --- Uptime from /proc to avoid locale-dependent uptime(1) formatting ---
UPTIME_SEC=$(cut -d. -f1 /proc/uptime)
UPTIME_H=$(( UPTIME_SEC / 3600 ))
UPTIME_M=$(( (UPTIME_SEC % 3600) / 60 ))

# --- Memory (GB, from /proc/meminfo) ---
MEM_TOTAL=$(awk '/^MemTotal:/{printf "%.1f", $2/1024/1024}' /proc/meminfo)
MEM_USED=$(awk '/^MemTotal:/{t=$2} /^MemAvailable:/{a=$2} END{printf "%.1f", (t-a)/1024/1024}' /proc/meminfo)

# --- Disk usage (human-readable: K/M/G decided by df -h) ---
disk_info() {
    mountpoint -q "$1" 2>/dev/null || { echo "not mounted"; return; }
    df -h "$1" | awk 'NR==2{printf "%s / %s (%s)", $3, $2, $5}'
}
DISK_SD=$(disk_info /mnt/sdcard)
DISK_UFS=$(disk_info /mnt/ufscard)

# --- Non-loopback IPv4 addresses ---
IPS=$(ip -4 addr | awk '/inet /{print $2}' | grep -v '^127' \
    | sed 's|/.*||' | tr '\n' '  ')

printf "  Image  : %s\n"         "$IMAGE_ID"
printf "  Built  : %s\n"         "$IMAGE_BUILDDATE"
printf "  Host   : %s\n"         "$(hostname)"
printf "  Kernel : %s\n"         "$KERNEL"
printf "  Uptime : %dh %dm\n"    "$UPTIME_H" "$UPTIME_M"
printf "  Memory : %sG / %sG\n"  "$MEM_USED" "$MEM_TOTAL"
printf "  SD     : %s\n"         "$DISK_SD"
printf "  UFS    : %s\n"         "$DISK_UFS"
printf "  A/B    : slot %s  (tries_left: %s)\n" "$SLOT" "$TRIES"
printf "  IP     : %s\n"         "$IPS"
printf "\n"
