# Lamps, roads and Vulcanite TNT

Source implementation added2026-10-16 from Metallurgy1.4.1 revision `ef94cb94cfc08ccebe8a0df307ce6b390dcd4704`: `BlockOrientable`, `block/gadget/BlockPhosphorusLamp`, `PhosphorusLampSavedData`, `BlockMetallurgyTNT`, `entity/MetallurgyPrimedTNT`, `world/explosive/ExplosiveType`, `MetallurgyExplosion`, `handler/GadgetsHandler` and `RegistrationHandler`.

## Phosphorus Lamp

`metallurgy:phosphorus_lamp` retains hardness4, pickaxe tier1, light8, six orientations, original meshes/PNG textures and the original selection/collision boxes. Placement chooses the clicked supported face or falls back to facing up. Modern full collision shapes replace the removed `isFullCube` support query. Losing support creates a native falling block oriented up; unloaded support chunks defer the check rather than being loaded by it. Native falling-block logic owns motion, placement, persistence and drops.

Spawns passing through the native mob position check are denied within Euclidean distance8 of a loaded lamp. This includes the same broad mob category as the source; the implementation does not invent a hostile-only restriction. Commands or spawn mechanisms bypassing the native position event are not intercepted.

The old persistent position list is replaced by a synchronized loaded-chunk index. Palette checks skip sections with no lamp; matching sections rebuild their positions on chunk load. Placement indexes immediately, unload removes the chunk, and queries prune stale entries. The blocks themselves provide persistence, so an old duplicate/stale SavedData list cannot keep blocking spawns after a lamp is removed. No1.12 SavedData migration is needed for new-target worlds; old worlds are not imported by this port.

## Roads

`road_block` and `striped_road_block` retain hardness3, original textures and placement facing. Their native state has only the four horizontal directions the source actually used. The original side/up/down texture keys now use the matching native cube parent, and legacy `inventory`/unused vertical variants are removed. The striped top rotates90° for east/west.

Client movement multiplies horizontal velocity by1.5 while grounded on either road and movement input is nonzero. The modern input event replaces the old PlayerTick callback. Native buckler movement changes remain separate. This is client movement source, not evidence of measured travel speed or multiplayer correction behavior.

## Vulcanite TNT

`vulcanite_tnt` and entity `metallurgy_primed_tnt` retain the original IDs, original block artwork, normal80-tick fuse, initial horizontal speed0.02/upward0.2 and64-block tracking range with updates every tick. Native PrimedTnt owns movement, fuse synchronization and ordinary lifecycle. The type is implicitly Vulcanite through its registered entity type; `tnt_type:0` remains in saves. The ignition owner is persisted separately and resolved in the active level. No cross-version world migration is claimed.

Ignition uses native fire/TNT hooks for lighters, redstone and projectiles. Chained explosions create the custom explosive with the native10..29-tick short fuse, correcting the old base TNT path that could create vanilla TNT.26.1.2 respects its native `TNT_EXPLODES` game rule for ignition, chains and detonation.

The blast has power6, does not create fire and preserves source entity damage: floor(((exposure×falloff)²+(exposure×falloff))/2×7×6+1), rather than vanilla's double-radius damage factor. Forge1.20 uses a narrowly scoped mixin before the native Entity.hurt call, because its damage calculator lacks the newer callback.1.21.1 and26.1.2 use their native calculator callbacks, including26's supplied exposure argument. Shield/armor processing receives the reduced amount; unrelated explosions are untouched.

Source rock resistance is divided by7, and blocks above Vulcanite's configured harvest tier are excluded. The removed Material.ROCK category is represented by the extensible `metallurgy:vulcanite_softened` block tag, populated with source-like native stone/ore families and our rock ores/roads. New or third-party stone families can extend that tag. This is an explicit material-category adaptation, not proof of identical ray paths across game versions. Native explosion protection/cancellation, loot processing, knockback and networking remain in use. Native TNT drop-decay settings apply if the user changes that game rule; its normal setting keeps full explosion drops. The original flame/colored-ore detonation visual now compiles on all targets; its4096-position network/sample bound and exact source equation are recorded in CLIENT-VISUALS.md.

The20 renderer draws the original custom block model with native fuse swelling/flashing. Later targets synchronize that block state and use the native TNT renderer. Rendering and gameplay remain unobserved; the user prohibits all assistant game launches.
