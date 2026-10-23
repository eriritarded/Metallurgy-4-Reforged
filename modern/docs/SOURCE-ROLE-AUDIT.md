# Remaining source support roles

Reviewed2026-10-16 against Metallurgy1.4.1 `ef94cb94cfc08ccebe8a0df307ce6b390dcd4704` and Chests `f8f0b1853d93471d1abc4dd5436429475f0ef4c0`. The machine-readable record, source hashes, callable names, target replacements and limitations are in [source-role-audit.json](source-role-audit.json). This is a support-role inventory with targeted method-body review; it is not execution or a claim that every line or every combination has been checked.

The106 previously unclassified support files now have explicit dispositions. The full382-file inventory has363 adapted roles and19 omitted roles. These are Java file roles, not unique features, runtime checks or a percentage of gameplay parity. The12 earlier retired effect/helper types remain recorded separately in [inert-source-audit.json](inert-source-audit.json).

| Original role | Native replacement or deliberate difference |
|---|---|
| Entry points, proxies, registry handlers and GUI IDs | Target mod/event buses, typed registries, menus and client-only registration. The merged chest content belongs to `metallurgy`; no separate old addon is required. |
| Material/stat classes and equipment wrappers | Shared49-material catalog, native per-form items/attributes, native repair and validated restart overrides. No automatic1.12 world, registry or item-component migration is claimed. |
| Effect wrappers and progressive bundles | Explicit category dispatch, authoritative server timers and dirty native state transactions. Client intent cannot supply jump limits, arbitrary motion or unbounded height. Relevant presentation has explicit payloads. |
| OreDictionary and ingredient wrappers | Native Forge/common/Metallurgy tag families, counted processing inputs and dynamic detector/Paragon serializers. Tar retains slimeball membership. |
| `BlockMetal.isBeaconBase` | Native `minecraft:beacon_base_blocks` tag contains only raw metal blocks satisfying the original tier>1 rule; configured material tiers update this built-in pack in memory. Decorative forms are excluded. |
| `ConfigPack` | Preserve user files and validated defaults. The old entire-folder reset on a version-prefix change is replaced; legacy.cfg files are not imported or erased. |
| CraftTweaker | Native JSON recipe manager on reviewed20/21 APIs. Adds and removals use native recipe IDs; the obsolete custom Zen classes/effect-object removal signature are not retained. No reviewed26 API is fabricated. |
| JEI support wrappers | Native optional machine/crafting/subtype and fluid-information APIs on all3 targets. Current UI/data decoding remains unobserved. |
| Ender IO |18 native alloy recipes, with opt-in preserved editable JSON folders replacing copied XML. |
| IF and Thermal | Reviewed native data adapters: IF20/21 and Thermal20. Unsupported target APIs are explicitly omitted. |
| Miscellaneous utility functions | Native XP, attributes, tags, geometry, localization, recipes, components and classpath streams at their actual call sites. The removed utility class shape is not a separate missing gameplay feature. |

## Additional omissions from this review

- Official Chisel `add_variation` IMC: no matching official target API was found in the reviewed branch inventory; see [chisel-api-review.json](chisel-api-review.json). Decoration blocks and ordinary crafting are preserved. Unofficial forks are not certified.
- `MonocleModel`: no reference outside its own definition. The original goggles provide head-slot eligibility and their overlay without a model hook; those active roles are adapted.
- `MetallurgyTweaks`: deprecated, with no initialization/install call sites. Its old script-overwrite behavior is not activated.
- `PacketMovePlayer`: registered but has no send sites; its unchecked arbitrary motion is not exposed. Active movement uses authoritative native paths.
- Enderman `EntityData`, provider and storage: registered default fields only. Their initialization is reached solely by the retired Desichalkos borrowing effect, which is absent from the active1.4.1 effect roster.

## Limits and evidence

The current particle/native-tool batch compiles on all targets with exact staged/maintained source matches in [particle-fidelity-compile-checkpoint.json](../SOURCE-PROVENANCE.md#historical-evidence). The beacon/tag follow-up is included in subsequent production packaging. Earlier tests remain historical evidence for their exact snapshots. No game, server, headless test or graphical client is launched under the user's restriction.

Native resource decoding, optional dependency runtime loading, rendering, networking under latency, gameplay balance and cross-mod combinations remain unobserved. These restrictions must accompany the compiled packages; file coverage does not establish runtime correctness.
