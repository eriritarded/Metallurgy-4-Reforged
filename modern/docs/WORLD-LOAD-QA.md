# Authorized client world-load troubleshooting — 2026-10-17

The user explicitly requested repeated game launches and fresh-world creation, correcting crashes/freezes and retrying until a world loads successfully, then closing the game. This supersedes the prior game/test ban for this task. The one-Minecraft-window limit and workspace no-focus/storage rules remain in force.

## Isolation and production identity

- `tools/client_worldload_smoke.py` copies the actual Simply NeoForged mods/config/resource setup into one owned H: scratch profile. Real saves, launcher accounts, launcher configuration and the real game directory are not used for generated worlds/logs.
- Each launch creates a private inactive Windows desktop. `IsolationBootstrap` checks the process station, thread desktop and current input desktop **before FML or GLFW starts**, rejecting the input desktop. The launcher never switches desktops or injects Windows input. A separate noninteractive window station was attempted first; Windows denied its creation before any game was launched.
- Windows only displays and routes input to the active input desktop; windows/messages/hooks are confined to their desktop. See [Microsoft desktop documentation](https://learn.microsoft.com/en-us/windows/win32/winstation/desktops) and [startup desktop assignment](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/ns-processthreadsapi-startupinfow).
- A required test-only Mixin cancels `MouseHandler.grabMouse`, suppressing Minecraft cursor capture/warping throughout the test. This harness is separate from the unmodified production Metallurgy JAR. Copied options are then set muted, windowed, with narration off and pause-on-lost-focus disabled.
- Native Java25/NeoForge26.1.2.122 production launching uses the installed official libraries. Compilation uses the patched native API; launch uses the vanilla parent JAR so FML correctly selects production discovery and its patched game content. No launcher credentials are read or passed.

## Selected check

The separate QA mod opens native `CreateWorldScreen.openFresh`, selects normal world generation, creative mode and seed701726122, then invokes the native Create button. It waits for the actual integrated-server client connection, level and player. Success requires at least90 seconds unpaused,1600 client-world ticks,1600 integrated-server ticks and600 rendered frames, with a live connected player and no screen at completion. Each second it also calls three production ore blocks' `animateTick` methods, exercising the particle factory/atlas path that previously crashed. It saves a game-rendered screenshot, saves/disconnects through the native method, then calls `Minecraft.stop()`.

This check establishes only the observed fresh-world startup/connection/render/tick path in this pack. It does not establish gameplay parity, every item/machine/integration, multiplayer, performance across arbitrary worlds, or the other two Minecraft ports.

## Completed result

The fifth launch passed on2026-10-17,15:44:15Z. The actual connected world ran unpaused for90.051 seconds, with1802 client-world ticks,1794 integrated-server ticks and3196 rendered world frames. Adamantine, Atlarus and Eximite production ore `animateTick` paths ran each second with ore particles enabled. The native screenshot was visually inspected: overworld terrain, local-player hotbar/held book and ore particles rendered. Native save/disconnect completed at15:44:17Z and the owned process exited normally with code0. See [the concise runtime receipt](../SOURCE-PROVENANCE.md#historical-evidence).

The39 enabled pack JARs were copied byte-for-byte, with one separate QA JAR added only to the disposable profile. All40 top-level main mods-folder files are unchanged. The tested JAR matches both the installed and ordinary deliverable port.7, SHA256 `2c3619326d1af0ea54ff15e608b3984413ea790f57f256c596a128b44caedcef`. No additional production patch or installation was needed. The previous atlas/recipe/login failure strings did not recur in the successful run. Nonfatal third-party resource warnings and offline-test account-service errors are not reported as fixed.

The earlier attempts corrected QA setup: patched Minecraft on the parent classpath selected development discovery; the Mixin package initially included the QA entrypoint; native experimental-world confirmation needed handling; and a nonexistent Tartarite ore probe was corrected to registered ores. The fourth attempt entered a real world before that QA assertion failed. These were harness errors, not new production mod fixes. Preflight waited when other Minecraft clients were open and launched after the slot became free.

The requested check is complete and the game is closed. Disposable worlds, profiles, binaries, copied dependencies, raw logs and screenshot are removed after recording their hashes; see [the cleanup receipt](../SOURCE-PROVENANCE.md#historical-evidence). Maintained QA source and ordinary deliverables remain. Full three-target gameplay validation remains incomplete; this bounded check does not certify all port features.
