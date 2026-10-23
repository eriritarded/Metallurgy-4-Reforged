# Configured mining tiers

Implemented source2026-10-16; all three targets compile in `evidence/configured-tiers-compile-checkpoint.json`. Native tag loading, dependency mining, reload and gameplay have not been run. This closes the initial stat override mismatch between runtime material definitions and static generated ore requirements.

Forty maintained mining-tag files moved into the always-enabled `core_tiers` built-in pack. `core-tier-resource-migration.json` records each original/destination path and SHA256 before regeneration. `ConfiguredPack` delegates the exact target's normal resource supplier and transforms these JSON values in memory from the restart-scoped `Materials` definitions. This pack has no optional mod dependency and writes no user files.

Only Metallurgy metal-ore members are recalculated. Existing non-ore metal blocks, machines, chests and vanilla tag references stay in the templates; the pack does not replace other mods' tag contributions. Minecraft `needs_stone_tool` uses configured ore level1, `needs_iron_tool` level2, and `needs_diamond_tool` level3 or higher. Neo native `incorrect_for_<tool>_tool` rejects ores above the vanilla tool's level; `metallurgy:incorrect_for_level_0` through7 rejects ores above each custom level. Forge modular `needs_level_5` through7 identifies ores at the exact configured custom tier. Source catalog ore IDs remain stable; stat overrides cannot add/remove an ore registry ID.

Resource generators now write these tags directly into the core pack, including the phosphorus lamp's stone-tool requirement and Forge modular tiers. Base-pack copies are removed, so their old requirements cannot remain merged alongside the configured values. The same optional pack factory used for modular data provides exact Forge path resources, Neo1.21 path resources and Neo26 JarContents resources. Modified server data has no unchanged KnownPack identity.

Custom stats still require a full restart on server/client. Common configurations must match. Optional native material traits remain their documented theme adaptations; this change does not attach complete armor-set powers to modular components. No real world, save, launcher, mod list or installed JAR is changed during development.

The complete resource generator was refreshed after moving the tag templates. Its gadget helper now uses the configured tag path, acquisition conditions accept native multi-output recipe arrays, and Python text reads explicitly use UTF-8. Full source resource generation completed successfully before the recorded final compiles.

## Beacon base blocks

The always-active configured pack also supplies `minecraft:beacon_base_blocks`. Raw metal blocks qualify when the original ore tier is greater than1, or for alloys when their configured tool harvest tier is greater than1. Native configuration transforms this list at load without changing registry IDs. Decoration forms are excluded. This replaces the removed `BlockMetal.isBeaconBase` callback. The source does not add every metal ingot as a beacon payment; no such expansion is invented. This follow-up is included in the ordinary production packages and source receipt; native beacon behavior remains unobserved.
