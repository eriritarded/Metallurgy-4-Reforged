# Rubracium gauntlet: iron pickaxe and shovel

## Requested behavior — 2026-10-18

The user revised the original diamond-level/gold-speed idea to iron mining level
and iron speed, then added shovel behavior. The implementation therefore uses
native iron harvesting rules and speed 6.0 for both pickaxe- and shovel-mineable
blocks. The default speed on other blocks remains 1.0.

| Target | Implementation | Current state |
|---|---|---|
| Forge1.20.1 / port.9 | Native iron tier check, pickaxe/shovel block tags, tool actions and ordinary mining damage | Compiled, reobfuscated and packaged; uninstalled |
| NeoForge1.21.1 / port.10 | Native `Tool` component with iron deny-drop rule and both mining tags | Compiled and packaged; uninstalled |
| NeoForge26.1.2 / port.17 | Native `Tool` component and current `ItemInstance` shovel-ability API | Compiled and packaged; uninstalled |

## Tool behavior and enchantments

Only `metallurgy:rubracium_gauntlet` gains this behavior. Forge uses
`TierSortingRegistry.isCorrectTierForDrops(Tiers.IRON,state)` plus the pickaxe/
shovel mineable tags. The component-era targets use the native iron incorrect-
block tag before the two efficient-mining rules. They follow the same harvest
eligibility as the corresponding vanilla iron tool, including loaded tag changes.
This does not grant axe/hoe mining or diamond-tier harvesting.

Mining a nonzero-hardness block costs one durability through the native damage
pipeline, so Unbreaking applies normally. The newer targets use the ordinary
`Item.mineBlock` implementation rather than adding a second damage call.
Right-click delegates to vanilla Iron Shovel `useOn` with the actual gauntlet's
use context. It therefore flattens paths and douses campfires, including native
tool-modification hooks, effects, server ownership, hand selection and durability.
All other gadgets retain their previous behavior.

The item is added to `minecraft:pickaxes` and `minecraft:shovels` on each target.
On the newer targets these feed native enchantable mining/mining-loot/durability
tags. The gauntlet's support hook accepts enchantments whose data definition
supports the actual stack, alongside its existing weapon-enchantment list.
Efficiency, Fortune and Silk Touch therefore use native mining speed and loot
processing; native incompatibility between Fortune and Silk Touch remains.
For Forge, the enchanting hook accepts enchantments that accept a vanilla Iron
Pickaxe or Iron Shovel, plus the existing combat list. That native Forge hook is
also consulted by its `Enchantment.canEnchant` path.

Existing Unbreaking, Mending and combat-enchantment eligibility remains.
Modded enchantments are supported through the respective native eligibility
rules; no promise is made for a mod requiring a different specific item class or
custom effect trigger. Enchantability remains1; it is not changed to iron's14.

## Preserved behavior, saves and multiplayer

The gauntlet remains the existing `GadgetItem`, with default durability750 and
its existing configuration, combat attributes, dual-wield punch, equip behavior,
enchantments, rendering, recipe and item ID. Tool setup adds no vanilla pickaxe
attack attributes and does not replace the gauntlet's configured durability.
The prior one-gauntlet recipe correction remains. Existing ordinary saved items
receive the new default components; damage and enchantment data are preserved.
No custom packet, saved field, migration or configuration option is introduced.
The server must load the matching package for authoritative harvesting/drops.

`tools/generate_gauntlet_tools.py` owns the marked native tool block, enchanting
hook and item tags. The version adapter entry points invoke it after conversion,
so regeneration retains the behavior without copying obsolete tier/action APIs
into the native26 target.

## Native references and validation limits

Reviewed exact local Forge47.4.10, NeoForge21.1.262 and NeoForge26.1.2.122 source/
platform APIs: `Tiers`/`ToolMaterial.IRON`, `Tool`, `Item.mineBlock`, shovel use,
item tool actions/abilities, enchanting support/primary-item checks, the vanilla
mining/mining-loot/durability item tags and corresponding enchantment definitions.
The earlier targets' digging abilities differ from native26, where digging is
represented by the tool component and the ability override covers shovel use.
See also [the official native1.21.1 custom-tool documentation](https://docs.neoforged.net/docs/1.21.1/items/tools/#custom-tools).

## Compilation and package record

Exact changed-source commit: `71552e9e53e7b5777c6bbd9018c55e12714c08d6`.
Java 17, 21 and 25 compiled the respective `GadgetItem` adapters against their
native platforms. Forge output was remapped for production with Forge Renamer
2.0.4. This is changed-class compilation using the previous ordinary packages,
not a clean whole-mod rebuild. Original dependency identities and compiler
commands are pinned in [the compilation receipt](../SOURCE-PROVENANCE.md#historical-evidence).

| Playable artifact in `artifacts/` | SHA-256 |
|---|---|
| `Metallurgy-Reforged-MC-1.20.1-forge-1.4.1-port.9.jar` | `5b6f0db09a9f829d18cc949eb48c55c240b6da01a934d83f5954b716d36cb38b` |
| `Metallurgy-Reforged-MC-1.21.1-neoforge-1.4.1-port.10.jar` | `38f96c917a057080390cc289cedc42fa8841b04fa59402f4b0d53805cb980a44` |
| `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.17.jar` | `a8acf156c9acdd6b5a762cbbf4d785e524fac4d4e2594318c7a9a398b5f63f42` |

Matching source JARs and their hashes are in [the release receipt](../SOURCE-PROVENANCE.md#historical-evidence).
Compared with port.8/9/16, only the gadget adapter classes, version/provenance
metadata and two new item tags differ in the playable packages. The respective
273/257/265 other classes, 1176/1086/1175 PNGs and 3505/1311/1283 recipe JSONs are
byte-identical. Archive inspection also confirms the shipped class hashes match
the compiled/reobfuscated outputs. Existing combat, equip, client extension and
durability-bar method bodies are preserved; see [the inspection receipt](../SOURCE-PROVENANCE.md#historical-evidence).
Native source/API evidence is in [the source review](../SOURCE-PROVENANCE.md#historical-evidence).
All 25 owned temporary files were removed after checking for active processes
and open handles; see [cleanup](../SOURCE-PROVENANCE.md#historical-evidence).

No game, native runtime or tests were launched by this task. Compilation/static
inspection does not establish actual mining, enchantment rolls, loot, rendering,
latency or optional mod interaction. These new packages are not installed. The
last recorded Simply NeoForged installation remains native26 port.16.
