"""Template for generating multi-density raster icons (WebP/PNG) with PIL.

Prefer hand-authored vector drawables for ordinary UI glyphs; use this only
for artwork that is awkward as path data (see SKILL.md).

Copy this file to the scratchpad, fill in the ICONS dict with one
draw_xxx(draw) function per icon, then run from the repo root:

    python3 gen_icons.py                       # lossless WebP, all densities
    python3 gen_icons.py --png                 # PNG instead of WebP
    python3 gen_icons.py --preview sheet.png   # also write an xxxhdpi sheet

Each icon is drawn on a 24x24dp canvas, supersampled for anti-aliasing, and
exported at mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi as opaque black on transparent.
Tint at the call site (Compose `Icon`).
"""
import argparse
import os
from PIL import Image, ImageDraw

SS = 20            # supersample factor
BASE = 24          # dp viewport (matches Material icons)
SIZE = BASE * SS   # working canvas size in px
BLACK = (0, 0, 0, 255)

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}

RES_DIR = "app/src/main/res"  # run from the repo root, or make this absolute


def new_canvas():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def d(v):
    """dp -> supersampled px (PIL needs ints for coords/widths)."""
    return round(v * SS)


# --- Example icon: replace/add with your own draw_xxx(draw) functions ---
def draw_example(draw):
    draw.rounded_rectangle([d(3), d(3), d(21), d(21)], radius=d(2), outline=BLACK, width=d(1.6))
    draw.line([d(6), d(12), d(18), d(12)], fill=BLACK, width=d(1.6))


ICONS = {
    # "ic_feature_name": draw_example,
}


def render(drawer):
    canvas = new_canvas()
    drawer(ImageDraw.Draw(canvas))
    return canvas


def export(name, canvas, ext):
    for density, factor in DENSITIES.items():
        px = round(BASE * factor)
        resized = canvas.resize((px, px), Image.LANCZOS)
        out_dir = os.path.join(RES_DIR, f"drawable-{density}")
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, f"{name}.{ext}")
        if ext == "webp":
            resized.save(out_path, lossless=True)
        else:
            resized.save(out_path)
        print("wrote", out_path, resized.size)


def write_preview(canvases, path, pad=16, scale=4):
    """White sheet of xxxhdpi icons, NEAREST-upscaled so real pixels show."""
    px = round(BASE * DENSITIES["xxxhdpi"])
    cell = px * scale + pad
    sheet = Image.new("RGBA", (cell * max(len(canvases), 1) + pad, cell + pad), "white")
    for i, canvas in enumerate(canvases):
        icon = canvas.resize((px, px), Image.LANCZOS).resize((px * scale, px * scale), Image.NEAREST)
        sheet.alpha_composite(icon, (pad + i * cell, pad))
    sheet.save(path)
    print("wrote preview", path)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--png", action="store_true", help="write PNG instead of lossless WebP")
    parser.add_argument("--preview", metavar="PATH", help="also write an xxxhdpi preview sheet")
    args = parser.parse_args()

    canvases = []
    for name, drawer in ICONS.items():
        canvas = render(drawer)
        canvases.append(canvas)
        export(name, canvas, "png" if args.png else "webp")
    if args.preview:
        write_preview(canvases, args.preview)


if __name__ == "__main__":
    main()
