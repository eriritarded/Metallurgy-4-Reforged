# Hephaestus adaptation

Source implementation, 2026-10-18. Target: NeoForge 26.1.2 with Hephaestus 1.0.17 (`hephaestus`, package `com.titammods`) and Hephaestus Tools 1.0.8 (`hephaestus_tools`, package `com.titammods.hephaestus_tools`), the versions in the Simply NeoForged instance. Hephaestus is its own smeltery mod, not a TConstruct fork: it has no `tconstruct` namespace, no Mantle, no material-fluid or modifier API. So the Tinkers integration is carried over as plain data, with no Hephaestus classes linked.

## Loading and switches

`resourcepacks/hephaestus` loads only when Hephaestus is installed and `GeneralConfig.tiCIntegration` is on. Tool materials and part recipes additionally require Hephaestus Tools. `tinkerMaterialsBlacklist` filters a metal's materials and recipes, as for TConstruct. `HephaestusData` recomputes tool stats from the configured Metallurgy stats at load and applies the Thermite melting temperature setting.

`tools/import_hephaestus_native.py` records every native Hephaestus recipe key and tool material, with its conditions, in `content/hephaestus-native.json`. `tools/generate_hephaestus_resources.py` writes the pack from that snapshot. A native recipe or material with no conditions always wins and ours is not written. A native one behind `neoforge:mod_loaded` (for example All The Ores tin) gets ours as its complement, so exactly one applies. Re-run both scripts when Hephaestus updates.

## Smeltery

Hephaestus always registers molten brass, bronze, copper, electrum, osmium, platinum, silver, steel, tin and zinc. Metallurgy's versions of those metals melt into the Hephaestus fluids and cast back into Metallurgy items; the other 39 metals use Metallurgy's own `metallurgy:<metal>` fluids. Ingots, dusts and nuggets melt at 90/90/10 mB, blocks at 810 mB and ores at 180 mB, as on the TConstruct branch. Hephaestus has no ore doubling of its own and does not melt vanilla ores.

Temperatures use the TConstruct branch's Celsius conversion, 700 + 200 × tier. Hephaestus takes a fuel's heat from the hottest melting recipe that burns it, so recipes up to 1000 burn lava and hotter ones burn `hephaestus:molten_blaze`; lava stays at its native 1000. Thermite dust melts into molten Thermite, which then works as a 3700-degree fuel.

Fourteen Metallurgy alloys are added. Bronze, brass, electrum and steel are native Hephaestus alloys and stay native.

## Tools

Hephaestus Tools names a cast part's material after the fluid (`hephaestus_tools:<fluid path without molten_>`), so Metallurgy materials use ids such as `hephaestus_tools:adamantine`. 35 materials are added; brass, bronze, copper, electrum, osmium, platinum, silver, steel, tin and zinc keep the native Hephaestus Tools materials. Prometheum yields to the native Oritech material when Oritech is installed.

Part roles follow the same TCon source rules and six overrides as the TConstruct branch. Head stats feed the eight head parts, handle stats the two handles and binding stats the two bindings; a metal only casts the parts its roles allow (Rubracium casts handles and bindings only). Head durability is twice the legacy TCon head value, which is Metallurgy's tool durability. Mining speed, attack and enchantability come straight from Metallurgy. Harvest levels 0 to 6 map to Hephaestus wood, stone, iron, diamond, netherite, adamantite and aurichalcum. Level-6 Metallurgy ores are added to the adamantite-incorrect tag, so they still need a level-6 tool.

Hephaestus Tools traits are a closed enum. Damascus Steel keeps Dense; the other Metallurgy traits have no Hephaestus equivalent and are not added. Hephaestus Tools has no armor parts, so the Construct's Armory plating has no counterpart.

## Status

The data and loader compile (NeoForge26.1.2 build, 2026-10-18; jar in `artifacts/` as `…port.17-hephaestus.jar`; installed in Simply NeoForged on 2026-10-20 as `…port.17.jar`, replacing port.16). Hephaestus loading, smeltery behaviour and assembled tools have not been checked in a running game.
