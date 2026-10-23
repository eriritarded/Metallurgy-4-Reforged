# Genetic Industry recipe sync and removed wiki references

2026-10-18. User-authorized repair and replacement in Simply NeoForged, Minecraft26.1.2 / NeoForge26.1.2.122 / Java25.

## Genetic Industry disconnect

The user-run log shows two local-player disconnects while encoding `neoforge:recipe_content` for `RecipeGeneticTemplate`; see [the original diagnosis](WORLD-CREATION-2026-10-18.md). The source is maintained at `H:/AI_MC_Modding/ComplicatedBees`.

Previously `recipe/modRecipes.java` supplied `MapCodec.unit(RecipeGeneticTemplate::new)` for JSON and `StreamCodec.unit(new RecipeGeneticTemplate())` for networking. JSON therefore created instances unequal to the network constant. The registration now creates one stateless `RecipeGeneticTemplate` and passes it to both unit codecs. Matching and assembly remain byte-identical in their original class: one gene-bearing sample plus one template, with the existing gene merge, item count and component behavior. The registry ID remains `genindustry_bees:genetic_template_with_data`; its zero-byte recipe payload format is unchanged. The server-side encoder needs this correction; in singleplayer the installed JAR also supplies the integrated server.

The maintained recipe registration and historical `tools/rewrite_addon_core.py` generator both include the fix. The addon build version advances to port.2 while the base Complicated Bees version remains port.1. Shared26.2 source also receives the correction, but no new26.2 binary was compiled or installed in this task.

Only `modRecipes.java` was compiled, using Java25 with annotation processing and implicit source compilation disabled, using74 pinned classpath entries from the exact installed26.1.2.122 profile, including patched Minecraft/NeoForge and the prior addon/base mod. The result replaces only `recipe/modRecipes.class` in the original production addon; the addon version metadata is the only other changed JAR entry. Bytecode inspection shows one recipe construction followed by loading the same local for both `MapCodec.unit(Object)` and `StreamCodec.unit(Object)`. No game classes are executed by compilation or disassembly. Every other class/resource, including recipes, art, license and gene handling, is byte-identical.

## Metallurgy wiki-item references

Both `metallurgy:main/root` and `metallurgy:challenges/root` used removed `metallurgy:wiki` as their display icon. The roots failed decoding, preventing their dependent advancement trees from loading.

| Root | New registered base-mod icon |
|---|---|
|`metallurgy:main/root`|`metallurgy:copper_ingot`|
|`metallurgy:challenges/root`|`metallurgy:tartarite_ingot`|

The correction applies to all three maintained targets, using `item` on Forge1.20.1 and `id` on the component-based targets. `tools/convert_advancements.py` maps the two old source icons during regeneration. All advancement IDs, parent relationships, criteria, requirements, titles and rewards remain. No active resource references the removed item. The optional Patchouli handbook still uses the original book-cover artwork; that texture is not a registry reference to the deleted item.

Metallurgy port.7/7/13 is resource-only: every compiled class is byte-identical to port.6/6/12. Only each target's two root JSON files and version metadata differ. No world/save migration, new configuration, dependency or packet is introduced.

## Coverage and installed state

| Feature | Status/evidence |
|---|---|
|Genetic template serializer identity|Implemented; one changed class compiled and statically inspected against the exact target|
|Metallurgy root icons|Implemented and packaged on all three targets; all other archive entries retained|
|Genetic Industry26.1.2 port.2|Installed in Simply NeoForged; previous port.1 JAR backed up outside mods|
|Metallurgy26.1.2 port.13|Installed in Simply NeoForged; previous port.12 JAR backed up outside mods|
|Other mods-folder files|All43 retained with identical hashes|
|Successful world login after replacement|Unverified; requires the user's full restart and retry|

`evidence/bees-login-source-review.json` records pre-edit source hashes; `bees-login-compile-inputs.json` pins the exact compiler inputs. `bees-login-release.json` records archive deltas, identities and both installations. `bees-login-cleanup.json` records guarded removal of the owned H: temporary sources, compiled class, packages, compiler arguments, raw compiler/disassembly logs and process temporary directory. No game or tests were launched; earlier Bees validation and Metallurgy port.7 world-load evidence do not establish successful runtime behavior of these replacement binaries.
