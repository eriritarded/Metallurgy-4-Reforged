# Optional native recipe data

Source implementation2026-10-16. The native configuration-condition adapter compiles on all three targets; its matching maintained/staged source/resource manifests are recorded in `evidence/optional-recipes-compile-checkpoint.json`. Dependency recipe decoding and gameplay have not been run. The user prohibits all games and tests.

`tools/import_optional_recipes.py` recovers29 source Industrial Foregoing laser entries and18 source Ender IO XML alloys into `content/optional-legacy.json`, with original file hashes. `generate_optional_recipes.py` emits each reviewed native format. Dependency revisions, example paths and Git blob hashes are in `optional-recipe-baseline.json` and `optional-compat-research.json`.

|Integration|Forge1.20.1|NeoForge1.21.1|NeoForge26.1.2|
|---|---:|---:|---:|
|Mekanism Lutetium enrichment|1|1|1|
|Ender IO alloy smelting|18|18|18|
|Industrial Foregoing laser ores|29|29|No reviewed compatible release/schema|
|Thermal|559|No reviewed compatible release/schema|No reviewed compatible release/schema|

Resources require both the matching mod ID and `metallurgy:integration_enabled`. The latter honors existing configuration switches where the original mod provided them, including `inForegoingIntegration`. Metallurgy item inputs/outputs also receive acquisition conditions. They are normal data-pack recipes and may be replaced by higher-priority user packs. No foreign serializer/class is bundled or resolved when its mod is absent. Native condition short-circuit behavior remains unobserved in a game.

## Ender IO

All18 original XML recipes retain their exact ingredients/counts, output/count, energy and XP, even where their ratios differ from the ordinary Metallurgy alloyer.1.20 uses nested ingredients and `result.item`;1.21 uses flat SizedIngredient and `output.id`;26 uses counted string ingredients and `output.id`. Current common ingot/dust tags replace old OreDictionary names. No obsolete XML IMC is sent to modern Ender IO.

The source `enableEnderIOAlloyConfig` controlled an editable copied XML file. Native user data packs replace that customization mechanism; the precise legacy configuration/migration policy is still under review and is not declared complete. No user's XML/configuration file is copied, replaced or edited during development.

## Industrial Foregoing

Original lens colors, ore results, depth intervals and weights are retained. The removed `minecraft:hell`/`minecraft:sky` biome IDs expand to their five current vanilla Nether/End biomes. Depth0..255 remains source0..255; it is not silently shifted downward for modern terrain.1.20 uses numbered lenses, native biome list objects and pointer0.1.21 uses named color lenses and native biome/dimension filters. These follow the reviewed dependency examples.29 entries are source laser definitions, not a claim that every metal has a laser recipe.

## Thermal

The reviewed Thermal Expansion source is1.20.1 version11.1.0 at `9dc6061173c6732fd3bcdbf4d3c2baf3dc82f089`; its modern mod ID is `thermal_expansion`. Thermal Core fuel source is pinned separately. Added recipes cover the original centrifugal separation, equipment/shield/gauntlet recycling, Potash pulverizing, Thermite fuel and metal melting.

Recycling consumes one sand plus the specific equipment, costs6000RF, and returns source ingots: helmet2/chest4/legs3/boots1, tools1, gauntlet2 and Lemurite shield3. Source slag chances15/25/20/15%, tools/gauntlet10%, shield25% are retained. Separation reverses source alloyer ingredient counts at `max(input counts)*2000+4000` energy. Duplicate Angmallen fallback definitions collapse to the canonical ingot-return recipe instead of relying on duplicate-input registration order. Potash to fertilizer costs4000;1000mB Thermite yields200000 energy.

All metal integrations share the modern TConstruct3 fluid unit:90mB/ingot,10/nugget,810/block and180/ore. This is verified in upstream `FluidValues` (linked by the baseline); source1.12 used144/16/1296/288. Using one unit prevents a144mB-melter/90mB-caster duplication loop. Melting energy retains500/4000/36000/8000 respectively. Bucket volume stays1000. TConstruct's remaining material/casting adapter must use these same units.

The original `IntegrationThermal.buildRecipeCompound` incorrectly serialized its input as the output when exactly one output was supplied. The native recipes explicitly emit the intended fertilizer/fluid result rather than copying that invalid IMC behavior. Modern native JSON replaces the removed IMC API. Result counts, resource decoding, damaged-equipment matching, chances, fluid transactions and combined-mod economy remain unverified.

## Remaining work

Supported native integrations, machine recipe ownership, customization and the source-role audit are implemented. See RELEASE.md for current packages and unsupported target APIs. The coverage JSON counts generated resources; native loading and live recipes remain unobserved.
