# Model atlas and older-target startup corrections

## Report and scope — 2026-10-18

The user supplied another agent's report from Fieldframe integration work:
native26 port.14 logged 1,373 missing-texture model warnings (812 `blocks/`,
561 `items/`), native1.21.1 port.7 rejected abstract `InputEvent`, and Forge1.20.1
port.7 failed its six-argument block-drop redirect on Forge47.4.28.
Those warning counts and observed client failures are supplied evidence, not a
new reproduction by this task.

| Target / affected source | Correction | Current evidence |
|---|---|---|
| 26.1.2 / `assets/minecraft/atlases/{blocks,items}.json` | Add plural-directory sources while retaining every original texture ID and PNG | Implemented, resource-packaged and installed; no uncovered own texture references in inspected models; rendering unconfirmed |
| 1.21.1 / `client/GadgetVisuals.java` | Subscribe to concrete `InputEvent.MouseButton.Post` and `InputEvent.Key` | Implemented, compiled and packaged; native class flags and production handlers inspected |
| 1.20.1 / `mixin/BlockLootMixin.java` | Handle the original emission site or Forge's seven-argument overload, exactly once | Implemented, compiled, reobfuscated and packaged; both reviewed Forge layouts have one matching emission call |

New packages are Forge1.20.1 port.8, NeoForge1.21.1 port.8 and NeoForge26.1.2
port.15. All three are packaged. Native26 port.15 is installed in Simply NeoForged;
the older-target packages remain built only. Client rendering/startup remain
unconfirmed. No game was launched or reloaded.

## Native26 atlas coverage

The exact native26 patched merged JAR's vanilla `blocks.json` scans `block/` and
its `items.json` scans `item/`. Our licensed legacy PNGs use `blocks/` and
`items/`; model-file existence alone did not show whether those sprites were
stitched into the selected atlas. This corrects the gap in the earlier static
asset checks.

The block atlas now adds `source: blocks`, `prefix: blocks/`. The item atlas adds
`items` / `items/` and `blocks` / `blocks/`, covering item models that inherit
block models. Existing fluid sprites and all ten ore-particle singles remain.
Vanilla atlas definitions merge with these additions; no `replace` is set.
The other targets' atlas layouts are unchanged. `generate_client_resources.py`
retains these additions during future resource generation.

No pixel art, model geometry, texture references, item IDs, recipes, saves or
rendering code changes. Directory sources also see matching folders from other
loaded namespaces, following the native directory-source behavior.

## Native1.21.1 input

`GadgetVisuals` previously registered `attack(InputEvent)`, but this exact
NeoForge event is abstract. Mouse Post and Key are concrete and run after native
key mappings update. Both delegate to the same private attack method. Its
player/game-mode/screen/buckler guards, attack-click consumption, target handling,
cooldown reset and main-hand swing are unchanged. The source generator applies
the same conversion; native26 generation accepts the concrete handler names.
Forge's existing input implementation is unchanged.

Primary reference: [NeoForge InputEvent source](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/client/event/InputEvent.java).

## Forge block-drop hook

Bytecode inspection of the exact cached Forge47.4.10 and Fieldframe Forge47.4.28
platforms shows the same layout: the six-argument `dropResources` delegates to
Forge's seven-argument overload, and only that overload invokes `getDrops`.
The report therefore exposes a defect in our pinned47.4.10 layout too; it is not
evidence that only47.4.28 is affected.

Two redirect selectors cover the layouts, grouped with `min=1,max=1`; each
selector is individually optional, and the group requires exactly one real
emission call across them. The Forge-added method selector is not remapped;
its native `getDrops` invocation is remapped through the existing refmap.
The shared ToolEffects drop transformation and Forge's XP flag remain intact.
Arbitrary `getDrops` simulations are still untouched. No extra drop or XP call is
added. Unknown future layouts fail explicitly rather than silently disabling
the hook or applying it twice.

Primary reference: [Forge Block patch](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/patches/minecraft/net/minecraft/world/level/block/Block.java.patch).
Local exact-platform bytecode is authoritative for the reviewed versions.

## Release and limits

Compilation was limited to the changed Forge mixin and NeoForge1.21.1 subscriber;
Forge output was reobfuscated before packaging. Native26 is a resource-only
package over the port.14 baseline. Existing classes/resources outside the named
changes must remain byte-identical. New packages embed the source commit,
baseline SHA-256 and changed-source/class identities. Legacy baseline source
linkage remains unknown where it was never established.

No game/test launch, in-game checkerboard observation, zero-warning client log,
JEI visual confirmation, startup pass or drop-mechanics runtime pass is claimed.
No networking/save migration or configuration is needed. Rollback uses the
previous ordinary packages.

## Packaged results and source tracking

The exact source commit is `e9d7877b6647d706becd112256a61d20f1a77f83`.
Both changed classes compiled with implicit compilation and annotation processing
disabled. The Forge class was reobfuscated with the exact Fieldframe47.4.28
named-to-intermediate mapping and platform libraries. An initial renamer attempt
lacked its inheritance libraries and failed; the build helper was corrected and
the recorded final invocation succeeded. The existing refmap is retained;
production calls use `Block.m_49874_`, while the Forge-added overload selector
remains named `dropResources`.

| Playable artifact in `artifacts/` | SHA-256 | Preserved original entries |
|---|---|---|
| `Metallurgy-Reforged-MC-1.20.1-forge-1.4.1-port.8.jar` | `64459f9c2175680178d463200d8dcd82f21cc3bee741e784790821c6c68aa2f0` | 274 other classes, all 1,176 PNGs |
| `Metallurgy-Reforged-MC-1.21.1-neoforge-1.4.1-port.8.jar` | `09e988c2793e0f26bd3eda675699d6fe989dffa4944d48edfe6ba23db67edb1d` | 258 other classes, all 1,086 PNGs |
| `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.15.jar` | `0267ceaebed785679ae692d342ef448105c9f19421dc29bc26bbea434a6862ea` | All 266 classes and 1,175 PNGs |

Source-JAR pairs and their hashes are recorded in
[the package receipt](../SOURCE-PROVENANCE.md#historical-evidence). Archive inspection
also checks Java classfile targets 61/65/69, licenses, metadata, UTF-8 JSON and
absence of bundled dependencies or QA payloads. Every entry outside the named
class/resource/version/provenance changes is byte-identical to its pinned
baseline. The native26 package retains the port.14 gauntlet renderer.

The packaged atlas inspection follows inherited model texture slots against the
exact native26 atlas definitions. It covers 467 block-model roots (3,019 own
texture uses) and 1,489 item-model roots (1,115 `items/` and 2,943 `blocks/`
uses), with zero uncovered atlas references or absent PNGs. These roots include
model templates and variants; they are not a count of runtime warnings or unique
registered items. The actual PNG counts are 424 under `blocks/` and 550 under
`items/`; metadata files account for the larger folder-file counts in the report.

Evidence: [compiler/reobfuscation inputs](../SOURCE-PROVENANCE.md#historical-evidence),
[exact platform and packaged-byte inspection](../SOURCE-PROVENANCE.md#historical-evidence),
and [package identities/deltas](../SOURCE-PROVENANCE.md#historical-evidence).
All 19 disposable compiler outputs, staging archives, raw logs and disassembly
files were deleted from the owned H: directory after recording results and
hashes; see [cleanup](../SOURCE-PROVENANCE.md#historical-evidence).

## Installation — 2026-10-18

At the user's request, native26 port.15 replaced port.14 in Simply NeoForged at
`2026-10-18T11:33:25.7351345-04:00`. The installed SHA-256 matches the playable
package above. The prior port.14 JAR is backed up under
`artifacts/backups/report-fixes-port14/` in the directory named by its SHA-256
`0318dd200a81adb854afb24d9da80ca45c875d25d7bb04be9449f9d4ad24c501`.

All 144 other mod-folder files retain their exact hashes, including Career Bees
cb.2. Launcher configuration and real saves were unchanged. Only the native26
playable package was installed; source JARs and the other two targets were not.
No game was launched or reloaded, so rendered appearance, zero-warning client
logging and successful world login remain unconfirmed. See [the installation
and rollback receipt](../SOURCE-PROVENANCE.md#historical-evidence).
