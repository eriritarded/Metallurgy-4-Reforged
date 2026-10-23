# Optional Metallurgist's Handbook

## Player use

Install Patchouli for the matching Minecraft/loader version. Craft **one Book + one Minecraft Copper Ingot**, shapeless, to receive **The Metallurgist's Handbook**. Right-click it to open the index and search. It also appears in the Metallurgy creative tab while Patchouli is present.

The book is `metallurgy:handbook`, carried by Patchouli's `patchouli:guide_book` item. No replacement Metallurgy item is registered. Without Patchouli, the loader condition removes the recipe before its optional serializer/component is decoded, and Patchouli's book loader/item are absent. Metallurgy has an optional metadata dependency only; no Patchouli Java API is linked or bundled.

## Contents

Eight categories cover getting started, materials, machines, metal chests, equipment, gadgets, fluids/resources, and installed integrations. The integrations category is hidden when no supported integration mod is present.

- All49 metals: deposits/height/mining requirements or exact alloy ratios, supplied tool/armor stats, and the active source trait descriptions.
- Crusher, Alloyer and Sublimation Chamber: operation, fuel, automation and experience, with crafting pages. The chamber's14 stock block batches list real ingredient counts and duration; its applied aura is levelI, following the current source behavior.
- All five chest tiers:54/72/81/90/108 slots and crafting pages.
- Equipment controls, Tartarite absorption, repairs, detectors, magnets, knuckles, gauntlets, three bucklers, shields, lighters, goggles, fixtures, molten fluids, tar, thermite and fertilizer.
- Recipe pages reference the target's actual native crafting recipe IDs. Processing batches and material statistics describe supplied defaults; server recipe/configuration overrides take priority. SHIFT/CTRL tooltips remain the place to inspect current equipment.

Generated content totals, including all possible integration entries:

| Target | Entries | Pages | Optional integration entries |
|---|---:|---:|---:|
|1.20.1 Forge, port.7|97|419|13|
|1.21.1 NeoForge, port.7|93|411|9|
|26.1.2 NeoForge, port.13|89|403|5|

Only entries for loaded mods are displayed. Each integration entry has Patchouli's `mod:<modid>` flag; the category uses an OR of that target's supported mod flags. This documents supported integrations without promising unavailable APIs on newer targets. A present mod's chapter also explains its integration settings; presence alone does not override a disabled integration setting.

| Target | Integration chapters |
|---|---|
|All three|JEI, Silent Gear, Silent's Gems, Ender IO, Mekanism|
|Additional on1.20.1 and1.21.1|Immersive Engineering, Industrial Foregoing, ProjectE, CraftTweaker|
|Additional on1.20.1|Tetra, Tinkers' Construct, Spartan Weaponry, Thermal|

## Implementation and references

`content/handbook-topics.json` contains authored explanations. `tools/generate_handbook.py` combines them with the maintained material catalog, worldgen defaults, effect roster/translations and native recipe data. `docs/handbook-content.json` records generated coverage. The ordinary resource and legacy recipe generators call it last to retain the handbook after regeneration.

Book roots live in each target's `data/metallurgy/patchouli_books/handbook/book.json`; categories and entries live under `assets/metallurgy/patchouli_books/handbook/en_us`. Native26 also supplies an item-model definition for Patchouli's model lookup. The cover reuses the original `textures/items/wiki.png` pixel art through the new `handbook` model; no generated bitmap is introduced.

Reviewed official [Patchouli1.20 resource layout](https://vazkiimods.github.io/Patchouli/docs/upgrading/upgrade-guide-120/), [book recipes](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/recipe/), and [mod flags](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/config-gating/). The1.21 upstream `PatchouliItems`/`PatchouliDataComponents` source confirms `patchouli:guide_book` and persistent `patchouli:book`. The installed `patchouli-neoforge-26.1-94.jar` confirms the same identifiers, book root/content loading and mod flags. Forge1.20.1 uses Patchouli's shapeless-book serializer; the two Neo targets use native shapeless output components. No dependency classes or assets are packaged.

## Wiki item removal and saves

The old `metallurgy:wiki` registration, crafting recipe, item model/definition and clickable-link action are removed on all three targets. The original cover PNG is retained for the handbook. Existing wiki stacks have no registered item in this version and are not converted into handbook stacks. No real save is edited. Rollback to the preserved old JAR restores its registry, but cannot recover items already discarded by a save made without that item; keep normal world backups before changing mods.

## State

The initial handbook packages were port.2/2/8, recorded in `evidence/handbook-release.json`. Port.3/3/9 improved the detector entry; port.4/4/10 and5/5/11 retained that content while adding magnet feedback and correcting goggles performance. Port.6/6/12 retained all handbook content byte-for-byte while adding the missing JEI category title key. These resource-only packages retain every compiled class from the prior production packages and passed archive inspection. Native26 port.12 was installed in Simply NeoForged. See `evidence/jei-lang-release.json` and `evidence/jei-lang-cleanup.json`.

The new book and visual changes have not been observed in an assistant-launched Minecraft client. Port.7's historical world-load success applies only to that exact JAR.

Port.7/7/13 retains the handbook content and cover artwork while replacing the two stale wiki-item advancement icons with registered base-mod ingots. Native26 port.13 is installed; see `evidence/bees-login-release.json`. The book stays optional and does not supply advancement root icons.
