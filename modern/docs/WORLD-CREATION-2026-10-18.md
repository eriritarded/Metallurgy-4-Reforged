# World-creation disconnect, 2026-10-18

Follow-up: the user subsequently authorized fixing and replacing both mods. Genetic Industry port.2 and Metallurgy port.13 are now installed; see [the repair](WORLD-LOGIN-REPAIR.md). The diagnosis and initial no-change scope below describe the earlier investigation. Successful login with the replacements remains unverified.

## Cause established by the instance log

Simply NeoForged loads Minecraft26.1.2 / NeoForge26.1.2.122 with Metallurgy port.12 and `genindustry_bees-neoforge-1.0.0+26.1.2-port.1.jar`. Two attempts reach local-player login, then disconnect at01:28:10 and01:29:13. Both fail encoding `neoforge:recipe_content` for `com.thesmartnoob880.genindustry_bees.recipe.RecipeGeneticTemplate`: the native unit stream codec expects a different recipe instance. The packet failure closes the local client/server connection and produces the singleplayer connection-lost screen.

The failing recipe belongs to GenIndustry Bees. This is the same kind of native unit-codec identity error previously corrected in Metallurgy's own two special recipes, but the new failures name the Bees recipe rather than a Metallurgy recipe. World creation itself reaches spawn preparation and login before this packet failure.

`evidence/world-create-disconnect-2026-10-18.json` preserves the log-snapshot hash, selected lines, exact failing class and both disconnects. It is a bounded snapshot of an existing user-run log, not an assistant-launched reproduction.

## Separate Metallurgy defect

Metallurgy's `main/root` and `challenges/root` advancement icons still reference removed `metallurgy:wiki`, causing both roots and their dependent trees to fail loading. The same stale icon is present in all three maintained resource targets. The logged game proceeds past these errors to the later Bees recipe-packet failure, so those advancement errors do not explain the shown disconnects.

This advancement defect is recorded as open, not implemented or installed as fixed. The root icons need a registered base-mod item that remains available without optional Patchouli. Existing advancement IDs, criteria and parent relationships should remain when corrected.

## Actions and limits

The user's request conditioned implementation on Metallurgy causing the disconnect. No mod source, installed JAR, enabled state, launcher setting or save was changed. GenIndustry Bees requires a serializer correction or a corrected release to address the observed connection failure; that separate mod has not been patched or disabled here. No game or tests were launched.
