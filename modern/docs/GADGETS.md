# Gadgets and shields

**Gauntlet tool expansion:** iron pickaxe and shovel harvesting/speed, native
shovel right-click actions and tool-enchantment support are compiled and packaged
in Forge1.20.1 port.9, NeoForge1.21.1 port.10 and NeoForge26.1.2 port.17. These
packages are uninstalled and runtime unconfirmed. See [mining behavior and exact scope](GAUNTLET-MINING.md).

**Modern gauntlet crafting correction:** the 1.21.1/26.1.2 recipe now produces
one gauntlet per craft; the legacy count of two exceeds its stack limit. A pair
requires two crafts. See [the output-count correction](GAUNTLET-RECIPE-COUNT.md).

**Native26 port.14:** [first-person Rubracium gauntlet arm/skin correction](GAUNTLET-FIRST-PERSON.md).
The native avatar hand renderer and original metal mesh share one moving arm pose.

## Held gadget correction — 2026-10-17

Gauntlet poses, sleeve clearances, detector housing clearance and detector instructions are corrected in [HELD-GADGETS.md](HELD-GADGETS.md). Original PNGs are retained. This source review/build is separate from in-game visual or scan confirmation.

## 2026-10-17 handbook and icon update

The wiki item has been removed in favor of an optional [Patchouli handbook](HANDBOOK.md). The three [buckler GUI icons](BUCKLER-ICONS.md) are8% larger. The following source mechanics retain their existing implementation.

## Source implementation,2026-10-16

Reference: Metallurgy1.4.1 revision `ef94cb94cfc08ccebe8a0df307ce6b390dcd4704`, `item/ModItems`, `item/gadget/**`, `handler/GadgetsHandler`, `capabilities/punch/PunchEffect`, `recipe/OreDetectorRecipe`, `config/GadgetsConfig`, `util/Constants.GAUNTLET_ENCHANTMENTS` and `block/gadget/BlockIceShield`.

This checkpoint adds native item registration and server mechanics on the three maintained targets. It does not establish full gadget parity, client rendering, config equivalence or a finished port. Compilation results are recorded separately below. No game is launched for this work.

## Items and original defaults

|ID|Behavior|
|---|---|
|`flint_and_ignatius`|150 durability. Normal use delegates to native flint and steel. Sneak use plays the ignition sound, then has a50% chance to attempt a3×3 fire square centered one horizontal step past the clicked adjacent position; successful coin flip costs5 durability even if no fire can be placed. TNT costs1.|
|`flint_and_vulcanite`|500 durability. Normal use always attempts the same fire square for1 durability and plays ghast shooting at pitch2. Sneak use places a lava source for25 durability and a400-tick noncreative cooldown. TNT costs1.26.1.2 respects native TNT ignition failure, including its game rule.|
|`electrum_magnet`|Sneak use cycles disabled→metal only→all items→disabled and briefly announces the selected mode above the hotbar. See [mode feedback](MAGNET-MODE-FEEDBACK.md). Original `status` integer is retained. Carried magnets attract dropped items in a12-block-wide cube centered on the holder to velocity0.4 toward their feet and clear pickup delay. Metal mode accepts our material items and modern ingot/dust/ore tags. Mode2 glints; both active modes select the original active model.|
|`brass_knuckles`|250 durability, main-hand+1 attack damage and+16 attack speed. Successful hit costs1 durability, scales victim horizontal velocity by0.25, sets hurt time10 and invulnerability13.|
|`rubracium_gauntlet`|750 durability, main-hand+3 attack damage and+16 attack speed. Equipping one into an otherwise empty pair of hands moves a spare from inventory into the opposite hand without copying it. Dual gauntlets enable the punch effect below. Successful hits charge durability to either the main or off-hand stack with equal probability, matching the source's off-hand choice even if it is a different damageable item.|
|`ore_detector`|Stores up to3 source metal names in `metal_1`..`metal_3`. Use requires a programmed detector, plays the original ping and starts a100-tick cooldown. Each selected metal supplies25 uses. It clears to a fresh unprogrammed detector on the use after the last charge. `detector_uses` replaces the old item-damage counter to prevent the modern durability pipeline from destroying the detector. The charge bar interpolates through programmed source colors. A native loaded-chunk scan supplies colored through-wall outlines and three blinking LED tints/beeps; see CLIENT-VISUALS.md for scope and evidence.|
|Optional handbook|Replaces the removed wiki item; craft Book + vanilla Copper Ingot when Patchouli is installed. See HANDBOOK.md.|

Knuckles retain Bane of Arthropods, Smite, Mending, Vanishing, Unbreaking and Sharpness eligibility. The gauntlet also accepts native pickaxe/shovel enchantments as described in [its mining expansion](GAUNTLET-MINING.md). They do not receive metal weapon effects or Divine Grace. The original defaults have validated configuration consumers; see CONFIGURATION.md.

### Delayed gauntlet throw

With both gauntlets held, attacking a live target requires food≥2 unless creative. The source saturation/food deduction is retained, including its rounded-saturation branches. The attack stores the puncher's UUID, head yaw and pitch. Each qualifying hit while charging resets a5-tick delay. Charge ticks freeze velocity and set target invulnerability11; mob AI is suspended with the prior flag retained. When the delay ends, prior AI is restored and the stored look direction produces velocity1, or2 for charge>10. Throw duration follows the source integer `hitTicks/4` clock. Crit/cloud particle counts retain20 per burst using native particles.

Charges above10 can break contacted nonliquid, breakable blocks with harvest tier<5. Each successfully broken block inflicts0.5 damage. This adapts the original0.1-block triple loop to one visit per block, with a512-block volume guard for giant entities. The puncher supplies the modern damage/protection identity; the old source used the victim's last-attacked entity, which could be null or unrelated. PvP permission, loaded chunks, world border, edit permission and cancellation hooks are honored. The stored charge count clamps at10000 to bound corrupt/unending state.

Punch state is server-owned persistent entity data. Missing/dead owners terminate it and restore owned AI; owner logout/death/dimension callbacks also clean affected loaded entities. Already disabled AI is preserved. No global no-clip flag is changed.

## Shields

|ID|Durability|Use duration|Enchantability|Cooldown/effect|
|---|---:|---:|---:|---|
|`lemurite_shield`|256|200|17|Invisible while using; release cooldown=max(used ticks,20), capped200. Durability cost=round(cooldown/8). Mob visibility becomes0 and new target acquisition is denied.|
|`zinc_buckler`|396|40|8|30-tick cooldown on release, expiry or a blocked hit.|
|`vulcanite_buckler`|662|50|20|40-tick cooldown. A directly blocked living attacker receives a0.45-power nonterrain explosion and2.5 knockback.|
|`ceruclase_shield`|500|200|18|Creates the radius5 temporary ice shell when the other hand is not eating/drinking; halves fall distance.140-tick cooldown on ending the dome.|
|`orichalcum_buckler`|413|30|22|30-tick cooldown. A blocked projectile deals1.45×blocked damage back to its living owner using the original source identity, then attempts a native collision-checked teleport two look-vector units ahead and two blocks above the blocker.|

Blocking eligibility/durability remain in the native shield pipeline. Forge uses `ShieldBlockEvent`; NeoForge uses `LivingShieldBlockEvent` only when its final blocked flag is true. Bucklers defer lowering until the next player tick so the pipeline retains the active stack for damage.26.1.2 supplies native `BLOCKS_ATTACKS` from the vanilla shield component. Original use durations and enchantability are supplied explicitly.

Retaliation has a scoped recursion guard: a retaliation explosion/reflected hit can still be blocked, but cannot recursively retaliate back through two shields until the server stack overflows. The flag clears in `finally` after each callback.

Lemurite restores its previous invisibility flag or an existing invisibility potion on cleanup. The old source blindly cleared invisibility. Native entity invisibility, target filtering and full player/armor render cancellation are present. Buckler client input restores movement/sprinting and permits entity attacks while blocking.26.1.2 uses native `UseEffects` for normal speed/sprinting. Gauntlets redirect alternating native arm swings. These hooks compiled on all targets in `evidence/gadgets-confirmed-compile-checkpoint.json`; controls and appearance have not been observed in a game.

### Temporary ice

`ice_shield` is a nonitem, no-drop native block with hardness0.5, translated blast resistance1200, slipperiness0.98, glass sounds and age0..3. Its original models use native frosted-ice textures. The source shell is the integer annulus16<distance²≤25, including offsets−6..5 per axis. Only replaceable, loaded, editable positions passing placement hooks are changed.

Each created block receives10..20-tick scheduled updates. Age0 stays stable while a live holder actively owns that exact created position; afterward it advances through four updates and disappears with the original volume0.15/pitch1.25 glass sound. Leases are transient and scheduled ticks persist, so a server restart safely melts leftover ice rather than leaving an abandoned dome forever. Early release no longer needs to schedule another tick into an already occupied native scheduler slot. Overlapping domes retain separate created-position lists. Exact client transparency/light-opacity appearance remains unobserved.

## Detector crafting

One dynamic native `ore_detector_shapeless` recipe covers cleaning a detector by itself, and programming an empty detector with1..3 nonalloy ingots. Original metal-name fields and slot order are retained. The source only rejects consecutive duplicate metal names; A/B/A remains allowed. Modern tags replace OreDictionary ingot matching. A programmed detector must first be cleaned; no unrelated ingredient is consumed by a failed match.

## Native resources and remaining work

Original creator models and PNGs are preserved. Forge/1.21.1 register `blocking` and `active` model properties;26.1.2 uses native conditional item-model JSON. Knuckles retain their original GUI icon and now have native source skin-arm/swing/glint renderers; all targets compile. See CLIENT-VISUALS.md for geometry and limitations. Gadget artwork is retained.

Etherium goggles now have a native head-slot item using their original model; their native Redstone-tab block-model overlay is source-adapted; see CLIENT-VISUALS.md. The original Potash fertilizer is source-adapted to the native bonemeal hook with50 dripping-lava particles and FAIL on unsupported blocks; underwater seagrass generation from the temporary generic BoneMealItem adapter is removed. The particles are server-broadcast so observers also see the result; native particle spread is an adaptation. All three compilers passed in `evidence/compat-packs-confirmed-compile-checkpoint.json`.

Native knuckles arms and specialized tooltips compile. Gameplay/multiplayer/client evidence remains unverified. Configuration consumers are recorded in CONFIGURATION.md. The unregistered/commented-out Sanguinite shield remains unregistered. Lamps, roads and explosives are recorded separately in BLOCK-GADGETS.md. Metal ores additionally restore the source75% explosion-drop eligibility gate, before native explosion decay; this addition compiles on all three targets in the same receipt.

## Etherium goggles inventory performance

The [goggles correction](GOGGLES-PERFORMANCE.md) suspends discovery and rendering while a screen is open. During normal gameplay, an immutable five-tick scan cache replaces render-driven discovery; redstone-tab membership refreshes every20 active ticks. The eight-block-per-axis range and through-wall original block art remain. See that record for coverage, build/install receipts and unmeasured runtime performance.
