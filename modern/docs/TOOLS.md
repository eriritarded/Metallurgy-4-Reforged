# Tool, pickaxe and hoe port

## Source checkpoint — 2026-10-16

Implemented shared `effect/ToolEffects.java`, native `effect/ToolBridge.java`, `mixin/BlockLootMixin.java`, and `content/LockedOreItem.java`. Native callbacks are bound from `EffectEvents`; Forge/Neo adapters handle their version-specific APIs. This is source implementation, not a gameplay or full-port completion claim. Compilation and native behavior checks are pending at this checkpoint.

Reference: original 1.4.1 source at `ef94cb94cfc08ccebe8a0df307ce6b390dcd4704`, active classes registered by `effect/MetallurgyEffects.java`. Original classes without registration/subscription (including ShadowSteelToolEffect and AtlarusAxeEffect) are not promoted into new abilities.

## Active defaults

| Material/category | Behavior |
|---|---|
|Astral Silver tool|2× original break speed outside Overworld, when held native destroy speed >1.|
|Deep Iron tool|3× original speed while underwater.|
|Desichalkos tool|Transfers actual harvested drops into inventory, retaining partial remainders on the ground.|
|Electrum tool|Right-click toggles voltage control:4× original speed, +1 harvest level, +4 direct durability on break in addition to normal mining wear.|
|Ignatius tool|Chance (fortune+1)/4 to smelt the harvested batch; preserves stack counts.|
|Kalendrite tool|When missing health, breaking heals1 and adds round(missing health/2) direct durability damage.|
|Krik tool|Right-click adds look×1.8 horizontal and×1.2 vertical velocity;2 durability;48 ticks airborne /120 grounded cooldown.|
|Inolashite tool|Right-click mines a6-block tunnel along eye ray and a lower ray at pitches between−60 and60;1 durability per broken block;200 ticks cooldown;20 ticks invulnerability after successful teleport.|
|Quicksilver tool|For harvestable blocks, original speed×hardness/2.|
|Sanguinite tool|Right-click costs6 Wither damage, repairs6 durability, grants+1 harvest level and1.75× speed during320-tick cooldown. Source harvest boost clears when mining speed is next evaluated after cooldown.|
|Midasium tool|Excludes block/self drops; chance (fortune+1)×0.3 compacts drops then applies original Greed (gold chance0.03+fortune×0.05; otherwise +2 items at fortune0, +1 at positive fortune).|
|Vulcanite tool|Chance0.4+0.2×fortune selects one nonair adjacent block, attempts protected harvest without extra durability cost.|
|Ceruclase tool|Speed100 when tool type matches the block's native mining tag and harvest requirements.|
|Carmot tool|Three possible spherical shells11 ticks apart; same block type only; maximum shell3−max(0,required harvest level−2).1 durability per auxiliary block. Overlapping primary breaks cancel while active.|
|Celenegil tool|After5/15/60 prior breaks, Haste I/II/III for30 ticks. Original progressive step0..3 every10 ticks marks inactivity at step1 and resets at step3, restoring captured durability.|
|Angmallen pickaxe|On source ore: integer roll0..9<3+3×fortune chooses a different source metal ore within±1 harvest requirement;1 extra durability; result locked.|
|Shadow Iron pickaxe|On Shadow Iron ore: same chance, chooses different source ore at same or+1 requirement; result is not locked.|
|Mithril pickaxe|On source ore: locks ore block drop; chance (fortune+1)/4 adds1 to first drop and2 durability. Empty loot is safely ignored.|
|Oureclase pickaxe|Source/tagged metal ore drops become one matching dust per source stack, preserving the source count quirk and early-return ordering.|
|Damascus Steel pickaxe|50% chance for tagged/source ore to add required harvest level+1 XP.|
|Atlarus hoe|Right-click starts square rings0..5,±5 vertical, every4 ticks; destroys native plant blocks; round(count×0.5) durability per ring;30-tick cooldown. Equipping clears a stale sequence.|
|Midasium hoe|Right-click harvests crops at any age. Harvest conversion chance0.35+fortune×0.15: seeds→gold nuggets, other drops→gold ingots, original counts;1 direct durability.|
|Prometheum hoe|Crops/melons: each stack has50% chance to become count2, retaining the original expression precedence rather than adding2. Grass/ferns/leaves: four additional raw native loot rolls;1 durability if extra drops/effect occurred.|

## Native adaptation and lifecycle

The removed Forge HarvestDropsEvent is replaced on Forge by a redirect inside the native six-argument **drop emission** method. NeoForge instead uses its native cancellable BlockDropsEvent at LOWEST priority, after earlier protection handlers; inventory transfers do not precede that event. Arbitrary `Block.getDrops` queries remain pure: recipe browsers or loot simulations cannot move items into player inventories. Copies are transformed at actual drop emission only.

Auxiliary block destruction is bounded, rejects unloaded chunks/unbreakable blocks/outside-border positions, respects player build permission and the native cancellable break event, and suppresses recursive scheduling. It uses source-style plain block drops; it does not recursively reapply every tool loot effect to its own bonus breaks. Durability is charged after successful destruction.

Inolashite additionally requires a loaded, collision-free teleport destination. Its bounded eighth-block traversal replaces the old collision iterator. This deliberately prevents teleporting into a solid/unloaded/protected destination. Atlarus advances once per game tick phase, avoiding the source's unfiltered START/END duplicate ring progression. Original ten-frame particles, explicit motion/placement, native cloud/portal effects, advancement triggers, configuration consumers and tooltips are implemented. See CLIENT-VISUALS.md and the production receipt; native execution and appearance remain unobserved.

`locked` is persisted through native stack NBT/components; locked ore items glint and return FAIL on placement, closing transmutation/duplication replacement loops. Other sources of the same ore remain placeable. Electrum/Sanguinite harvest boosts use native HarvestCheck; unknown third-party custom tier systems retain their own checks instead of guessing their tier.

Carmot and captured Celenegil stack references are per-player bounded weak maps, removed on death, logout and dimension change. They are not resumed across process restart; persistent clock/counter cleanup is explicit. Existing original save files/items are not migrated. Tartarite uses the already implemented absorbed material/category eligibility.

## Pending evidence

Fresh target compilation, actual block-drop emission, locked placement, protection cancellation, fortune boundaries, auxiliary recursion, duplicate hand packets, duration/charge accounting, save/reload and connected-client rendering remain unverified for this addition. No graphical client has been launched.

Follow-up: `evidence/tools-confirmed-compile-checkpoint.json` records successful compilation on all three exact targets after the API corrections and native NeoForge drop-event integration. This establishes compilation only. No game or test was launched. The gameplay/client checks above remain pending.
