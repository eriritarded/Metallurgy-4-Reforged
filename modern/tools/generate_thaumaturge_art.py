"""Generate the Thaumaturge-compat textures (wand caps, rods, native clusters, gear).

    python tools/generate_thaumaturge_art.py <out_dir>

Everything is drawn here or recoloured from Metallurgy's own GPL textures: caps and rods are
procedural shapes shaded with each metal's ingot palette; clusters put Metallurgy's ore chips on a
stone lump drawn here; the knuckles, bucklers and gauntlet are palette swaps of
Metallurgy's brass knuckles, zinc buckler and rubracium gauntlet. No Thaumaturge art is copied: only the model UV
layout (which pixels a cap or rod box uses) is matched so the textures map onto its wand model.
Output mirrors assets/metallurgy/textures/. Art ships only after review; see docs/THAUMATURGE.md.
"""
import colorsys
import math
import os
import random
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "metallurgy", "textures")

TIER_CAPS = ["bronze", "steel", "platinum", "mithril", "orichalcum", "adamantine"]
THEMED_CAPS = ["ignatius", "ceruclase", "shadow_steel", "sanguinite", "midasium", "etherium"]
WACKY_CAPS = ["quicksilver", "kalendrite", "astral_silver", "tartarite", "krik", "amordrine", "desichalkos", "prometheum"]
CHARGED_CAPS = {"mithril", "orichalcum", "adamantine"}  # crafted inert, charged by infusion
# Metallurgy ores without a Thaumaturge cluster (it has copper, iron, gold, silver, tin, lead).
CLUSTERS = ["adamantine", "alduorite", "astral_silver", "atlarus", "carmot", "ceruclase", "deep_iron", "eximite",
            "ignatius", "infuscolium", "kalendrite", "lemurite", "lutetium", "manganese", "meutoite", "midasium",
            "mithril", "orichalcum", "osmium", "oureclase", "platinum", "prometheum", "rubracium", "sanguinite",
            "shadow_iron", "vulcanite", "vyroxeres", "zinc"]
# Hand-picked ramps (dark -> light) for the two Thaumaturge metals.
THAUMIUM = ["#1c1433", "#33245c", "#4d3a8a", "#6b56b3", "#8f7fd6", "#c3b8f2"]
VOID = ["#07040c", "#140b21", "#241638", "#3a2556", "#5a3f7d", "#8a6fae"]


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramp_from(path, n=6):
    """n-step dark->light ramp from the opaque pixels of a Metallurgy texture."""
    img = Image.open(path).convert("RGBA")
    cols = sorted((p[:3] for p in img.get_flattened_data() if p[3] > 200), key=lum)
    return [cols[min(len(cols) - 1, round(i * (len(cols) - 1) / (n - 1)))] for i in range(n)]


def metal_ramp(metal):
    return ramp_from(os.path.join(TEX, "items", f"{metal}_ingot.png"))


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade_rod(img, a, b, radius, ramp, outline=True, pattern=None, seed=0):
    """Rasterise a lit cylinder from a to b (pixel coords) with a radius profile radius(t)."""
    rng = random.Random(seed)
    ax, ay = b[0] - a[0], b[1] - a[1]
    length = math.hypot(ax, ay)
    ux, uy = ax / length, ay / length
    inside = {}
    for y in range(img.height):
        for x in range(img.width):
            px, py = x + 0.5 - a[0], y + 0.5 - a[1]
            t = (px * ux + py * uy) / length
            v = px * -uy + py * ux  # signed distance across the axis
            if -0.04 <= t <= 1.04:
                r = radius(max(0.0, min(1.0, t)))
                if r > 0 and abs(v) <= r:
                    inside[(x, y)] = (t, v / r)
    for (x, y), (t, k) in inside.items():
        # Light from the upper left: k < 0 is the lit side.
        level = 3.4 - 2.2 * k
        if -0.55 < k < -0.1:
            level += 1.2  # specular stripe
        if pattern:
            level += pattern(t, k, rng)
        level = max(1, min(len(ramp) - 1, round(level)))
        if outline and any((x + dx, y + dy) not in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            level = 0 if k > -0.3 else 1
        img.putpixel((x, y), ramp[level] + (255,))
    return inside


def gradient_map(img, ramp, select=None):
    out = img.copy()
    px = out.load()
    lums = [lum(p[:3]) for p in img.get_flattened_data() if p[3] and (select is None or select(p))]
    lo, hi = min(lums), max(lums)
    for y in range(img.height):
        for x in range(img.width):
            p = px[x, y]
            if p[3] and (select is None or select(p)):
                t = (lum(p[:3]) - lo) / max(1, hi - lo)
                i = t * (len(ramp) - 1)
                c = mix(ramp[int(i)], ramp[min(len(ramp) - 1, int(i) + 1)], i - int(i))
                px[x, y] = c + (p[3],)
    return out


STONE = [(48, 48, 52), (72, 72, 76), (96, 95, 98), (120, 119, 121), (142, 141, 142), (166, 165, 166)]


def cluster(metal):
    """A shaded stone lump (drawn here) carrying the metal's own ore chips from Metallurgy's ore texture."""
    rng = random.Random(metal)
    cx, cy = 7.6, 8.4
    edge = [6.2 + rng.uniform(-0.7, 0.5) for _ in range(12)]  # lumpy outline, radius per 30 degrees

    def r_at(ang):
        i = ang / (2 * math.pi) * 12
        j = int(i) % 12
        return edge[j] + (edge[(j + 1) % 12] - edge[j]) * (i - int(i))

    inside = set()
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, (y + 0.5 - cy) * 1.05
            if math.hypot(dx, dy) <= r_at(math.atan2(dy, dx) % (2 * math.pi)):
                inside.add((x, y))
    chips = Image.open(os.path.join(TEX, "blocks", f"{metal}_ore.png")).convert("RGBA")
    out = Image.new("RGBA", (16, 16))
    for (x, y) in inside:
        dx, dy = x + 0.5 - cx, y + 0.5 - cy
        level = 3.2 - (dx + dy) / 5.0 + rng.uniform(-0.6, 0.6)  # lit from the upper left
        if any((x + ox, y + oy) not in inside for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            level = 0 if dx + dy > -2 else 1
        out.putpixel((x, y), STONE[max(0, min(5, round(level)))] + (255,))
    chip = {(x, y) for x in range(16) for y in range(16)
            if chips.getpixel((x, y))[3] and (x, y) in inside
            and math.hypot(x + 0.5 - cx, y + 0.5 - cy) < 4.6}  # leave a stone rim
    for (x, y) in chip:
        for ox, oy in ((1, 0), (0, 1), (1, 1)):  # shadow under each chip
            q = (x + ox, y + oy)
            if q in inside and q not in chip:
                out.putpixel(q, STONE[0] + (255,))
    for (x, y) in chip:
        out.putpixel((x, y), chips.getpixel((x, y))[:3] + (255,))
    return out


def is_metal(p):
    h, l, s = colorsys.rgb_to_hls(*(v / 255 for v in p[:3]))
    return not (0.03 < h < 0.12 and s < 0.6 and l < 0.5)  # leave the brown wood back alone


def save(img, out, rel):
    path = os.path.join(out, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    out = sys.argv[1]
    # Wand caps and rods are recolours of Thaumaturge's own (All Rights Reserved) textures, so they live in a
    # private art repository for the private pack and are not generated or committed here.
    for metal in CLUSTERS:
        save(cluster(metal), out, f"items/cluster_{metal}.png")
    thaumium = [hexrgb(h) for h in THAUMIUM]
    void = [hexrgb(h) for h in VOID]
    gadgets = os.path.join(TEX, "items", "gadgets")
    for src, dst in (("brass_knuckles.png", "thaumium_knuckles.png"), ("brass_knuckles_item.png", "thaumium_knuckles_item.png")):
        save(gradient_map(Image.open(os.path.join(gadgets, src)).convert("RGBA"), thaumium), out, "items/gadgets/" + dst)
    for name, ramp in (("void", void), ("thaumium", thaumium)):
        save(gradient_map(Image.open(os.path.join(gadgets, "zinc_buckler.png")).convert("RGBA"), ramp, is_metal),
             out, f"items/gadgets/{name}_buckler.png")
    save(gradient_map(Image.open(os.path.join(gadgets, "rubracium_gauntlet.png")).convert("RGBA"), void),
         out, "items/gadgets/void_gauntlet.png")
    print("wrote textures to", out)


if __name__ == "__main__":
    main()
