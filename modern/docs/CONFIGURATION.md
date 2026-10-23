# Configuration port

This module is in progress. The original six configuration definition classes are pinned in `config-source-baseline.json`; their public field names, descriptions, literal defaults and range annotations are retained under `common/.../config`. `tools/import_configs.py` imports only those definitions from the pinned1.4.1 source.

## Loading

`ConfigBootstrap` calls `ConfigFiles.initialize` before content registration. Five UTF-8 JSON documents are created under the launch profile's `config/metallurgy_reforged`: `general`, `registry`, `effects`, `gadgets`, and `worldgen`. The two effect categories share a document. Existing files are read without being rewritten. Missing fields use original defaults. All settings are restart-scoped; no live reload or graphical configuration screen is implemented yet.

Files are limited to4 MiB, arrays to4096 entries, strings to256 characters. Invalid types, nulls, nonfinite numbers, fractional integer values and original range violations abort startup with the file/field context. Original literal defaults which contradict their own annotation are preserved. Native lamp light must be0..15; gauntlet durability must be positive; values which overflow native calculations are rejected.

When `enableCustomMaterialStatsConfig` is true, `materials.json` starts with the original49 definitions and accepts overrides for existing material IDs. Partial entries retain original fields. Unknown/duplicate IDs, null/unknown stats, invalid colors, unsupported ore/tool tiers, nonpositive/overflowing durability or malformed four-slot armor defense arrays are rejected. Supported optional tool attributes from `JsonMaterialHelper`, including `attack_damage`, can be added even where omitted in the default material. Removing an entry preserves its original definition: registration and saved IDs remain stable. An ore-bearing material cannot become oreless or vice versa through a stat override. Adding new materials/tool sets/assets is not supported by this file. Native Tetra/Silent Gear resources now adapt configured stats and tints in memory; see MODULAR-COMPAT.md.

## Consumers implemented in the first compile checkpoint

- Original effect roster: per-metal `ALL`, weapon, armor, tool, pickaxe and hoe flags. Tartarite absorption additionally respects its own `ALL` switch.
- Ignatius drinking damage, Shadow Iron critical upper bound, Etherium phasing block blacklist, Mithril enchantment attributes.
- Original34 ore profiles: min/max height, indicative vein size, rarity attempts, biome whitelist, warm-biome and fortress boosts. Legacy biome aliases are translated to current IDs. Tar chance and resource-location dimension whitelist are read; cross-dimension biome insertion is a subsequent addition.
- Road speed; lamp light/collision; brass knuckle damage/speed; gauntlet durability/damage/speed/hunger/block damage and terrain-griefing switch; Thermite and Ignatius furnace fuel values.

The original integer tar dimension0 is adapted to `minecraft:overworld`. Modern custom dimension IDs replace removed numeric dimension IDs. The original Thermite Tinkers temperature setting is distinct from the source fluid temperature and must not silently change the latter.

## Additional consumers

Registration category switches preserve the registered item/block IDs for old saves, while removing disabled outputs from the creative tab and conditional crafting/smelting recipes. Crusher metal-dust output and dynamic Tartarite crafting also respect availability. Original standalone dusts remain available, as in `ModItems`; the dust switch covers generated metal dusts. Disabling fluid blocks prevents bucket placement without consuming the filled bucket; fluid IDs, saved fluid blocks and processing remain valid. This is an explicit adaptation from the old removal-based registry toggles, whose own descriptions warned about missing items/recipes.

`GeneralEvents` restores the original mob equipment chances: peaceful0%, easy5%, normal10%, hard15%, then uniform selection among eligible armor materials after the blacklist. Empty armor slots receive the selected set; an empty main hand receives an axe25% of the time, otherwise a sword, when that material has tools. Category availability applies. Current entity type IDs replace old exact Java-class lookup. A server-persistent attempt flag and the native `loadedFromDisk` flag prevent repeated rolls on save load or dimension transfers; the1.12 join handler rerolled those empty slots. Existing equipment is preserved. Join messages use the original four translations and the warning toggle. Tool/armor repair switches reject ingot repair through the native anvil event, while enchantments and same-item combining remain usable.

High-tier metal ores emit light5 when enabled, for source ore tiers5 and above. The crusher's optional external tag matching now obeys the original OreDictionary integration switch; native iron/gold/deepslate variants remain supported. Generic external dust/gem/raw families use current common tags; see MACHINE-COMPAT.md.

Tar's placed feature is inserted in every biome by the native `metallurgy:tar_dimensions` biome modifier, with the configured dimension whitelist checked inside placement. This supports custom dimension IDs without implicitly enabling tar there. Default remains Overworld only.

Retrogen source and save behavior are recorded in [WORLDGEN.md](WORLDGEN.md). These additions compile on all three targets in `evidence/config-confirmed-compile-checkpoint.json`; the initial checkpoint above predates them.

## Pending consumers

Material tooltip statistics/borders now have native client consumers; see CLIENT-VISUALS.md. Tetra's new `tetraIntegration` and the original `silentGemsIntegration` now control dependency-gated native bundled packs; see MODULAR-COMPAT.md. TConstruct/armor and Spartan switches now have native consumers on the reviewed Forge target (TINKERS.md, SPARTAN.md). Ender IO's original editable-alloy flag now selects persistent per-target native JSON folders (ENDERIO-CUSTOMIZATION.md). No unsupported foreign API is fabricated. A loaded field without a consumer is not presented as an implemented feature. No1.12 `.cfg` migration or client configuration GUI is claimed.

## Multiplayer configuration boundary

The source `GeneralConfig.ChangeListener` calls `ConfigManager.sync` after its local configuration GUI changes; this is disk/local configuration refresh, not a server-to-client configuration packet. The original packet manager has no configuration payload. This port keeps common gameplay/material/compatibility settings restart-scoped and requires matching common settings on server and clients. Client-only tooltip, warning and ore-particle preferences can differ. Automatic common configuration synchronization is not an original feature being silently dropped. Explicit phasing, corpse and Krik height state use native server payloads, while inventory/entity attributes remain server-authoritative. Custom material values baked into native items need a restart on both sides.

## Evidence

`evidence/config-first-compile-checkpoint.json` records compilation of the initial loader/consumers on the exact three target loaders. This is compilation evidence only. No game or tests were launched; no user profile, enabled list or installed package was changed.

`evidence/config-confirmed-compile-checkpoint.json` includes the subsequent native consumers and retrogen. All three compilers succeeded with no difference between maintained and staged source manifests. The improved recorder also detects new or removed source files. Disposable copied stages and raw logs were deleted under the guarded cleanup helper; only concise receipts remain. Compilation does not establish native recipe-condition loading, saved attachment persistence or gameplay parity.

## Native dimension and editable recipe follow-up

`worldgen.json` under `ore_generation` now supports `oreDimensionOverrides`, with the three vanilla ID mappings by default and explicit `dimension=overworld/nether/end/none` entries for custom dimensions. Invalid/duplicate mappings fail loading. Native26 uses its replacement skybox/environment APIs; other targets use natural/ultra-warm flags. Source details and terrain extension tags are in WORLDGEN.md. Ender IO native alloy folders are created only with the dependency loaded and the original customization flag enabled; existing user files are never replaced. All target compilers passed; data loading/world generation/profile migration remain unverified.
