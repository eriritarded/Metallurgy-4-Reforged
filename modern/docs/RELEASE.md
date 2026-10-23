# Metallurgy modern packages

## Native26 port.18: Hephaestus tool traits

Source implementation 2026-10-23 restores 14 original tool-trait behaviors over
12 source-assigned materials, respecting head/handle/binding roles. Includes
optional API loading, trait presentation, native durability/slot integration and
server-owned effects/drops. See [source identities, mechanics and limits](HEPHAESTUS-TRAITS.md).
Eight classes compile and the ordinary binary/source packages are retained in
`modern/artifacts/`, linked to source commit
`f22613f903acd0e2c62ab2ee340e311caa6b1f2d`. Archive/native-descriptor inspection
preserves all 265 other classes, 1,175 PNGs and 2,504 recipe JSONs. Owned scratch
was removed. [Hashes and receipts](HEPHAESTUS-TRAITS.md) distinguish this
incremental build from a full clean rebuild. No game/tests or installation;
trait activation and mixin loading are unconfirmed.
Forge1.20.1 port.9 and NeoForge1.21.1 port.10 remain unchanged.

**2026-10-23 source review:** the agent's later Hephaestus build of native26 port.17
is the installed package and differs from the earlier gauntlet-only port.17.
The installed and retained Hephaestus hashes match. See [the current fork and
review record](GITHUB-FORK.md). The dated package/install events below remain
historical. This source-publication task creates no new release or runtime claim.

This is the local modern adaptation of Metallurgy4 Reforged1.4.1 with Metallurgy Chests1.2.0 merged into the same `metallurgy` mod. Production artifact identities and archive inspection are recorded in [the artifact manifest](../SOURCE-PROVENANCE.json). The original supplied JARs, real profiles and saves are preserved.

| Artifact target | Required loader | Java |
|---|---|---|
| Minecraft1.20.1 | Forge47.4.10 or compatible47.x |17 |
| Minecraft1.21.1 | NeoForge21.1.262 |21 |
| Minecraft26.1.2 | NeoForge26.1.2.122 |25 |

Use exactly one matching playable JAR. Source JARs are development deliverables, not mods to install. Do not add the original1.12 Metallurgy or Metallurgy Chests JARs to a modern instance. No old world/item conversion is provided.

## Current gauntlet mining expansion

**Packaged 2026-10-18:** Forge1.20.1 port.9, NeoForge1.21.1 port.10 and
NeoForge26.1.2 port.17 give Rubracium gauntlets native iron pickaxe and shovel
harvesting at base speed 6.0, tool enchantments and shovel path/campfire use.
The existing combat, configuration, one-gauntlet recipe and artwork are retained.
See [exact mechanics, hashes and limits](GAUNTLET-MINING.md).

All three gadget adapters compiled and Forge output was reobfuscated. These are
incremental packages based on port.8/9/16. Archive/source inspection preserves
all other classes, PNGs and recipes and confirms shipped adapter hashes. Source
commit `71552e9e53e7b5777c6bbd9018c55e12714c08d6` is embedded in each package;
see [the compilation receipt](../SOURCE-PROVENANCE.md#historical-evidence),
[release receipt](../SOURCE-PROVENANCE.md#historical-evidence) and
[inspection](../SOURCE-PROVENANCE.md#historical-evidence).
Owned scratch has been [removed](../SOURCE-PROVENANCE.md#historical-evidence).
No installation, game or tests; live mining, enchanting and shovel use remain
unconfirmed. The last recorded Simply NeoForged installation remains port.16.

## Previous modern gauntlet recipe correction

**Packaged 2026-10-18:** NeoForge1.21.1 port.9 and NeoForge26.1.2 port.16 make
`metallurgy:legacy/gadget/rubracium_gauntlet` produce one gauntlet, matching its
stack limit. The existing three Rubracium ingots / two Leather pattern and
condition remain; crafting twice supplies a pair. The importer is corrected too.
Both are resource-only packages with every compiled class, PNG and other recipe
byte-identical to the previous package. Forge1.20.1 remains port.8. See
[the correction and hashes](GAUNTLET-RECIPE-COUNT.md) and [package receipt](../SOURCE-PROVENANCE.md#historical-evidence).

Native26 port.16 is installed in Simply NeoForged, with port.15 backed up and all
153 other mod-folder files unchanged. The1.21.1 port.9 package remains built only;
see [the installation receipt](../SOURCE-PROVENANCE.md#historical-evidence).
Runtime remains unconfirmed. No game/tests or Java
compilation were run for this correction; all owned staging files were cleaned.

## Previous model atlas and older-target startup corrections

**Packaged 2026-10-18:** Forge1.20.1 port.8, NeoForge1.21.1 port.8 and
NeoForge26.1.2 port.15 correct the supplied report's Forge drop-hook target,
abstract NeoForge input listener and unstitched legacy model textures. Both changed
classes compiled; Forge output was reobfuscated. The packaged native26 atlas
inspection covers 467 block and 1,489 item model roots with zero uncovered own
texture references. Every PNG is unchanged. See [the implementation, hashes and
limits](PORT-REPORT-FIXES.md) and [release receipt](../SOURCE-PROVENANCE.md#historical-evidence).

Native26 port.15 is installed in Simply NeoForged, with port.14 backed up and all
144 other mod-folder files unchanged, including Career Bees cb.2. The older
target packages remain built only; see [the installation receipt](../SOURCE-PROVENANCE.md#historical-evidence).
No game/tests were launched; startup, rendering,
client warning counts and actual drop behavior remain unconfirmed. All owned
temporary files were cleaned after recording their results.

## Previous native26 gauntlet arm correction

**Native26 port.14, installed 2026-10-18:** the actual player's arm/skin is
drawn inside the Rubracium gauntlet and shares its native swing/equip pose.
Compiled/package inspection passed; all265 prior classes and1175 PNGs remain
byte-identical. Installed in Simply NeoForged with the package hash verified,
port.13 backed up and all 45 other mods-folder files unchanged; see
[the installation receipt](../SOURCE-PROVENANCE.md#historical-evidence).
No game/test launch or rendered-frame confirmation. Other target packages remained
port.7 at that event. The compiled renderer is retained in port.15. See
[the correction and source-commit provenance](GAUNTLET-FIRST-PERSON.md).

## Previous root-icon and companion login repair

The previous Metallurgy packages were Forge1.20.1 port.7, NeoForge1.21.1 port.7 and NeoForge26.1.2 port.13. The main/challenge root icons now use registered Copper/Tartarite ingots instead of the removed wiki item. IDs, requirements and progression remain. These were resource-only Metallurgy packages with all compiled classes byte-identical to port.6/6/12. The older target packages retain port.7; native26 port.14 retains this correction.

The separately maintained Genetic Industry26.1.2 port.2 fixes the logged recipe-sync identity mismatch with one shared stateless recipe instance for JSON and stream codecs. One registration class compiled against the exact installed native runtime; all other addon classes/resources remain unchanged. No new26.2 addon package is delivered here.

Metallurgy port.13 and Genetic Industry port.2 were installed in Simply NeoForged; both prior JARs were backed up and all43 other mods-folder files were preserved. Metallurgy has since been replaced by port.14 as recorded above. No game or tests were launched for that repair, so replacement world login and advancement decoding remain unverified. See [the repair](WORLD-LOGIN-REPAIR.md) and [its package/install receipt](../SOURCE-PROVENANCE.md#historical-evidence).

## Previous JEI chamber category title correction

The previous packages were Forge1.20.1 port.6, NeoForge1.21.1 port.6 and NeoForge26.1.2 port.12. The missing `container.chamber` key now resolves to Sublimation Chamber in English, with the existing chamber name supplied in each of the eight bundled locales. See [recipe viewer](RECIPE-VIEWER.md).

These are resource-only packages, not a new Java compilation: the prior inspected/reobfuscated production classes are byte-identical, as are all non-language assets/data. Only eight language files and version metadata change. All archives passed inspection. Native26 port.12 was installed in Simply NeoForged; port.11 is backed up and all42 other mods-folder files are unchanged. Restart Minecraft to load it. No game or tests were launched; the rendered JEI title remains unobserved. See [that package/install receipt](../SOURCE-PROVENANCE.md#historical-evidence).

## Previous goggles inventory performance correction

The previous packages were Forge1.20.1 port.5, NeoForge1.21.1 port.5 and NeoForge26.1.2 port.11. Etherium goggles no longer scan or render their block overlay behind inventory and other screens. During continuous active gameplay, block discovery refreshes every five client ticks; redstone membership refreshes every20 active ticks. Rendering consumes cached positions rather than discovering blocks per frame. See [goggles performance](GOGGLES-PERFORMANCE.md).

All three compiled and passed archive inspection. Every asset/data resource and all original PNGs are byte-identical to the prior packages. Native26 port.11 was installed in Simply NeoForged; port.10 is backed up outside mods and all43 other mods-folder files are unchanged. Restart Minecraft to load the replacement. No game or tests were launched, and FPS improvement remains unmeasured. See [that package/install receipt](../SOURCE-PROVENANCE.md#historical-evidence).

## Previous magnet mode feedback

The previous packages were Forge1.20.1 port.4, NeoForge1.21.1 port.4 and NeoForge26.1.2 port.10. Sneak-right-clicking the Electrum Magnet now briefly displays its selected mode above the hotbar: Off, Metals Only or All Items. The server sends the new mode using each target's native action-bar API. See [mode feedback](MAGNET-MODE-FEEDBACK.md).

All three compiled and passed archive inspection. Only GadgetLogic, GadgetBridge, native version metadata and the English language resource differ from the previous playable packages. Native26 port.10 was installed in Simply NeoForged; port.9 is backed up outside mods and all43 other mods-folder files are unchanged. No game or tests were launched; the new HUD message remains unobserved in a running client. See [that release/install receipt](../SOURCE-PROVENANCE.md#historical-evidence).

## Previous held-gadget corrections

The previous packages were Forge1.20.1 port.3, NeoForge1.21.1 port.3 and NeoForge26.1.2 port.9. They correct the reported gauntlet poses and gauntlet/detector arm clearance, and add clear detector programming/use/charge feedback. Original pixel art and all earlier handbook, bucket-color and buckler changes are preserved. See [held gadgets](HELD-GADGETS.md) for exact transforms, dimensions, behavior and native references.

All three compiled and passed archive inspection. Native26 port.9 was installed in Simply NeoForged with port.8 backed up and all43 other mods-folder files unchanged. No game/tests were launched; appearance and actual ore-scan output remain unverified. See [that installation receipt](../SOURCE-PROVENANCE.md#historical-evidence).

## Previous handbook, buckler and bucket-color update

The previous packages were Forge1.20.1 port.2, NeoForge1.21.1 port.2 and NeoForge26.1.2 port.8. They remove the wiki item, add the optional [Patchouli handbook](HANDBOOK.md), and enlarge three [buckler GUI icons](BUCKLER-ICONS.md) by8%. The native26 package also registers the fluid capability needed for [colored bucket contents](BUCKET-COLORS.md). All original PNGs are byte-identical to the prior target packages.

All three compiled and passed archive inspection. Native26 port.8 was installed in Simply NeoForged; port.7 is backed up outside mods and all41 other mods-folder files are unchanged. No game or tests were launched for this update. Book loading/rendering and the visual corrections remain unobserved; the previous successful runtime check belongs to the exact port.7 JAR. See [the package/install receipt](../SOURCE-PROVENANCE.md#historical-evidence).

## Previous26.1.2 ore-particle atlas correction

The previous26.1.2 artifact was `1.4.1-port.7`. It corrects the observed `Invalid atlas id` crash by using native `AtlasIds.BLOCKS` for sprite lookup. The generator is corrected too. Only OreParticle.class and native version metadata differ from port.6; original frames, particle settings, all assets/recipes and other compiled behavior are preserved. Build and packaged bytecode inspection passed. It is installed in Simply NeoForged with all38 other JARs and70 files unchanged. The unmodified production JAR subsequently passed an actual-client fresh-world check in an isolated copy of that full pack, including90+ seconds of responsive rendering/client/server ticks, production ore particles and native save/disconnect; the game exited0. See [the correction](ORE-ATLAS-FIX.md), [package/install event](../SOURCE-PROVENANCE.md#historical-evidence) and [runtime check](WORLD-LOAD-QA.md).

## Previous26.1.2 singleplayer recipe-sync correction

The previous26.1.2 artifact was `1.4.1-port.6`. It fixes the logged `neoforge:recipe_content` encoder failure by sharing one stateless value between the detector and Paragon JSON/stream unit codecs. The separate JEI information failure now looks up registered `lemurite_shield`. Build and packaged bytecode inspection succeeded; only Content.class, JeiPlugin.class and version metadata differ from port.5. Assets and recipes are identical. It was installed in Simply NeoForged with all38 other JARs and70 files preserved. See [the correction](SINGLEPLAYER-DISCONNECT.md) and [package/install event](../SOURCE-PROVENANCE.md#historical-evidence). No game/tests launched; replacement login/runtime remains unverified.

## Previous26.1.2 texture/model correction

The previous26.1.2 artifact was `1.4.1-port.5`. It removes unassigned model faces, avoids loading two abstract templates as standalone items and uses native-compatible knuckles bases. The packaged static parent/slot/file resolver reports no missing references across1428 own model roots and11832 texture uses. All265 compiled classes and every PNG are byte-identical to port.4. It was installed in Simply NeoForged with all38 other JARs and70 files preserved. See [the correction](TEXTURE-WARNINGS.md) and [historical package/install event](../SOURCE-PROVENANCE.md#historical-evidence). No game/tests were launched; live resource reload/rendering remains unverified.

## Previous26.1.2 processing recipe correction

The previous26.1.2 artifact was `1.4.1-port.4`. It stores native `ItemStackTemplate` outputs during recipe preparation, reports invalid JSON definitions through `DataResult`, and uses processing snapshot protocol4. All174 bundled processing JSON files and the prior input/shield corrections are preserved. It compiled and was installed in Simply NeoForged, with all38 other JARs and70 other files unchanged. See [the correction](RECIPE-LOAD-FIX.md) and [historical package/install record](../SOURCE-PROVENANCE.md#historical-evidence). No game/tests launched; replacement decoding/runtime remains unconfirmed.

## Previous26.1.2 shield registration correction

The previous26.1.2 artifact was `1.4.1-port.3`. It uses native delayed component initialization for the five shields/bucklers, correcting the user-observed `Components not bound yet` registration failure. Native blocking values and the prior input-listener correction are preserved. Clean packaging succeeded; only the shield class/version metadata differ from port.2. See [the correction](COMPONENT-LOAD-FIX.md) and [historical package/install record](../SOURCE-PROVENANCE.md#historical-evidence). The user’s later port.3 launch reached the title screen, then failed while preparing recipes; see the current correction above.

## Previous26.1.2 input startup hotfix

The previous26.1.2 artifact was `1.4.1-port.2`. It corrects the user-observed abstract `InputEvent` registration failure with concrete mouse and keyboard subscribers. All69 packaged event listeners resolve to concrete classes. The ordinary build succeeded with zero compiler errors; only `GadgetVisuals.class` and native version metadata differ from the prior binary. See [the fix details](INPUT-LOAD-FIX.md) and [historical package/install receipt](../SOURCE-PROVENANCE.md#historical-evidence). The1.20.1 and1.21.1 artifacts retain `1.4.1-port.1`.

## Included adaptation

-49 source materials,39 tool sets,41 armor sets,51 fluids,34 ore profiles, native crafting/processing, machines and65 advancements.
- Five integrated chest tiers with54/72/81/90/108 slots, native menus, persistence, automation and original art.
- Active weapon, armor, ALL, mining, hoe and gadget mechanics, including the source-specific timers, shields, companion, phasing, revival, Paragon and detector behavior. Detailed source values/adaptations remain in the module records.
- Issue388: qualifying Divine Grace applies only to direct native melee, including absorbed Amordrine on Tartarite equipment.
- Original pixel art and sounds, native HUDs/tooltips, detector/goggles/knuckles presentation, active models, effect-specific ore particles, cloud/portal/drip effects, Adamantine sacrificed-armor activation and bounded Vulcanite explosion visuals.
- Validated restart configuration, custom material stats, custom ore dimensions, retrogen, native removable processing recipes and opt-in editable Ender IO alloys.

## Optional compatibility by target

All optional integrations activate only when their receiving mod is present, with their documented configuration gates. Unsupported native APIs are not linked into the base mod.

| Integration |1.20.1 Forge |1.21.1 NeoForge |26.1.2 NeoForge |
|---|---|---|---|
| Patchouli handbook | Optional,97 entries | Optional,93 entries | Optional,89 entries |
| Tetra |39 native material definitions | No reviewed official target API | No reviewed official target API |
| Silent’s Gems / Silent Gear |41 native Gear materials; Gems uses its own modern material definitions |41 native Gear materials |41 native Gear materials |
| Tinkers / legacy Construct’s Armory role | Native TCon3 parts,40 own materials, tool traits, plating/maille armor,738 smeltery recipes | No matching reviewed official API | No matching reviewed official API |
| Spartan Weaponry | API12,897 source weapons/recipes, original layered sprites and native action models | No matching reviewed official API | No matching reviewed official API |
| JEI | Native machine/crafting/subtype/fluid pages | Native adapter | Native adapter and bounded processing snapshot |
| CraftTweaker | Native JSON recipe-manager interface | Native JSON recipe-manager interface | No reviewed API; native datapacks remain available |
| Ender IO |18 native alloys and editable JSON mode | Native adapter | Native adapter |
| Mekanism | Native Lutetium enriching data | Native data | Native data |
| Industrial Foregoing | Native source laser data | Native source laser data | No reviewed native API |
| Thermal |559 native recipes/fuel entries | No reviewed native API | No reviewed native API |
| Immersive Engineering | Native external-heater adapter | Native external-heater adapter | No upstream combination certified |
| ProjectE | Native custom conversion data | Native custom conversion data | No reviewed native API |
| Chisel IMC | No matching official target API | No matching official target API | No matching official target API |

Exact dependency revisions, version ranges, native formats and differences are in the linked module/API records. These are source adapters compiled against the selected APIs, not certifications of live modpack combinations.

## Evidence boundary

The original packaging work used static source/resource/archive inspection and native compilation under the then-current no-game restriction. The user explicitly authorized fresh-world troubleshooting on2026-10-17; that selected26.1.2 client check is now complete. Its separate [receipt](../SOURCE-PROVENANCE.md#historical-evidence) records exact tested production bytes, native creation/login/render/ore-particle/save/disconnect observations and clean exit. Packaging and historical checkpoint records retain their event-specific evidence.

The bounded26.1.2 check observes native startup, fresh-world decoding/login, a live rendered world, ore particles and saving/disconnecting. It does not establish optional dependency combinations, every item/machine/effect, manual controls, audio, external multiplayer, gameplay balance, reopening arbitrary saves or current validation of the other two ports. Earlier headless evidence applies only to its recorded older snapshots. The full runtime validation flag remains false.

The source-role inventory gives all382 Java source files explicit dispositions:363 adapted and19 omitted. File counts are not a gameplay parity percentage. See [SOURCE-ROLE-AUDIT.md](SOURCE-ROLE-AUDIT.md) for retired/unreferenced helpers and unsupported optional APIs.

## Requested installation

On2026-10-16 the user explicitly chose updating Simply NeoForged toNeoForge26.1.2.122 and installing the matching26.1.2 JAR. The instance changed from26.1.2.117 to26.1.2.122 concurrently during preparation; this operation preserved that selection and confirmed official Prism metadata plus all32 loader library sizes/SHA1 hashes.

`Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.1.jar` was initially installed in the instance's actual `minecraft/mods` directory, with SHA256 `74b711967cc7cd90e1a1b6d6dfc7ee2e64c989f01b7d8c51fecdd0d76d4b6f9b`. All37 existing JARs and69 existing mods-folder files were preserved byte for byte. Exact current launcher configuration and the baseline inventory are backed up outside mods; no older Metallurgy package existed. The backup captures26.1.2.122, because the concurrent loader change occurred before the backup. See [the chronological receipt](../SOURCE-PROVENANCE.md#historical-evidence).

The initial26 package failed during the user's launch. It was replaced with `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.2.jar`, SHA256 `82c6d9b87407762b62107ce98995e046ba80565d76d601aea19ed28fb8144047`. The old copy had been disabled during the rebuild and is now backed up outside mods. All37 other JARs and69 other mods-folder files were preserved; launcher configuration was unchanged.

No client, server, test or game was launched by the assistant. The replacement is not confirmed running; the runtime limits above still apply. The1.20.1 and1.21.1 packages remain built deliverables only. The source-project ZIP, packaged NOTICE and production checkpoint preserve their earlier build-time state; this later receipt records installation without changing those artifact bytes.

The subsequent installed package was `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.3.jar`, SHA256 `9b398f528f9ec722bf5aa76fd248dbe6655242c6ce68df2e06cae9fa11b2e39a`. The disabled port.2 copy is backed up outside mods. All38 other JARs and70 other mods-folder files were preserved. No launcher configuration was edited and no game/test was launched by the assistant.

The subsequent installed package was `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.4.jar`, SHA256 `f77095eb9af0208c398764f11869410b2f184e1bce4bddf723cd67a14914e99e`. Port.3 is preserved under `artifacts/backups/recipe-load-fix-20261017T041726Z`. Windows blocked the initial move while the user’s game held it open; replacement completed after the user confirmed the game was closed. All38 other JARs and70 other mods-folder files are unchanged. Launcher configuration was preserved. The assistant did not launch a game.

The subsequent installed package was `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.5.jar`, SHA256 `c49133e771794e6eb04afee2d785b82f9fe0bdf77c54d23b07a6f38f44af5c15`. Port.4 is backed up in `artifacts/backups/texture-warnings-fix-20261017T042617Z`. All38 other JARs and70 other files are preserved byte for byte; launcher configuration is unchanged. No game/test was launched by the assistant.

The subsequent installed package was `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.6.jar`, SHA256 `53ec8266a0e5bfa396564d0fc97d3c1fa0b1aa196419c4dc1da58f6d43eb4b8b`. Port.5 is backed up in `artifacts/backups/singleplayer-join-fix-20261017T151356Z`. All38 other JARs and70 other files are preserved byte for byte; launcher configuration and user saves are unchanged. No game/test was launched by the assistant.

The previous installed package was `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.7.jar`, SHA256 `2c3619326d1af0ea54ff15e608b3984413ea790f57f256c596a128b44caedcef`. Port.6 is backed up in `artifacts/backups/ore-atlas-fix-20261017T152537Z`. All38 other JARs and70 other files are preserved byte for byte; launcher settings and user saves were not modified. No game/test was launched by the assistant.

## Rebuilding

Build maintained native sources with `tools/build.ps1`, using an owned H: scratch directory and the target’s listed JDK. The wrapper resolves pinned dependencies into H:. `jar` and `sourcesJar` are sufficient for ordinary packages; Forge’s `jar` finalizes `reobfJar`. Tests, verification builds, clients and servers are blocked by the current helper/Gradle guard. Temporary stages/caches are removed after delivery; they are not required release payloads. Import helpers are provenance/bootstrap tools, not a routine rebuild step.

A reproducible PowerShell build from the exported source project (with the listed JDKs installed):

```powershell
$metallurgyScratch='H:/CodexScratch/terraria_modding/.test-runtime/metallurgy-port-db483af2b05d489dbffdbf8f18398e4c'
New-Item -ItemType Directory -Path $metallurgyScratch -Force | Out-Null
& ./tools/build.ps1 -Target forge-1.20.1 -Scratch $metallurgyScratch -RefreshSource -Tasks @('jar','sourcesJar')
```

Run from the `MetallurgyModern` source folder; replace the target with `neoforge-1.21.1` or `neoforge-26.1.2` as needed. The included portable storage helper supports the source export outside the parent workspace. H: availability and20GiB free space are enforced by the build helper; C: overrides are refused. No launch or test task is part of this command.

Installation cleanup limitation: four temporary metadata/download/inventory files remain only in the owned H: installation scratch directory because automatic approval review rejected both cleanup commands. See the installation cleanup receipt. Production-build scratch was already removed.
