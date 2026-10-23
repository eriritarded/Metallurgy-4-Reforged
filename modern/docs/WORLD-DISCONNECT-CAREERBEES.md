# Simply NeoForged world login — 2026-10-18 11:11 EDT

The user-run `minecraft/logs/latest.log` records a world/player login followed by
an encoder failure for `neoforge:recipe_content`. The deepest cause explicitly
names two unequal `com.rwtema.careerbees.CareerRecipe` instances. The server
disconnects the local player and stops. This run loaded Metallurgy port.14,
Career Bees `0.4.0+26.1.2-cb.1`, Minecraft26.1.2 and NeoForge26.1.2.122.

The immediate disconnect cause is Career Bees recipe serialization. Its
maintained source at `H:/AI_MC_Modding/CareerBees/tools/recipe_sources.py` created
separate JSON and stream unit-codec constants. The correction shares one recipe
object per serializer for both `careerbees:bark_wood` and
`careerbees:poison_frame`. A new `cb.2` package is compiled and packaged there;
see [the Career Bees correction](H:/AI_MC_Modding/CareerBees/docs/RECIPE-SYNC-FIX.md)
and [selected user-log evidence](H:/AI_MC_Modding/CareerBees/evidence/recipe-sync-user-log-26.1.2.json).

Metallurgy's separate native26 port.15 atlas package was built and uninstalled
when this disconnect was diagnosed. It was subsequently installed at the user's
request, with port.14 backed up and all 144 other mod-folder files unchanged;
see [its installation record](PORT-REPORT-FIXES.md#installation--2026-10-18).
The log also contains a malformed Metallurgy gauntlet output count, caught
Sophisticated JEI/configuration errors and Thaumaturge JEI linkage errors. Those
are not the cause named by this fatal packet stack. They remain separate work;
this correction does not disable mods or change their content/recipes.

No game or tests were launched. The user's real log/world files are preserved.
The Career Bees fix changes one compiled recipe class plus version/provenance
metadata; all other classes, PNGs and data JSON are unchanged.

The user subsequently requested cb.2 installation and confirmed the game was
closed. It was installed at `2026-10-18T11:22:59.4151112-04:00`, with the exact
deliverable hash verified, cb.1 backed up and all 144 other mod-folder files
unchanged. Metallurgy port.14 and the Complicated Bees base were preserved.
No game was launched or reloaded; corrected world login remains unconfirmed.
See [the Career Bees install receipt](H:/AI_MC_Modding/CareerBees/evidence/recipe-sync-install-26.1.2.json).
