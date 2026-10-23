# Native machine recipes

Implemented source, 2026-10-16. All three targets compile (evidence/processing-recipes-compile-checkpoint.json); resource decoding, synchronization and live scripting have not been run. No game is launched. This replaces the initial port's hardcoded runtime lookup with Minecraft's recipe manager.

## Ownership and defaults

The native recipe type and serializer are both `metallurgy:processing`. The maintained generator writes 174 recipes per target under `processing/`: 19 alloy equations, 14 chamber recipes, 140 explicit crusher routes and one automatic crusher rule. Counts include optional raw-material routes whose input tags can be empty. These are data entries, not 174 distinct metals.

`ProcessingAccess` reads the server manager, orders explicit recipes by full ID, and places the automatic rule last. Its native manager cache is invalidated on data-pack synchronization, recipe update, disconnect and server-stop events; the recipe-viewer follow-up avoids repeatedly flattening Forge's collection. Inventories, hopper acceptance, manual chamber insertion and shift-click input selection use the same managed definitions. No removed recipe is restored from `MachineRecipes`; that class now supplies historical default tables, ingot tiers and old-save XP fallback only. Processing time remains 140 ticks, with the existing fuel boosts, output capacity and persisted XP ledger.

Known ingot/ore/raw equivalence uses generated input tags containing the original items and optional common `forge`/`c` tags. Iron/gold alloy fallback inputs retain their concrete vanilla ingot/Metallurgy dust behavior. Angmallen remains two separate ingot/ingot and dust/dust equations. Alloy orientation is reversible, with consumption counts swapped correctly. Chamber batches still require the exact recipe count. Default source amplifier metadata is retained; the chamber's original applied-amplifier-zero behavior remains intentional.

`metallurgy:processing/crusher/generic` owns automatic processing of other mods' common-tag inputs. Remove that ID to disable the fallback through a pack or script; `enableOreDictCrusherRecipes` also gates it. It excludes Metallurgy items and all explicitly covered material names, so removing an explicit route cannot recreate it through a common tag. Adding an explicit recipe for an unknown material takes priority over the generic rule. Generic output selection, counts and XP remain as documented in MACHINE-COMPAT.md.

## Data format

Put recipes in `data/<namespace>/recipes/<path>.json` on 1.20.1, or `data/<namespace>/recipe/<path>.json` on the two newer targets. A Forge 1.20.1 example:

```json
{
  "type": "metallurgy:processing",
  "kind": "alloyer",
  "ingredients": [{"item": "minecraft:iron_ingot"}, {"tag": "forge:ingots/copper"}],
  "amounts": [1, 2],
  "result": {"item": "metallurgy:bronze_ingot", "count": 3},
  "experience": 0.5
}
```

On 1.21.1 use output `id` instead of `item`; ingredient objects remain native ingredient objects. On 26.1.2 use native string ingredients (`"minecraft:iron_ingot"`, `"#c:ingots/copper"`) and result `id`. Custom native ingredients use their loader's own codec. `crusher` has one ingredient and one amount; `alloyer` has two. Input counts are 1–64 and XP is finite 0–1000 per produced item. Omitted amounts default to one per ingredient. On 26.1.2 port.4, results use native `ItemStackTemplate` (count 1–99); live component/stack-capacity validation is deferred until runtime stack creation. Invalid output templates cannot consume machine inputs. The older targets retain their native live stack validation. See [the recipe load correction](RECIPE-LOAD-FIX.md).

For `chamber`, omit `result`, supply one ingredient/amount, an `effect` registry ID, `duration` in ticks (1–1728000), and `amplifier` metadata (0–255). Namespaced effects now pass correctly through aura lookup, vapor colors and HUD names/icons. Existing packed chambers with bare vanilla effect names remain readable. Active saved chambers finish their stored clock even if their recipe is removed; new batches use the current recipe set. Removing a recipe does not delete existing inventory.

The follow-up source rejects chamber recipe IDs that are absent from the native effect registry, in addition to syntax/range validation. Modded effects work when their registering mod is loaded. This prevents a new batch from consuming inputs for an unknown effect; it does not migrate or erase already saved chamber state. All three native target compilers passed; decoding/runtime behavior remains unverified.

The `generic_crusher` kind has no fixed ingredients/output. Use the supplied single default entry rather than creating multiple indistinguishable generic entries. Optional content acquisition conditions still gate generated outputs; registry IDs remain stable.

## Synchronization and scripts

On 26.1.2 port.4 the processing snapshot uses protocol `4` and native template outputs; both endpoints require matching binaries. This avoids resolving live output components during recipe preparation. Packet ordering, limits and authoritative recipe selection are preserved. See [RECIPE-LOAD-FIX.md](RECIPE-LOAD-FIX.md) for the user-observed port.3 failure and correction; the static/compile evidence does not establish runtime decoding or delivery.

Forge 1.20.1 and NeoForge 1.21.1 use their normal full recipe synchronization. NeoForge26.1.2 now sends a dedicated bounded processing snapshot from `OnDatapackSyncEvent`, atomically replaces the client list after all parts arrive, and clears it on disconnect. It does not request or replace the global native recipe map. This26-only follow-up compiles in `evidence/processing-sync26-compile-checkpoint.json`; runtime delivery/ordering remains unverified. See `jei26-sync-review.json`. Client input prediction and viewer categories consume that list. Dedicated servers do not load the client subscriber.

CraftTweaker's reviewed native `IRecipeManager` supports JSON recipes and removal by full name. On the reviewed 1.20.1 and 1.21.1 branches, use `<recipetype:metallurgy:processing>.addJsonRecipe(...)`, `.removeByName(...)` or `.removeAll()`. Supply the same native JSON fields shown above. This adapts the legacy custom crusher/alloyer/chamber managers to one native manager; old 1.12 ZenScript class names are not preserved. No CraftTweaker API classes are packaged or required. No 26.1.2 CraftTweaker release/API has been reviewed, and no script execution is claimed.

Source references: CraftTweaker 1.20.1 `f63201946a95c0944571fb92e9172f63196d7c4d`, `Common/src/main/java/com/blamejared/crafttweaker/api/recipe/manager/base/IRecipeManager.java` (blob `106e6c624c76c3ddee69488253c78d1c406be251`); 1.21.1 `c0467befe01d83632929378a337b64d44a0cbe67`, lowercase `common` equivalent (blob `46ba01d0537a05d85a4c379e44f3ad9809de1467`). Native recipe interfaces/codecs and synchronization were reviewed from the exact target dependency sources.

The machine recipe viewer is implemented in source (RECIPE-VIEWER.md); special crafting views, optional integrations, final packaging and remaining source behavior still have separate work. This document is not a release or installation receipt.
