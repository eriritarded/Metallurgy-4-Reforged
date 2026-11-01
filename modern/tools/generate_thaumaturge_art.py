"""Generate the Thaumaturge-compat textures (wand caps, rods, native clusters, gear).

    python tools/generate_thaumaturge_art.py <out_dir>

Everything is drawn here or recoloured from Metallurgy's own GPL textures: caps and rods are
procedural shapes shaded with each metal's ingot palette; clusters put Metallurgy's ore chips on a
stone lump drawn here; the knuckles and buckler are palette swaps of
Metallurgy's brass knuckles and zinc buckler. No Thaumaturge art is copied: only the model UV
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
RODS = {"vulcanite": True, "eximite": False}  # metal -> glows
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


def dull(ramp):
    """Inert caps: greyer and darker than the charged ones."""
    out = []
    for c in ramp:
        h, l, s = colorsys.rgb_to_hls(*(v / 255 for v in c))
        out.append(tuple(round(v * 255) for v in colorsys.hls_to_rgb(h, l * 0.85, s * 0.45)))
    return out


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


def binned_ramp(metal, n=8):
    """n tones from the ingot: its pixels sorted by brightness, averaged per bin (no single stray pixel wins)."""
    img = Image.open(os.path.join(TEX, "items", f"{metal}_ingot.png")).convert("RGBA")
    cols = sorted((p[:3] for p in img.get_flattened_data() if p[3] > 200), key=lum)
    out = []
    for i in range(n):
        part = cols[i * len(cols) // n:max(i * len(cols) // n + 1, (i + 1) * len(cols) // n)]
        out.append(tuple(round(sum(c[k] for c in part) / len(part)) for k in range(3)))
    # Outline tone: the darkest bin pushed darker, keeping its hue.
    h, l, sat = colorsys.rgb_to_hls(*(v / 255 for v in out[0]))
    return [tuple(round(v * 255) for v in colorsys.hls_to_rgb(h, l * 0.55, sat))] + out


# Upright wand cap, rows top to bottom: (half width, part). Centre is between pixel columns 7 and 8.
CAP_ROWS = [(1, "knob"), (2, "knob"), (1, "neck"), (3, "dome"), (4, "dome"), (5, "body"), (5, "body"),
            (6, "band_top"), (6, "band"), (5, "under"), (5, "body"), (4, "body"), (5, "rim"), (6, "rim_low")]


def cap_item(ramp, frame=None, frames=4):
    """Original upright cap: a knob, a domed top, a raised band and a flared rim, lit from the upper left."""
    img = Image.new("RGBA", (16, 16))
    top = 1
    shape = {}
    for i, (half, part) in enumerate(CAP_ROWS):
        for x in range(8 - half, 8 + half):
            shape[(x, top + i)] = (half, part, i)
    lx, ly, lz = -0.62, -0.45, 0.64  # light: left, up, towards the viewer
    for (x, y), (half, part, i) in shape.items():
        u = (x + 0.5 - 8) / half  # -1 left edge .. 1 right edge
        nx = u
        ny = {"knob": -0.5, "neck": 0.2, "dome": -0.55 + 0.15 * (i - 3), "band_top": -0.6, "under": 0.55,
              "rim_low": 0.5}.get(part, 0.0)
        nz = math.sqrt(max(0.05, 1 - nx * nx - ny * ny))
        light = max(0.0, (nx * lx + ny * ly + nz * lz) / math.sqrt(nx * nx + ny * ny + nz * nz))
        level = 1.0 + light * 5.6
        level += {"band_top": 1.4, "under": -1.6, "rim": 0.6, "rim_low": -1.2, "neck": -1.0}.get(part, 0.0)
        level -= 0.12 * max(0, i - 9)  # a little darker towards the open end
        if -0.75 < u < -0.3 and part not in ("under", "rim_low"):
            level += 0.9  # soft specular streak down the lit side
        if frame is not None:  # charged shimmer: a bright band sliding down the cap
            centre = (frame + 0.5) / frames * len(CAP_ROWS)
            level += 2.2 * max(0.0, 1 - abs(i - centre) / 1.6)
        edge_r = (x + 1, y) not in shape
        edge_l = (x - 1, y) not in shape
        edge_b = (x, y + 1) not in shape
        edge_t = (x, y - 1) not in shape
        if edge_r or edge_b:
            level = 0  # hard outline away from the light
        elif edge_l or edge_t:
            level = min(level, 2.4)  # softer outline on the lit side
        img.putpixel((x, y), ramp[max(0, min(len(ramp) - 1, round(level)))] + (255,))
    return img


def cap_model(ramp):
    """32x32 model texture: two 2x2x2 boxes joined by a 2px stem, at the wand cap UV layout."""
    img = Image.new("RGBA", (32, 32))
    face = [3, 4, 2, 1]  # per side face: lit, top-lit, shaded, back
    for y in (2, 3, 7, 8):
        for x in range(8):
            level = face[x // 2] + (1 if y in (2, 7) else 0)
            img.putpixel((x, y), ramp[min(5, level)] + (255,))
    for y in (0, 1):
        for x in range(2, 6):
            img.putpixel((x, y), ramp[5 if x < 4 else 2] + (255,))
    for y in (4, 5, 6):
        for x in (3, 4):
            img.putpixel((x, y), ramp[3 if x == 3 else 1] + (255,))
    return img


def rod_pattern(metal):
    if metal == "vulcanite":  # glowing magma veins
        return lambda t, k, rng: 1.6 if (int(t * 14) % 4 == 0 and abs(k) < 0.5) else 0.0
    return lambda t, k, rng: (1.0 if rng.random() < 0.12 else 0.0)  # eximite: starry fleck


def rod_item(metal, staff):
    img = Image.new("RGBA", (16, 16))
    ramp = metal_ramp(metal)
    a, b = ((1.5, 14.5), (14.5, 1.5)) if staff else ((3.0, 13.0), (13.0, 3.0))

    def radius(t):
        return 1.9 if staff and (t < 0.07 or t > 0.93) else 1.45
    shade_rod(img, a, b, radius, ramp, pattern=rod_pattern(metal), seed=7)
    return img


def rod_model(metal):
    """32x32 model texture at the rod UV layout: 2x2 ends, four 2px side faces 18 tall."""
    img = Image.new("RGBA", (32, 32))
    ramp = metal_ramp(metal)
    rng = random.Random(11)
    face = [3, 4, 2, 1]
    for y in (8, 9):
        for x in range(2, 6):
            img.putpixel((x, y), ramp[4] + (255,))
    for y in range(10, 28):
        for x in range(8):
            level = face[x // 2]
            if metal == "vulcanite" and (y - 10) % 5 == 0 and x % 2 == 0:
                level = 5
            elif metal == "eximite" and rng.random() < 0.1:
                level = 5
            img.putpixel((x, y), ramp[level] + (255,))
    return img


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
    for metal in TIER_CAPS + THEMED_CAPS + WACKY_CAPS:
        ramp = metal_ramp(metal)
        save(cap_model(ramp), out, f"models/wand_cap_{metal}.png")
        ramp = binned_ramp(metal)
        if metal in CHARGED_CAPS:
            save(cap_item(dull(ramp)), out, f"items/wand_cap_{metal}_inert.png")
            strip = Image.new("RGBA", (16, 64))
            for f in range(4):
                strip.alpha_composite(cap_item(ramp, frame=f), (0, f * 16))
            save(strip, out, f"items/wand_cap_{metal}.png")
        else:
            save(cap_item(ramp), out, f"items/wand_cap_{metal}.png")
    for metal in RODS:
        save(rod_item(metal, False), out, f"items/wand_rod_{metal}.png")
        save(rod_item(metal, True), out, f"items/staff_rod_{metal}.png")
        save(rod_model(metal), out, f"models/wand_rod_{metal}.png")
    for metal in CLUSTERS:
        save(cluster(metal), out, f"items/cluster_{metal}.png")
    thaumium = [hexrgb(h) for h in THAUMIUM]
    void = [hexrgb(h) for h in VOID]
    gadgets = os.path.join(TEX, "items", "gadgets")
    for src, dst in (("brass_knuckles.png", "thaumium_knuckles.png"), ("brass_knuckles_item.png", "thaumium_knuckles_item.png")):
        save(gradient_map(Image.open(os.path.join(gadgets, src)).convert("RGBA"), thaumium), out, "items/gadgets/" + dst)
    save(gradient_map(Image.open(os.path.join(gadgets, "zinc_buckler.png")).convert("RGBA"), void, is_metal),
         out, "items/gadgets/void_buckler.png")
    print("wrote textures to", out)


if __name__ == "__main__":
    main()
