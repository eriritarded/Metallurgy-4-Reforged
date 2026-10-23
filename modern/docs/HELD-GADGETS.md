# Gauntlet poses, detector clearance and detector instructions

**Native26 follow-up, 2026-10-18:** the missing first-person player arm is handled
by a new native hand subscriber; see [the arm/skin correction](GAUNTLET-FIRST-PERSON.md).
The prior JSON-only pose correction below is historical and did not add an arm.

## Report and baseline

The user observed third-person gauntlet/player z-fighting, an incorrect first-person gauntlet orientation, and detector z-fighting on the26.1.2 port.8 installation. They also asked what the detector does and whether it works. The exact port.8 identity and native API review are recorded in `evidence/held-model-source-review.json`. These observations establish the reported visual failures; the detector scan itself has not been observed in a controlled client check.

## Model corrections

The four affected model resources are root/gadget variants of `rubracium_gauntlet` and `ore_detector`. Original Matpac textures, UVs, credits, GUI/ground/fixed transforms and item IDs are retained. `tools/fix_held_gadget_models.py` applies the correction idempotently and is called by the normal gadget-resource generator.

Native26 review uses the installed patched client JAR and `ItemInHandLayer.submitArmWithItem`, `ItemTransform.apply`, `ItemInHandRenderer.renderPlayerArm`, and `AvatarRenderer.renderHand`. The matching arm-layer rotations and first-person pose constants were also reviewed in the exact1.20.1 Forge mapped JAR and1.21.1 NeoForge source archive; their hashes are in the same source-review record. The arm layer rotates X−90/Y180, translates by signed1/16,2/16,−10/16, then applies the JSON pose. Native left-hand handling already mirrors translation X and rotation Y/Z.

The old gauntlet pose places its sleeve at arm-local Z−2.25, exactly matching the outer player sleeve. Its old arm-local bounds are X−3.5..1.5, Y−4.5..10.5, Z−2.25..2.75. The corrected third-person pose uses rotation `[90,0,0]`, translation `[0,-2,6]` and scale `[1.3,1.08,1.3]` (negative X for the mirrored left mesh). Its bounds become X−3.6..1.6, Y−2.48..10.48, Z−2.6..2.6, clearing the standard outer skin sleeve by at least0.23 model pixels. This is a geometric clearance calculation, not a rendered frame or a guarantee for arbitrary third-party armor meshes.

The new first-person rotation is `[81.694,60.2207,47.9266]`, translation `[0.9107,-1.9315,-1.4667]`, with the same scales. It is solved from the native idle arm matrix, including its0.1-radian wrist roll, against the normal held-item origin. Both hand JSON poses use the same angles; the native mirroring and negative mesh X scale produce the corresponding left hand. The obsolete160-degree pose and extra left Y180 correction are removed. Small overlaps at the plate/finger-to-sleeve contacts replace coplanar internal faces without changing UVs.

The detector's cuff was narrower than the player's outer sleeve and shared its Z−2.25 plane. Its new bounds are X−3.65..1.65, Y0.65..5.65, Z−2.65..2.65. Housing, handle, search head and LEDs move together0.8 model units away from the arm, including their rotation pivots, so the housing's nearest plane is Z2.8 rather than coinciding with the bare arm's Z2. The device's authored pose and LED tint indices remain intact.

## Detector use and audited behavior

1. Craft an empty Metal Detector with one to three eligible non-alloy metal ingots. They select the ores to find; an unprogrammed detector has no selected ores.
2. Hold it in either hand and right-click. Its existing server cooldown is100 ticks. The client scans while more than20% of that cooldown remains, approximately80 ticks/four seconds at20TPS.
3. It scans a17×17×17 cube, eight blocks along each axis, using already loaded client chunks. Matching Metallurgy ores and equivalent ore-item tags produce colored through-wall outlines, flashing lights and material beeps. It does not load/generate chunks or request hidden server ore data.
4. There are25 uses per selected metal, up to75. Creative does not consume uses. The use after exhaustion clears the detector. Craft a programmed detector by itself to clear it before reprogramming.

The code path is implemented and wired: `DetectorRecipe` → `DetectorCrafting` stores `metal_1..3`; `GadgetItem.use` → `GadgetLogic.use` activates the native cooldown; `ArmorInput` calls `DetectorDisplay.tick`; `GadgetOverlay` extracts its ore list and collects always-on-top gizmos after opaque features. Reviewed native `LevelRenderer` collects these before `finalizeGizmoCollection`; its always-on-top draw path is not contingent on the ordinary debug display. This code audit does not establish that the user has seen the scan, its audio, or compatibility with every rendering mod.

## New feedback

Previously an empty detector silently returned PASS, and its tooltip had no programming instructions. It now consumes the use action and sends a server-owned action-bar programming hint, without consuming a charge or starting a cooldown. A programmed use announces scanning; the exhausted clearing use announces clearing. Tooltips show programming/use instructions, remaining scans and one-based Metal1/2/3 labels. Existing saved fields, matching rules, cooldown, range, charge rules and client-only ore visibility are preserved. No new packet or save migration is introduced. The optional handbook's detector entry now states the range and visible scan duration.

## Coverage and state

| Feature | Implementation | Status/evidence |
|---|---|---|
|Gauntlet poses and contact surfaces|Four shared display contexts in each of two model variants; contact overlap correction|Implemented; mathematical/native call-chain review recorded|
|Detector cuff/housing|Two shared model variants; existing UVs/tints|Implemented; arm-local clearance recorded|
|Programming/scanning feedback|Shared `GadgetLogic`, native `GadgetBridge` on all targets, native tooltips and English fallback strings|Compiled on all three targets; archived Java matches maintained source|
|Actual ore scan|Existing native tick/extraction/outline/sound path|Source audited; runtime scan remains unverified|

All three ordinary packages compiled and passed archive inspection: Forge1.20.1/NeoForge1.21.1 port.3 and native26 port.9. Native26 is installed in Simply NeoForged, port.8 is backed up outside mods, and all43 other mods-folder files are byte-identical. Each package preserves all prior PNGs and changes only the four model resources, English instructions, the detector handbook entry, associated classes and version metadata. Build/install results are recorded in `evidence/held-gadget-release.json`; guarded cleanup is recorded in `evidence/held-gadget-cleanup.json`. No new game or tests are launched for these corrections. The prior port.7 world-load check does not validate these new poses or the detector scan.
