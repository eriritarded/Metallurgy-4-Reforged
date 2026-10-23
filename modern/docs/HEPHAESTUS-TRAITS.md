# Hephaestus: recovered Metallurgy Tinkers traits

Implemented 2026-10-23 for Minecraft 26.1.2 / NeoForge 26.1.2.122,
Hephaestus Tools 1.0.8 and Hephaestus Forge 1.0.17. New package: port.18.

## Original source and missing coverage

Metallurgy Reforged 1.4.1 **did have its own Tinkers traits**. Its exact source
[`SetTinkerTraits`](https://github.com/Davoleo/Metallurgy-4-Reforged/blob/ef94cb94cfc08ccebe8a0df307ce6b390dcd4704/src/main/java/it/hurts/metallurgy_reforged/integration/tic/SetTinkerTraits.java)
assigns traits to 12 metals, with different head, handle and extra assignments.
Mithril is a TODO with no actual trait. The other metals have no assigned trait
in that source. Our original Forge TConstruct3 port already adapted these
callbacks; the first Hephaestus adapter retained only the native Dense name.

The reviewed PlusTiC 1.12 source is
[`TeamDman/PlusTiC@107f2b7196961b87b7a4db559ff0c703b68067a9`](https://github.com/TeamDman/PlusTiC/tree/107f2b7196961b87b7a4db559ff0c703b68067a9).
Its checked Java sources contain no Metallurgy material registration, and no
Midasium, Tartarite or Vyroxeres references. No PlusTiC Metallurgy behavior is
invented or attributed to it. ExtraTiC was a separate 1.7 integration; its old
original implementation was not recovered for this change. Existing, verified
Reforged/TConstruct2 traits supply the behavior here.

The exact TConstruct2 source archive is 1.12.2-2.13.0.184, SHA-256
`38f03b5008cb0d6cc89a9299d819a461a208d69259c15ecfc2053033f0527a7c`.
Dense, Duritos, Poisonous, Unnatural, Superheat and Enderference were read from
that archive. Original Reforged custom callbacks were read from the retained
legacy source. See [source identities](hephaestus-traits-source-review.json).

## Part assignments and effects

Hephaestus head/handle/binding correspond to legacy head/handle/extra.
Multiple identical parts do not multiply a trait. A tool containing both Vulcan
strengths uses the stronger fire duration. These are 14 distinct behaviors;
Vulcan has two source-assigned strengths.

| Material | Head | Handle | Binding |
|---|---|---|---|
| Midasium | Writable, Duplication | Writable, Duplication | Writable, Duplication |
| Ignatius | Vulcan: 3 seconds | — | — |
| Vulcanite | Vulcan: 5 seconds | Superheat | — |
| Eximite | Enderference | — | — |
| Vyroxeres | Poisonous | Poisonous | Poisonous |
| Damascus Steel | Dense, Duritos | Duritos | Duritos |
| Shadow Iron | Obscure | Obscure | Obscure |
| Sanguinite | Lifesteal | Lifesteal | — |
| Tartarite | Wither | — | — |
| Alduorite | Unnatural | — | Unnatural |
| Desichalkos | — | — | King Dice |
| Deep Iron | Opistognathus | Opistognathus | Opistognathus |

- **Writable:** unlock the next native upgrade slot early, once per tool, capped
  at Hephaestus's five-slot maximum. The old Writable(2) implementation granted
  one slot, not two. The native five-slot menus are preserved; no inaccessible
  sixth slot is added. Existing installed modifiers are retained. The bonus is
  calculated from progression, not used slots, so installing upgrades cannot
  repeatedly unlock further slots.
- **Duplication:** 50% chance to copy actual generated non-player item drops
  after a qualifying direct melee kill. Each copied stack is independent; no XP
  or player drops are duplicated. `enableDuplicationTrait` still controls it.
- **Vulcan:** 3/5 seconds of fire on successful direct melee hits. Effective
  block drops use native smelting recipes. Fortune is already represented in
  the actual generated input drops; obsolete TCon lapis arithmetic is not
  repeated. Cooking XP uses the original once-per-drop-stack stochastic rule.
  Native recipe output counts are respected and split into legal stacks. Silk
  Touch/Squeaky take precedence if commands/datapacks combine them; this adapter
  does not alter Hephaestus's modifier compatibility table.
- **Superheat:** add 35% of the original incoming melee attack amount against
  burning targets, before armor. Other native damage changes remain in place.
- **Enderference:** successful direct hits mark Endermen for 100 ticks with
  `metallurgy:enderference` (amplifier 1); the native EnderEntity teleport event
  is canceled while active. No permanent entity flag or world scan is used.
- **Poisonous:** surviving successful-hit targets receive Poison I, 101 ticks.
- **Dense (Tinkers):** damage-fraction cubic probability `(0.75 * fraction)^3`
  subtracts `max(originalWear / 2, 1)` wear. This replaces our earlier native
  Hephaestus Dense mapping, whose effect was a flat +40% maximum durability.
  Other mods' native Dense materials are unaffected.
- **Duritos:** one wear roll: 10% adds the original wear, 40% subtracts it,
  50% leaves wear unchanged. Dense runs first; subtraction is clamped to zero.
  Native damage hooks and Unbreaking still process the resulting wear.
- **Obscure:** successful hits inflict Blindness III for 30 ticks; block drops
  emit the original 100 smoke particles.
- **Lifesteal:** while injured, chance `0.20 + round(luck) * 0.20` heals 40% of
  actual post-armor health damage. No projectile/secondary-damage healing.
- **Wither:** original 21% chance (`nextInt(100) <= 20`), Wither II, 30 ticks.
- **Unnatural:** effective mining adds the positive difference between the
  tool's native mining level and the lowest native tier that can harvest the
  block. It does not grant harvesting eligibility or invent numeric mod tiers.
- **King Dice:** one of Blindness, Levitation, Hunger, Instant Damage, Nausea,
  Night Vision, Poison, Slowness or Regeneration for 80 ticks.
- **Opistognathus:** selected usable tool grants mining speed ×3 and +2.7
  attack speed while in water. The transient owned modifier is removed on
  deselection, breakage, leaving water, death, logout or dimension change.

No traits are fabricated for the source's unassigned metals or Mithril TODO.
No base Metallurgy weapon/armor effects are granted to Hephaestus tools.

## Optional loading, data and presentation

The native Hephaestus trait enum is not extended. Material role stats instead
carry a `trait` marker such as `metallurgy:vulcanite`. The adapter resolves the
actual tool layout, each part's role, loaded material stats and our source
assignment map in `assets/metallurgy/hephaestus_traits.json`. Replacing/removing
that marker in a higher-priority material datapack removes these effects from
that role; native non-Metallurgy traits remain native. `tiCIntegration` and
`tinkerMaterialsBlacklist` retain control. No per-tick inventory/world search or
per-entity retained cache is introduced.

Parts and assembled tools show trait names in their item tooltips. Shift adds
descriptions; the native part-stat builder also includes the matching trait in
its detailed tooltip lists. The separate workbench/arsenal enum-only trait
panels are not extended; the normal part/tool item tooltips expose this adapter's
traits. Repeated names are suppressed. English strings are supplied as the fallback.
No new bitmap or modified original art is introduced.

All combat, drops, durability, XP and transient player attributes are server
owned; mining speed also computes on the local prediction path. Material IDs
and native construction/components remain unchanged. Native material-fingerprint
refresh handles the changed Dense/role markers on existing tools. The temporary
Enderference effect uses native effect persistence/expiry. With the optional mod
absent, the adapter is not registered and `@Pseudo` mixins have no target.

Existing Damascus Steel tools lose the previous erroneous +40% native Dense
maximum-durability bonus when refreshed. Native recalculation clamps wear to the
new maximum and can mark an already worn tool broken, requiring normal repair;
the tool/parts/modifiers are retained. Rolling back restores the old material
definition and native recalculation; the new temporary effect is unavailable on
port.17. Preserve ordinary world backups before changing gameplay versions.

Three narrowly targeted optional mixins cover `ToolDurability.hurt`,
`ToolUpgrades.unlockedSlots` and `ToolTooltipBuilder.partStats`, all reviewed
against the exact 1.0.8 signatures. Base loading does not instantiate optional
API classes. No Hephaestus implementation classes/assets are bundled.

## Building and current verification state

For a normal native26 Gradle build, supply the author's Hephaestus Tools 1.0.8
JAR as an API-only input:

```sh
./gradlew --no-daemon build -x test -PhephaestusToolsJar=/path/to/hephaestus_tools-neoforge-26.1.2-1.0.8.jar
```

`HEPHAESTUS_TOOLS_JAR` is an equivalent environment option. The reviewed JAR
hash is `6f41198cf8eb8e2b415bcaa5a6be41fa6718a51132b7a94919373c861e3453ed`.
Download it from the [author's project](https://www.curseforge.com/minecraft/mc-mods/hephaestus-tools).
The API input is compile-only; players without this optional mod can load the
ordinary Metallurgy package. Older target build inputs are unchanged.

The eight changed/new classes compile against the pinned native runtime and
optional API. Final source-linked production packaging is recorded separately
after committing source. No game, tests or installation is authorized by this
implementation request. Compilation/static inspection does not establish mixin
application, actual trait activation, tooltip appearance, loot outcomes or
multiplayer behavior. Hephaestus Tools 1.0.9 and other future APIs are unreviewed.
