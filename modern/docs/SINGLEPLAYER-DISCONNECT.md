# Singleplayer disconnect: native recipe synchronization

2026-10-17. Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25. User-observed binary: `1.4.1-port.5`. Replacement: `1.4.1-port.6`.

## What the instance log establishes

At 11:04:07 the local server had created the world and announced the player joining. Its Netty thread then failed to encode the native `neoforge:recipe_content` payload. The deepest exception names `DetectorRecipe` and says it cannot encode one instance because it expected a different instance. The player lost the local connection at 11:04:07.703 and the singleplayer server stopped. Singleplayer uses an internal client/server connection, so a packet failure can produce the multiplayer-style “Connection lost” screen.

This is a Metallurgy serialization defect. The evidence is in `evidence/singleplayer-disconnect-log-26.1.2.json`, including the exact log hash and selected exception lines. The same user-run log contains no earlier Metallurgy texture-warning headers; that is a narrow log observation, not a rendering certification.

## Implementation and scope

| Boundary | Previous behavior | Correction and status |
|---|---|---|
| `content/Content.java`, detector recipe serializer | JSON and stream unit codecs held separate `new DetectorRecipe()` objects | Adapted: one `statelessRecipe(T)` helper gives both codecs the same immutable recipe instance |
| Same file, Paragon serializer | Same defect with two `new ParagonRecipe()` objects | Adapted together to prevent the equivalent failure on the other special recipe |
| `compat/JeiPlugin.java`, shield information | Looked up nonexistent `invisibility_shield`, aborting plugin recipe registration | Adapted: use registered `lemurite_shield`, preserving the invisibility description translation key |

Native `StreamCodec.unit` checks equality against its captured constant. These stateless recipe classes use object identity and carry no per-recipe data; sharing the instance between JSON decoding and network serialization retains the native zero-byte payload. Recipe IDs remain in their native recipe holders. No equality override, removed recipe, new packet format or broad exception suppression is introduced.

Recipe encoding is server-side. The integrated server loads the installed client mod; a dedicated server also needs the corrected binary to avoid its old encoder failure. The wire format itself is unchanged.

Affected serializer IDs remain `metallurgy:ore_detector_shapeless` and `metallurgy:tartarite_paragon_shaped`. Detector programming/clearing and Tartarite absorption logic, inputs, outputs, recipe IDs, packet authority and the separate processing snapshot protocol4 are unchanged. The native26 content adapter is maintained directly and already excluded from the generator. The shield JEI ID correction is also applied to the older maintained Forge/NeoForge sources so regeneration preserves it; those older artifacts are not rebuilt or installed here.

The JEI fix follows the logged `Content.stack` exception at `JeiPlugin.registerRecipes:50`. `ShieldLogic` identifies the Lemurite shield as the item providing invisibility. The registered Krik armor information and molten Thermite fluid reference were also checked against the maintained catalog. `Content.stack` remains strict; unknown IDs are not silently replaced with empty stacks.

## Other messages in this run

Advanced Loot Info starts waiting for server data after the recipe encoding failure and times out three times, adding about90 seconds before its JEI integration completes. The ordering makes these waits a consequence of the already closed connection in this run. Neo Vitae separately throws caught tooltip errors while JEI reads Deus items before its configuration is available. These mods are not modified or disabled by this correction.

## Evidence, installation and limits

The exact native archive is `minecraft-client-patched-26.1.2.122.jar`, SHA256 `0621b17307aa4e75340366bf90970bc296ace07b286a5f194f2a78a98aec4346`. Its `net.minecraft.network.codec.StreamCodec.unit`, inner constant codec `encode`, and `RecipeSerializer` signatures were inspected without loading game classes.

`evidence/singleplayer-join-fix-26.1.2.json` records the ordinary production build, bytecode inspection, package/source hashes, asset/listener inspection, installation and rollback backup. The prior input, shield, processing-template and texture corrections remain in the replacement. No item/world migration or launcher configuration change is required. The user's world and unrelated mods are preserved.

No assistant game, server, headless game or tests are authorized. Compilation and bytecode inspection do not establish successful live login, native packet delivery, gameplay or rendering. The user must launch their own instance to load the new binary. Only the installed version and exact file hash are asserted by the installation receipt.

The ordinary build completed with zero errors and12 deprecation warnings. Packaged disassembly confirms the helper passes the same argument to both unit codecs. All1428 model roots and69 event listeners pass static inspection; assets and recipe resources are byte-identical to port.5. Port.6 is installed with SHA256 `53ec8266a0e5bfa396564d0fc97d3c1fa0b1aa196419c4dc1da58f6d43eb4b8b`, with all38 other JARs and70 other files preserved. The owned H: build/cache/process temporary files and raw log were removed after recording hashes; see `evidence/singleplayer-join-fix-cleanup-26.1.2.json`.

Later user observation: the port.6 log contains no repeat of the detector encoding or Metallurgy JEI-plugin errors and reaches client-world ticks before the separate ore-atlas crash. See `evidence/port6-before-atlas-crash-observation.json` and [ORE-ATLAS-FIX.md](ORE-ATLAS-FIX.md). This is a narrow observation of the prior corrections, not full gameplay validation.
