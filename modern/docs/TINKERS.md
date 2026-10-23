# Tinkers’ Construct adaptation

Source implementation,2026-10-16. Target: Forge1.20.1 with TConstruct3.12.1.237 and Mantle1.11.115. The reviewed native source revision is `f4e77d43f39ba1047bf93ae816b7e1beaab514c2`; the original Metallurgy reference is1.4.1, `ef94cb94cfc08ccebe8a0df307ce6b390dcd4704`. Original TCon2 source dependency and hashes are in `assets/metallurgy/tconstruct_legacy.json`; native API references are in `tconstruct-api-baseline.json`. No official matching TConstruct branch/API was found for the two requested NeoForge targets. They contain no linked TConstruct implementation.

## Materials and configuration

The optional built-in `resourcepacks/tconstruct` data pack loads only with TConstruct and `GeneralConfig.tiCIntegration`. It adds40 namespaced Metallurgy material definitions. Copper, bronze, zinc, tin, silver, steel and electrum retain the original exclusion from custom materials; native TConstruct handles those common metals. Hepatizon and Osmium additionally use the real native TCon3 materials, whose first-match casting recipes already own these fluids. Native Platinum is only a redirect to seared stone, so the Metallurgy Platinum material remains. All49 own molten fluids extend the native Forge fluid aliases so tag outputs can unify with TConstruct's fluids. Own part-fluid casting recipes use the extension tags `metallurgy:molten/<metal>`, defaulting to the canonical Forge molten aliases. All melting, alloys and ordinary casting share those canonical aliases: excluding the aliases would make shared-ingredient native melting produce an incompatible fluid. Native casting caches the first matching material-fluid recipe; arbitrary third-party addons that assign another material to the same fluid may need datapack unification. Source ingot/nugget/block repair values are1/one-ninth/9 material units; dusts melt but are not repair ingredients.

`tinkerMaterialsBlacklist` filters material definitions, stats, traits and their per-metal recipes in both direct lookup and resource enumeration. It does not delete IDs or rewrite a user's pack/config files. Removing a material can invalidate assembled tools that use it, as with the original startup blacklist. Restore the setting and restart to recover normal material resolution. Custom material stats feed the native data in memory. Server/client restart settings must match; these files do not introduce a new configuration sync protocol. Higher-priority data packs retain native override/removal control.

The importer preserves the six explicit Alduorite, Infuscolium, Rubracium, Midasium, Tartarite and Platinum overrides. Rubracium has only the old handle/extra roles. Alduorite, Midasium, Tartarite and Platinum do not gain invented bow stats. Original automatic head/extra/handle/bow equations are retained for the other tool materials. Definitions without any applicable stats remain definitions rather than acquiring generic equipment stats.

## Native part model

TCon3 has different part roles and stat combination rules. Legacy head maps to head; handle multiplier maps to native additive durability percentage; extra maps to binding. A custom native modifier module adds the old flat handle/extra durability once per matching part, using the tool definition's native part scaling. Repeated modifier levels do not multiply a second full scan. The final total still follows TCon3's stat builder, not a replacement TCon2 tool implementation.

Legacy bow plus head permits limb stats. Draw-time factor becomes reciprocal native draw-speed bonus, range becomes velocity bonus, and the original bonus damage is added through native projectile damage stats. The reciprocal has a0.05 floor for otherwise invalid custom draw-time values. Handle plus extra permits grip, combining those two old subordinate durability contributions. Head traits apply to head/limb, handle traits to handle/grip, and extra traits to binding. Source numeric part rules and generated supported roles are recorded in `tconstruct-coverage.json`.

Client material definitions use TConstruct's own metal fallback and seven-shade recoloring of its pixel art. The original metal RGB anchors are retained. This adapts the old brightness/shininess renderer to native recoloring; it does not reproduce the old render class or add generated bitmap art. Appearance and data decoding remain unobserved.

## Tool traits

Native serializable `metallurgy:legacy_trait` modules use public TConstruct hooks. They are registered only after the loader confirms TConstruct is present. The base mod and the two NeoForge variants do not instantiate optional classes. The autosmelt recipe cache is invalidated on native recipe reload.

| Original assignment | Behavior |
|---|---|
| Midasium head/handle/extra | One native upgrade slot once per qualifying tool; optional50% duplication of generated non-player mob loot |
| Ignatius head | Vulcan:3 seconds of fire; effective block loot autosmelts |
| Vulcanite head / handle | Vulcan:5 seconds of fire / burning targets take35% of base attack damage extra |
| Eximite head | Native Enderference |
| Vyroxeres head/handle/extra | Poison101 ticks on successful hits to surviving targets |
| Damascus Steel head / every old role | Original Dense cubic wear probability / Duritos10% double wear,40% canceled wear,50% normal wear |
| Shadow Iron head/handle/extra | Blindness30 ticks, amplifier2, and smoke after block breaks |
| Sanguinite head/handle | Player heal chance20% plus20% per rounded luck point; heal40% of damage dealt |
| Tartarite head |21% Wither chance,30 ticks, amplifier1 |
| Alduorite head/extra | Positive harvest-tier difference added to effective mining speed |
| Desichalkos extra | One of the original nine effects for80 ticks |
| Deep Iron head/handle/extra | Underwater mining ×3; selected underwater tool adds2.7 attack speed |

Dice retains blindness, levitation, hunger, instant damage, nausea, night vision, poison, slowness and regeneration. Mithril's source glowing TODO does not acquire a fabricated power. The unassigned third Vulcan severity is not inferred from repeated Ignatius heads.

The pinned TCon2 `TraitWritable(2)` passes suffix2 but increment1 to `AbstractTraitLeveled`. Its flag applies that increment once per specific trait, so repeated Midasium parts do not grant six extra slots. The native module retains one flat upgrade slot regardless of its summed material-trait level.

Duplication runs during native generated-loot processing and copies stacks. This replaces the old after-hit access to transient `capturedDrops`, whose timing and stack aliasing were unsafe. It does not clone XP or player drops. Native TConstruct chooses the actual qualifying melee tool or projectile modifier snapshot, rather than applying any held item to unrelated kills. Vulcan uses native smelting with zero native extra-drop chance, only for effective block harvests; entity loot is not smelted. Silky/Squeaky conflicts are validated when tools rebuild. TCon2's special optional lapis/Fortune arithmetic is adapted to the current native loot/Fortune result rather than implementing the obsolete lapis configuration flag.

The selected-tool attack-speed modifier is transient and checked centrally once per server player tick. Leaving water, breaking/deselecting the tool, death or disabling the integration removes the bonus; a spare inventory tool cannot repeatedly remove the selected tool's modifier.

## Armor from the original Construct’s Armory integration

The old ConArmory API is replaced with TConstruct3's built-in plating/maille system on the supported Forge target. `conarmIntegration` controls these stats and power consumers. Original core durability factor (`armor durability /3`) maps to each native plating slot multiplier13/15/16/11. Source per-slot defense becomes native plating armor. Maille combines the old plates (`0.75 × durability`) and trim (`durability /6`) flat contributions, the old square-root plates multiplier and source toughness. These roles and final formulas are deliberate adaptations to TCon3's two-part armor model.

Powers count worn, correctly slotted, unbroken native armor pieces once per piece. Parts do not spoof Metallurgy base item IDs or receive full-set armor powers. Every-role source traits map to both native roles; Platinum Cat Eyes is plating only and Prometheum's poison ward is maille only.

| Power | Native adaptation |
|---|---|
| Quickly | Original four-tick item-use acceleration, based on worn piece count |
| Strongly | StrengthII refreshed40 ticks. Both legacy global subscribers accepted either Strongly variant, so both Amordrine and Kalendrite effectively receivedII; this observed code result is retained. |
| Jump Master / Resistance | Jump BoostII40 ticks / ResistanceII100 ticks |
| Deeply | In-water, noncreative wearers receive230-tick Water BreathingIV and Night VisionII, sink at−0.6, halve airborne horizontal motion, step one block and gain the original swim-speed increment. Menu armor-slot clicks are blocked until leaving water. |
| Volcano | Extinguish every server tick |
| Foodly | Each second when hungry and sufficiently experienced:4 XP →1 food point with0.5 saturation modifier and original eat sound. Native XP subtraction fixes the old negative-progress wrap. |
| Cat Eyes |220-tick Night VisionI while worn, with bounded source-style removal |
| Prometheum / Blindness | Clear poison / clear blindness |

Deeply uses native menu intent validation instead of replacing vanilla inventory slot objects. It covers normal menu clicks targeting the player's armor inventory; arbitrary third-party automation is not claimed. Server authority validates clicks, buffs, food and XP. Client movement/step code also runs locally; runtime prediction, packets and compatibility remain unverified. Step restoration checks ownership, swim uses a transient modifier, and short breathing/vision buffs expire naturally after leaving water rather than deleting another mod's longer effect. Weak player references avoid retaining disconnected players.

## Smeltery

The pack supplies738 removable native recipes: material values/part-fluid recipes, common metal forms, table/basin casting,16 alloy equations and Thermite. Native units are90mB per ingot,10 per nugget and810 per block. Plate/gear melting uses90/360. Ore base180 and raw base90 use TConstruct's native configurable metal ore rate; this follows the reviewed native ore/raw distinction and can differ from the old single ore-to-ingot ratio. Old Nether/dense/poor/nugget OreDictionary categories are not assigned invented modern tags.

Alloy input/output counts preserve the source equations, including Steel and Tartarite losses; Bronze/Electrum remain native. Alloy recipes have a1000°C native threshold; the old alloy API had no temperature field. Metal melting still requires its own temperature. Native smeltery recipe temperature/time replace the old amount-scaled pseudo-temperature clock. Metal temperature uses original fluid Kelvin minus300; native recipe time scales by form volume. Thermite dust melts to1000mB at configured Kelvin minus300 (default100°C), and fuel retains50mB per100 ticks, with native heat rate10 and3700°C. Fluid tags canonicalize outputs without bundling another mod's fluid implementation.

## Evidence limits

This record describes source. All three target compilers passed with exact staged/maintained source manifests in `evidence/optional-tools-compile-checkpoint.json`, including armor, the Writable correction and optional dependency metadata. No dependency data loading, assembled-tool construction, repair, trait execution, client rendering, menu injection, multiplayer behavior or game launch is claimed. The native40-material/casting delegation follow-up compiles in particle-fidelity-compile-checkpoint.json and is included in the ordinary production package. Current source/packaging status and runtime limits are in RELEASE.md. Nothing is installed.
