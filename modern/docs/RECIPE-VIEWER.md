# Native JEI integration

Source implementation2026-10-16. Optional API coordinates are pinned to JEI15.71.0.224 (Forge1.20.1),19.64.0.455 (NeoForge1.21.1), and29.47.0.111 (NeoForge26.1.2). The exact upstream revisions and reviewed interface blobs are recorded in `recipe-api-baseline.json`. Only compile-time API dependencies are used; no JEI library or API implementation is bundled. The base mod does not reference the plugin class. Loader/API presence and UI behavior have not been observed in a game.

## Machine pages

`compat/JeiPlugin` registers crusher, alloyer and sublimation chamber categories with the original artwork and animated flame/progress/metal-flow regions. Slot coordinates are adapted from the source1.4.1 category classes. The pages use the client's synchronized processing definitions, so data-pack/script additions and removals can be represented rather than displaying a second hardcoded table. Native ingredients expand into counted item alternatives; empty tags are omitted. Recipe registry/source IDs remain available to JEI. Automatic crusher rows are expanded from current registry items only when the removable generic rule exists; their source ID points to that rule.

The chamber shows its actual effect name and duration; source amplifier metadata remains distinct from its applied-amplifier-zero behavior. Crusher/alloyer tooltips show the default7-second process and route-specific XP per produced item. Original machine catalysts and ingredient information for tar, bitumen, the invisibility shield, Thermite dust and Krik armor are included. The labels and original description localization keys are retained.

### Missing chamber category title, 2026-10-17

The user reported an untranslated Sublimation Chamber JEI category. On all three targets, `JeiPlugin.Category.getTitle` requests `container.chamber`, but this key was absent from every bundled locale. All eight language files now define it using their existing `block.metallurgy.sublimation_chamber` name with legacy color formatting removed. English resolves to **Sublimation Chamber**. Existing English fallbacks in incompletely translated locales are preserved; no new translation is invented. Source hashes and exact labels are recorded in `evidence/jei-lang-source-review.json`.

| Feature | Targets | State/evidence |
|---|---|---|
|`container.chamber` category title|All three targets; all eight bundled locales|Implemented and packaged; one added key per locale|
|Existing machine recipes, slots, effects and transfers|All three targets|Unchanged; compiled class bytes retained exactly|
|Rendered JEI category header|User's graphical client|Unverified after this resource correction; no game or tests launched|

Port.6/6/12 is a resource-only release: `tools/package_jei_lang.py` retains the prior inspected/reobfuscated class files, updates language resources and version metadata in ordinary/source archives, and inspects all target packages. Java was not recompiled. The native26 port.12 package is installed in Simply NeoForged with port.11 backed up and all42 other mods-folder files preserved; a restart loads the replacement. No packet, dependency, configuration, recipe or save migration is introduced. See `evidence/jei-lang-release.json` and `evidence/jei-lang-cleanup.json` for the package/install and guarded scratch cleanup events.

Transfer uses JEI's native counted-item transfer implementation. A custom `IRecipeTransferInfo` checks the actual `MachineMenu.alloy` flag because both machines share a native menu type. Only input slots0/1 or0 are destinations; fuel/output slots are excluded; the36 player inventory slots supply ingredients. A mismatched machine receives an appropriate machine-name error. The no-menu chamber has no transfer action. Clicking the current machine's progress gauge selects its matching category using a per-screen handler.

## Reload lifecycle

Machine lookup now caches one sorted list per native manager and clears it on data-pack synchronization, client recipe updates/disconnect and server stop. This avoids repeatedly flattening all vanilla recipes: the Forge1.20.1 native `RecipeManager.getRecipes()` creates a fresh set on each call, so collection-identity polling would allocate on every machine query. Access to the weak-key cache is synchronized for integrated-server/client event overlap. The client list is replaced after native synchronization;26.1.2 now uses a dedicated bounded processing payload, avoiding the global native recipe map.

## Remaining work

Detector programming/clearing and Tartarite absorption now have native crafting examples and subtype keys, recorded in the follow-up below. Fluid Thermite information and optional dependency metadata compile in the optional-tools checkpoint; runtime UI evidence remains unavailable. All three targets compile in `evidence/jei-machine-compile-checkpoint.json`, with matching staged/maintained source manifests; no rendered page, ingredient search, native packet transfer, data reload or server/client interaction is claimed. No game or tests have been launched.

##26 client-only JEI synchronization review

Source inspection found a pre-release edge case: JEI29 treats any nonempty native recipe-type sync as the authoritative synced recipe source. If only Metallurgy processing is sent and server JEI is absent, client-only JEI could lose its fallback crafting pages. This is an inference from the pinned StartEventObserver, not a reproduced game result. A dedicated bounded processing-list payload is now implemented in source and compiles on26.1.2 (`evidence/processing-sync26-compile-checkpoint.json`). It sends at most32 entries/256KiB per part,4096 recipes/8MiB per snapshot, validates IDs/counts/trailing bytes, and atomically replaces the client list after a complete generation. Disconnect discards pending state. The existing native recipe serializer retains ingredient and item component data. No runtime reproduction or release/installation is claimed. See `jei26-sync-review.json`.

## Special crafting follow-up

All three targets compile in `evidence/jei-crafting-compile-checkpoint.json`: detector programming/clearing and all nine Tartarite equipment absorption forms use JEI's own serializable unregistered shaped-recipe builder. Outputs are assembled by the actual common DetectorCrafting/Paragon code, including detector metals and absorbed durability/components. Detector pages show valid example arrangements and explain that the real recipe is shapeless; Paragon pages explain the top-block selector and mixed side blocks. Current native crafting-rule presence gates examples, with two bounded source-rule IDs included in the26 snapshot. Removed source rules therefore do not leave fabricated example pages.

Item subtype interpreters distinguish the source detector states (empty or one/two/three programmed metals) and the absorbed Tartarite material, while ignoring usage/damage counters. Grouping detector lookup by count keeps arbitrary valid metal combinations searchable without enumerating tens of thousands of combinations; each shown recipe still displays its actual example metals and colors. Original empty state remains the base subtype. Native26 uses ItemStackTemplate-backed SlotDisplays so component-bearing inputs/results retain their actual state. No JEI implementation class is linked. The reviewed native Bag already saves only dirty State transactions, so subtype reads do not mutate items.

## Login follow-up, 2026-10-17

Port.6 corrects the separate logged JEI plugin failure from looking up nonexistent `invisibility_shield`. The information page now uses registered `lemurite_shield`, preserving the description key. The same package fixes native recipe-content encoding for detector/Paragon unit recipes; see [SINGLEPLAYER-DISCONNECT.md](SINGLEPLAYER-DISCONNECT.md). This is compiled/installed source, with no assistant game or tests and no live replacement UI/login claim. Older maintained plugin sources include the ID fix; their deliverables are not rebuilt here.
