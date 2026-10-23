# Etherium goggles inventory lag

## Report and source finding

The user reports major inventory lag while wearing `metallurgy:etherium_goggles`. The previous `DetectorDisplay.redstone(Vec3)` ran from every world-render callback, including behind inventory screens. Each call invoked the native creative-tab rebuild check, recreated its redstone-block set, visited up to 4,913 loaded block positions in a 17 x 17 x 17 cube, and sorted all matches. Native26 then extracted a block model for every match. This is an identified expensive path consistent with the report; no runtime profile or before/after FPS measurement was captured.

The baseline is Forge1.20.1/NeoForge1.21.1 port.4 and NeoForge26.1.2 port.10, recorded in `evidence/magnet-mode-release.json`. Pre-edit file hashes and the source finding are recorded in `evidence/goggles-perf-source-review.json`. The latest available main-instance log identifies port.8; a log version is historical launch evidence and does not identify the JAR currently installed or prove which binary is running now.

## Implementation

- With any screen open, including survival/creative inventory, goggles discovery and overlay rendering stop. The render getter checks the screen immediately, so it does not wait for a client tick to hide cached positions. Native26 additionally rejects model submission behind screens and stores an empty model list for such extracted frames.
- `DetectorDisplay.tick` now owns goggles discovery. During continuous active gameplay it scans once every five client ticks, nominally every 250 ms at 20 TPS. Render callbacks only read the immutable cached position list. Closing a screen or equipping the goggles allows a fresh scan on the next client tick.
- Native creative-tab initialization runs only when the level/registry, feature flags or game-master permission context changes. Redstone block membership is cached and refreshed from the tab every 20 active client ticks, retaining mod-added tab contents without rebuilding the set every frame.
- Disconnecting/changing levels discards positions, block membership and registry context. Pausing or removing the goggles clears visible positions. An empty redstone membership set skips the block scan entirely.
- Renderers recheck that each cached position still contains a redstone-tab block before drawing it. Native26 model states remain frame-local, so resource reloads do not retain stale extracted GPU/model state.

The eye-centered eight-block-per-axis range, loaded-client-chunk restriction, far-to-near ordering, original full-bright block art and through-wall presentation remain. Detector scanning, charges, beeps and ore outlines are unchanged. There is no new packet, dependency, configuration, item/save migration or server-world scan. Discovery of a newly placed block can wait until the next five-tick refresh; creative-tab membership changes can wait 20 active ticks.

## Coverage and release state

| Source feature | Targets | State/evidence |
|---|---|---|
|Menu/pause/equipment gate and cached discovery|Native `client/DetectorDisplay.java`, all three targets|Implemented; source hashes and baseline recorded|
|Render cache consumption and stale-block check|Native `client/GadgetOverlay.java`, all three targets|Implemented; no render-driven block discovery|
|Native26 regeneration|Existing `generate_neoforge26.py` DetectorDisplay adapter|Retains shared logic and native registry lookup; hand-adapted overlay stays excluded from regeneration|
|Performance while wearing goggles in inventory|User's graphical client|Unverified after this source change; no new game or tests launched|

Replacement packages are port.5/5/11. Compilation, archive inspection and installation results are recorded in `evidence/goggles-perf-release.json`; guarded removal of owned H: build/cache/log files is recorded in `evidence/goggles-perf-cleanup.json`. Those receipts distinguish built, installed and observed runtime states. The previous port.7 world-load check does not validate this change.

All three ordinary packages compiled with zero compiler errors and passed archive inspection. Changed class files are limited to the DetectorDisplay/GadgetOverlay families; every asset/data resource and all original PNGs are byte-identical to the baseline packages. Native26 port.11 is installed in Simply NeoForged, with port.10 backed up and all43 other mods-folder files preserved. The replacement loads on a full restart. Inventory performance has not been measured in a running client.
