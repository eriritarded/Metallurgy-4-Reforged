# Rubracium gauntlet: native first-person arm

## Report, target and baseline

On 2026-10-18 the user reported that `metallurgy:rubracium_gauntlet` has no player
arm inside it in first person. The affected native26 source selected the ordinary
held-item model. Vanilla draws a bare arm for an empty hand, not for this ordinary
nonempty held item; changing the JSON pose alone cannot supply the missing skin.

This correction targets **Minecraft26.1.2 / NeoForge26.1.2.122 / Java25**,
candidate **1.4.1-port.14**, over the hash-pinned port.13 artifact. Forge1.20.1
port.7 and NeoForge1.21.1 port.7 packages and their render paths are unchanged.
Those targets have not acquired this native26 correction by inference.

## Implementation

`neoforge-26.1.2/src/main/java/it/hurts/metallurgy_reforged/client/GauntletHandRenderer.java`
is a client-only subscriber to the concrete cancellable `RenderHandEvent`.

1. Only `rubracium_gauntlet` is handled. Logical main/off hand is mapped through
   the player's selected dominant arm, so left-handed players and either hand
   use the corresponding physical arm.
2. The incoming camera pose receives the exact native `renderPlayerArm` swing
   and equip transforms. The selected native avatar arm is reset and given the
   matching ±0.1-radian wrist roll.
3. `AvatarRenderer.renderRightHand` / `renderLeftHand` submits the actual player's
   skin, native arm geometry and selected sleeve layer, through the native
   NeoForge first-person-arm hooks. No replacement skin texture or newly baked
   model is allocated per frame. Native invisibility and the existing Metallurgy
   shield invisibility suppress skin submission.
4. The gauntlet is attached to that same arm bone. The existing third-person
   item frame (X−90°, Y180°, signed1/16,2/16,−10/16 hand translation) and the
   existing third-person gauntlet display transform place its original geometry
   around the arm. Slim skins receive a signed half-pixel center correction;
   the gauntlet's mesh dimensions stay unchanged.
5. The ordinary first-person item draw is cancelled to prevent a duplicate mesh
   and an independent second first-person transform. The pose stack is restored
   in `finally`. Rendering the attached item does not reenter `RenderHandEvent`.

The skin and metal share every swing/equip transform. The original JSON
first-person pose remains as a fallback resource; the new native26 subscriber
owns this gauntlet's standard first-person path. The normal item renderer still
draws its original metal material and enchantment glint.

Third-person rendering, GUI/ground/fixed models, PNGs, UVs, model geometry,
tooltips, damage, durability, dual-gauntlet mechanics, server authority, recipes,
save fields and network packets are unchanged. No configuration or migration is
needed. Removing the candidate restores the old first-person rendering behavior.
Third-party custom arm-rendering and hand-event overrides remain unobserved.

## Native evidence and source generation

The exact patched client is
`minecraft-client-patched/26.1.2.122/minecraft-client-patched-26.1.2.122.jar`,
SHA-256 `0621b17307aa4e75340366bf90970bc296ace07b286a5f194f2a78a98aec4346`.
The NeoForge universal JAR SHA-256 is
`650705f7000994a12b2619fabfcaa3d23b5b3361e658a56a17ce7ae2e9b9c213`.
Reviewed APIs/methods are `RenderHandEvent`, `ItemInHandRenderer.renderPlayerArm`,
`ItemInHandRenderer.renderItem`, `AvatarRenderer.renderHand` and its public hand
overloads, and `ItemInHandLayer.submitArmWithItem`.

`tools/generate_neoforge26.py` preserves the hand-written native renderer rather
than copying an older target's implementation over it. No bulk regeneration of
unrelated game source/resources is required.

## Coverage and current state

| Feature | State | Evidence / limitation |
|---|---|---|
| Actual skin/arm and optional sleeve | Implemented | Calls the exact native avatar hand renderer; client view unobserved |
| Right/left, main/off hand, slim center | Implemented | Explicit physical-hand selection and native bone attachment; visual review pending |
| Swing/equip attachment | Implemented | Both submissions share the native arm pose; no runtime animation claim |
| Original art and other game behavior | Archive preservation confirmed | All265 prior classes and1175 PNGs are byte-identical |
| Ordinary production package | Compiled and packaged | Exact committed-source compiler result and archive deltas recorded below |
| Installation | Installed | Port.14 hash verified in Simply NeoForged; port.13 backed up and all 45 other mods-folder files byte-identical |
| Actual rendering | Unverified | No game/test launch was performed |

The implementation-phase class compiled successfully against the exact installed
native runtime with annotation processing and implicit compilation disabled.
The final committed-source compilation uses `tools/build_gauntlet_arm.py` and
writes `evidence/gauntlet-arm-compile.json`; packaging uses
`tools/package_gauntlet_arm.py` and writes `evidence/gauntlet-arm-release.json`.
These are compiler/archive checks, not a rendered-frame or in-game result.

Packaging retains the prior compiled classes and resources byte for byte and
adds one client subscriber plus source/build metadata. `META-INF/metallurgy-build.json`
records the changed source's Git commit/hash, compiled-class hash and prior JAR
hash. The inherited baseline's historical source commit remains explicitly
unknown; this incremental build does not invent one or claim a clean rebuild of
all legacy classes. The source pair includes the new Java implementation.

## Packaged result — 2026-10-18

The changed source commit is `bea90a5271bac2f2c0c38c4628d790feac216365`.
The final committed-source compilation returned0 against73 exact-target classpath
entries, without annotation processing or implicit source compilation. The JAR
has266 own classes (Java classfile69), no validation harness or foreign classes.

- Playable `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.14.jar`, SHA-256
  `0318dd200a81adb854afb24d9da80ca45c875d25d7bb04be9449f9d4ad24c501`.
- Source `Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.14-sources.jar`, SHA-256
  `9f399abb28682de639675d26107b4b704990b9a008d6577d869996bccf3c221a`.

Every existing class and PNG is byte-identical to port.13. The only changed
existing entry is native version metadata; the only added binary entries are the
new hand-renderer class and build-provenance JSON. The source pair adds the Java
file and corresponding metadata. Earlier target packages remain unchanged.

Evidence: [native source review](../SOURCE-PROVENANCE.md#historical-evidence),
[committed-source compiler inputs/result](../SOURCE-PROVENANCE.md#historical-evidence),
[package identities and deltas](../SOURCE-PROVENANCE.md#historical-evidence).
All disposable compiler files, raw logs and staged archive copies were removed
from the owned H: directory; [cleanup receipt](../SOURCE-PROVENANCE.md#historical-evidence).
The [installation readiness record](../SOURCE-PROVENANCE.md#historical-evidence)
records the earlier Windows sharing violation without changing the main profile.
The checked installer `tools/install-gauntlet-arm-release.ps1` requires the live
26.1.2/26.1.2.122 target, an unlocked prior JAR, a verified rollback backup and
byte-identical preservation of every other mods-folder file. It never launches,
closes or reloads the user's game.

## Installation — 2026-10-18

After the user confirmed the game was closed and authorized replacement, port.14
was installed in Simply NeoForged at `2026-10-18T02:28:33.0603043-04:00`.
The installed SHA-256 matches the playable package above. Port.13 was preserved
under `artifacts/backups/gauntlet-arm-port13/` with its original SHA-256
`1b15120851469301bf598fe5152d3ab93d02d1c60c7f59bf68873842f2c69ff2`.
All 45 other mods-folder files retain their exact hashes. Loader configuration and
real saves were unchanged. No game was launched or reloaded; corrected rendering
remains unobserved. See [the installation and rollback receipt](../SOURCE-PROVENANCE.md#historical-evidence).
