#!/usr/bin/env python3
"""Prepare a firmware DT snapshot for overlay application.

The SPI NOR UEFI provides a DT without __symbols__ and without phandles on
nodes nothing references yet. Radxa overlays target labelled nodes, so we
inject (a) a __symbols__ table for the labels the BPK overlays use and
(b) phandles on target nodes that lack one. Input/output are .dts text
(dtc does the compiling around this).

Usage: inject_symbols.py <in.dts> <out.dts>
"""
import re
import sys

# label -> node path (verified against the firmware DT of the Dragon Q6A;
# paths match the upstream qcs6490-radxa-dragon-q6a.dts at qcom-next 7.0)
SYMBOLS = {
    "camss": "/soc@0/isp@acb3000",
    "cci0": "/soc@0/cci@ac4a000",
    "cci0_i2c0": "/soc@0/cci@ac4a000/i2c-bus@0",
    "tlmm": "/soc@0/pinctrl@f100000",
    "camcc": "/soc@0/clock-controller@ad00000",
    "uart6": "/soc@0/geniqup@9c0000/serial@998000",
    "spi6": "/soc@0/geniqup@9c0000/spi@998000",
    "i2c6": "/soc@0/geniqup@9c0000/i2c@998000",
}


def node_block_span(src: str, path: str):
    """Return (start, end) of the body of the node at path."""
    pos = 0
    depth_names = [p for p in path.split("/") if p]
    for name in depth_names:
        m = re.compile(r"(^|\n)(\t*)" + re.escape(name) + r" \{").search(src, pos)
        if not m:
            raise SystemExit(f"node not found: {name} (path {path})")
        pos = m.end()
    # find matching closing brace from pos
    depth = 1
    i = pos
    while depth > 0:
        nxt_open = src.find("{", i)
        nxt_close = src.find("}", i)
        if nxt_close == -1:
            raise SystemExit(f"unbalanced braces under {path}")
        if nxt_open != -1 and nxt_open < nxt_close:
            depth += 1
            i = nxt_open + 1
        else:
            depth -= 1
            i = nxt_close + 1
    return pos, i - 1


def main() -> None:
    src = open(sys.argv[1]).read()

    max_ph = max(
        (int(m, 16) for m in re.findall(r"phandle = <(0x[0-9a-f]+)>", src)),
        default=0,
    )

    # Inject phandles where the target node itself has none (children with
    # phandles must not mask the parent: strip nested blocks first).
    for path in SYMBOLS.values():
        start, end = node_block_span(src, path)
        top = src[start:end]
        while True:
            stripped = re.sub(r"\{[^{}]*\}", "", top)
            if stripped == top:
                break
            top = stripped
        if "phandle = <" in top:
            continue
        max_ph += 1
        src = src[:start] + f"\n\t\tphandle = <{max_ph:#x}>;" + src[start:end] + src[end:]

    sym_block = "\n\t__symbols__ {\n" + "".join(
        f'\t\t{k} = "{v}";\n' for k, v in SYMBOLS.items()
    ) + "\t};\n"
    idx = src.rstrip().rfind("};")
    src = src[:idx] + sym_block + src[idx:]

    open(sys.argv[2], "w").write(src)
    print(f"injected {len(SYMBOLS)} symbols, max phandle now {max_ph:#x}")


if __name__ == "__main__":
    main()
