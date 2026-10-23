# Molten metals, tar and thermite

## 26.1.2 bucket capability correction — 2026-10-17

Port.8 explicitly registers NeoForge's native `BucketResourceHandler` for all51 custom bucket items. Native fallback registration skips subclasses; missing `Capabilities.Fluid.ITEM` caused inventory fluid tint lookup to see empty contents and draw gray. The model, original fluid colors/art,1000mB native bucket capacity and placement gate are retained. Source is implemented; packaging/install status and rendering limits are tracked in [BUCKET-COLORS.md](BUCKET-COLORS.md).

## Source implementation checkpoint — 2026-10-16

Added `fluid/FluidCatalog.java`, native `fluid/FluidContent.java`, `MetallurgyLiquidBlock.java`, client `FluidRendering.java` and `tools/generate_fluid_resources.py`. The maintained source, including tar lakes and the nested effect-compound fix, compiled on all three targets. This does not establish runtime behavior or full-port completion. No client or game has been launched for this addition.

Reference: original1.4.1 revision `ef94cb94cfc08ccebe8a0df307ce6b390dcd4704`, `fluid/ModFluids`, `FluidMolten`, `FluidEvents`, `block/fluid/FluidBlockTar`, `material/MetalStats.automaticTemperature` and `util/Constants.METAL_TIERS`.

## Registered content and values

- 49 metal fluid types keep names `metallurgy:<metal>`, with flowing counterparts `metallurgy:flowing_<metal>`, placed blocks `metallurgy:molten_<metal>` and native buckets `<metal>_bucket`.
- Tar uses `metallurgy:molten_tar`; thermite uses `metallurgy:molten_thermite`. Both have flowing counterparts and `_bucket` items.
- Metal temperature is exactly1000 +200×original ore harvest tier; alloy fallback uses the original Constants tier map. The original initialization calls `automaticTemperature()` even when the data's separate temperature field was overridden; source behavior is retained here.
- Metal/thermite light level9; tar0. All types retain density800 and viscosity4000. Tar temperature400; thermite4000. Original metal RGB values and original animated still/flow PNGs are retained.
- Native flow uses decrease1, slope search4 and20-tick updates, with no infinite source conversion. This is a native-flow adaptation of the old BlockFluidClassic implementation.
- Tar retains water-like swimming, drowning, hydration, extinguishing and boat support. Contact scales all entity velocity axes by0.25 for each contacted tar block, following the original callback.
- An isolated tar source hardens into `metallurgy:tar_ore` on a random block tick when no adjacent face has another tar source. Flowing/falling tar does not qualify. Unloaded neighboring chunks defer the decision rather than being loaded by the tick.
- Hot fluid contact uses native lava damage and15-second ignition, at most once per entity per game tick, even when touching multiple fluid blocks. Fire-immune entities are exempt. This restores material-LAVA contact behavior that custom modern fluid types do not inherit automatically.
- Entity effect transactions now attach their live compound before nested callbacks run. First-contact lava damage therefore cannot overwrite armor/effect state created by a nested damage hook when the entity had no prior effect compound.
- The original vaporization quirk is retained: hot fluids vaporize on placement in ultra-warm dimensions, while tar can be placed there. This follows FluidMolten.doesVaporize's comparison against Material.LAVA.

## Native client resources

Forge uses its fluid client extension; NeoForge uses its client extension registration event.26.1.2 additionally registers native `FluidModel.Unbaked` entries with constant original color through `RegisterFluidModelsEvent`, because its fluid textures/tint no longer come from the old extension methods. Tar retains the original screen overlay texture.

Buckets use Forge/NeoForge dynamic fluid-container models;26.1.2 uses its item-model codec with explicit base/mask textures and luminosity. The original plural `blocks/` texture paths are added to the native block atlas. No generated artwork or third-party shader assets are introduced.

## Persistence and remaining work

Native registries own bucket contents and flowing-fluid state. Old1.12 universal-bucket NBT/world saves are not migrated. Registration/config controls, smeltery/other optional fluid integrations, precise fog appearance and full client/source-placement/drain/contact/flow checks remain pending at this source checkpoint. The new content is not installed in Prism.

## Tar lakes

`world/TarLakeFeature.java` now restores the default Overworld attempt:20 successful rolls out of500 per chunk (4%), horizontal offsets8..23 and a random height0..255, descending through air and rejecting heights≤4. The old lake generator's−8 horizontal origin adjustment is preserved. Native LakeFeature performs cavity carving and terrain checks with tar as fluid and air as the optional barrier; this avoids inventing a stone liner for source water-material tar.

The modern generation stream supplies deterministic chunk RNG, replacing the old unseeded `new Random()`. The feature runs in the native lakes generation step rather than removed PopulateChunkEvent.Pre. Legacy numeric custom-dimension configuration and exact old/new terrain-shape distributions remain unverified/pending. No existing chunks are regenerated by this implementation.

## Compilation follow-up

The first fluid pass compiled on Forge1.20.1 and NeoForge1.21.1.26.1.2 needed the modern environment attribute for water vaporization and corrected `noCollision()` spelling; that corrected source compiled. `evidence/fluids-confirmed-compile-checkpoint.json` records a later successful compile on all three targets, including tar lakes and the nested effect compound fix, with no maintained-source differences at recording time. Completed staged builds and raw logs were removed after hashes were recorded. All gameplay and client rendering remain unverified.
