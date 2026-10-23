# Equipment port record

Baseline: upstream tag1.4.1, commit `ef94cb94cfc08ccebe8a0df307ce6b390dcd4704`; exact source hashes are in `source-baseline.json` and `coverage.json`. This record describes implementation, not full gameplay verification.

## Native item construction

All49 original material definitions are retained, with39 five-tool sets and41 four-piece armor sets. Durability, enchantability, mining levels, efficiency, protection and toughness use the original JSON. Extra health and movement are added as native equipment attributes. Armor health, signed knockback resistance and movement are divided by4 per piece. Tool reach adapts to native block interaction reach. Armor JSON fields that the old `ArmorStats` parser ignored are also ignored here.

Axes retain total attribute damage `1.25 * material damage +3` and attack speed `-2.5 - material damage /10`. Modern constructors already add material damage, so their constructor argument is `0.25 * material damage +3`. Hoe damage remains zero and its speed modifier is `material damage -3`.

## Weapons

Source methods are in `effect/weapon/*WeaponEffect.java`. Native boundaries are in each target's `effect/VersionBridge.java` and `EffectEvents.java`. Shared equations and callbacks are in `WeaponDamage`, `WeaponLoot` and `WeaponLifecycle`.

| Metal | Default behavior implemented |
|---|---|
|Amordrine|Missing-health damage curve: `damage * (1 +2 * missingHealthFraction)`. Issue388 fix requires direct player/mob melee damage; held weapons do not amplify off-hand arrows or indirect spells.|
|Angmallen|+75% damage against a victim wearing native armor.|
|Astral Silver|Direct attack damage *1.45 outside the Overworld.|
|Atlarus|Right-click5-block gust, entity push, leaves/vines removal and40-tick item cooldown. Block destruction checks player permissions and the native loader break event.|
|Black Steel|Held weapon absorbs25% incoming damage at durability cost `floor(absorbed *2)`.|
|Carmot|Three delayed pulses at10-tick intervals, targeting other instances of the victim's class within2 blocks. A new hit advances/reset progress as the old bundle did. Float damage is converted to integer for the pulse.|
|Celenegil|Right-click Glory Seeker toggle. Enabled hits gain35%; a lethal hit grants160-tick Speed/Strength, a nonlethal hit gives100-tick item cooldown. Cooldown prevents damage.|
|Ceruclase|75% chance of60-tick Slowness. The old amplifier1000 is adapted to the modern legal maximum255.|
|Damascus Steel|Nonplayer kill XP becomes `XP *3 +2`.|
|Deep Iron|+6 damage while the attacker is in water.|
|Desichalkos|Health damage bypasses armor and absorption, and removes absorption separately. Native hurt processing retains death dispatch.|
|Electrum|Right-click8-block look-ray attraction, nearest eligible target,120-tick cooldown and particle line.|
|Etherium|1/3 chance of+2..4 final damage; heal attacker by that amount, or grant1200-tick Absorption when already healthy.|
|Eximite|True-owner damage *1.5 in the End; End death drops gain0..2 items each.|
|Haderoth|Reborn weapon tracks consecutive target types and adds the streak, capped at12 damage. ALL-category rebirth is a separate prerequisite.|
|Hepatizon|1/8 chance to disarm one occupied equipment slot and alter damageable dropped equipment's remaining durability. Nondamageable items avoid the old zero-bound RNG crash.|
|Ignatius|Loot smelting proc probability `(looting +1) /4`, with original stack counts.|
|Inolashite|10-tick delayed attack with the currently held item against the current last target. Temporary base attack speed is restored in `finally`.|
|Krik|Double damage against airborne targets;25-tick Levitation.|
|Midasium|Loot proc probability `(looting +1) *0.3`; gold conversion probability `0.03 +looting *0.05`. The old arithmetic quirk is retained: otherwise +2 items at Looting0 and +1 at positive levels.|
|Mithril|20-tick delayed magic hit, `max(1, original held item's material damage *0.3)`. An absorbed Mithril effect on Tartarite uses Tartarite's material damage.|
|Oureclase|Double damage against a full-health victim.|
|Platinum|+4 damage and4 seconds fire against undead targets.|
|Quicksilver|11 one-tick steps; dash velocity, overlapping4-damage hits and temporary invulnerability. Completion, death, logout and dimension change clean owned state. Pre-existing invulnerability is restored.|
|Sanguinite|Player damage becomes `min(damage +missingFood,20)`; kills restore `round(victimMaxHealth /5)` food and grant80-tick Hunger.|
|Shadow Iron|1/6 chance of a random1.2..3.2 damage multiplier. The configured upper bound is read from EffectTweaksConfig.|
|Shadow Steel|Darkness-dependent temporary attack speed, or extra damage in bright light;40-tick effect lifecycle.|
|Vulcanite|Right-click priming,50-tick item cooldown and timed self-explosion; hitting while primed instead explodes at the target with source-derived power and knockback. Explosions do not destroy terrain. The old initial null-player timestamp behavior is retained.|
|Vyroxeres|60-tick Poison stacking from the prior amplifier.|

`VulcaniteIgnatiusSwordEffect` is deprecated and never instantiated upstream; it is not an active weapon ability.

The old effect table did not define a stable ordering across handlers. The port uses deterministic stages: ALL-category incoming effects, weapon hurt effects, Divine Grace, then final-damage weapon callbacks. Target-native NeoForge incoming-damage hooks occur earlier in the native damage pipeline than Forge1.20.1's hurt hook; remaining equivalence checks must account for shields and invulnerability frames.

Original ten-frame particles, source-specific motion/placements, audio, active models and specialized tooltips are implemented; see CLIENT-VISUALS.md for exact compilation evidence and rendering limits.

## Tartarite and ALL-category effects

These additions are newer than the weapon/machine verification checkpoints and still require fresh compilation/runtime checks.

- Tartarite uses one dynamic recipe for all9 original equipment forms. The cross's top block selects the metal; the other3 blocks may differ. Existing item data, enchantments and damage are copied. Absorption uses original `paragon`, `paragon_absorbed` and `durability_boost` keys. Armor counts double for absorbed effects, capped at4.
- The original durability equation is `(base +infused) / (base -infused)`. Positive finite results are retained. Only an invalid/nonpositive result falls back to combined durability, since modern `MAX_DAMAGE` requires a valid positive integer. Metals with neither tool nor armor stats are rejected instead of triggering the old null dereference. Native1.20 item hooks read the boost; modern targets store native `MAX_DAMAGE` on the result.
- Haderoth rebirth restores tools at their final remaining use, gives25x base durability and+2 attack damage, and doubles mining speed. Armor rebirth triggers at the source's projected damage threshold. Its original replacement of default armor attributes is retained: base protection+2 and base toughness+0.5, without the overwritten extra health/movement attributes.
- Mithril equipment derives its arcane bonus from enchantment count, with the mining-speed counter capped at5. Source armor modifier identity is shared across slots. The source's extra weapon attack bonus and speed restoration are retained.
- Platinum tools become unbreakable on main-hand equip. Armor is excluded.
- Prometheum has50% chance to repair1 durability every100 ticks in Overworld daylight under visible sky. Shadow Steel's repair interval varies with darkness.
- Adamantine equipment degrades when the carrier's food is below85%, with the source food-dependent interval. Eating repairs carried/equipped Adamantine by twice nutrition.
- Orichalcum kills repair equipped matching pieces by the victim's maximum health.
- Amordrine equipment is removed from player death drops, retained on the old player and copied at death respawn unless `keepInventory` already applies. Sanguinite equipment is destroyed from death drops.

Effect configuration and active armor/tool/hoe callbacks are implemented. Original default-enchantment preset setters have no source callers; no invented presets are added. Connected-client behavior and broader runtime lifecycle remain unobserved.

## Evidence boundaries

- `weapon-forge120-server-pass.json`:7 required headless Forge fixtures passed. Includes12 Divine Grace damage cases and selected weapon/equipment behavior.
- `machine-forge120-server-pass.json`:9 required fixtures passed after machine and crafting additions. These do not include the newer ALL/Paragon changes.
- The three-target compile receipts identify exact historical source snapshots. They do not prove the current source, a packaged production JAR, a graphical client or multiplayer behavior.

## Armor callback port in progress

Source: pinned upstream1.4.1 commit `ef94cb94cfc08ccebe8a0df307ce6b390dcd4704`, `effect/armor/*ArmorEffect.java`. Defaults below are implemented in `ArmorEffects` and `ArmorLifecycle`; target damage/event/persistence boundaries are native. This is not a claim that all armor effects or client controls are finished.

| Metal | Implemented default callbacks |
|---|---|
|Atlarus|Every living tick, deflect incoming projectiles or entities moving at least0.6 blocks/tick in a3-block expanded wearer box. Excludes the inner1-block expanded box. The projected path must intersect the wearer. Reverse normalized motion, add independent `(1 - Gaussian *2) *0.252` noise per axis, then multiply by0.5; update rotation and velocity. Zero motion is safely ignored instead of producing invalid coordinates.|
|Ceruclase|Every living tick, players debuff mobs and nonplayers debuff players in x/z±8, y±4. Weakness and Mining Fatigue last10 ticks at amplifier `max(level -2,0)`. A last fire damage source gives50% extinguish chance each tick. The source comment says every second, but its callback has no such gate.|
|Celenegil|Player's sixth incoming hurt callback clears all potion effects, grants RegenerationII for `20 +20 *level` ticks, and repels other entities within x/z±5,y±1. The source inactive timer is never started, so no automatic hit-count decay is invented.|
|Desichalkos|Player shields regenerate after200 quiet ticks, one charge per140-tick entity-age boundary, up to armor level. Incoming hurt consumes one charge and resets quiet time. Existing charges retain the source behavior when unequipped; a smaller equipped level clamps them.|
|Ignatius|Fire immunity; player lava immunity starts a10-tick-step timer and ends at step `level *4`, then applies200-tick armor cooldown. Nonplayers do not use that timer. Lava healing retains source precedence: `tickCount %25 -level *5 ==0`, heal `incoming *level /6`. Water/rain deals1 armor-bypassing damage every10 ticks unless Fire Resistance is active; drinking deals2. New native damage type `metallurgy:water_damage`.|
|Prometheum|Crouch begins four10-tick steps; odd steps sample crouch, even steps grow nearby plants if crouch changed. Each growable in x/z±3,y±1 has `level *0.1875` chance, with at most one grass block. Successful growth damages matching armor2 each. Completion gives180-tick cooldown. Native bonemeal hooks and player edit permission are respected.|
|Shadow Steel|In darkness above0.5, incoming hurt starts100-tick player speed multiplier `0.2 *level`, with later hits evaded during that interval. Completion gives `20 *(30 -5 *level)` cooldown. In light, nearby living entities within x/z±3,y±1 take `3 *level` fire damage and `20 +10 *level` ticks Blindness. Nonplayers use the source temporary Speed potion branch. Owned speed modifiers are removed on unequip/death/logout/dimension change.|
|Vulcanite|Player's sixth incoming hurt callback explodes once at every nearby entity within x/z±5,y±1, power `0.7 +0.1 *level`, no terrain destruction. Above level2, explosions create fire and grant200-tick Fire Resistance. Its source inactive timer is also never started.|

The other17 passive armor classes are mapped in `tools/update_coverage.py`. Source-derived client particle models, activation HUD, connected-client movement, all callback ordering combinations, and configuration still need work. NeoForge's incoming damage hook occupies a different pipeline point from Forge's attack/hurt callbacks; shield and invulnerability equivalence is not established by these selected fixtures.

### Chamber checkpoint

`chamber-native-first-checkpoint.json` records17 custom fixture passes on each NeoForge target (26.1.2 additionally includes `minecraft:always_pass`). Forge's first compile needed public `getDrops`. `armor2-first-failures.json` records the subsequent Forge mock-player network-channel fixture failure and the removed two-BlockPos AABB constructor on NeoForge. These failures were recorded before disposable files were removed; corrections require fresh passing evidence. No client was launched.

## Movement and controls implementation

The source roles are `AmordrineArmorEffect`, `InolashiteArmorEffect`, `KrikArmorEffect`, `QuicksilverArmorEffect`, `EtheriumArmorEffect`, `PacketAmordrineJump` and `PacketKrikEditPlayerLevel`. `ArmorControls`, `ArmorMovement` and `ArmorPhasing` implement their native equivalents. New movement/mixin changes still require a fresh checkpoint; `controls-first-checkpoint.json` identifies the earlier26-fixture NeoForge1.21.1 pass, before phasing and Quicksilver collision were added.

- Amordrine: press jump again while airborne for up to one extra jump per armor level. Native jump plus0.15 vertical velocity, fall-distance reset and dragon flap. The server derives the budget from actual equipped items, rejects unknown actions, and limits requests per tick. Landing resets the budget; removing the armor clears it. The old packet's client-supplied maximum is intentionally not trusted.
- Krik: a full set enables the remappable Up/Down arrow controls. Maximum level equals empty inventory slots9..35; target height is level*9, preserving the old integer `255/27` arithmetic. Below target, vertical velocity becomes0.4; at rounded target it becomes0. Full set cancels fall damage. Native client movement/flight checks and original levitation HUD need connected-client review.
- Inolashite: records block position, health, food and saturation every5 player ticks in a bounded queue,8 snapshots per armor level. Sneak-jump schedules the oldest snapshot for10 ticks later. Cooldown is `2^(7-level) *20` ticks for the four native armor item types. Source food/saturation-delta arithmetic is retained, with invalid negative resulting saturation clamped to zero. History and pending recalls clear on death/logout/dimension change, preventing cross-world recalls. The recall fixture overrides only network teleport delivery for a fake player; it does not establish connected-client teleport behavior.
- Quicksilver: sprint acceleration lasts120/80/40/10 ticks at levels1/2/3/4. Maximum total speed multipliers are0.05/0.15/0.30/0.50. At maximum, native water collision becomes a full block; lava also does so at level3+. Step height becomes1. Forge restores the prior field value it owns; NeoForge uses an owned+0.4 step attribute, preserving unrelated modifiers. The source cancels `IN_FIRE` at maximum with level3+, not arbitrary fire damage. Stopping or unequipping removes owned changes.
- Etherium: sneaking next to colliding blocks starts/resumes a30-step,10-tick phasing clock and holds vertical motion at zero. The clock pauses away from collisions. Bedrock retains the source0.1 horizontal repulsion. Completion gives400-tick item cooldown. Server authority sends the owner's client a phase flag. Modern cleanup restores collision on sneak release, unequip, death, logout and dimension change; if ending inside a solid block, a still-safe recorded entry position is used. This fixes the old possibility of remaining in paused no-clip indefinitely. The configured block blacklist and default bedrock behavior are implemented.

The bounded native collision hooks are `LivingArmorMixin` (after Player's normal per-tick noPhysics reset, before living movement) and `LiquidBlockMixin` (the exact missing full-block liquid collision hook). Forge uses [MixinGradle's documented refmap integration](https://github.com/SpongePowered/MixinGradle); NeoForge uses its native mixin configuration. Neither compile success nor server collision fixtures establish graphical/client movement quality.

## Corpse and companion lifecycles

- Sanguinite uses the original slain mob. On its native death clock reaching16, a kill credited to a player with Sanguinite armor marks corpse stage1 and resets entity age. It holds death pose18 for20 ticks, then reverses the death clock by2 per tick, restoring40% max health and marking stage2. The native dead flag is cleared through a bounded accessor. Stage2 cannot revive again; its next death additionally invokes the original loot-table callback, retaining the source's extra final loot. Modern Minecraft already drops XP during the first native death, so the old delayed-XP workaround is not duplicated. Tracking clients receive corpse stage, including when they begin tracking late. Player victims retain native respawn rather than attempting to resurrect a dead player behind their respawn screen. Custom mobs that bypass native `LivingEntity.tickDeath` require separate compatibility review. Native corpse body tint and shake now use client-only render adapters. Source color100/25/25 and Gaussian jitter0.01 are retained; the old immediately popped translation is replaced with a transform that lasts for the entity render. Native20 uses float vertex color,21 packed ARGB,26 extracted render-state ownership and the native model tint. Existing tint and alpha are multiplied. Current runtime injection and appearance remain unobserved.
- Damascus Steel summons `metallurgy:pierknight` when a living attacker damages an equipped owner and its sampled armor item is not on cooldown. One active owned UUID is tracked. Maximum health is `10 *armorLevel`, base attack `2 *armorLevel`, movement speed0.3; its held Damascus sword does not add its normal item attack attribute to that base strike. Native enchantment damage, knockback and post-attack effects are adapted per target. The source's40-tick arrival suppresses AI; its30-second lifespan decreases only while idle with no target. It follows the owner at1.1 speed, starts beyond10 blocks, stops within2, recalculates every10 ticks, and may teleport to a safe outer5x5 ring when pathing fails beyond12 blocks. Owner revenge and attack timestamps provide its first two target priorities, followed by native retaliation. Owner absence/armor loss ends the summon; departure takes40 ticks and clears the owner identity, with200-tick armor cooldown. Stale unloaded summons lose authority when a newer owner UUID is established, preventing two active owned companions when chunks return.
- Knight persistence retains `OwnerUUID`, `PierThickness`, `PierLifespan`, `WidePutin` and `VanishTime`. Original `pierknight_skin.png` and player model layout are used. Renaming an equipped armor item to include `putin` retains the original wide3x rendering. Body alpha and standing death rotation are implemented; held-item fade and client appearance have not been visually checked. Summoned equipment drops nothing.

`corpse-first-checkpoint.json` records29 custom Forge and29 custom26.1.2 fixture passes before the knight addition. The earlier Forge step assertion assumed a default below1; the corrected assertion compares against the actual pre-effect value. Controlled damage victims now disable gravity and wear a zero-armor pumpkin where needed to exclude falls/sunlight. Neo1.21.1's corpse loot invoker initially used the newer signature, causing a recorded startup failure; its actual two-argument method has been restored in maintained source and needs a fresh run.
