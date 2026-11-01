"""Generate the optional Thaumaturge data for the NeoForge 26.1.2 target.

    python tools/generate_thaumaturge_data.py

Writes Thaumaturge data maps under neoforge-26.1.2/src/main/resources/data/thaumaturge.
Thaumaturge only reads these files when it is installed, and every entry also carries
the integration_enabled condition, so the "Thaumaturge Integration" config switch turns them off. Edit the ASPECTS table here, never the generated JSON.
See docs/THAUMATURGE.md.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "neoforge-26.1.2", "src", "main", "resources")
LANG = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "metallurgy", "lang", "en_us.json")

CONDITIONS = [{"type": "metallurgy:integration_enabled", "integration": "thaumaturge"}]

# Ingot aspects per metal: Metallum by tier, plus the metal's theme.
ASPECTS = {
    # Overworld ores, low tier
    "copper": {"metallum": 10, "permutatio": 5},
    "tin": {"metallum": 10, "vitreus": 5},
    "zinc": {"metallum": 10, "praemunio": 3},
    "deep_iron": {"metallum": 15, "tenebrae": 5, "terra": 3},
    "prometheum": {"metallum": 10, "herba": 5, "victus": 3},
    "infuscolium": {"metallum": 10, "tenebrae": 5, "praecantatio": 3},
    "manganese": {"metallum": 10, "ordo": 5},
    "osmium": {"metallum": 15, "terra": 5},
    "silver": {"metallum": 10, "desiderium": 5},
    "astral_silver": {"metallum": 10, "lux": 10, "auram": 5},
    "oureclase": {"metallum": 10, "potentia": 10},
    "rubracium": {"metallum": 10, "instrumentum": 5, "aversio": 3},
    "carmot": {"metallum": 15, "desiderium": 10, "praecantatio": 5},
    "orichalcum": {"metallum": 20, "praemunio": 10},
    "platinum": {"metallum": 15, "desiderium": 15},
    "mithril": {"metallum": 20, "praecantatio": 10},
    "adamantine": {"metallum": 25, "ignis": 10, "praemunio": 10},
    "atlarus": {"metallum": 25, "potentia": 10, "desiderium": 5},
    "lutetium": {"metallum": 20, "potentia": 10, "vitreus": 5},
    # Nether ores
    "ignatius": {"metallum": 10, "ignis": 10},
    "shadow_iron": {"metallum": 15, "tenebrae": 10},
    "ceruclase": {"metallum": 15, "gelum": 10},
    "kalendrite": {"metallum": 15, "tenebrae": 5, "praecantatio": 5},
    "midasium": {"metallum": 10, "desiderium": 20},
    "vulcanite": {"metallum": 15, "ignis": 15},
    "sanguinite": {"metallum": 20, "victus": 10, "mortuus": 5},
    "vyroxeres": {"metallum": 15, "mortuus": 5, "herba": 5},
    "alduorite": {"metallum": 20, "ignis": 5, "vitreus": 5},
    "lemurite": {"metallum": 15, "motus": 5, "vitreus": 5},
    # End ores
    "eximite": {"metallum": 15, "alienis": 10},
    "meutoite": {"metallum": 15, "alienis": 15, "vacuos": 5},
    # Alloys (the alloyer is invisible to Thaumaturge's recipe scan, so these are set by hand)
    "angmallen": {"metallum": 20, "desiderium": 5},
    "black_steel": {"metallum": 25, "tenebrae": 5, "praemunio": 5},
    "brass": {"metallum": 10, "instrumentum": 5},
    "bronze": {"metallum": 15, "praemunio": 5},
    "celenegil": {"metallum": 25, "praemunio": 10, "desiderium": 5},
    "damascus_steel": {"metallum": 20, "instrumentum": 5, "ordo": 5},
    "desichalkos": {"metallum": 25, "alienis": 15, "praecantatio": 5},
    "electrum": {"metallum": 15, "desiderium": 10, "potentia": 5},
    "etherium": {"metallum": 25, "praecantatio": 10, "alienis": 5},
    "haderoth": {"metallum": 25, "ignis": 10, "praecantatio": 5},
    "hepatizon": {"metallum": 20, "tenebrae": 5, "desiderium": 5},
    "inolashite": {"metallum": 20, "gelum": 5, "praecantatio": 5},
    "krik": {"metallum": 20, "aer": 10, "motus": 5},
    "quicksilver": {"metallum": 15, "aqua": 5, "permutatio": 10},
    "shadow_steel": {"metallum": 20, "tenebrae": 10, "praemunio": 5},
    "steel": {"metallum": 20, "ordo": 5},
    "tartarite": {"metallum": 30, "ignis": 10, "mortuus": 5, "praemunio": 10},
    "amordrine": {"metallum": 20, "praecantatio": 10, "desiderium": 5},
}

# Metallurgy's own iron/gold dusts; the ingots, nuggets and blocks are vanilla and already aspected.
VANILLA_DUSTS = {"iron": {"metallum": 15, "perditio": 1}, "gold": {"metallum": 10, "desiderium": 10, "perditio": 1}}

# Non-metal resources and their ores.
OTHER = {
    "metallurgy:sulfur_dust": {"ignis": 5, "alkimia": 5},
    "metallurgy:sulfur_block": {"ignis": 15, "alkimia": 15, "ordo": 5},
    "metallurgy:phosphorus": {"lux": 10, "ignis": 5},
    "metallurgy:potash": {"terra": 5, "alkimia": 3, "herba": 3},
    "metallurgy:potash_fertilizer": {"herba": 10, "victus": 5},
    "metallurgy:bitumen": {"terra": 5, "ignis": 5, "tenebrae": 3},
    "metallurgy:bitumen_block": {"terra": 15, "ignis": 15, "tenebrae": 9, "ordo": 5},
    "metallurgy:tar": {"aqua": 5, "tenebrae": 5, "vinculum": 3},
    "metallurgy:thermite_dust": {"ignis": 15, "potentia": 10},
    "metallurgy:thermite_nugget": {"ignis": 2, "potentia": 1},
    "metallurgy:charcoal_block": {"ignis": 30, "potentia": 30, "ordo": 5},
    "#c:ores/sulfur": {"terra": 5, "ignis": 5, "alkimia": 5},
    "#c:ores/phosphorus": {"terra": 5, "lux": 10, "ignis": 5},
    "#c:ores/potash": {"terra": 10, "alkimia": 3, "herba": 3},
    "#c:ores/tar": {"terra": 5, "ignis": 5, "tenebrae": 3},
}

# Ore tags Thaumaturge already maps; adding them again would clash.
THAUMATURGE_ORE_KEYS = {"copper", "iron", "gold"}


def entry(aspects):
    return [{"aspect": "thaumaturge:" + a, "amount": n} for a, n in aspects.items() if n > 0]


def scaled(aspects, factor):
    return {a: max(1, round(n * factor)) for a, n in aspects.items()}


def write(path, values):
    path = os.path.join(RES, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        # NeoForge data maps take conditions per entry, not per file.
        wrapped = {k: {"neoforge:conditions": CONDITIONS, "neoforge:value": v} for k, v in values.items()}
        json.dump({"values": wrapped}, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(path, ROOT), len(values), "entries")


def main():
    with open(LANG, encoding="utf-8") as f:
        lang = json.load(f)

    def exists(item):
        ns, path = item.split(":")
        return f"item.{ns}.{path}" in lang or f"block.{ns}.{path}" in lang

    ore_tags = {n[:-5] for n in os.listdir(os.path.join(RES, "data", "c", "tags", "item", "ores"))}
    values, missing = {}, []

    def put(key, aspects):
        if key.startswith("#") or exists(key):
            values[key] = entry(aspects)
        else:
            missing.append(key)

    for metal, ingot in ASPECTS.items():
        put(f"metallurgy:{metal}_ingot", ingot)
        put(f"metallurgy:{metal}_nugget", scaled(ingot, 1 / 7))
        put(f"metallurgy:{metal}_dust", {**ingot, "perditio": 1})
        put(f"metallurgy:{metal}_block", {**scaled(ingot, 3), "ordo": 5})
        if metal in ore_tags and metal not in THAUMATURGE_ORE_KEYS:
            put(f"#c:ores/{metal}", {**ingot, "terra": 5})
    for metal, dust in VANILLA_DUSTS.items():
        put(f"metallurgy:{metal}_dust", dust)
    for key, aspects in OTHER.items():
        put(key, aspects)
    if missing:
        raise SystemExit(f"unknown items: {missing}")
    write("data/thaumaturge/data_maps/item/base_aspects.json", values)

    # Infernal Furnace: 33% chance of 2 bonus nuggets, as Thaumaturge does for vanilla ores.
    bonus = {}
    for metal in sorted(ore_tags):
        if metal in ASPECTS and metal not in {"copper", "silver", "tin"} and exists(f"metallurgy:{metal}_nugget"):
            bonus[f"#c:ores/{metal}"] = [{"chance": 0.33, "count": 2, "items": f"#c:nuggets/{metal}"}]
    write("data/thaumaturge/data_maps/item/infernal_bonus.json", bonus)
    unify_brass()


UNIFY = [{"type": "neoforge:mod_loaded", "modid": "thaumaturge"},
         {"type": "metallurgy:integration_enabled", "integration": "thaumaturge_unify"}]
NOT_UNIFIED = {"type": "neoforge:not", "value": {"type": "neoforge:and", "values": UNIFY}}


def unify_brass():
    """Both mods' brass share c:ingots/brass. Unified, Thaumaturge's crucible brass is the only brass made."""
    for rel in ("processing/alloyer/brass.json", "smelting/brass_dust.json"):
        path = os.path.join(RES, "data", "metallurgy", "recipe", rel)
        with open(path, "rb") as f:
            raw = f.read().decode("utf-8")
        recipe = json.loads(raw)
        if NOT_UNIFIED not in recipe["neoforge:conditions"]:
            recipe["neoforge:conditions"].append(NOT_UNIFIED)
            nl = "\r\n" if "\r\n" in raw else "\n"  # keep the file's own line endings
            with open(path, "wb") as f:
                f.write((json.dumps(recipe, indent=2) + "\n").replace("\n", nl).encode("utf-8"))
            print("unify condition added to", rel)
    path = os.path.join(RES, "data", "metallurgy", "recipe", "compat", "thaumaturge", "brass_dust_smelting.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"type": "minecraft:smelting", "ingredient": "metallurgy:brass_dust",
                   "result": {"id": "thaumaturge:ingot_brass"}, "experience": 0.6, "cookingtime": 200,
                   "neoforge:conditions": UNIFY}, f, indent=2)
        f.write("\n")


if __name__ == "__main__":
    main()
