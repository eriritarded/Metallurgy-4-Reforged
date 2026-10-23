# Electrum Magnet mode feedback

## Change

Previously, sneak-right-clicking `metallurgy:electrum_magnet` changed its mode and played a sound without an on-screen mode label. Each successful mode change now sends the holder a translated action-bar message above the hotbar. Minecraft supplies the brief display and fade; repeated switches replace the previous message.

| Saved `status` | Action-bar text |
|---|---|
|0|Magnet: Off|
|1|Magnet: Metals Only|
|2|Magnet: All Items|

`GadgetLogic.use` changes the saved mode on the server, then calls `GadgetBridge.magnetMessage` with that new mode. The native bridges use `Player.displayClientMessage(component, true)` on Forge1.20.1 and NeoForge1.21.1, and `Player.sendOverlayMessage(component)` on NeoForge26.1.2. These are the same target-specific action-bar APIs already used for detector feedback. The26.1.2 source generator preserves this adaptation.

The mode cycle, saved field, attraction rules, models, original textures and sound are unchanged. No new packet, dependency, configuration or save migration is required. Only the player changing the mode receives the message. English fallback keys are `message.metallurgy.magnet_off`, `magnet_metals` and `magnet_all`.

## Coverage and evidence

| Source feature | Target implementation | Status |
|---|---|---|
|Mode-change dispatch|Shared `gadget/GadgetLogic.java`|Implemented; server branch after saved-mode write|
|Brief HUD feedback|Native `gadget/GadgetBridge.java` in all three targets|Implemented using native overlay delivery|
|Translated text|Shared `assets/metallurgy/lang/en_us.json`|Implemented; three explicit mode labels|
|Regeneration|`tools/generate_neoforge26.py`|Implemented; native overlay translation retained|

Baseline packages are Forge1.20.1/NeoForge1.21.1 port.3 and NeoForge26.1.2 port.9, as recorded in `evidence/held-gadget-release.json`. Replacement packages are port.4/4/10. Build, archive inspection and installation results belong to `evidence/magnet-mode-release.json`; scratch cleanup belongs to `evidence/magnet-mode-cleanup.json`. No game or tests are launched for this change. Action-bar presentation remains unobserved in a running client; prior port.7 runtime evidence does not validate this update.

All three ordinary packages compiled with zero compiler errors and passed archive inspection. Their only changed class files are GadgetLogic and GadgetBridge; the English language JSON is their only changed asset/data resource. All original PNGs, models, recipes and handbook contents are byte-identical to the baseline packages. Native26 port.10 is installed in Simply NeoForged, with port.9 backed up and all43 other mods-folder files preserved. Installation changes the next launch; no game reload or runtime observation is claimed.
