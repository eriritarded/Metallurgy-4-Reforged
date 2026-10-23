# Editable Ender IO alloys

Source follow-up2026-10-16; all three native target compilers passed. The original `IntegrationEnderIO.init` copied its XML file and sent1.12 IMC. Supported modern Ender IO versions use native recipe data, with different ingredient/output layouts across the three target versions. Their exact source revisions and examples are pinned in `optional-recipe-baseline.json`.

## Default and editable modes

With Ender IO loaded, a required Metallurgy built-in data pack supplies the18 source-derived alloy recipes. `enableEnderIOAlloyConfig=false` retains those bundled defaults. The recipe IDs remain `metallurgy:compat/enderio/<name>` after moving the files into the pack; the old copies were removed only after hash comparison, recorded in `enderio-pack-migration.json`. Ender IO classes are not linked or bundled.

When `general.json` sets `enableEnderIOAlloyConfig=true`, the mod initializes a persistent folder under its native configuration directory:

| Target | Folder beneath `config/metallurgy_reforged/enderio_alloys/` |
|---|---|
| Forge1.20.1 | `forge-1.20.1/` |
| NeoForge1.21.1 | `neoforge-1.21.1/` |
| NeoForge26.1.2 | `neoforge-26.1.2/` |

On first use, the18 defaults and a short README are written there without replacing existing files. A `.initialized` marker makes that a one-time operation. Subsequently that folder is authoritative: edit recipes, delete files to remove recipes, or add lower-case `.json` filenames and subfolders. Run the server's native data-pack reload afterward. A filename `extra/custom.json` becomes `metallurgy:compat/enderio/extra/custom`. Native recipe decoding validates ingredients, results, energy and other Ender IO fields.

Deleting every JSON file intentionally removes all of these alloy recipes while editable mode remains enabled. Disabling the setting and restarting restores bundled defaults while preserving the editable files. Higher-priority ordinary data packs/scripts retain native overrides and removal control. No item registry IDs or stored inventories change.

## Safety and migration boundaries

The code reads a snapshot at pack opening/reload. Files must contain JSON objects of type `enderio:alloy_smelting`; symbolic links and invalid resource filenames are rejected. Limits are1024 recipe files,4MiB per file and16MiB total. Failure reports the editable folder rather than silently reverting to defaults. Existing XML is preserved and a first-initialization warning explains that custom1.12 XML must be converted to the current target's native JSON. There is no hidden XML parser, deleted user file or fabricated old IMC API.

The setting requires a restart; recipe content changes use native reload. Customization is server data and uses the loader's native recipe sync. Configured NeoForge packs intentionally do not advertise an unchanged KnownPack identity, so modified server bytes are not mistaken for bundled client data. No client GUI, live file watcher or extra network protocol is added.

This implementation is not a runtime or migration check. No game was launched, and no actual user's configuration/profile was initialized during coding. Compilation is recorded in native-config-compile-checkpoint.json and the production source receipt. Native data loading/reload remain unobserved. See RELEASE.md for current packages; nothing is installed.
