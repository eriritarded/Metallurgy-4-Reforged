# Advancement tree

## Wiki-icon correction, 2026-10-18

The port.12 main-instance log rejected both root icons because they referenced removed `metallurgy:wiki`; their dependent trees failed to load. Port.7/7/13 replaces the main root icon with `metallurgy:copper_ingot` and the challenge root icon with `metallurgy:tartarite_ingot`, retaining all advancement IDs and criteria. The converter preserves this correction. Native26 port.13 is installed alongside the separately repaired Genetic Industry addon. See [the repair](WORLD-LOGIN-REPAIR.md) and `evidence/bees-login-release.json`. Resource packaging/static inspection passed; decoding and progression with the replacement remain unobserved. The older compilation/source-adaptation record below is historical.

Source: Metallurgy1.4.1 commit `ef94cb94cfc08ccebe8a0df307ce6b390dcd4704`, all15 files under `advancement`, the65 JSON files under `assets/metallurgy/advancements`, `block/BlockOre`, and the referenced equipment effects. `tools/convert_advancements.py` records each JSON input hash and destination in `advancement-coverage.json`.

## Maintained adaptation

All65 source advancement files are converted per target, retaining IDs, parent tree, translated titles/descriptions, rewards, requirement groups and criteria. Native item/tag predicates replace the removed Forge OreDictionary predicates. Tartarite's `paragon_absorbed` predicate uses original NBT on1.20.1 and the native custom-data subpredicate on the component-based targets. Icon item syntax and singular advancement resource paths follow the target version. Original language data is retained.

`Advancements` provides the eight original trigger IDs: `harvest_level`, `jack_of_all_trades`, `yin_yang`, `low_temperatures`, `overclocked_tools`, `loyal_friends`, `strong_opponent`, and `advancement_granted`. Forge uses native `SimpleCriterionTrigger` instances and main-thread common-setup registration. NeoForge uses the trigger registry and native record codecs;26 uses the renamed `criterion` package. Modern player predicates remain supported. Harvest levels compare for equality, preserving the source rule; advancement-granted conditions match the full advancement ID.

Server callbacks retain:

- Actual successful metal-ore destruction triggers that ore's harvest tier.
- Four distinct base metal armor pieces trigger Jack of All Trades; two Sanguinite plus two Amordrine trigger Yin Yang. The replacement slot is used explicitly in the equipment-change event; absorbed Tartarite counts as its base material, as in the original dispatcher.
- Killing an entity wearing a Metallurgy chest piece triggers Strong Opponent, including indirect damage credited to the player.
- Completed advancements dispatch their IDs so Metal Rainbow tracks all49 individual material advancements.
- Existing loyalty/defense and Ceruclase weapon callbacks now fire the native criteria instead of force-completing every criterion of a fixed advancement file. Ceruclase's matching-tool speed path, Electrum's enabled voltage-control break and Sanguinite's active-cooldown harvest additionally dispatch the original challenges.

The generic native trigger implementation replaces the old listener/base wrapper classes without dropping their conditions. Server authority and native advancement persistence remain in Minecraft. Data packs can reuse the original trigger IDs. Registration availability retains saved IDs, so the source tree is retained even if a category is unavailable through crafting.

All three target compilers passed, with exact matching staged-source/resource manifests in `evidence/advancements-confirmed-compile-checkpoint.json`. Disposable builds and raw logs were removed after recording the receipt. No resource reload, predicate decoding, progression, reward, save/reload or client advancement-screen check has run. No game or tests were launched, and no installed package/profile was changed.
