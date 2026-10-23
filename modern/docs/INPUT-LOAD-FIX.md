# 26.1.2 input listener loading failure

The same abstract-listener defect in the separate 1.21.1 port.7 target is now
corrected in its port.8 package; see [the later report corrections](PORT-REPORT-FIXES.md).
The native26 event below remains historical evidence for that target only.

## Observed failure

The user's Simply NeoForged instance failed during client mod construction on 2026-10-16 at 13:18:37, with Minecraft 26.1.2, NeoForge 26.1.2.122 and Metallurgy `1.4.1-port.1`. `GadgetVisuals.attack(InputEvent)` was registered automatically as an event subscriber. NeoForge bus 8.0.5 rejects abstract event classes, so loading stopped before the game was usable.

The failing installed artifact SHA256 is `74b711967cc7cd90e1a1b6d6dfc7ee2e64c989f01b7d8c51fecdd0d76d4b6f9b`. The user's original log is retained in the instance. Its observed SHA256 was `4ee7de171d7a8cd1e94deeea25de1b7748ebf27ccc23d2780298760a3438c984`; this is user-provided runtime failure evidence, not an assistant-launched run.

## Implementation

| Previous registration | Replacement | Preserved behavior |
|---|---|---|
| Abstract `InputEvent` | Concrete `InputEvent.MouseButton.Post` and `InputEvent.Key` | Both call the same private buckler attack handler after native input processing. |

The private handler still checks for a player, game mode, no open screen and an actively used buckler before consuming attack clicks. Entity attacks, miss cooldown reset and main-hand swing retain their existing logic. Unrelated input is not canceled. There are no content ID, save, recipe, balance or network protocol changes.

The maintained 26.1.2 adapter is `neoforge-26.1.2/src/main/java/it/hurts/metallurgy_reforged/client/GadgetVisuals.java`. `tools/generate_neoforge26.py` emits the same correction, so regeneration does not restore the abstract subscription. The 26.1.2 artifact version is bumped to `1.4.1-port.2`; the two earlier Minecraft target artifacts remain their existing versions.

## API and evidence

Reviewed the exact NeoForge 26.1.2.122 source artifact from its official Maven repository. Its SHA256 is `839fe2b28223b0cbfb2d8a1ef27b95435283ee37b509e6f1105bb518f49dc43b`. `InputEvent.java` declares the abstract parent and concrete `MouseButton.Post` / `Key` classes. `ClientHooks.onMouseButtonPost` and `ClientHooks.onKeyInput` post those respective events.

`tools/inspect_event_subscribers.py` reads classfile flags and subscriber annotations without loading game classes. The original packaged JAR has 68 listeners, with exactly one invalid abstract listener, matching the user's log. See `evidence/input-subscriber-before-26.1.2.json`. The replacement build, archive inspection, installed file identity and cleanup are recorded separately after packaging.

No client, server or test is launched by the assistant. Compilation and static event metadata inspection do not establish successful startup, buckler gameplay, optional mod interactions or rendering. The user's next launch must establish whether loading proceeds past this failure.

## Delivered correction

The clean `jar sourcesJar` build succeeded with zero compiler errors and12 existing JEI deprecation warnings. Port.2 contains264 own classes (Java25/major69), no verification payload or bundled dependency implementation, and matching license/notice files. Static inspection finds69 concrete event listeners with zero unresolved or abstract types. The binary differs only in `GadgetVisuals.class` and `META-INF/neoforge.mods.toml`; no archive paths were added or removed.

Installed SHA256: `82c6d9b87407762b62107ce98995e046ba80565d76d601aea19ed28fb8144047`. All37 other JARs and69 other mods-folder files were preserved. The disabled old package is backed up outside mods. This correction is packaged/installed only for26.1.2; the two earlier target packages remain unchanged. See `evidence/input-load-fix-26.1.2.json` and the before/after listener receipts.

Owned hotfix staging, dependency caches, the downloaded API source JAR and raw build log were removed after artifact/install hashes, source receipts, process use and exclusive file access were checked. See `evidence/input-load-fix-cleanup-26.1.2.json`. No game or test was launched. The later one-Java maximum instruction is recorded in AGENTS.md and the launch-hold record; no additional Java was started after it.
