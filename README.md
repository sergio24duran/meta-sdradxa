# meta-sdradxa

Yocto/OpenEmbedded BSP layer for Radxa boards with Qualcomm SoCs.

## Supported boards

| Board | SoC | Machine name | Status |
|-------|-----|-------------|--------|
| [Radxa Dragon Q6A](https://radxa.com/products/dragon/q6a/) | QCS6490 (Snapdragon 7c+ Gen 3) | `sdradxa-dragon-q6a` | Boots from SD card, serial console, GPIO, SSH |

## Layer dependencies

This layer must be used with the **scarthgap** branch of all dependencies.

| Layer | Repository | Branch |
|-------|-----------|--------|
| openembedded-core (meta) | https://git.yoctoproject.org/poky | scarthgap |
| meta-poky | (included in poky) | scarthgap |
| meta-oe | https://github.com/openembedded/meta-openembedded | scarthgap |
| meta-qcom | https://github.com/qualcomm-linux/meta-qcom | scarthgap |

## Quick start

### 1. Set up the Yocto build environment

```bash
# Clone Poky (OE-Core + Poky distro)
git clone -b scarthgap https://git.yoctoproject.org/poky

# Clone meta-openembedded
git clone -b scarthgap https://github.com/openembedded/meta-openembedded.git

# Clone meta-qcom
git clone -b scarthgap https://github.com/qualcomm-linux/meta-qcom.git

# Clone this layer
git clone https://github.com/<your-user>/meta-sdradxa.git
```

### 2. Initialize the build

```bash
source poky/oe-init-build-env build
```

### 3. Configure `conf/bblayers.conf`

Add the required layers (adjust paths to your setup):

```bitbake
BBLAYERS ?= " \
    /path/to/poky/meta \
    /path/to/poky/meta-poky \
    /path/to/poky/meta-yocto-bsp \
    /path/to/meta-openembedded/meta-oe \
    /path/to/meta-qcom \
    /path/to/meta-sdradxa \
"
```

### 4. Configure `conf/local.conf`

```bitbake
MACHINE = "sdradxa-dragon-q6a"
PREFERRED_PROVIDER_virtual/kernel = "linux-linaro-qcomlt"
```

> **Note:** `linux-linaro-qcomlt` must be set with a hard `=` assignment. Using
> `?=` will not override meta-qcom's default kernel. See
> [Kernel](#kernel-linux-linaro-qcomlt) for details.

### 5. Build

```bash
bitbake sdradxa-image-minimal
```

First build takes 1-2 hours depending on hardware. Output is placed in
`build/tmp/deploy/images/sdradxa-dragon-q6a/`.

## Flashing to SD card

The build produces a complete SD card image (`.wic.gz`) with the correct
partition layout. No manual partitioning required.

### Flash with dd

```bash
DEPLOY=build/tmp/deploy/images/sdradxa-dragon-q6a

gunzip -c ${DEPLOY}/sdradxa-image-minimal-sdradxa-dragon-q6a.rootfs.wic.gz \
  | sudo dd of=/dev/sdX bs=4M status=progress iflag=fullblock conv=fsync
```

> **Important:** The `.wic.gz` file is gzip-compressed (not zstd). Use `gunzip`, not `zstdcat`.

### Flash with bmaptool (faster)

```bash
sudo bmaptool copy \
  ${DEPLOY}/sdradxa-image-minimal-sdradxa-dragon-q6a.rootfs.wic.gz \
  /dev/sdX
```

### Expand rootfs partition

The wic image creates a rootfs partition just large enough for the filesystem.
To use the full SD card capacity:

```bash
sudo e2fsck -f /dev/sdX2
sudo resize2fs /dev/sdX2
sudo e2fsck -f /dev/sdX2
sudo sync
```

### Verify (optional)

```bash
# Partition types
sudo sgdisk -p /dev/sdX
# Partition 1: EF00 (EFI System)
# Partition 2: 8300 (Linux filesystem), name "rootfs"

# Kernel file
sudo mount /dev/sdX1 /mnt
file /mnt/EFI/BOOT/BOOTAA64.EFI
# Expected: "Linux kernel ARM64 boot executable Image"
sudo umount /mnt
```

## Booting

### Serial console

Connect a USB-to-UART adapter to the board's debug header.

| Setting | Value |
|---------|-------|
| Baud rate | 115200 |
| Data bits | 8 |
| Parity | None |
| Stop bits | 1 |
| Flow control | None |

### Boot sequence

Insert the SD card and power on the board. The expected serial output:

```
SBL1 banner           → Qualcomm primary bootloader
UEFI banner           → UEFI firmware
Trying device 1: SD   → UEFI finds the SD card
EFI stub: Booting...  → Kernel EFI stub starts
Kernel command line:   → Compiled-in CONFIG_CMDLINE applied
mmcblk1: p1 p2        → SD card partitions detected
EXT4-fs mounted        → rootfs mounted
systemd[1]: Detected   → systemd starts
                       → Login prompt (~16 seconds)
```

### Default credentials

| User | Password |
|------|----------|
| root | root |
| sdradxa | sdradxa |

## SD card layout

The wic image (`sdradxa-dragon-q6a.wks`) creates a GPT disk with two partitions:

```
+-------------------+-----------------------------------+
| Partition 1       | Partition 2                       |
| GPT type: EF00    | GPT type: 8300                    |
| FAT32, 64 MiB     | ext4, remaining space             |
| Label: "efi"      | Label: "rootfs"                   |
| EFI/BOOT/         | /                                 |
|   BOOTAA64.EFI    | (Yocto rootfs)                    |
+-------------------+-----------------------------------+
```

- **Partition 1 (ESP):** Contains the uncompressed kernel `Image` renamed to
  `BOOTAA64.EFI`. Qualcomm UEFI auto-boots from this EFI removable media
  fallback path.
- **Partition 2 (rootfs):** ext4 root filesystem. The kernel finds it via
  `root=PARTLABEL=rootfs` (compiled into `CONFIG_CMDLINE`).

## Architecture

### Boot flow

```
QCM6490 PMIC → SBL1 → UEFI firmware → SD card ESP → BOOTAA64.EFI (kernel)
                                                           ↓
                                                 EFI stub + CONFIG_CMDLINE
                                                           ↓
                                                 root=PARTLABEL=rootfs
                                                           ↓
                                                 systemd → login prompt
```

No intermediate bootloader (GRUB, systemd-boot, U-Boot) is used. UEFI launches
the kernel's EFI stub directly. The kernel command line is compiled in via
`CONFIG_CMDLINE_EXTEND`, ensuring `root=` and `rootwait` are always present even
if UEFI passes its own (incomplete) command line.

### Machine config inheritance

```
sdradxa-dragon-q6a.conf
  └── sdradxa-qcm6490.inc         (SoC-level: tune, serial, Qualcomm services)
       └── qcom-common.inc        (from meta-qcom)
            └── soc-family.inc
```

### Layer priority

`meta-sdradxa` has priority **6**, overriding `meta-qcom` (priority 5) where
needed (e.g., `COMPATIBLE_MACHINE` extensions).

## Kernel: linux-linaro-qcomlt

This layer uses `linux-linaro-qcomlt` (kernel 6.6.x from Linaro/Qualcomm
Landing Team). It is set as the preferred kernel provider via:

```bitbake
PREFERRED_PROVIDER_virtual/kernel = "linux-linaro-qcomlt"
```

### Device tree

`linux-linaro-qcomlt` 6.6 does not include a device tree for QCM6490/QCS6490.
The board receives its DTB from UEFI firmware at boot time. A board-specific DTB
(`qcs6490-radxa-dragon-q6a.dtb`) is under review on LKML (patch v4 by Xilin Wu,
Radxa) and will be added once it lands in the kernel.

Because there is no compiled-in DTB:
- `KERNEL_DEVICETREE` is set to an empty string (prevents a `None.split()` crash
  in `linux-qcom-bootimg.bbclass`).
- The empty `/dtb` directory created by `KERNEL_DTBDEST="dtb"` is removed in
  `do_install:append` to avoid a QA error.

### Kernel config fragment

`recipes-kernel/linux/linux-linaro-qcomlt/sdradxa-dragon-q6a.cfg` applies
board-specific kernel configuration on top of `arm64 defconfig`. Key settings:

| Config | Value | Reason |
|--------|-------|--------|
| `CONFIG_CMDLINE` | `earlycon root=PARTLABEL=rootfs rootwait rw console=ttyMSM0,115200n8 ...` | UEFI auto-boot passes empty cmdline |
| `CONFIG_CMDLINE_EXTEND` | `y` | Append compiled-in cmdline to UEFI cmdline |
| `CONFIG_MMC`, `_SDHCI`, `_SDHCI_MSM` | `y` (built-in) | No initramfs — SD/MMC drivers must be built-in |
| `CONFIG_EFI_EARLYCON` | `y` | Early console before ttyMSM0 is available |

### Disabled drivers (QCS6490 + UEFI DTB workarounds)

The UEFI-provided DTB for QCS6490 lacks some bindings that kernel 6.6 drivers
expect (regulators, clocks). These drivers crash during probe, causing a warm
reset loop (DLOAD mode). They are disabled because they are not needed for SD
card boot:

| Config | Driver | Crash symptom | Needed for |
|--------|--------|--------------|------------|
| `CONFIG_PCIE_QCOM` | Qualcomm PCIe controller | Crash at `PCI host bridge to bus 0000:00` | PCIe peripherals (NVMe, etc.) |
| `CONFIG_SCSI_UFS_QCOM` | Qualcomm UFS controller | Crash at `ufshcd_populate_vreg: Unable to find vdd-hba-supply` | eMMC/UFS storage |

> **Re-enable these** once a proper QCS6490 DTB with complete regulator/clock
> bindings is available (kernel 6.13+ or when the Radxa DTB patch is merged
> upstream).

## Image: sdradxa-image-minimal

A minimal bootable image with:
- Terminal shell (bash)
- SSH server (dropbear)
- GPIO userspace tools (libgpiod)
- Core utilities (util-linux)
- Qualcomm userspace services (pd-mapper, qrtr, rmtfs, tqftpserv)

The image disables ext4 `orphan_file` feature (`EXTRA_IMAGECMD:ext4`) to avoid
a checksum incompatibility between e2fsprogs 1.47.0 (host) and Linux kernel 6.6.

## Adding a new board

To add support for a new Radxa board with a Qualcomm SoC:

1. If the SoC is new, create `conf/machine/include/sdradxa-<soc>.inc`
2. Create `conf/machine/sdradxa-<board>.conf` (require the SoC include)
3. Add the machine to the `COMPATIBLE_MACHINE` regex in `sdradxa-image-minimal.bb`
4. If the kernel needs board-specific configuration, create a `.cfg` fragment
   and add it via the bbappend pattern used for Dragon Q6A
5. If the board uses a different partition layout, create a new `.wks` file in
   `wic/`

## Rebuilding after changes

```bash
# After kernel config fragment changes
bitbake -c cleansstate linux-linaro-qcomlt
bitbake sdradxa-image-minimal

# After recipe/package changes (no kernel change)
bitbake sdradxa-image-minimal

# After wks partition layout changes
bitbake sdradxa-image-minimal -c clean
bitbake sdradxa-image-minimal
```

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Nothing on serial after power on | Wrong serial port/baud rate | Verify 115200/8N1, check USB cable |
| UEFI shows but doesn't try SD | Partition 1 is not type EF00 | Re-flash; the wks sets `--part-type EF00` |
| "Trying device 1: SD" then nothing | `BOOTAA64.EFI` is `Image.gz` not `Image` | Rebuild; `IMAGE_BOOT_FILES` uses `Image` |
| Kernel panic: no root device | Missing `root=` in cmdline | Rebuild with the `.cfg` fragment |
| `EXT4-fs error: orphan file` | Host e2fsprogs orphan_file incompatibility | Run `sudo tune2fs -O ^orphan_file /dev/sdX2` |
| Board loops in DLOAD mode (SBL1 banner every 6s) | PMIC recorded warm reset from kernel panic | Long-press power button 8-10 seconds |
| 90-second boot delay | Wrong `/etc/fstab` entry | Rebuild; wks has `--no-fstab-update` |
| Crash at `PCI host bridge to bus` | PCIe driver vs UEFI DTB | Verify `CONFIG_PCIE_QCOM` is disabled |
| Crash at `ufshcd_populate_vreg` | UFS driver vs UEFI DTB | Verify `CONFIG_SCSI_UFS_QCOM` is disabled |

## UEFI firmware

The Radxa Dragon Q6A R1+ requires SPI boot firmware version **20251230 or
newer**. Older firmware may cause boot failures. Check the version in the UEFI
BIOS menu. Firmware updates are done via EDL mode (hold the button next to the
audio jack while connecting USB3 to a PC).

See: https://docs.radxa.com/en/dragon/q6a/low-level-dev/bios

## License

This layer is licensed under the MIT License. See [COPYING.MIT](COPYING.MIT).
