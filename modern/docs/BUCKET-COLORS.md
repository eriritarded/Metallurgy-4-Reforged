# Bucket fluid colors — 26.1.2 port.8

## Report and cause

The user reported all molten-fluid bucket contents appearing gray in the inventory. The installed port.7 uses native `neoforge:fluid_container` models and registers the original per-fluid RGB through `RegisterFluidModelsEvent`. The missing step is the item fluid capability for the custom `MetalBucketItem` subclass.

Reviewed NeoForge26.1.2.122 methods in the installed universal JAR:

- `CapabilityHooks.registerFallbackVanillaProviders`: automatic fluid capability registration requires `item.getClass() == BucketItem.class`. Our subclass is excluded.
- `FluidContentsTint.calculate`: calls `FluidUtil.getFirstStackContained`, then gets the contained fluid's model tint source. It does not use the dynamic model's configured fallback fluid.
- `FluidUtil.getFirstStackContained`: queries `Capabilities.Fluid.ITEM` through `ItemAccess.forStack(...).oneByOne()`. Missing capability returns `FluidStack.EMPTY`, leading to an untinted/white fluid surface.
- `DynamicFluidContainerModel.bakeModelForFluid`: uses the configured fluid as a geometry fallback, but assigns native `FluidContentsTint` to the liquid surface. This explains why the liquid shape appeared without its metal color.
- `BucketResourceHandler.getResourceFrom`: supports `instanceof BucketItem`, including our subclass, and reads its actual contained fluid. Native capacity is1000mB with transactional filled/empty bucket replacement.

## Implementation

`neoforge-26.1.2/.../Metallurgy.java::capabilities` explicitly registers `Capabilities.Fluid.ITEM` with NeoForge's `BucketResourceHandler` for every `FluidContent.ENTRIES` bucket. This covers49 metal buckets plus `molten_tar_bucket` and `molten_thermite_bucket` (51 total). The native26 generator emits the same registration so regeneration retains the correction.

The existing native tint pipeline can now obtain the actual contained fluid and its original color. Native machine/container fluid access uses the same registration. No custom tint renderer, recolored sprite, registry rename or item migration is introduced. Original bucket masks, animated fluid art, colors, luminosity, temperatures, recipes and IDs remain unchanged. The custom placement/configuration gate in `MetalBucketItem.emptyContents` remains. The1.20.1/1.21.1 adapters are not changed by this native26-specific correction.

## Initial correction and current limits

Port.8 compiled, passed package inspection and was installed in Simply NeoForged. Current port.9 retains this capability registration unchanged; see `evidence/held-gadget-release.json`. The same release contains the optional handbook and buckler GUI enlargement. The prior port.7 JAR is backed up and all41 other mods-folder files are unchanged. No new game or test run is authorized by this rendering bug report. Port.7's previous successful world-load check is historical evidence for that exact JAR and is not a rendered-bucket check. Package identity, installation state and native archive hash are recorded in `evidence/bucket-color-fix-26.1.2.json`; the combined release and cleanup are recorded in `evidence/handbook-release.json` and `evidence/handbook-cleanup.json`.
