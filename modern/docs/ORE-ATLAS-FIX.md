# Ore-particle atlas lookup correction

2026-10-17. Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25. Observed binary: `1.4.1-port.6`. Replacement: `1.4.1-port.7`.

## Observed crash

`crash-2026-10-17_11.18.11-client.txt` reports `IllegalArgumentException: Invalid atlas id: minecraft:textures/atlas/blocks.png`. The stack goes through `AtlasManager.getAtlasOrThrow`, `OreParticle.frame:19`, the particle constructor, `OreParticles.block` and the registered ore block's `animateTick`. This happens after the user's local-server login; it is a client particle crash. The crash identity and selected frames are preserved in `evidence/ore-atlas-crash-26.1.2.json`.

## Native distinction and implementation

| Source/API boundary | Previous behavior | Correction/status |
|---|---|---|
| 26-only `client/OreParticle.java`, `frame(int)` | Passed `TextureAtlas.LOCATION_BLOCKS` (`minecraft:textures/atlas/blocks.png`) to an atlas-ID lookup | Adapted: pass native `net.minecraft.data.AtlasIds.BLOCKS` (`minecraft:blocks`) |
| `tools/generate_neoforge26.py`, particle adapter | Regenerated the same incorrect argument | Adapted: generate the logical native atlas ID |
| `client/GadgetRenderTypes.java`, texture binding | Uses `TextureAtlas.LOCATION_BLOCKS` as an actual texture location | Reviewed and retained; this binding does not call `getAtlasOrThrow` |

The exact installed native archive is `minecraft-client-patched-26.1.2.122.jar`, SHA256 `0621b17307aa4e75340366bf90970bc296ace07b286a5f194f2a78a98aec4346`. `AtlasIds` initializes `BLOCKS` from `blocks`; `AtlasManager.getAtlasOrThrow` reads `atlasById`. Its constructor separately registers atlas textures with `TextureManager` using `AtlasConfig.textureId`. These two identifiers share the Java type but have different meanings. This review uses native bytecode, without loading or executing game classes.

The ten original `metallurgy:particles/ore_particle_1` through `_10` sprites remain declared in `assets/minecraft/atlases/blocks.json`. Lifetime remains32 ticks, the existing integer frame stepping and detail clamp remain intact, final30% shrink and original color/scale/motion rules are preserved, and rendering remains on `OPAQUE_TERRAIN`. Particle creation is retained. No catch-and-ignore fallback, feature disable, atlas relocation, new PNG or graphics-mod change is introduced.

All other common/native atlas lookups were searched: the particle helper is the only maintained `getAtlasOrThrow` call. The older1.20.1 and1.21.1 particle APIs are unchanged and their deliverables are not rebuilt here. Recipe synchronization, JEI shield information, prior texture fixes, item IDs, saves, recipes and packet protocols retain port.6 behavior.

## Evidence and installation limits

`evidence/ore-atlas-fix-26.1.2.json` records the ordinary build, maintained/staged source identity, packaged bytecode inspection, unchanged resource hashes, static model/listener checks and the installed artifact/rollback copy. Other mods and launcher settings are preserved. The user's original archive and saves are not modified.

No assistant game, server, headless game or tests are authorized. Static bytecode and resource inspection do not establish rendered particles, successful replacement startup, resource reload, gameplay or multiplayer behavior. The user must launch their own instance to observe the replacement.

The ordinary build completed with zero errors and12 deprecation warnings. Packaged bytecode reads native AtlasIds.BLOCKS; the ten frames remain declared and present. All1428 model roots and69 event listeners pass static inspection. Only OreParticle.class and version metadata differ from port.6. Port.7 is installed with SHA256 `2c3619326d1af0ea54ff15e608b3984413ea790f57f256c596a128b44caedcef`; all38 other JARs and70 other files are unchanged. The owned H: build/cache/process temporary files and raw log were removed after recording hashes; see `evidence/ore-atlas-fix-cleanup-26.1.2.json`.
