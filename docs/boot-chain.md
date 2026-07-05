# Boot chain: systemd-boot, kernel banks and boot counting

How a Radxa Dragon Q6A running this BSP boots, and how kernel/DTB updates
stay unbrickable. Rootfs A/B is documented in the ab-tools recipe; this page
covers everything before the initramfs.

## Chain

```
SPI NOR: Qualcomm EDK2 (never touched by us)
  └─ removable-media path only: ESP EFI/BOOT/BOOTAA64.EFI  (no Boot#### vars)
       └─ systemd-boot
            ├─ loader/entries/bpk-new+N.conf   candidate, if staged (default)
            └─ loader/entries/bpk-good.conf    known-good fallback
                 └─ EFI/bpk/<bank>/Image.efi   kernel+initramfs EFI bundle
                    EFI/bpk/<bank>/bpk.dtb     merged BPK DTB (sd-boot
                                               "devicetree" directive)
                      └─ initramfs: reads slot.conf → rootfs_a / rootfs_b
```

Banks are `EFI/bpk/0` and `EFI/bpk/1`. `bpk-good.conf` points at the active
bank; a candidate is always staged into the other one. The kernel is shared
between rootfs slots (DEC-024 in bpk_system): kernel updates are decoupled
from rootfs updates.

## Why

LES-010 (bpk_system 99_work/LESSONS.md): the kernel bundle used to *be*
`BOOTAA64.EFI`, with the DTB loaded via `dtb=` in `CONFIG_CMDLINE`. A missing
DTB file aborts the kernel EFI stub — not a graceful fallback — and a bad
kernel bricks both rootfs slots at once. EDK2 exposes no boot variables, so
the fallback logic must live in the ESP payload: systemd-boot's automatic
boot assessment counts attempts by renaming the entry file (`+3` → `+2-1` →
…), which works on plain FAT with no NVRAM.

## Update flow (kernel or DTB)

```
host$ scripts/ab-push.sh -k Image-initramfs-*.bin -d sdradxa-dragon-q6a-bpk.dtb \
        -t root@<board>
board: ab-kernel stage …      # inactive bank + bpk-new+3.conf (atomic)
       COLD power-cycle       # warm reboot can hang in XBL (LES-017)
       ab-kernel status       # BLESSED after a successful boot
       ab-kernel promote      # rewrites bpk-good.conf = commit point
```

- A candidate that never reaches `boot-complete.target` burns one attempt per
  power cycle; at `+0` sd-boot boots `bpk-good` again. Recovery = up to 3
  power cycles, no SD surgery.
- Blessing is done by `systemd-bless-boot.service`, pulled in automatically
  by systemd's bless-boot-generator when it sees the `LoaderBootCountPath`
  EFI variable (efivarfs verified working on this EDK2).
  `ab-mark-good.service` is ordered `Before=boot-complete.target`, so the
  rootfs slot and the kernel candidate are declared good together.
- Every ESP write in `ab-kernel` is temp+rename; the entry-file rename is the
  single commit point, so a power cut mid-update leaves a bootable ESP.

## Rules that still apply

- Never touch SPI NOR / EDK2.
- Updating systemd-boot itself (`EFI/BOOT/BOOTAA64.EFI`) is manual and rare:
  keep a `BOOTAA64.EFI.bak` and copy via temp+rename.
- Prefer a COLD power-cycle after staging (LES-017: warm reboot can hang in
  the XBL USB-download loop; unrelated to this mechanism).
- The ESP is auto-mounted read-write at `/efi` on demand (`efi.automount`,
  60 s idle unmount) — do not leave shells parked inside `/efi`.

## Pieces

| Piece | Where |
|-------|-------|
| systemd-boot binary | `systemd-boot` recipe (poky), deployed by image via `IMAGE_BOOT_FILES` |
| loader.conf, bpk-good.conf | `recipes-core/sdradxa-ab-tools/` (deployed to ESP by wic) |
| ab-kernel | `recipes-core/sdradxa-ab-tools/` → `/usr/bin/ab-kernel` |
| efi.mount / efi.automount | `recipes-core/sdradxa-ab-tools/` |
| Kernel cmdline (no `dtb=`) | `recipes-kernel/linux/linux-qcom-next/configs/sdradxa-dragon-q6a.cfg` |
| Host push | `scripts/ab-push.sh -k … -d …` |
