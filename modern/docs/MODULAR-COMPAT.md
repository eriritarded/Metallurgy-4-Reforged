# Tetra and Silent's Gems / Silent Gear

## State and references

Source implemented on2026-10-16 during the user's initial launch hold. After the user authorized finishing the port with minimal game launches, the base adapter code compiled on the exact three targets. No dependency loading, game launch, compatibility playtest, production package or installation was performed for this addition. Actual Tetra/Silent Gear material decoding, assembly and rendering remain unverified. Current launch constraints are recorded in `launch-hold.json`.

The [source baseline](modular-compat-baseline.json) pins upstream revisions and retrieved file blob hashes. The [coverage manifest](modular-compat-coverage.json) and [material catalog](MODULAR-MATERIALS.md) describe source resources, not a loaded or verified binary. The full Metallurgy port remains unfinished.

## Targets and activation

| Target | Tetra | Silent Gear | Silent's Gems |
|---|---|---|---|
|1.20.1 / Forge 47.4.10|39 native 6.x materials; metadata 6.12–<7. Public 6.x source inspected; listed 6.17 binary not inspected.|41 native 3.6 materials; metadata 3.6.7–<4.|4.7–<5; modern tools come from Silent Gear.|
|1.21.1 / NeoForge 21.1.262|No compatible upstream release found; no Tetra adapter packaged.|41 native 4.2 materials; metadata 4.2.1–<5.|5.1.3–<6; native gem settings coexist with added metals.|
|26.1.2 / NeoForge 26.1.2.122|No compatible upstream release found; no Tetra adapter packaged.|41 native 4.2 materials; metadata 4.2.3–<5.|5.1.4–<6; native gem settings coexist with added metals.|

These ranges declare the targeted data/API generation, not certification of every release in the range. Silent Lib and Tetra's Mutil remain dependencies of their upstream mods; Metallurgy does not package or install them.

All compatibility dependencies are optional. No optional Java API class is referenced. Tetra reads its own `data/tetra/materials` and `data/tetra/tiers` directories; Silent Gear reads `data/*/silentgear_materials`. These resources now live in separate bundled packs under `resourcepacks/tetra` and `resourcepacks/silentgear`, mounted through native `AddPackFindersEvent` only when the supported dependency is loaded and its config switch is enabled. Tetra uses the new restart-scoped `tetraIntegration` switch; the original `silentGemsIntegration` switch controls the modern Silent Gear successor. No Tetra pack is present on the two unsupported Neo targets. Active compatibility packs are required built-in data packs with bottom priority, allowing ordinary higher-priority user data packs to override definitions.

This layout is necessary because the reviewed Gear20 material manager does not evaluate Forge recipe conditions, and Tetra's material store is its own data loader. A recipe condition would leave material definitions loaded. The old root copies were removed only after exact hash comparisons with the relocated definitions; `evidence/modular-pack-layout-migration.json` records163 files and confirms no profile changes. Pack format15 is used for1.20.1,48 for1.21.1, and native min/max101.1 for26.1.2, read from the exact target sources. Pack registration compiles on all three targets in `evidence/compat-packs-confirmed-compile-checkpoint.json`; actual mounting/decoding remains unverified.

Without the corresponding mod or with its switch disabled, the compatibility pack is not registered. No foreign serializer is added to vanilla's recipe registry. Gear support activates with Silent Gear alone; Gems is not required for the metal materials. Gems alone does not gain an invented standalone tool system. Changing compatibility configuration after creating modular equipment may remove its material data; no cross-version save migration is claimed.

The original 1.12 integration registered 39 `MetallurgyToolPart` objects through `net.silentchaos512.gems.api.tool.part.ToolPartRegistry`. Silent's Gems' [modern README](https://github.com/SilentChaos512/SilentGems/blob/56daee7c83558d64b1e7cfb3594ad8d3afc57fe1/README.md) directs tool/armor users to Silent Gear. The replacement uses its native material loaders instead of the removed Gems API or renamed Tinkers parts.

## Material eligibility and ownership

- The 39 original tool metals receive Tetra material definitions and Silent Gear main/rod/tip definitions.
- Lutetium and Osmium have armor statistics but no tool statistics. They receive Silent Gear materials restricted from tool/projectile gear, without invented tool stats or Tetra entries.
- Alduorite, Infuscolium, Lemurite, Manganese, Meutoite, Rubracium, Tin and Zinc have neither tool nor armor stats. They remain alloy ingredients/decorative metals without fabricated equipment properties.
- Gear IDs are `metallurgy:<metal>`. Tetra keys are `metallurgy_<metal>` under `data/tetra/materials/metallurgy/`. Existing definitions such as another addon's copper or silver are not replaced.
- Crafting/repair identify `metallurgy:<metal>_ingot` explicitly, limiting these definitions to our ingots. The receiving loader still chooses among any competing predicates. Current native resource managers sort IDs, and the inspected Gear managers preserve insertion order, placing same-named `metallurgy` resources before `silentgear` resources. Custom competing packs/add-ons and material synchronization remain unverified.

No upstream metal or gem definitions are overwritten. Native recipes create parts, and the receiving mod owns repair, salvage, mixing, blueprints, upgrades and persistence. No free-output or ingot-conversion recipes are added.

## Tetra design

Native material JSON supplies these exact mappings:

- primary = source tool damage + 3;
- secondary = 2.8 + source block hardness / 3;
- tertiary = source efficiency / 2, limited to 1–6;
- durability and mining efficiency = original tool values;
- magic capacity = 6 × source enchantability;
- integrity cost = max(2, floor((harvest level + 2) / 2)); integrity gain = min(8, 3 + harvest level);
- glyph/texture tints = original metal RGB, using native metal/default module artwork;
- hammer requirement: gold through harvest level 2, iron through 4, diamond through 6, netherite at 7.

Primary/secondary/tertiary are Tetra module multipliers, not flat vanilla attributes. Integrity and hammer requirements adapt the metals to workbench progression. Effects use Tetra's native levels, efficiencies and stacking: Carmot extraction, Damascus bleeding, Etherium reach, Silver armor penetration, Sanguinite blood-for-durability, and the other profiles in the catalog. Material-derived schematic outcomes supply native repair definitions.

## Silent Gear design

The 1.20 adapter emits `silentgear:standard` with `availability`, `crafting_items`, `stats`, `traits` and tinted native model layers. Newer adapters emit `silentgear:simple` with an explicit empty parent, `crafting`, `display` and namespaced part `properties`. 26.1.2 uses string ingredients and required equippable display information; 1.21.1 uses its object ingredient codec.

Main parts use original tool durability, mining speed, damage, enchantability, armor durability, per-slot armor and toughness. Ranged damage/projectile speed/projectile accuracy use neutral 1 multipliers, and attack/draw-speed modifiers use 0. No original ranged statistic exists, so melee damage is not relabeled as projectile damage. The old unused magic-damage value is not converted into a new combat bonus. Forge availability tier follows source harvest level. Newer native harvest-tier properties reference exact Metallurgy incorrect-block tags.

Rod bonus: +15% of original tool durability. Tip bonuses: +15% durability, +10% mining speed, +15% damage; newer native tips also carry that metal's harvest tier. Bonuses are additive part properties, not another complete tool head. Modern main materials allow native gem settings, using Silent's Gems' own existing material definitions.

Profiles use upstream native traits, including their gear restrictions, level caps and stacking. Subordinate-part trait levels decrease by 1, with minimum 1. Traits require the native trait-bearing-component ratio of 0.5; Platinum's main indestructible trait requires 0.75 and is entirely omitted from rods/tips. This condition counts trait-bearing components using Gear's own implementation; it does not measure exact metal mass percentage. Examples: Deep Iron aquatic/swift swim, Ignatius fiery/magmatic/fireproof, Midasium greedy/lucky, Vyroxeres venom and Tartarite synergistic/sturdy.

These are native material adaptations. Original full-set abilities, summoned knights, phasing, extra jumps, Tartarite absorption and the original Amordrine Divine Grace callback are not attached to modular parts. Ordinary Metallurgy item-ID effects do not run on modular tools, preventing duplicate damage/repair callbacks. No indirect-damage Divine Grace path is added, preserving issue 388's direct-melee boundary.

## Forge ore correction

Previously `Content.MetalOre.canHarvestBlock` required `item instanceof PickaxeItem` and read that class's fixed tier. Tetra's assembled items need not be that class; modular placeholder tiers can differ from assembled tiers.

Forge-only `compat/ModularTiers.java` registers harvest levels 5, 6 and 7 with Forge 47.4.10's `TierSortingRegistry` when Tetra or Gear exists. They sort above netherite and, if Tetra exists, below its special maxed forge hammer. Exact `metallurgy:needs_level_5/6/7` tags identify source ore requirements. Tetra's native tier list includes these three IDs.

For `tetra` and `silentgear` items, the ore hook requires both the native pickaxe action and the assembled stack's correct-tool-for-drops result. Arbitrary items from those mods do not qualify. Other items retain the existing fixed-tier check. NeoForge already uses native incorrect-block tags, so its source generator deliberately excludes the Forge-only class.

## Generation, saves and pending evidence

`tools/generate_modular_compat.py` writes maintained source resources and the material catalog. `generate_resources.py` invokes it so later resource generation retains the integration. Loader metadata declares optional dependencies and orders Metallurgy after each available upstream mod.

This addition to an unfinished port does not migrate 1.12 modular items/worlds. Receiving mods persist the new material IDs. Removing/renaming compatibility data after crafting could invalidate those items; preserve save backups before a later rollback. No real saves have been changed.

Proposed checks include dependency-absent startup, exact dependency sets, material decoding counts, actual assembly/repair, low-tier rejection, high-tier drops, mixed-part traits/caps, salvage quantities, server sync, save/reload, connected-client tints and tooltips. Static generation, base compilation and older core fixtures do not establish these behaviors. No compatibility test harness was added or run during this work.

## Change history

- 2026-10-16: added separate native Tetra and Silent Gear adapters, 39/41 material coverage, original palette tints, part bonuses/traits, Forge modular ore eligibility and sorted high tiers. Modernized the old Silent's Gems integration through the tool system's actual successor. Source-only checkpoint under the user's launch hold.
## Runtime material configuration follow-up

Implemented source2026-10-16: bundled Tetra/Silent Gear data now passes through `ConfiguredPack`/`DynamicPackData` in memory. Custom material durability, damage, speed, enchantability, armor durability/defense/toughness, harvest tier and RGB tint are read from the same restart-scoped `Materials` definitions as native equipment. Tetra integrity/magic/hammer requirements and Silent Gear rod/tip bonuses are recomputed using the documented formulas. Fixed thematic native trait profiles remain explicit adaptations. Pack files on disk are not rewritten; external higher-priority packs keep their native override behavior.

Forge uses its native path pack. Neo1.21 uses a path resource supplier;26 uses its actual JarContents supplier. The wrapper preserves native full/primary pack loading and delegates metadata/close. Modified server data deliberately has no unchanged KnownPack identity, avoiding an optimization that could skip sending configured bytes. ProjectE uses the same wrapper for acquisition-category filtering. All three targets compile in `evidence/configured-packs-compile-checkpoint.json`; runtime dependency decoding remains unverified.

Configured mining tags are now implemented and compile on all targets; see [CONFIGURED-TIERS.md](CONFIGURED-TIERS.md). Native mining behavior remains unverified.
