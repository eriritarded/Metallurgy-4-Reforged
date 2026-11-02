"""Generate the Thaumaturge compat content for NeoForge 26.1.2: item models, recipes, research, tags and lang.

    python tools/generate_thaumaturge_content.py

The registrations live in compat/thaumaturge/ThaumaturgeCompat.java; keep the tables here in step with it.
Cluster and gear textures come from tools/generate_thaumaturge_art.py. Wand cap and rod textures are recolours of
Thaumaturge's All Rights Reserved art: they live in a private repository and are never written here.
Everything Thaumaturge-only is written into neoforge-26.1.2; only lang lines go into common.
"""
import json
import os
import re
import subprocess
import sys

from generate_thaumaturge_data import LANG, NOT_UNIFIED, RES, ROOT, UNIFY

COMMON = os.path.join(ROOT, "common", "src", "main", "resources")
NS = "metallurgy"
COND = [{"type": "neoforge:mod_loaded", "modid": "thaumaturge"},
        {"type": "metallurgy:integration_enabled", "integration": "thaumaturge"}]
# Etherium Goggles are rebuilt around Thaumaturge's goggles when the integration is on.
NOT_COND = {"type": "neoforge:not", "value": {"type": "neoforge:and", "values": COND}}

# metal, cost, special primal, craft cost, charged; mirrors ThaumaturgeCompat.register
TIER = [("bronze", 1.05, 2, False), ("steel", 1.00, 3, False), ("platinum", 0.95, 4, False),
        ("mithril", 0.92, 5, True), ("orichalcum", 0.90, 6, True), ("adamantine", 0.85, 8, True)]
THEMED = [("ignatius", "ignis"), ("ceruclase", "aqua"), ("shadow_steel", "perditio"), ("sanguinite", "terra"),
          ("midasium", "ordo"), ("etherium", "aer")]
WACKY = {
    "quicksilver": "Every cast costs a random 60 to 130 percent.",
    "kalendrite": "20 percent cheaper at night, 10 percent dearer by day.",
    "astral_silver": "Spells are 20 percent stronger at night under open sky, 35 percent at full moon.",
    "tartarite": "30 percent cheaper, but every cast costs half a heart.",
    "krik": "25 percent cheaper while you are off the ground.",
    "amordrine": "Spells grow stronger with your experience, up to 25 percent at level 30.",
    "desichalkos": "30 percent cheaper in the End, 10 percent dearer anywhere else.",
    "prometheum": "20 percent cheaper in sunlight near growing plants, which it sometimes makes grow.",
}
RODS = {"vulcanite": dict(primal="ignis", aspects=[("ignis", 12), ("praecantatio", 6), ("metallum", 6)], extra="minecraft:blaze_powder",
                          staff_vis=14, parent="thaumaturge:rod_blaze", rarity="Vulcanite"),
        "eximite": dict(primal="alienis", aspects=[("alienis", 16), ("praecantatio", 8), ("metallum", 8)], extra="minecraft:ender_pearl",
                        staff_vis=18, parent="thaumaturge:rod_silverwood", rarity="Eximite")}
CLUSTERS = ["adamantine", "alduorite", "astral_silver", "atlarus", "carmot", "ceruclase", "deep_iron", "eximite", "ignatius",
            "infuscolium", "kalendrite", "lemurite", "lutetium", "manganese", "meutoite", "midasium", "mithril", "orichalcum",
            "osmium", "oureclase", "platinum", "prometheum", "rubracium", "sanguinite", "shadow_iron", "vulcanite", "vyroxeres", "zinc"]
SWAPS = {  # Thaumaturge recipe -> (ingredient key, Metallurgy tag); the rest of each recipe is Thaumaturge's own.
    "amulet_fancy": ("G", "#c:ingots/midasium"), "ring_fancy": ("G", "#c:ingots/platinum"),
    "girdle_fancy": ("G", "#c:ingots/midasium"), "arcane_workbench/recharge_pedestal": ("D", "#c:ingots/orichalcum"),
    "arcane_workbench/bellows": ("I", "#c:ingots/steel"), "arcane_workbench/golem_fetter": ("I", "#c:ingots/steel"),
}

lang = {}
recipe_ids = {}


def dump(path, value):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(value, f, indent=2)
        f.write("\n")


def res(*parts):
    return os.path.join(RES, *parts)


def aspects(pairs):
    return [{"aspect": "thaumaturge:" + a, "amount": n} for a, n in pairs]


def recipe(group, name, value, entry=None):
    """Write data/metallurgy/recipe/compat/thaumaturge/<group>/<name>.json; remember it for its research entry."""
    rid = f"{NS}:compat/thaumaturge/{group}/{name}"
    dump(res("data", NS, "recipe", "compat", "thaumaturge", group, name + ".json"), {"neoforge:conditions": COND, **value})
    if entry:
        recipe_ids.setdefault(entry, []).append(rid)
    return rid


def crystal(aspect):
    return {"neoforge:ingredient_type": "neoforge:components", "items": "thaumaturge:essentia_crystal",
            "components": {"thaumaturge:crystal_aspect": {"aspect": "thaumaturge:" + aspect}}}


def names():
    with open(LANG, "rb") as f:
        current = json.loads(f.read().decode("utf-8"))
    def metal(m):
        return current.get(f"item.metallurgy.{m}_ingot", m.replace("_", " ").title()).removesuffix(" Ingot")
    return metal


def simple_item(item, texture, parent="minecraft:item/generated"):
    dump(res("assets", NS, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/{item}"}})
    dump(res("assets", NS, "models", "item", item + ".json"), {"parent": parent, "textures": {"layer0": texture}})


def caps(metal):
    entry = {}
    for m, cost, craft, charged in TIER:
        entry[m] = "caps_charged" if charged else "caps_tier"
    for m, _ in THEMED:
        entry[m] = "caps_themed"
    for m in WACKY:
        entry[m] = "caps_wacky"
    crafts = {m: c for m, _, c, _ in TIER} | {m: 4 for m in entry if m not in {t[0] for t in TIER}}
    for m, research in entry.items():
        charged = research == "caps_charged"
        item = f"wand_cap_{m}" + ("_inert" if charged else "")
        recipe("cap", m, {"type": "thaumaturge:arcane_workbench_shaped", "crystals": [], "key": {"N": f"#c:nuggets/{m}"},
                          "pattern": ["NNN", "N N"], "research": {"entry": f"{NS}:{research}"},
                          "result": {"id": f"{NS}:{item}"}, "vis": crafts[m] * 3}, research)
        simple_item(f"wand_cap_{m}", f"{NS}:item/wand_cap_{m}")
        lang[f"item.{NS}.wand_cap_{m}"] = ("Charged " if charged else "") + metal(m) + " Cap"
        # Thaumaturge names a wand from wand.thaumaturge.cap/rod.<path>, whatever mod registered the part.
        lang[f"wand.thaumaturge.cap.{m}"] = metal(m)
        if charged:
            simple_item(item, f"{NS}:item/{item}")
            lang[f"item.{NS}.{item}"] = f"Inert {metal(m)} Cap"
            scale = crafts[m] / 6  # thaumium's charge (Potentia 12, Auram 6) scaled by the cap's craft cost
            recipe("cap", m + "_charge", {"type": "thaumaturge:infusion", "aspects": aspects([("potentia", round(12 * scale)), ("auram", round(6 * scale))]),
                                          "catalyst": f"{NS}:{item}", "components": ["thaumaturge:salis_mundus"] * 3, "instability": 5,
                                          "research": {"entry": f"{NS}:{research}"}, "result": {"id": f"{NS}:wand_cap_{m}"}}, research)
    for m, text in WACKY.items():
        lang[f"tooltip.{NS}.wand_cap_{m}"] = text
    return list(entry)


def rods(metal):
    for m, r in RODS.items():
        research = f"rod_{m}"
        recipe("rod", "wand_" + m, {"type": "thaumaturge:infusion", "aspects": aspects(r["aspects"]), "catalyst": f"#c:ingots/{m}",
                                    "components": ["thaumaturge:salis_mundus", f"#c:ingots/{m}", r["extra"], crystal(r["primal"])],
                                    "instability": 3, "research": {"entry": f"{NS}:{research}"}, "result": {"id": f"{NS}:wand_rod_{m}"}}, research)
        recipe("rod", "staff_" + m, {"type": "thaumaturge:arcane_workbench_shaped", "crystals": [],
                                     "key": {"G": f"{NS}:wand_rod_{m}", "S": "thaumaturge:primal_charm"}, "pattern": ["  S", " G ", "G  "],
                                     "research": {"entry": "thaumaturge:staves"}, "result": {"id": f"{NS}:staff_rod_{m}"}, "vis": r["staff_vis"]}, research)
        simple_item(f"wand_rod_{m}", f"{NS}:item/wand_rod_{m}", "minecraft:item/handheld")
        simple_item(f"staff_rod_{m}", f"{NS}:item/staff_rod_{m}", "minecraft:item/handheld")
        lang[f"item.{NS}.wand_rod_{m}"] = metal(m) + " Rod"
        lang[f"item.{NS}.staff_rod_{m}"] = metal(m) + " Staff Core"
        lang[f"wand.thaumaturge.rod.{m}"] = metal(m)  # also names the staff: Thaumaturge drops "_staff"


def clusters(metal):
    for m in CLUSTERS:
        ore = f"#c:ores/{m}"
        recipe("cluster", m, {"type": "thaumaturge:crucible", "aspects": aspects([("metallum", 5), ("ordo", 5)]), "catalyst": ore,
                              "research": {"entry": "thaumaturge:metal_purification"}, "result": {"id": f"{NS}:cluster_{m}"}}, "native_clusters")
        for kind, time in (("smelting", 200), ("blasting", 100)):
            recipe("cluster", f"{m}_{kind}", {"type": "minecraft:" + kind, "category": "misc", "cookingtime": time, "experience": 1.0,
                                              "ingredient": f"{NS}:cluster_{m}", "result": {"id": f"{NS}:{m}_ingot", "count": 2}})
        dust = f"{NS}:{m}_dust"
        dump(res("data", NS, "recipe", "compat", "thaumaturge", "cluster", m + "_crushing.json"),
             {"type": "metallurgy:processing", "kind": "crusher", "ingredients": [f"{NS}:cluster_{m}"], "amounts": [1],
              "result": {"id": dust, "count": 3}, "experience": 0.5,
              "neoforge:conditions": COND + [{"type": "metallurgy:content_enabled", "content": dust}]})
        simple_item(f"cluster_{m}", f"{NS}:items/cluster_{m}")
        lang[f"item.{NS}.cluster_{m}"] = f"Native {metal(m)} Cluster"


def clone(src, dst, swap, extra=None):
    """Copy a Metallurgy model/item file, renaming the gadget inside it."""
    with open(src, "rb") as f:
        text = f.read().decode("utf-8").replace("\r\n", "\n")
    for a, b in swap:
        text = text.replace(a, b)
    value = json.loads(text)
    if extra:
        extra(value)
    dump(dst, value)


def gear():
    common_models = os.path.join(COMMON, "assets", NS, "models", "item")
    items = res("assets", NS, "items")
    models = res("assets", NS, "models", "item")

    def knuckle_texture(value):
        def walk(node):
            if isinstance(node, dict):
                if node.get("type") == "metallurgy:knuckles":
                    node["texture"] = f"{NS}:textures/items/gadgets/thaumium_knuckles.png"
                for v in node.values():
                    walk(v)
            elif isinstance(node, list):
                for v in node:
                    walk(v)
        walk(value)

    swap = [("brass_knuckles", "thaumium_knuckles")]
    clone(os.path.join(items, "brass_knuckles.json"), os.path.join(items, "thaumium_knuckles.json"), swap, knuckle_texture)
    for rel in ("brass_knuckles.json", "gadget/brass_knuckles.json", "gadget/brass_knuckles_hand.json", "gadget/brass_knuckles_item.json"):
        clone(os.path.join(common_models, rel), os.path.join(models, rel.replace("brass", "thaumium")), swap)
    swap = [("rubracium_gauntlet", "void_gauntlet")]
    clone(os.path.join(items, "rubracium_gauntlet.json"), os.path.join(items, "void_gauntlet.json"), swap)
    for rel in ("rubracium_gauntlet.json", "gadget/rubracium_gauntlet.json"):
        clone(os.path.join(common_models, rel), os.path.join(models, rel.replace("rubracium", "void")), swap)
    for metal in ("thaumium", "void"):
        swap = [("zinc_buckler", f"{metal}_buckler")]
        for rel in ("zinc_buckler.json", "zinc_buckler_blocking.json"):
            clone(os.path.join(items, rel), os.path.join(items, rel.replace("zinc", metal)), swap)
        for rel in ("zinc_buckler.json", "zinc_buckler_blocking.json", "gadget/zinc_buckler.json", "gadget/zinc_buckler_blocking.json"):
            clone(os.path.join(common_models, rel), os.path.join(models, rel.replace("zinc", metal)), swap)

    def arcane(name, pattern, key, research, vis):
        recipe("gear", name, {"type": "thaumaturge:arcane_workbench_shaped", "crystals": [], "key": key, "pattern": pattern,
                              "research": {"entry": f"{NS}:{research}"}, "result": {"id": f"{NS}:{name}"}, "vis": vis}, research)
    arcane("thaumium_knuckles", ["I I", "III"], {"I": "#c:ingots/thaumium"}, "thaumium_gadgets", 15)
    arcane("thaumium_buckler", [" T ", "TWT", " T "], {"T": "#c:ingots/thaumium", "W": "#minecraft:planks"}, "thaumium_gadgets", 15)
    arcane("void_gauntlet", ["L L", "III"], {"I": "#c:ingots/void_metal", "L": "#c:leathers"}, "void_gadgets", 30)
    arcane("void_buckler", [" V ", "VWV", " V "], {"V": "#c:ingots/void_metal", "W": "#minecraft:planks"}, "void_gadgets", 30)
    # Etherium Goggles take Thaumaturge's own goggles, so they come after them in progression.
    arcane("etherium_goggles", ["RER", "EGE"], {"E": "#c:ingots/etherium", "R": "minecraft:redstone", "G": "thaumaturge:goggles_revealing"},
           "etherium_goggles", 40)
    lang.update({f"item.{NS}.thaumium_knuckles": "Thaumium Knuckles", f"item.{NS}.void_gauntlet": "Void Gauntlet",
                 f"item.{NS}.thaumium_buckler": "Thaumium Buckler", f"item.{NS}.void_buckler": "Void Buckler",
                 f"tooltip.{NS}.thaumium_buckler": "A thaumium buckler: sturdier than zinc and as light. While active, it does not slow you, but it has a cooldown after each block. ",
                 f"tooltip.{NS}.void_buckler": "A void metal buckler that slowly mends itself. While active, it does not slow you, but it has a cooldown after each block. "})


def append_condition(rel, condition):
    """Add a condition to an existing Metallurgy recipe, keeping its line endings."""
    path = res("data", NS, "recipe", *rel.split("/"))
    with open(path, "rb") as f:
        raw = f.read().decode("utf-8")
    value = json.loads(raw)
    if condition not in value["neoforge:conditions"]:
        value["neoforge:conditions"].append(condition)
        nl = "\r\n" if "\r\n" in raw else "\n"
        with open(path, "wb") as f:
            f.write((json.dumps(value, indent=2) + "\n").replace("\n", nl).encode("utf-8"))


def quicksilver_and_swaps(thaumaturge_recipes):
    """Unified quicksilver: Thaumaturge's nugget is the nugget; nine of them make Metallurgy's ingot and back."""
    for rel in ("legacy/generated/item/shapeless/nugget_shapeless_quicksilver.json", "processing/alloyer/quicksilver.json"):
        append_condition(rel, NOT_UNIFIED)
    append_condition("legacy/gadget/etherium_goggles.json", NOT_COND)
    for name, value in (("quicksilver_nuggets", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ["#c:ingots/quicksilver"],
                                                 "result": {"id": "thaumaturge:nugget_quicksilver", "count": 9}}),
                        ("quicksilver_nugget_conversion", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                           "ingredients": [f"{NS}:quicksilver_nugget"], "result": {"id": "thaumaturge:nugget_quicksilver"}})):
        dump(res("data", NS, "recipe", "compat", "thaumaturge", name + ".json"), {**value, "neoforge:conditions": UNIFY})
    pack = res("resourcepacks", "thaumaturge")
    dump(os.path.join(pack, "pack.mcmeta"), {"pack": {"description": "Metallurgy recipe swaps for Thaumaturge", "min_format": [101, 1], "max_format": [101, 1]}})
    # Files here replace Thaumaturge's own; DynamicPackData.thaumaturge gives each its config switch.
    dump(os.path.join(pack, "data", "thaumaturge", "recipe", "nugget_quicksilver_from_nuggets.json"),
         {"neoforge:conditions": [{"type": "neoforge:never"}]})
    for name, (key, tag) in SWAPS.items():
        with open(os.path.join(thaumaturge_recipes, name + ".json"), encoding="utf-8") as f:
            value = json.load(f)
        value["key"][key] = tag
        value["neoforge:conditions"] = [{"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": tag[1:]}}]
        dump(os.path.join(pack, "data", "thaumaturge", "recipe", *name.split("/")) + ".json", value)


def research(metal, cap_metals):
    base = res("data", NS, "thaumaturge")
    dump(os.path.join(base, "research_category", "metallurgy.json"), {
        "background": "thaumaturge:textures/gui/gui_research_back_4.png",
        "formula": aspects([("metallum", 15), ("instrumentum", 10), ("fabrico", 10), ("ordo", 5), ("ignis", 5), ("terra", 5)]),
        "icon": f"{NS}:textures/items/tartarite_ingot.png", "index": 30,
        "overlay_background": "thaumaturge:textures/gui/gui_research_back_over.png", "required_research": "thaumaturge:metallurgy"})
    lang[f"research_category.{NS}.metallurgy"] = "Metallurgy"
    ores = sorted(f"{NS}:{os.path.splitext(f)[0]}" for f in os.listdir(res("data", NS, "loot_table", "blocks"))
                  if f.endswith("_ore.json") and f[:-9] in set(CLUSTERS) | {"silver", "tin", "copper", "sulfur", "phosphorite", "tar", "potash"})
    dump(res("data", NS, "tags", "block", "thaumaturge_scan_ores.json"), {"values": [{"id": o, "required": False} for o in ores]})
    dump(os.path.join(base, "scan", "ores.json"), {"blocks": f"#{NS}:thaumaturge_scan_ores", "key": f"{NS}:scanned/ores"})
    lang[f"research.{NS}.scanned/ores"] = "Metallurgy Ores"

    def entry(name, title, col, row, parents, icon, text0, text1, knowledge, notes, required=None):
        stage0 = {"text": f"research.{NS}.{name}.stage_0"}
        if knowledge:
            stage0["required_knowledge"] = [{"type": "theory", "category": "thaumaturge:" + knowledge, "amount": 1}]
        if required:
            stage0["required_research"] = required
        stages = [stage0]
        if name in recipe_ids:
            stages.append({"text": f"research.{NS}.{name}.stage_1", "recipes": recipe_ids[name]})
        dump(os.path.join(base, "research_entry", name + ".json"), {
            "category": f"{NS}:metallurgy", "name": f"research.{NS}.{name}.title", "parents": parents, "column": col, "row": row,
            "icons": [icon], "stages": stages, "note_aspects": aspects(notes), "complexity": 1 if not parents else 2})
        lang[f"research.{NS}.{name}.title"] = title
        lang[f"research.{NS}.{name}.stage_0"] = text0
        if name in recipe_ids:
            lang[f"research.{NS}.{name}.stage_1"] = text1

    entry("metallurgy", "Metallurgy", 0, 0, [], f"{NS}:tartarite_ingot",
          "The stones of this world hold dozens of metals the old texts never named. Look closely at their ores and the aspects in them begin to make sense.",
          "", None, [("metallum", 4), ("terra", 2)], [f"{NS}:scanned/ores"])
    entry("native_clusters", "Native Clusters", 0, 3, [f"{NS}:metallurgy", "thaumaturge:metal_purification"], f"{NS}:cluster_mithril",
          "Purification works on the new metals as well as the old ones. Drop one of their ores into a crucible with Metallum and Ordo.",
          "A native cluster smelts into two ingots, or crushes into three dusts in a Crusher.", "alchemy", [("metallum", 4), ("ordo", 4)])
    entry("caps_tier", "Metal Wand Caps", -2, 2, [f"{NS}:metallurgy", "thaumaturge:cap_gold"], f"{NS}:wand_cap_platinum",
          "Bronze, steel and platinum bind vis about as well as the metals already used for caps, each a little better than the last.",
          "Five nuggets make a cap. Bronze costs 5 percent more vis, steel the same, platinum 5 percent less.", "auromancy",
          [("metallum", 4), ("praecantatio", 3)])
    entry("caps_charged", "Charged Metal Caps", -2, 4, [f"{NS}:caps_tier", "thaumaturge:cap_thaumium"], f"{NS}:wand_cap_mithril",
          "Mithril, orichalcum and adamantine are too dense to hold vis as they are. Like thaumium, they must be charged first.",
          "Make the inert cap, then charge it on the infusion altar. Mithril costs 8 percent less vis, orichalcum 10, adamantine 15.",
          "auromancy", [("metallum", 4), ("potentia", 3), ("praecantatio", 3)])
    entry("caps_themed", "Elemental Caps", -4, 3, [f"{NS}:caps_tier"], f"{NS}:wand_cap_ignatius",
          "Some metals lean towards one element. A cap of such a metal spends that element more gently.",
          "Each costs the normal amount, but 20 percent less of one primal: Ignatius for Ignis, Ceruclase for Aqua, Shadow Steel for Perditio, "
          "Sanguinite for Terra, Midasium for Ordo and Etherium for Aer.", "auromancy", [("praecantatio", 4), ("ignis", 2), ("aqua", 2)])
    entry("caps_wacky", "Strange Caps", -4, 5, [f"{NS}:caps_themed"], f"{NS}:wand_cap_quicksilver",
          "A few metals do odd things to the vis that passes through them. None of them are reliable, which is part of the fun.",
          " ".join(f"{metal(m)}: {t}" for m, t in WACKY.items()), "auromancy", [("praecantatio", 4), ("permutatio", 4)])
    for m, r in RODS.items():
        entry(f"rod_{m}", f"{metal(m)} Rods", 2, 2 if m == "vulcanite" else 4,
              [f"{NS}:metallurgy" if m == "vulcanite" else f"{NS}:rod_vulcanite", r["parent"]], f"{NS}:wand_rod_{m}",
              "Vulcanite holds fire like a blaze rod: it glows, and it slowly refills Ignis." if m == "vulcanite" else
              "Eximite fell from beyond the world. A rod of it holds as much as silverwood and slowly gathers stray vis, quickest of all in the End.",
              "Infuse an ingot to make the rod. With the Staves research, two rods and a primal charm make a staff core.",
              "auromancy", [("praecantatio", 4), ("ignis" if m == "vulcanite" else "alienis", 4)])
    entry("thaumium_gadgets", "Thaumium Gadgets", 4, 1, [f"{NS}:metallurgy", "thaumaturge:metallurgy"], f"{NS}:thaumium_knuckles",
          "Thaumium takes the shapes of the old gadgets well, and hits a little harder for it.",
          "Thaumium Knuckles hit one point harder than brass. The Thaumium Buckler is sturdier than zinc.", "artifice",
          [("metallum", 4), ("instrumentum", 3)])
    entry("void_gadgets", "Void Gadgets", 4, 3, [f"{NS}:thaumium_gadgets", "thaumaturge:cap_void"], f"{NS}:void_gauntlet",
          "Void metal mends itself, and so does anything made from it.",
          "The Void Gauntlet works like the Rubracium Gauntlet and hits one point harder. The Void Buckler is the toughest buckler. Both slowly repair themselves.",
          "artifice", [("vacuos", 4), ("metallum", 3)])
    entry("etherium_goggles", "Etherium Goggles", 4, 5, [f"{NS}:metallurgy", "thaumaturge:unlock_artifice"], f"{NS}:etherium_goggles",
          "Etherium lenses fitted over Goggles of Revealing keep everything the goggles show and sharpen the wearer's focus.",
          "They reveal like the goggles they are built on and take 5 percent off vis costs. Etherium, Astral Silver, Amordrine and Mithril armour also "
          "take a little off, from 1 to 3 percent a piece.", "artifice", [("sensus", 4), ("aer", 2)])
    return cap_metals


def tags(cap_metals):
    caps = [f"{NS}:wand_cap_{m}" for m in cap_metals]
    dump(res("data", "thaumaturge", "tags", "item", "wand_caps.json"), {"values": [{"id": c, "required": False} for c in caps]})
    rods = [f"{NS}:{k}_rod_{m}" for m in RODS for k in ("wand", "staff")]
    dump(res("data", "thaumaturge", "tags", "item", "wand_rods.json"), {"values": [{"id": r, "required": False} for r in rods]})
    dump(res("data", "thaumaturge", "tags", "item", "clusters.json"), {"values": [{"id": f"{NS}:cluster_{m}", "required": False} for m in CLUSTERS]})


def write_lang():
    with open(LANG, "rb") as f:
        raw = f.read().decode("utf-8")
    nl = "\r\n" if "\r\n" in raw else "\n"
    current = json.loads(raw)
    current.update(lang)
    with open(LANG, "wb") as f:
        f.write((json.dumps(current, indent=2, ensure_ascii=False) + "\n").replace("\n", nl).encode("utf-8"))
    print("lang:", len(lang), "keys")


def main():
    if len(sys.argv) != 2:
        sys.exit("usage: generate_thaumaturge_content.py <folder with Thaumaturge's data/thaumaturge/recipe>")
    metal = names()
    cap_metals = caps(metal)
    rods(metal)
    clusters(metal)
    gear()
    quicksilver_and_swaps(sys.argv[1])
    research(metal, cap_metals)
    tags(cap_metals)
    write_lang()
    print("recipes:", sum(map(len, recipe_ids.values())), "in research;", "entries:", len(recipe_ids))


if __name__ == "__main__":
    main()
