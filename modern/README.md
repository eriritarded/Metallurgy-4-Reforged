# Metallurgy modern ports

**Native26 port.18, 2026-10-23:** restored Reforged's 14 Tinkers trait behaviors
to 12 Hephaestus materials, with actual part roles and trait tooltips. The eight
adapter classes compile. Production packaging is recorded separately; this
update is uninstalled and not gameplay tested. See [traits and exact translations](docs/HEPHAESTUS-TRAITS.md).

**2026-10-23 source synchronization:** Claude added Hephaestus compatibility and
recorded installing its native26 port.17 build. Its installed hash matches the
retained Hephaestus package; older installation statements below are historical.
The upstream GitHub fork carries all three modern targets on `codex/modern-ports`.
See [the fork, reviewed changes and source workflow](docs/GITHUB-FORK.md).

**Gauntlet mining expansion:** iron pickaxe and shovel mining at speed 6.0, native
shovel use and tool enchantments are compiled and packaged in Forge1.20.1 port.9,
NeoForge1.21.1 port.10 and NeoForge26.1.2 port.17. These new packages are
uninstalled; no game/tests were launched. See
[the behavior and coverage record](docs/GAUNTLET-MINING.md).

**Gauntlet recipe correction:** modern recipes now produce one gauntlet per
craft, matching the item's stack limit. New packages1.21.1 port.9 /26.1.2 port.16
are packaged with all compiled classes and PNGs unchanged; see
[the correction](docs/GAUNTLET-RECIPE-COUNT.md). Native26 port.16 is installed in
Simply NeoForged, with port.15 backed up and all 153 other mod-folder files
unchanged. The 1.21.1 package remains built only; runtime is unconfirmed.

**Latest user-run world disconnect, 2026-10-18:** the fatal recipe-sync stack
names Career Bees' `CareerRecipe`, with Metallurgy port.14 loaded. A corrected
Career Bees cb.2 package is now installed separately, with all other mods preserved;
see [the diagnosis and installation](docs/WORLD-DISCONNECT-CAREERBEES.md). Corrected
login remains unconfirmed.

**Report corrections packaged:** native26 atlas coverage and the older
targets' input/drop-hook startup errors are corrected in port.8/8/15.
See [the report and implementation record](docs/PORT-REPORT-FIXES.md).
Both changed classes compile; Forge output is reobfuscated. Package inspection
finds no uncovered own texture references. Native26 port.15 was installed in
Simply NeoForged, with port.14 backed up and all 144 other mod-folder files
unchanged at that event. Port.16 retains the correction. Runtime remains
unconfirmed; [historical installation receipt](SOURCE-PROVENANCE.md#historical-evidence).

**Gauntlet first-person correction, native26 port.14:** native player skin,
arm/sleeve and the original gauntlet now share one swing/equip pose. The native26
package was compiled, archived and installed, with port.13 backed up and all 45
other mod-folder files unchanged at that event. Port.16 retains its exact compiled
renderer. In-game rendering remains unobserved.
Exact status and limits are in [the correction record](docs/GAUNTLET-FIRST-PERSON.md).

Native source adaptation and ordinary packages for Metallurgy 4: Reforged1.4.1, with Metallurgy Chests1.2.0 merged into one mod, for Minecraft 1.20.1 / Forge 47.4.10, Minecraft 1.21.1 / NeoForge 21.1.262, and Minecraft 26.1.2 / NeoForge 26.1.2.122.

The supplied jars remain untouched. Source revisions and original hashes are recorded in [the baseline](docs/source-baseline.json). Original creator credits and licenses are retained. Current compilation and disposable client checks use task-owned H: storage. Historical headless evidence is recorded separately. The26.1.2 port.16 package is installed in Simply NeoForged onNeoForge26.1.2.122; see [the latest installation receipt](SOURCE-PROVENANCE.md#historical-evidence).

Optional modular-tool source support is described in [Tetra and Silent's Gems / Silent Gear](docs/MODULAR-COMPAT.md), with a [complete material catalog](docs/MODULAR-MATERIALS.md). Its base adapter code compiles; dependency material loading and assembled-tool behavior remain unverified.

The original Tinkers/Construct’s Armory integration is adapted to [native TConstruct3 materials, traits and armor](docs/TINKERS.md) on Forge1.20.1. It compiles alongside [897 native Spartan weapons and recipes](docs/SPARTAN.md). Unsupported NeoForge APIs are not fabricated. On NeoForge26.1.2 the same switches drive [Hephaestus smeltery recipes and Hephaestus Tools materials](docs/HEPHAESTUS.md), written as plain data.

Implementation records: [optional handbook](docs/HANDBOOK.md), [buckler icons](docs/BUCKLER-ICONS.md), [equipment](docs/EQUIPMENT.md), [tools](docs/TOOLS.md), [fluids and tar lakes](docs/FLUIDS.md), [gadgets and shields](docs/GADGETS.md), [configuration](docs/CONFIGURATION.md), [configured mining tiers](docs/CONFIGURED-TIERS.md), [client visuals](docs/CLIENT-VISUALS.md), [advancements](docs/ADVANCEMENTS.md), and [machine compatibility](docs/MACHINE-COMPAT.md), [optional recipes](docs/OPTIONAL-RECIPES.md), [native processing recipes and scripts](docs/PROCESSING-RECIPES.md), [recipe viewer](docs/RECIPE-VIEWER.md), [ProjectE](docs/PROJECTE.md), and [overall status](docs/PORT-STATUS.md). Per-file coverage remains in `docs/coverage.json`; a source adaptation is not a gameplay parity claim.

The port includes a regression fix for upstream issue388: Divine Grace must apply only to the qualifying weapon's direct melee attack, including a Tartarite weapon that absorbed Amordrine. A held qualifying tool must not amplify off-hand arrows, thrown projectiles, spells or other indirect damage.

Current packages, compatibility by target and exact limits are in [RELEASE.md](docs/RELEASE.md) and [the artifact manifest](artifacts/manifest.json). Source adaptation and packaging are complete. Installed26.1.2 port.7 passed an actual-client fresh-world check in the full39-JAR pack:90.051 seconds unpaused,1802 client-world ticks,1794 server ticks and3196 rendered world frames, with production ore particles exercised. The game saved and exited0. See [the selected check and exact limits](docs/WORLD-LOAD-QA.md). Broader three-target gameplay validation remains incomplete.
# Controlled client checks

The explicitly authorized launch/create-world/retry troubleshooting is complete and the owned game is closed. `docs/launch-hold.json` disables further launches for this completed task. The separate private-desktop QA harness uses no Windows input injection and preserves real saves. Ordinary build helpers remain compilation-only.

## Change history

- 2026-10-23: recovered original Reforged/TConstruct2 tool traits for Hephaestus. Added optional role-based combat/mining/durability/drop effects, Enderference, Writable's native slot translation and part/tool tooltips. Corrected the earlier Dense mapping; no traits invented for unassigned materials. Native26 port.18 classes compile; no game/tests/install. See [the complete behavior record](docs/HEPHAESTUS-TRAITS.md).

- 2026-10-23: reviewed Claude's Hephaestus source/installation commits, verified the installed Hephaestus package identity and created the upstream GitHub fork. Added a guarded source-export workflow that preserves gameplay code/assets while keeping local forensic receipts and machine information out of publication. Source-only work; no new build/test/game/install. See [the fork record](docs/GITHUB-FORK.md).

- 2026-10-18: added iron pickaxe and shovel harvesting at base speed 6.0 to Rubracium gauntlets, with native path/campfire use, tool enchantments and mining durability. Port.9/10/17 passed changed-class compilation and archive inspection; Forge output was reobfuscated. Other classes, recipes and PNGs remain byte-identical; owned scratch was cleaned. Built only, not installed or runtime confirmed. See [the behavior and source-linked package record](docs/GAUNTLET-MINING.md).

- 2026-10-18: installed native26 port.16 at the user's request, with the installed hash verified, port.15 backed up and all 153 other mod-folder files unchanged. The separate1.21.1 port.9 package remains built only. No game/reload; live crafting and recipe loading remain unconfirmed. See [the installation record](docs/GAUNTLET-RECIPE-COUNT.md).

- 2026-10-18: corrected modern Rubracium gauntlet recipe output from two to one, preserving the three-ingot/two-Leather pattern and content condition. A pair now requires two crafts. The importer and crafting coverage preserve the adaptation. Native1.21.1 port.9 and native26 port.16 are resource-packaged and inspected; all classes, PNGs and other recipes are byte-identical. Forge1.20.1 port.8 is unchanged. New packages are uninstalled; no game/tests. See [the recipe correction](docs/GAUNTLET-RECIPE-COUNT.md).

- 2026-10-18: installed native26 port.15 at the user's request, with installed SHA-256 verified, port.14 backed up and all 144 other mod-folder files unchanged, including Career Bees cb.2. Older-target port.8 packages remain built only. No game or reload; runtime remains unconfirmed. See [the installation record](docs/PORT-REPORT-FIXES.md).

- 2026-10-18: implemented native26 plural-folder atlas sources, concrete NeoForge1.21.1 input listeners and a grouped Forge drop-emission hook covering its added overload. Packages port.8/8/15 passed changed-class compilation/reobfuscation and archive inspection; 467 block and 1,489 item model roots have no uncovered own texture references. All PNGs preserved and disposable files cleaned. See [the report corrections](docs/PORT-REPORT-FIXES.md); no game/tests or installation change.

- 2026-10-18: added the native player arm/skin and optional sleeve inside the first-person Rubracium gauntlet, sharing its native swing/equip pose and physical arm bone. Native26 port.14 compiled and is installed in Simply NeoForged; port.13 is backed up and all 45 other mods-folder files are byte-identical. Earlier targets remain port.7. No game/tests or runtime visual confirmation. See [the correction and installation](docs/GAUNTLET-FIRST-PERSON.md).

- 2026-10-18: replaced the two removed wiki-item advancement icons on all targets and fixed the separately maintained Genetic Industry addon recipe-sync failure. Metallurgy port.7/7/13 keeps every compiled class unchanged; Genetic Industry26.1.2 port.2 changes one compiled registration class. Both replacements are installed in Simply NeoForged with all43 other mods-folder files preserved and both old JARs backed up. No game/tests; replacement login remains unverified. See [world login repair](docs/WORLD-LOGIN-REPAIR.md).

- 2026-10-17: added the missing `container.chamber` JEI category title in all eight bundled locales, reusing their existing chamber names without color codes. Port.6/6/12 was resource-packaged and inspected with every compiled class byte unchanged; native26 port.12 is installed with all42 other mods-folder files preserved. No Java compilation, game or tests. See [recipe viewer](docs/RECIPE-VIEWER.md).

- 2026-10-17: corrected the Etherium goggles inventory-lag path: screen-gated discovery/rendering, five-client-tick cached block discovery and20-tick redstone membership refresh replace per-frame scan/set/sort work. Port.5/5/11 compiled and passed archive inspection; native26 port.11 is installed with all43 other mods-folder files preserved. No game or tests launched; FPS improvement is unmeasured. See [goggles performance](docs/GOGGLES-PERFORMANCE.md).

- 2026-10-17: added a brief server-owned action-bar message after each Electrum Magnet mode change on all three targets. Native overlay APIs provide the normal fade without chat spam or a persistent HUD. Port.4/4/10 compiled and passed archive inspection; native26 is installed with all43 other mods-folder files preserved. No game or tests launched. See [magnet mode feedback](docs/MAGNET-MODE-FEEDBACK.md).

- 2026-10-17: corrected shared gauntlet first/third-person poses and detector cuff/housing clearance against the native arm model, preserving original pixel art. Added detector programming/scanning feedback, charge instructions and one-based metal labels; audited the existing scan path without claiming a runtime observation. Ordinary port.3/3/9 packages compiled and passed archive inspection; native26 is installed, with all43 other mods-folder files preserved. No game/tests; new poses and scan presentation remain unobserved. See [held gadgets](docs/HELD-GADGETS.md).

- 2026-10-17: replaced the wiki item with an optional Patchouli handbook covering all49 metals and workshop/equipment/fluid use; integration chapters appear only for loaded supported mods. Enlarged the three buckler inventory icons by8% using original art. Forge1.20.1/Neo1.21.1 port.2 and native26 port.8 compiled and passed package inspection; native26 is installed with the bucket capability correction included. All41 other mods-folder files are unchanged. No new game/tests; rendering unverified. See [handbook](docs/HANDBOOK.md) and [icon scales](docs/BUCKLER-ICONS.md).

- 2026-10-17: implemented native26 fluid capability registration for all51 custom buckets to correct gray inventory liquid surfaces. NeoForge's automatic provider only covers exact vanilla bucket classes; the native tint source needs actual contained fluid. Original art/colors/IDs remain unchanged. Port.8 compiled and is installed with the optional handbook/icon update; no new game/test run or rendered tint confirmation. See [bucket colors](docs/BUCKET-COLORS.md).

- 2026-10-17: the separate private-desktop client harness passed fresh-world creation, login,90+ seconds of live rendering/client/server ticks, production ore particles and native save/disconnect in the full39-JAR pack. The game exited0. Four QA setup issues were corrected across the earlier attempts; production port.7 and all40 top-level main mods-folder files are unchanged. See [world-load QA and cleanup](docs/WORLD-LOAD-QA.md).

- 2026-10-17: corrected the logged ore-particle atlas crash after user port.6 reached client-world ticks. Native26 lookup now uses `AtlasIds.BLOCKS`, with the generator preserving that distinction from GPU texture IDs. Port.7 compiled and is installed; only OreParticle.class and version metadata differ from port.6. All assets, particle settings and38 other JARs are preserved. No assistant game/tests; replacement rendering unverified. See [the atlas fix](docs/ORE-ATLAS-FIX.md) and [build/install event](SOURCE-PROVENANCE.md#historical-evidence).

- 2026-10-17: corrected the user-observed singleplayer disconnect during native recipe synchronization. Detector/Paragon unit codecs now share the same stateless value; JEI shield information uses registered Lemurite. Port.6 compiled and is installed. Only Content.class, JeiPlugin.class and version metadata differ from port.5; all assets/recipes and38 other JARs are preserved. No assistant game/tests; live login unverified. See [the disconnect correction](docs/SINGLEPLAYER-DISCONNECT.md) and [build/install record](SOURCE-PROVENANCE.md#historical-evidence).

- 2026-10-17: corrected texture warnings by removing142 unassigned export faces, omitting two false template item definitions and replacing obsolete knuckles base parents. All original PNGs and265 compiled classes are unchanged from port.4. Packaged static resolution passes1428 model roots and11832 texture uses. Port.5 is installed with all38 other JARs preserved; live rendering remains unverified. See [texture/model corrections](docs/TEXTURE-WARNINGS.md) and [package/install evidence](SOURCE-PROVENANCE.md#historical-evidence).

- 2026-10-17: corrected the port.3 world-creation recipe failure by adopting native26 `ItemStackTemplate` outputs, codec diagnostics and processing snapshot protocol4. Port.4 compiled and is installed; all174 recipe JSON files, original assets and38 other JARs are unchanged. Prior port.3 is backed up. No game/tests launched; replacement runtime unverified. See [the recipe fix](docs/RECIPE-LOAD-FIX.md) and [package/install record](SOURCE-PROVENANCE.md#historical-evidence).

- 2026-10-16: corrected the subsequent26.1.2 registration error, `Components not bound yet`. The five shields/bucklers now use native delayed component initialization. Port.3 builds and is installed in Simply NeoForged; only the shield class and version metadata differ from port.2. All38 other JARs are preserved. No game/tests launched. See [the component fix](docs/COMPONENT-LOAD-FIX.md) and [package/install record](SOURCE-PROVENANCE.md#historical-evidence). User clarified one Minecraft window maximum; background compilers are permitted.

- 2026-10-16: corrected the user-reported26.1.2 startup failure caused by registering abstract `InputEvent`. Version `1.4.1-port.2` uses concrete mouse/keyboard listeners. Clean build succeeded; all69 packaged listeners resolve to concrete classes. Only the handler class and native version metadata differ from port.1. Installed in Simply NeoForged, with the disabled old package backed up and all37 other JARs preserved. No game/tests launched. See [the fix record](docs/INPUT-LOAD-FIX.md) and [package/install evidence](SOURCE-PROVENANCE.md#historical-evidence).

- 2026-10-16: installed the matching26.1.2 production JAR in Simply NeoForged following the explicit loader-update choice. Confirmed selectedNeoForge26.1.2.122,32 official loader library hashes and the installed JAR SHA256. Preserved all37 existing JARs and69 existing mods-folder files, and backed up the current launcher configuration. No game or tests launched; runtime remains unverified.

- 2026-10-16: completed source-role dispositions for382 Java files, source-specific particle motion/placements, Adamantine sacrificed-armor activation and Sanguinite corpse presentation. Restored native beacon bases and Tar slimeball tags. Fixed the atlas packaging collision and scaffolding model, archived unused legacy templates, and inspected explicit own resource references. Ordinary JAR/source packages target all3 requested loader versions. No game or tests run; runtime validation and compatible installation remain outstanding.

- 2026-10-16: all targets compile native TConstruct3 material/tool/smeltery/armor support and Spartan API12 support on Forge, JEI fluid information and optional dependency metadata. Spartan retains897 source recipes and90 licensed original sprites. Midasium Writable semantics were checked against the exact TCon2 dependency. See TINKERS.md, SPARTAN.md and optional-tools-compile-checkpoint.json. No game, runtime dependency decoding or installation is claimed.

- 2026-10-16: native JEI detector programming/clearing and all nine Tartarite absorption forms compile on all targets. Previews assemble actual output states; detector lookup groups by programming count and Tartarite by absorbed metal. Source-rule availability is authoritative, including bounded26 snapshot metadata. No game/UI/packet/recipe decoding or installation is claimed.

- 2026-10-16: all three targets compile configured ore mining requirements, an always-active native core tier pack and a full refreshed resource generator. Existing non-ore requirements and stable IDs are preserved. The source-inferred26 client-only JEI sync edge case was subsequently replaced by a dedicated bounded processing snapshot; its26 adapter compiles. Runtime packet ordering/UI remain unverified.

- 2026-10-16: all three targets compile configured optional pack resources and runtime Tetra/Silent Gear stat/tint transformations. Original ProjectE367 fixed values and195 native tool conversions are supplied on reviewed20/21 targets, gated by dependency/configuration and acquisition categories. No game/dependency decoding or installation occurred.

- 2026-10-16: native optional JEI machine categories, original category art/animations, counted transfer guards and recipe-cache lifecycle compile on all targets. Special crafting views and remaining integrations continue; UI/runtime behavior remains unverified.

- 2026-10-16: all three targets compile native managed machine recipes,174 generated data entries per target, removable automatic crushing, custom chamber effect IDs and native recipe synchronization. Runtime decoding/scripts/sync remain unverified; recipe-viewer work continues.

- 2026-10-16: generated native Ender IO alloy data on all targets, source IF laser data on20/21 and559 Thermal recipes on20, with loader/config/content conditions. All target compilers passed; dependency decoding and the remaining customization policy are unverified/unfinished.

- 2026-10-16: added native common-tag crusher fallback, modern raw ore handling, guarded optional IE external-heater capabilities and source Lutetium enriching data for Mekanism. All targets compile, including saved per-route weighted XP. Dependency decoding/loading and gameplay remain unverified.

- 2026-10-16: all targets compile source particle detail/static variants, bounded foot-level Amordrine emitter, corrected four-tick frames, direct-melee Divine Grace visual gating, original active equipment models and ingot tier labels. Remaining motion/placement and optional integration work continues.

- 2026-10-16: all targets compile native detector/monocle visuals, knuckles skin-arm geometry/swing/glint and specialized gadget/packed chamber tooltips. Original art is retained; appearance and runtime injection remain unobserved under the no-game rule.

- 2026-10-16: restored source machine ambience clocks and chamber vapor; all three targets compile. Continued native detector outlines, LED tints/beeps and color interpolation, plus actual redstone block-model monocle overlays. No games/tests or installation occurred.

- 2026-10-16: moved modular integration data into dependency/config-gated native built-in packs; all targets compile. Restored source Potash particles and metal ore explosion eligibility. Old generated data copies were removed only after exact hash comparison. No games/tests or installation occurred.
- 2026-10-16: ported all65 advancement files, eight original trigger IDs and source equipment/ore/effect dispatch; all targets compile. Recorded11 retired, uninstantiated source classes separately from active feature gaps. The full port remains in progress.
- 2026-10-16: restored original ore particle frames, native RGB material/effect tooltips, Krik/chamber HUD source and authoritative Krik height synchronization. All three compilers succeeded. Runtime/client appearance remains unverified; see CLIENT-VISUALS.md for precise scope and remaining work.
- 2026-10-16: added restart-scoped JSON loading of the original configuration definitions, validated material overrides and the initial effect/worldgen/gadget consumers. All three targets compile; additional consumers and synchronization remain in progress. The pinned1.4.1 source has no callers for the optional `setEnchanted` preset methods; no default equipment enchantment presets are invented.
- 2026-10-16: source-adapted the23 active tool/pickaxe/hoe effects and locked ore behavior; all target compilers passed. Added native Tetra/modern Silent Gear materials,51 fluid types/buckets and tar lakes. Added12 gadget/shield items, transient ice domes and detector programming. Continued with phosphorus lamps, roads, Vulcanite TNT and wearable Etherium goggles; see [block gadgets](docs/BLOCK-GADGETS.md). Detailed source behavior and remaining client/config/runtime work are linked above. That historical source checkpoint predates the production packages; current status is in RELEASE.md. Nothing is installed.

