# Client visuals and user interface

## Held gadget correction — 2026-10-17

Gauntlet poses, sleeve clearances, detector housing clearance and detector instructions are corrected in [HELD-GADGETS.md](HELD-GADGETS.md). Original PNGs are retained. This source review/build is separate from in-game visual or scan confirmation.

The native26 missing first-person arm has a separate [player-skin/attachment correction](GAUNTLET-FIRST-PERSON.md). It uses the native avatar hand renderer; actual client rendering remains unobserved.

## Handbook and buckler icons — 2026-10-17

The optional [Patchouli handbook](HANDBOOK.md) replaces the wiki item and reuses its original cover art. Three [buckler GUI icons](BUCKLER-ICONS.md) are8% larger, preserving their geometry and pixel art. These new resources have not been observed in a game.

## Bucket fluid colors — 2026-10-17

The native26 bucket model's color comes from the actual item fluid capability. Port.8 registers the native handler for all51 `MetalBucketItem` subclasses, which NeoForge's exact-class fallback omitted. Original models/colors/art remain unchanged. See [BUCKET-COLORS.md](BUCKET-COLORS.md) for the reviewed native call chain, implementation, packaging/install state and runtime limits.

This module records source adaptations, not observed rendering. The user prohibits all assistant game launches, including servers/headless checks. Compilation and static inspection are separate from user-observed runtime evidence. The 26.1.2 package is installed in Simply NeoForged; see [the input listener loading fix](INPUT-LOAD-FIX.md) and [the subsequent shield component registration fix](COMPONENT-LOAD-FIX.md) for user-reported failures and corrections. Real saves are preserved.

## Original ore particles

Source1.4.1 `block/BlockOre` and `particle/ParticleOre` define exposed-face sampling, source material RGB, scale1.5, detail `(tier-2)/5*9`, lifetime32 ticks and shrinking over the final30% of life. Ten byte-identical original `ore_particle_1..10` textures are declared in the native block atlas. Native26 uses `SingleQuadParticle` and the opaque terrain layer; earlier targets use `TextureSheetParticle`. `enableOreParticles` gates ore display ticks; tiers0/1 do not emit. Neighbor occlusion uses the native solid-render predicate.

The native class supports both low random motion and an explicit-motion constructor. Equipment currently uses a bounded server-to-client `ParticleBurst`: original frames, material palette, count and scale, uniform positions in entity bounds and one visual seed. Recipients are within64 blocks in the same level. Counts clamp0..128, scale0..16, detail0..9; nonfinite inputs are rejected/normalized. Weapon detail/static profiles now use source values: Deep Iron, Hepatizon, Krik and Oureclase are static; other profiles use their individual source detail levels. Armor variants restore Black Steel9, Eximite static3, Ceruclase static5, Shadow Iron2, Vulcanite3 and Desichalkos charge-dependent detail. Effect-specific motion and placement follow-up is recorded below. The frame step is corrected to source integer `32/7` = four ticks. Compilation of the existing adapter is recorded in `evidence/visual-bursts-confirmed-compile-checkpoint.json`. Visual protocol4 additionally carries uniform per-axis motion ranges, explosion samples and the sacrificed armor activation. Armor/processing payloads retain their separate existing protocol versions.

## Material tooltips

Source `util/ItemUtils`, `BaseMetallurgyEffect` and `gui/TooltipRenderHandler` are adapted through `MaterialTooltips`, `MaterialItems` and native `ItemVisuals`. Titles use native RGB styles. Metal blocks with multiword suffixes are recognized. SHIFT shows original statistics, tier stars, colored remaining durability and efficiency. Enabled effect names and CTRL descriptions resolve `<PC>` and `<NL>`. Tartarite lists its own and its absorbed material's eligible effects. Platinum ALL descriptions apply to tools, not armor; Quicksilver's descriptive ALL entry remains. Stats hiding and border-color settings are independent.

Forge1.20.1 and NeoForge1.21.1 use the native tooltip color event.26 uses a texture event and client-only mixin to tint one white frame at extraction time, including configured material colors. Two flat32x32 tooltip UI sprites are authored by explicit pixel coordinates in `generate_client_resources.py`; original gameplay art is unchanged. The redirect uses the inspected `TooltipRenderUtil.extractTooltipBackground` descriptor. Compilation does not establish successful runtime mixin injection or appearance. Initial source evidence: `evidence/client-ui-confirmed-compile-checkpoint.json`.

## HUD and chamber item renderer

Original128x128 Krik artwork, positions and27-slot maximum are retained. `VisualNetwork` sends server-validated selected height on initial state/change, bounded0..27. A weak connection-scoped server-player map is cleared on cleanup. Client state is associated with UUID. Flight authority stays on the server; no client-supplied limit is trusted.

The chamber HUD shows actual metal/fuel stacks and counts, effect icon/name and elapsed active seconds near the crosshair. It never changes inventory counts, fixing the source HUD's `metalStack.setCount` mutation. Client elapsed time advances while LIT and is corrected by server block-entity updates every20 ticks. Inventory, fuel and effects remain authoritative. Original Krik artwork and vanilla effect icons are used. Chat and hidden-GUI modes suppress the HUD.

The source chamber special renderer displays the actual metal item at(0.5,0.25,0.5), scale1.7, in ground context. It has no animated machine geometry.26 extracts `ItemStackRenderState` and submits through the native collector. All targets compile in the visual-bursts receipt. Nonmetal ores also restore the source75% explosion eligibility rule before native decay.

## Chamber vapor and machine sound

Source `BlockChamber.randomDisplayTick` emits15 effect-colored particles per horizontal face:60 total. x/z use0.3+random*0.8 plus face offset0.5; y uses0.1+random*0.3. The native source retains these bounds and actual potion RGB.1.20 uses ENTITY_EFFECT velocity channels for color; the newer targets use opaque `ColorParticleOption`. LIT gates display, avoiding a stale client fuel counter.

Crusher ambience begins at processing tick120 and repeats330; alloyer begins90 and repeats370; chamber begins elapsed30 and repeats300. Server clocks dispatch bounded same-level `SoundCue`s within64 blocks. Original OGGs play in BLOCKS category at volume1/pitch1. Source pause/stall conditions send stop cues. Client instances also stop on inactive/replaced/unloaded blocks or world changes; finished instances are removed, and repeated cues replace old instances. All targets compile in `evidence/ambience-confirmed-compile-checkpoint.json`. No sound playback, audibility, duration or live lifecycle check has occurred.

## Detector and Etherium goggles

The detector scans loaded blocks in a17x17x17 cube centered on its holder once per client tick while held and cooldown>0.2. Either hand works; source offhand preference is retained when both hold detectors. Full-block outlines use programmed source metal RGB. Original three model tint indices blink by RGB complement in the latter half of each10-tick interval. Each matching LED plays its original beep once at tick5, avoiding duplicate sound from repeated tint queries. A/B/A programming lights both matching LEDs. The source color clock restores charge-bar interpolation. No chunks are loaded/forced, and the client never asks the server to reveal hidden ores.

Goggles identify current native Redstone-tab blocks in an eye-centered17-cube, sort far to near, and draw real block states at full brightness through walls. The [inventory-lag correction](GOGGLES-PERFORMANCE.md) suppresses goggles work behind screens and caches discovery on a five-client-tick interval, with redstone membership refreshed every20 active ticks. Rendering reads cached positions and rechecks block membership. Forge/1.21 use dedicated no-depth render types without mutating global depth state.26 extracts frame-local block model states, submits a custom no-depth pipeline and collects always-on-top ore gizmos before native finalization. Runtime accessor injection, special block renderers and interaction with other rendering mods remain unverified. Historical adaptation compilation is in `evidence/detector-confirmed-compile-checkpoint.json`; current correction build/install evidence is separate in `evidence/goggles-perf-release.json`.

## Knuckles and specialized gadget tooltips

The renderer retains the source1x2x5 knuckles cube at(1,0,0.5), original16x16 texture,12x4 arms (3 pixels for slim skins),64x64 skin UVs and0.25 sleeve inflation at(0,-11,1). Native ModelPart scale2.4 reproduces source0.15. First-person swing translates bysin(pi*swing)*0.8 and rotates Z by-30*sin(pi*swing) degrees. Both hands choose appropriate UVs; arms appear only in first person. GUI, ground and fixed contexts retain the original flat icon. Source hand transforms are converted explicitly to native JSON units.

Forge/1.21 use a stateless baked-model selector and lazy item renderer, removing the old global last-perspective field and per-frame model bake. Item can invoke `initializeClient` before `GadgetItem.id` is assigned, so registration deliberately reads no instance fields.26 uses a native special-model codec, extracted skin/slim/swing data, model-part submission and native foil rendering. No client has observed pose, skin, reload or glint.

Tooltips show programmed detector metals in source colors, magnet description/current mode, original shield/goggles descriptions and packed chamber effect/elapsed min:sec. Packed state is read without creating/loading a block entity. Invalid/empty effects are ignored.1.20 reads native block-entity NBT;1.21 copies CustomData;26 copies TypedEntityData without its type ID. All targets compile in `evidence/knuckles-confirmed-compile-checkpoint.json`; maintained/staged manifests match.

## Jump emitter and equipment model states

The source Amordrine jump emitter now samples a fixed foot-level box expanded0.7 in X/Z for10 ticks,16 particles per tick, random scale0.5..1.5, static detail4 minus previous jumps, and downward motion-0.03. It follows the source snapshot box rather than moving with the player. Client emitters are bounded64, last at most100 ticks, pause with the game and are discarded on world changes. Motion clamps to finite[-16,16]. Divine Grace visual bursts use25 particles, scale1+missing-health*2 and floor(missing-health*9) detail; the same direct-native-melee condition as the issue388 damage fix gates them.

Celenegil glory_seeker and Electrum voltage_control now select original active tool artwork, including valid Tartarite absorption states. Older targets use native item predicates;26 uses component predicates. Ingot tooltips show source ore/alloy-ingredient tiers with bounded native colors. All three targets compile in `evidence/particle-variants-compile-checkpoint.json`; maintained/staged manifests match.

## Remaining work and limits

Native lifecycle/rendering evidence remains unavailable under the launch prohibition. Retired Enderman item-stealing AI belongs to an uninstantiated old Desichalkos effect (`inert-source-audit.json`), not an active missing feature. No current game, multiplayer latency, UI scaling, rendered frame, sound or runtime mixin evidence is claimed. Historical fixtures apply only to their recorded snapshots. Disposable stages/raw logs are removed after exact receipts are recorded; the final owned reference/dependency copies are deleted after production receipts and deliverables are preserved.

## Per-effect follow-up (compiled)

`EffectParticles` now retains Etherium attacker-directed motion and asymmetric0.5-cube placement, Mithril delayed eye-height drift, Platinum eye-position scatter, Ignatius tool/weapon rising particles and Midasium per-drop scatter. Armor uses the original Celenegil outward block burst, Shadow Iron front point, Carmot two use-tick particles and Platinum two-front/five-body split. Inolashite recall uses25 body particles; the source commented debug-point burst is excluded. Its charged ring retains20 angles at pi/10 increments.

Tool callbacks restore Kalendrite16 particles at scale2.5, Angmallen20 rising particles, Oureclase five static particles, Midasium hoe ten drifting particles and Prometheum20 per affected crop drop. Quicksilver, Astral Silver and Sanguinite mining-speed callbacks retain explicit zero/random motion where a native server speed callback occurs; native client-only callback timing remains unobserved. The explicit shader-independent motion ranges are transmitted once per burst and sampled independently per particle. Effect particle count remains bounded128 per message. Visual RNG does not claim the same sequence as the old independently seeded client/server code.

Atlarus weapon clouds use source uniform positions and explicit velocities; the hoe emits five outward/upward clouds per selected surface column. The removed legacy `MaterialTransparent` lookup is adapted to the first empty block in the original five-block upward search. Inolashite tunneling emits128 native portal particles along the old-to-new position path, rather than substituting colored ore particles at the destination. Potash fertilizer drips sample the full unit cube uniformly.

Adamantine revival uses the source fixed body box for40 ticks,16 particles/tick, random scale0.5..1.5, dynamic frames and material ore tier minus2. A native client activation payload displays the actual sacrificed armor stack, including its components, only for its wearer. The old vanilla totem broadcast is replaced by this source-specific visual; the native used-totem advancement remains server-authoritative.

Vulcanite TNT reads the native detonation affected-block list at lowest event priority without modifying it. A bounded4096-position spread sample produces the original one-third flame/two-thirds material-colored ore mix, scale3, detail6/7, with the source outward velocity equation for radius6. The native terrain blast and its server packets remain authoritative. The sample cap is a deliberate visual/network bound; very large mod-altered blasts do not produce an unbounded packet or particle workload. The source uses ore particles, not textured block debris. Ordinary explosion sound/flash remains native. All three targets compile with exact maintained/staged source matches in `evidence/particle-fidelity-compile-checkpoint.json`. No runtime packet, emitter, particle appearance or overlay check was run.

## Sanguinite corpse presentation

Client-only native render adapters multiply body RGB by100/255,25/255,25/255 while preserving alpha and any existing native body tint. Stage1 and revived stage2 receive Gaussian horizontal jitter with standard deviation0.01. Native20 changes the original float vertex-color arguments;21 changes packed ARGB;26 records the entity flag with its extracted living render state, then composes with native model tint during submission. Weak render-state keys prevent retaining discarded states. Rendering stays in the caller's native pose scope; no global graphics color is changed. Native equipment/render layers keep their own tint rules.

The original1.12 handler popped its translation before returning, so its shake did not survive into the actual draw. The new transform deliberately lasts for this entity render. This restores the source's visual intention without reproducing that ineffective matrix operation. Compilation/packaging is recorded separately; no runtime injection, rendered corpse or compatibility with other render mixins is claimed.

## Texture/model warning correction, 2026-10-17

Port.5 corrects the known missing texture-slot, false template-item and obsolete knuckles-parent warnings. Original PNGs and all port.4 class bytes are preserved. The packaged static resolver follows1428 own roots and11832 texture uses without missing slots/files. See [TEXTURE-WARNINGS.md](TEXTURE-WARNINGS.md) for exact resources, source provenance, generator changes, installation and runtime limitations.

## Ore atlas lookup, 2026-10-17

The user’s port.6 run reached client-world ticking, then failed creating an ore particle because native AtlasManager expects a logical atlas ID. Port.7 uses `net.minecraft.data.AtlasIds.BLOCKS`, preserving all ten original frames and particle behavior. GPU texture bindings retain their physical texture location. The native generator also preserves this fix. See [ORE-ATLAS-FIX.md](ORE-ATLAS-FIX.md) for exact API evidence, packaging, installation and live-rendering limits.

## Magnet mode action bar, 2026-10-17

Sneak-using the Electrum Magnet now sends its new mode to the holder's native action bar above the hotbar. Minecraft supplies the brief display and fade; subsequent switches replace the message. The server sends one message after changing the saved mode. See [MAGNET-MODE-FEEDBACK.md](MAGNET-MODE-FEEDBACK.md) for labels, target APIs, coverage and release evidence. No custom renderer or persistent HUD is introduced; client presentation remains unobserved.
