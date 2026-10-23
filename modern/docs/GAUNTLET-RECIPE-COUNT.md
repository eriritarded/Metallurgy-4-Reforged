# Rubracium gauntlet recipe output — 2026-10-18

## Report and cause

The user reports that the 1.21.1 recipe
`metallurgy:legacy/gadget/rubracium_gauntlet` fails to decode because its output
count is two, above the item's maximum stack size of one. The maintained26.1.2
recipe has the same count; the prior user-run26.1.2 log also recorded that
failure. The importer copied the original legacy result count without adapting
it to the newer recipe output validation.

`content/Content.java` registers the gauntlet as damageable, with configurable
durability (default750), so its native maximum stack size remains one. Making it
stackable would conflict with its equipment/durability role.

## Correction

| Target | Recipe output | State |
|---|---|---|
| NeoForge1.21.1 port.9 | One Rubracium gauntlet | Implemented and packaged; archive inspected |
| NeoForge26.1.2 port.16 | One Rubracium gauntlet | Implemented, packaged and installed; hash verified; runtime unconfirmed |
| Forge1.20.1 port.8 | Existing legacy recipe retained | No new package |

The pattern remains `L L` / `III`: three tagged Rubracium ingots and two Leather
make one gauntlet. A pair requires two crafts, costing six ingots and four
Leather. This changes the modern recipe's yield from its invalid two-item result
to one; ingredients, pattern and content-enabled condition remain unchanged.
Dual-wield mechanics and automatic spare-to-offhand behavior remain unchanged.
Item IDs, durability, damage, models, textures, networking and saves are unchanged.
No migration or configuration is needed. Existing valid gauntlets are unaffected.

`tools/convert_recipes.py` applies this conversion for the two component-era
targets, so future imports do not restore the invalid count. The crafting
coverage record also records the adapted yield. The Forge legacy recipe is
retained separately.

## Evidence and release limits

This is a resource-only correction over the existing production packages. Only
the one recipe, native version metadata and source/build provenance may differ;
all classes, PNGs and other recipe resources must remain byte-identical. Source
JARs receive the same resource correction. Package identities and source commit
are recorded below.

No game, native recipe decode or tests are run by this task. JSON/archive
inspection does not establish successful live loading, crafting or JEI display.
After the user's separate installation request, native26 port.16 is installed in
Simply NeoForged. The1.21.1 port.9 package remains built only.

## Packaged result

Source commit: `0bbcb4c1e88c32578bcb28c6f3b9a0fabfd8b7d2`.
`tools/package_gauntlet_recipe.py` packages the two native targets over their
hash-pinned production/source pairs and records exact archive deltas. It checks
that only the output count differs semantically in the recipe. There was no Java
compilation, reobfuscation or new class output.

| Playable artifact in `artifacts/` | SHA-256 | Byte-identical preserved entries |
|---|---|---|
| `Metallurgy-Reforged-MC-1.21.1-neoforge-1.4.1-port.9.jar` | `288ea58a4341372e9653d349aed667fd2f16870e83088feb137d1b238d97400a` | All 259 classes, 1,086 PNGs and 1,310 other recipe JSONs |
| `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.16.jar` | `7aadcabee70bd9bfe26a5bc836ae10de72afa92975212183cc8b4c8e6719a3e0` | All 266 classes, 1,175 PNGs and 1,282 other recipe JSONs |

Every other archive entry is preserved except native version metadata and the
embedded build-provenance JSON. No paths were added/removed. The existing atlas,
input, drop-hook, gauntlet rendering and earlier fixes remain in the corresponding
packages. Forge1.20.1 port.8 and its source pair are unchanged.

The [package receipt](../SOURCE-PROVENANCE.md#historical-evidence) includes both
source archives, hashes, baselines, class targets and inspection results.
All four disposable staging archives were removed from the owned H: directory;
see [cleanup](../SOURCE-PROVENANCE.md#historical-evidence). Native recipe loading,
crafting and JEI behavior remain unconfirmed for these new packages.

## Installation — 2026-10-18

At the user's request, native26 port.16 replaced port.15 in Simply NeoForged at
`2026-10-18T13:07:18.3292307-04:00`. The installed SHA-256 matches the ordinary
playable package above. Port.15 is preserved under
`artifacts/backups/gauntlet-recipe-port15/` in the directory named by its SHA-256
`0267ceaebed785679ae692d342ef448105c9f19421dc29bc26bbea434a6862ea`.

All 153 other mod-folder files retain their exact hashes. Launcher configuration
and real saves were unchanged. Only the native26 playable package was installed;
no other target or source JAR was installed. No game was launched or reloaded,
so successful live recipe loading and crafting remain unconfirmed. See
[the installation and rollback receipt](../SOURCE-PROVENANCE.md#historical-evidence).
