# Modern multi-target source

Follow the repository root instructions. Shared behavior/resources live in
`common/`; each target retains its native loader APIs and resource schemas.

- Forge 1.20.1: Java 17, Forge 47.4.10, production `reobfJar` required.
- NeoForge 1.21.1: Java 21, NeoForge 21.1.262.
- NeoForge 26.1.2: Java 25, NeoForge 26.1.2.122.

Use checked-in target sources for ordinary builds. Do not regenerate adapters
blindly: changes made specifically to one target, including Hephaestus support,
must be retained deliberately. Never add verification source to ordinary JARs.
The `no-game-launch.gradle` guard blocks run/test tasks in ordinary builds.

Every test client launch must be muted, with no exceptions: dev `runClient`,
autotest and GameTest clients, and Prism test instances. Before a launch,
confirm `soundCategory_master:0.0` in that run folder's `options.txt` for the
target being run; in a new run folder, create the file with that line first.

The imported `tools/` directory also contains historical workstation helpers.
Their paths/evidence are not a portable installation or incremental-release
workflow. Use the target Gradle wrapper for fresh source builds; see root README.
