# Machine compatibility

## State and source

Implemented source on2026-10-16. All three targets compile in `evidence/machine-compat-compile-checkpoint.json`, with matching staged/maintained manifests. The XP follow-up also compiles on all targets (`evidence/machine-xp-compile-checkpoint.json`). No game, tests, real configuration or installation is accessed. Original references are1.4.1 `recipe/CrusherRecipes`, `tileentity/TileEntityCrusher`, `TileEntityAlloyer` and `integration/IntegrationMekanism` at `ef94cb94cfc08ccebe8a0df307ce6b390dcd4704`. Dependency references and Git blob IDs are pinned in `optional-compat-research.json`; these are source reviews, not binary/runtime certifications.

## Tagged crusher inputs

Known source metal ores/ingots continue to match their common tags even when `enableOreDictCrusherRecipes=false`. The source switch only controls additional generic recipes; its `isValidInput` always performed OreDictionary equivalence for known inputs. Native tags replace those names. Modern vanilla raw iron/gold/copper and tagged raw materials are explicitly adapted as1 raw item to2 dust, with recipe XP0.75, alongside existing ore-block processing. This extends the1.12 system to modern vanilla mining drops.

`GenericCrushing` resolves other common `forge`/`c` tags when the switch is true: ore to2 dust (XP0.75), otherwise ore to2 gems (XP0.5), ingot to1 dust (XP0.1), and modern raw materials to2 dust (XP0.75). Known recipes retain priority. The input's common namespace is tried before the other convention; output members follow native registry order. Disabled Metallurgy output IDs are skipped. It reads current tags directly, so resource reloads do not leave an obsolete static tag cache. No new metal items or conversion loops are fabricated.

The new `MachineExperience` follow-up records actual per-route XP in each output slot. XP is weighted across identical stacked results; manual partial/shift-click extraction receives its proportional share and preserves fractional rounding. Hopper removals discard their share, retaining source player-extraction behavior. The bounded ledger is saved under `experience_ledger` and survives reload. Older local port saves without that field use the historical output-based rate for preexisting stacks; malformed ledger data is discarded without changing inventory. This deliberately replaces the source HashMap output ambiguity with stable route accounting. This follow-up compiles on all targets; no game behavior is established. Native managed recipe ownership is now implemented in source; see [PROCESSING-RECIPES.md](PROCESSING-RECIPES.md). Script execution remains unverified.

## Immersive Engineering external heaters

Reviewed IE1.20.1 `ExternalHeaterHandler` at `e63e4824800945eccf3684200a2c2270e2e1cdf2` exposes a Forge capability on the null side. IE1.21.1 at `75a27f03e4243544243567e8d5c38d336f4f10f4` exposes the same `IExternalHeatable.doHeatTick(int, boolean)` through a native sided block capability. The two exact API blob hashes are in the research record. No26.1.2 IE release/API has been reviewed, so its compiled loader adapter does not certify an upstream combination.

`compat/ExternalHeat` resolves the API only when `immersiveengineering` is loaded and implements its actual loaded interface with a cached per-machine proxy. No API stub, foreign class or IE dependency is packaged. Forge exposes a managed LazyOptional and invalidates it; Neo registers the actual native block capability at its registration event. The provider accepts only the null side.

The original heater warms burn time by2 toward111 and adds1 processing tick when fully warm. The native callback retains those values and reads IE's current base/speed costs. It is server-only and checks a valid recipe/output capacity. Redstone can keep the machine warm. Native API adaptations: no energy is charged when neither processing nor redstone warming is requested; no heat/progress is granted without sufficient supplied energy; costs never exceed `energyAvailable`; redstone warming does not grant the speed bonus, matching IE's documented argument. Fuel boost resets to1 so a previous Thermite/Ignatius fuel cannot multiply electrical heat. Overflow/corrupt burn counters are bounded. These changes deliberately correct the old implementation's ignored energy budget/redstone semantics. No live capability lookup, energy consumption, timing or multiplayer behavior is claimed.

## Mekanism

The original integration adds exactly one special recipe:1 Lutetium ore to2 Lutetium dust in the Enrichment Chamber. `tools/generate_optional_recipes.py` writes it in each native format. Mekanism source refs reviewed:

|MC target|Mekanism source version|Pinned revision|
|---|---|---|
|1.20.1|10.4.16|`96c736241e308a9cf1c73f7dcfa9aa221ea559d3`|
|1.21.1|10.7.19|`bcd7a8bf594cff9614eb12238fe3776f19da24d9`|
|26.1.2|10.8.0 alpha|`4718860a0c98598cf1b34e9f631b24eaa7c7ff47`|

The actual upstream enriching examples differ:1.20 nests `ingredient` and uses output `item`;1.21 uses flattened SizedIngredient and output `id`;26 nests a string ingredient. The native JSON follows each layout. Loader conditions require Mekanism and both enabled content IDs before recipe decoding. No dependency class is linked, and no recipe serializer from an absent mod should be resolved. Native condition/resource decoding and actual enriched output remain unverified under the no-game rule.

## Remaining work

Native data/script/runtime validation remains unavailable under the user restriction. Recipe-viewer, supported optional integrations, particle placement and source-role dispositions are implemented; production packaging is recorded separately. The initial machine batch is recorded and cleaned. XP ledger source manifests are recorded; disposable stages/raw logs were cleaned. Nothing is installed or declared complete by this document.
