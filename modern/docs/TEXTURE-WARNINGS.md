# Metallurgy texture warning correction

For the later native26 atlas-stitching defect reported against port.14, see
[the port.15 atlas correction](PORT-REPORT-FIXES.md). The earlier slot/file audit
below did not establish atlas membership or a warning-free client run.

2026-10-17. Target: Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25. Replacement: `1.4.1-port.5`.

## Causes and corrections

The user's `logs/2026-10-16-1.log.gz` reports unresolved/missing texture slots for the bimetal frame, both sublimation chamber states, their inventory models, the ore detector and the two extended item templates. It also reports missing `minecraft:builtin/entity`. The prior explicit-file audit could find a texture PNG while overlooking an unresolved `#slot` inside an otherwise valid model.

| Source model under `assets/metallurgy/models/` | Correction | Status |
|---|---|---|
| `block/bimetal_block.json` | Remove 24 faces referencing the unassigned `#missing` slot | Adapted |
| `block/sublimation_chamber.json` | Remove 47 unassigned faces | Adapted |
| `block/lit_sublimation_chamber.json` | Remove 51 unassigned faces | Adapted |
| `item/ore_detector.json` and `item/gadget/ore_detector.json` | Remove 6 unassigned faces in each copy | Adapted |
| `item/rubracium_gauntlet.json` and `item/gadget/rubracium_gauntlet.json` | Remove 4 unassigned faces in each copy | Adapted; also found by the expanded audit |
| `item/gadget/brass_knuckles_hand.json` | Replace obsolete entity parent with a native special-renderer base containing the same transforms, particle texture and empty JSON geometry | Adapted |
| `item/gadget/brass_knuckles.json` | Retain an empty base and the real knuckles particle texture, without the obsolete parent | Adapted |
| Native `items/extended.json` and `items/extended_handheld.json` | Remove these two false standalone item definitions | Adapted; parent model files retained for actual equipment |

There are 142 removed unassigned faces across seven model files. Every originally textured face, its UVs, element positions/rotations, authored display transforms and all original PNG bytes are preserved. Unassigned export faces are omitted rather than assigning them an invented material. The native knuckles renderer still owns the actual hand/knuckles geometry; GUI/ground/fixed icon selectors remain intact.

The unassigned faces already exist in the supplied original `Metallurgy-4-Reforged-1.4.1.jar`, SHA256 `feac090c1808cba5ce7e9ab559dce3a6e81da84f6c632e371511e73c0f821397`. The source archive is unchanged. Native special-model base conventions were inspected from `minecraft-client-patched-26.1.2.122.jar`, SHA256 `0621b17307aa4e75340366bf90970bc296ace07b286a5f194f2a78a98aec4346`, including `assets/minecraft/models/item/shield.json` and `chest.json`.

## Maintained implementation and evidence

- `tools/fix_model_resources.py` removes only faces referencing the unassigned slot in the seven named models. It runs at the end of the resource generator.
- `tools/generate_resources.py` omits standalone native definitions for the two parent templates. Their `layer0` remains supplied by real child equipment models.
- `tools/generate_gadget_resources.py` writes native-compatible empty knuckles bases while preserving source transforms.
- `tools/inspect_model_slots.py` reads JSON and archive bytes only. It follows reachable item/block roots, parent inheritance, face/particle/layer aliases and texture-file existence against the exact native archive. It does not treat an abstract parent as a separately textured item.
- `evidence/texture-slots-before-26.1.2.json` reproduces 12 grouped static errors in port.4, including the logged causes and the gauntlet. `texture-slots-source-26.1.2.json` resolves 1,428 own roots and 11,832 texture uses with no missing slots/files after the source correction. Packaged results are recorded separately.

The resource changes are in common source; the older 1.20.1 and 1.21.1 deliverables are not rebuilt or installed by this correction. The 26 package retains the prior input, shield and recipe-loading corrections. No item IDs, recipes, world data, effects, stats or processing network format change.

## Installation and limits

The production and installation event is recorded in `evidence/texture-warnings-fix-26.1.2.json`, with source/package hashes, rollback backup and preservation of unrelated mods. Only one current Metallurgy binary belongs in the instance. The user must launch their own game to observe resource reload and rendering. No assistant-launched client, server, headless game or tests are authorized; static resolution is not a claim that a live client produced a warning-free log or a rendered visual inspection.

The ordinary build completed with zero errors and12 deprecation warnings. Port.5 is installed with SHA256 `c49133e771794e6eb04afee2d785b82f9fe0bdf77c54d23b07a6f38f44af5c15`; port.4 is preserved outside mods. All38 other JARs and70 other mods-folder files are unchanged. The owned H: build stage, dependencies, process temporary files and raw log were deleted after recording their hashes; see `evidence/texture-warnings-fix-cleanup-26.1.2.json`.
