# 26.1.2 shield component registration failure

## User-observed failure

The user's Simply NeoForged launch with Minecraft 26.1.2, NeoForge 26.1.2.122 and Metallurgy `1.4.1-port.2` reached item registration, then failed with `NullPointerException: Components not bound yet`. The log identifies `MetallurgyShieldItem.<init>:10`, called from `Content.lambda$static$15:69`.

The constructor read `Items.SHIELD.components()` while item registry entries were still being constructed. In this Minecraft version those component maps are bound later. The prior input-listener fix remains in place; this is a separate registration failure. The exact user-log hash and reviewed native JAR hash are recorded in `evidence/components-load-fix-26.1.2.json`.

## Correction

The constructor now uses `Item.Properties.delayedComponent(DataComponents.BLOCKS_ATTACKS, MetallurgyShieldItem::blockingComponent)`. The initializer constructs the native shield blocking profile with the registry context supplied during component initialization. It does not read another item's component map. The existing buckler `USE_EFFECTS` values remain unchanged.

Reviewed the exact installed `minecraft-client-patched-26.1.2.122.jar`, SHA256 `0621b17307aa4e75340366bf90970bc296ace07b286a5f194f2a78a98aec4346`, using classfile disassembly only. Native `Items` also registers its shield blocking profile through `delayedComponent`; `Items.lambda$static$164` supplies these values:

| Setting | Preserved native value |
|---|---|
| Block delay | 0.25 seconds |
| Disable cooldown scale | 1 |
| Horizontal blocking angle | 90 degrees |
| Damage reduction base / factor | 0 / 1; no damage-type restriction |
| Item damage threshold / base / factor | 3 / 1 / 1 |
| Bypass tag | `minecraft:bypasses_shield`, resolved through the initializer's registry context |
| Block / disable sound | Native shield block / break sound holders |

`ModifyDefaultComponentsEvent` was also inspected from the exact NeoForge source artifact. Its callbacks operate before component binding, so merely moving the unsafe vanilla-item read into that event would not be a sound fix. The implemented change follows the native delayed initializer instead.

Affected IDs: `metallurgy:lemurite_shield`, `metallurgy:zinc_buckler`, `metallurgy:vulcanite_buckler`, `metallurgy:ceruclase_shield`, `metallurgy:orichalcum_buckler`. Durability, enchantability, use durations, buckler movement, shield abilities, recipes, models, IDs, saved item data and network protocols are unchanged. The initializer supplies vanilla baseline blocking behavior; unrelated mods' alterations to the vanilla shield are not automatically copied into these custom items.

Maintained implementation: `neoforge-26.1.2/src/main/java/it/hurts/metallurgy_reforged/gadget/MetallurgyShieldItem.java`. `tools/generate_neoforge26.py` emits the delayed initializer as well. The 26.1.2 target is bumped to `1.4.1-port.3`; earlier Minecraft targets are unchanged.

## Evidence boundary

A source scan of the maintained 26.1.2 target and shared source found no remaining `.components()` or `getDefaultInstance()` calls. This is a narrow static inspection, not proof that every possible registration or runtime operation succeeds. Build, packaged-bytecode comparison, installation identity and cleanup are recorded in the companion evidence receipt after packaging.

No client, server or test is launched by the assistant. The user clarified that the limit is one Minecraft game window, with background compilers allowed. The ban on assistant-launched games remains in force. The replacement's startup and blocking behavior require the user's own subsequent launch; they are not inferred from compilation.

## Delivered package

The clean `jar sourcesJar` build succeeded with zero compiler errors and12 existing JEI deprecation warnings. Port.3 contains264 own Java25 classes, with no verification/dependency implementation payload. Its only differences from port.2 are native version metadata and `MetallurgyShieldItem.class`. Packaged constructor disassembly confirms `delayedComponent` and no vanilla item component read. All69 event listeners still resolve to concrete classes.

Installed SHA256: `9b398f528f9ec722bf5aa76fd248dbe6655242c6ce68df2e06cae9fa11b2e39a`. The disabled old copy is backed up outside mods; all38 other JARs and70 other mods-folder files were preserved. No launcher configuration was changed and no game/test was launched by the assistant. The next user launch must establish replacement startup.

Owned staging, Gradle caches, API source copies, JVM temporary/performance data and the raw build log were removed after recording source/artifact identities and checking process use, exclusive access and path containment. See `evidence/components-load-fix-cleanup-26.1.2.json`. Installed files, rollback backups and user saves are retained.
