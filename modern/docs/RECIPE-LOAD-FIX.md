# Minecraft 26.1.2 processing recipe load correction

2026-10-17. Target: Minecraft 26.1.2, NeoForge 26.1.2.122, Java 25. Replacement version: `1.4.1-port.4`.

## Observed failure and native API

The user's port.3 instance reached the title screen, then failed while preparing recipes for world creation. `latest.log` at 00:00:13 records `IllegalArgumentException: Invalid processing output` from `ProcessingDefinition.<init>` through `ProcessingRecipe.Serializer`, `RecipeManager.prepare` and the mouse-click handler. This is a Metallurgy recipe-loading failure.

The exact installed patched Minecraft archive was inspected without executing game code: `minecraft-client-patched-26.1.2.122.jar`, SHA256 `0621b17307aa4e75340366bf90970bc296ace07b286a5f194f2a78a98aec4346`. Native `ShapedRecipe` and `AbstractCookingRecipe` retain `ItemStackTemplate` outputs. Its codec uses `Item.CODEC`; the live `ItemStack.CODEC` instead requires `Item.CODEC_WITH_BOUND_COMPONENTS`. Calling live stack construction/copy/maximum-stack validation during recipe preparation can precede component binding. The old constructor also threw directly inside the codec mapping, allowing a validation exception to escape recipe parsing.

## Implementation

| Source/native boundary | Target implementation | Status and evidence |
|---|---|---|
| Processing output definition | 26-only `machine/ProcessingDefinition.java`, immutable template until use | Adapted; native template bytecode inspected, compilation/archive receipt below |
| Recipe JSON / snapshot serialization | `recipe/ProcessingRecipe.java`, template codecs and `DataResult` diagnostics | Adapted; runtime decoding unverified |
| Processing client/server snapshot | `network/ProcessingNetwork.java`, protocol 4 | Adapted; packet delivery unverified |
| Older target definitions | Common `ProcessingDefinition.java` retained for 1.20.1/1.21.1 | Unchanged by this correction |

- The maintained 26-only `machine/ProcessingDefinition.java` stores an optional immutable `ItemStackTemplate`. Its constructor validates kind, ingredient/amount counts, finite XP, effect IDs, output item and native template count 1–99 without creating a stack or reading item components.
- `ProcessingRecipe.Serializer` uses native template JSON and stream codecs. Definition validation failures return `DataResult.error` with a diagnostic instead of escaping the JSON codec mapping.
- `output()` creates a fresh stack only for runtime consumers. Vanilla template creation performs strict component/capacity validation. An invalid custom output returns no machine result, preventing input consumption for an empty result.
- The 26 processing snapshot registrar changes from protocol `3` to `4`, because its result payload now carries an optional template. Use matching port.4 client/server binaries. Snapshot limits, ordering, server authority and existing cache lifecycle remain intact.
- Gradle excludes only the common older `ProcessingDefinition` source from the 26 compilation and source archive. The native replacement is compiled once. The 1.20.1 and 1.21.1 sources/packages retain their previous native stack APIs.
- The import generator preserves the maintained native recipe adapter. The native definition and processing network adapters are also maintained directly.

The recipe type/serializer remain `metallurgy:processing`. All 174 bundled processing JSON files, IDs, outputs, counts, XP and conditions are unchanged: 140 crusher entries, 19 alloyer entries, 14 chamber entries and one generic crusher rule. Processing time remains 140 ticks. Chamber behavior, quantities, generic routing, integrated chests, issue 388, prior input/shield corrections and original assets are preserved. There is no save or item-ID migration.

## Compilation, installation and limits

The package/install receipt is `evidence/recipe-load-fix-26.1.2.json`; it records compilation, source equality, archive differences, hashes, backups and the installed copy. No assistant game, server, headless game or tests are authorized. Compilation/static inspection do not establish runtime recipe decoding, gameplay, packets or rendering. The user must start/restart their own instance to load the replacement. Rollback uses the preserved prior package outside `minecraft/mods`; keep only one matching Metallurgy binary active.

## Separate Quiverfall warnings

The same startup session's rotated log, `logs/2026-10-16-1.log.gz`, reports missing sound events `quiverfall:weapon.reload.ironclad` and `quiverfall:weapon.hit.ironclad`. These identify Ironclad reload and hit sounds. They are separate warnings; the fatal stack above belongs to Metallurgy. This correction does not change Quiverfall.

Ordinary `jar`/`sourcesJar` packaging succeeded with zero compiler errors and 12 deprecation warnings. The playable archive contains 265 own classes (Java major 69); all 69 subscribed event parameter types resolve to concrete classes. The source archive contains exactly one native template definition. The installed SHA256 is `f77095eb9af0208c398764f11869410b2f184e1bce4bddf723cd67a14914e99e`. Port.3 was backed up; 38 other JARs and 70 other files were preserved byte for byte. The owned H: build stage, dependencies, process temporary files and raw log were removed after recording hashes; see `evidence/recipe-load-fix-cleanup-26.1.2.json`.
