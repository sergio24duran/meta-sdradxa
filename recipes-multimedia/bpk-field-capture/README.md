# bpk-field-capture

On-board dev tool to collect real IMX214 training frames. A systemd service waits
for a GPIO button press, grabs one libcamera ISP frame (the same path the drone
uses in flight) and saves it as ABGR8888 to the UFS card, blinking a status LED
per saved shot.

Installs:
- `/usr/bin/bpk-field-capture` — the capture loop
- `bpk-field-capture.service` — auto-enabled, `Restart=always`, gated on `mnt-ufscard.mount`

Output: `/mnt/ufscard/tuning/shot/<scenario>_<timestamp>.abgr` (4088x2304, ~37 MB).
Convert on a PC with `models_bpk/data_collection/abgr_to_png.py`.

## Hardware wiring (40-pin header)

Mapping is `gpiochip4 line offset == SoC GPIO number` (Radxa's own convention).

| Function | gpiochip4 line | Physical pin |
|----------|----------------|--------------|
| Button   | 98 (GPIO_98)   | pin 38       |
| LED      | 99 (GPIO_99)   | pin 40       |

```
Button:   pin 38 ──[ button ]── GND (pin 37)
Pull-up:  pin 38 ──[ 10 kOhm ]── 3V3 (pin 17)
LED:      pin 40 ──[ 330 Ohm ]──|>|── GND (pin 39)
```

The 10 kOhm external pull-up is REQUIRED: runtime libgpiod bias (`gpiomon -b`) is
ineffective on this SoC's TLMM header pins (verified: lines 98/100/97 float with
`-b pull-up`), so the rest level must come from hardware.

## Including it in a build

Not in the production image by default. Add to a dev image or local.conf:

```
IMAGE_INSTALL:append = " bpk-field-capture"
```

## Future: drop the external resistor (device-tree gpio-keys)

To remove the external pull-up in the product image, add a `gpio-keys` node with a
`pinctrl-0` state setting `bias-pull-up` on `gpio98` (device-tree bias IS applied
by pinctrl-msm at probe, unlike the ineffective runtime bias). That exposes the
button as `/dev/input/eventX`; the capture loop would then read the input event
instead of `gpiomon`. Not implemented here to keep the validated gpiomon path.
