"""Prepare maintained target build definitions from pinned, inspected MDKs.

No game launch or installation. Downloaded/extracted MDKs live on H:.
"""
from pathlib import Path
import argparse
import shutil

ROOT = Path(__file__).resolve().parents[1]


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--reference', required=True, type=Path)
    args = p.parse_args()
    ref = args.reference.resolve()
    if ref.drive.upper() != 'H:':
        raise ValueError('Reference storage must be on H:')
    for target, source, version, java, loader in [
        ('forge-1.20.1', 'mdk-1.20.1', '47.4.10', 17, 'forge'),
        ('neoforge-1.21.1', 'mdk-1.21.1', '21.1.262', 21, 'neoforge'),
        ('neoforge-26.1.2', 'mdk-26.1.2', '26.1.2.122', 25, 'neoforge'),
    ]:
        out = ROOT / target
        if out.exists():
            raise ValueError('Inspect existing target before replacing it: ' + target)
        out.mkdir()
        for name in ['gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties']:
            dest = out / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ref / source / name, dest)
        (out / 'settings.gradle').write_text("""pluginManagement {
    repositories { gradlePluginPortal(); maven { url = 'https://maven.minecraftforge.net' }; maven { url = 'https://maven.neoforged.net/releases' } }
}
rootProject.name = 'metallurgy'
""", encoding='utf-8')
        (out / 'gradle.properties').write_text('org.gradle.jvmargs=-Xmx4G -Dfile.encoding=UTF-8\norg.gradle.daemon=false\norg.gradle.workers.max=4\norg.gradle.java.installations.auto-download=false\n', encoding='utf-8')
        common = """
version = '1.4.1-port.1'
group = 'it.hurts.metallurgy_reforged'
base { archivesName = 'Metallurgy-Reforged-MC-@MC@-@LOADER@' }
java.toolchain.languageVersion = JavaLanguageVersion.of(@JAVA@)
java.withSourcesJar()
sourceSets.main.java.srcDir('../common/src/main/java')
sourceSets.main.resources.srcDir('../common/src/main/resources')
tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
jar { from('../LICENSE'); from('../NOTICE.md') }
""".replace('@MC@', target.split('-', 1)[1]).replace('@LOADER@', loader).replace('@JAVA@', str(java))
        if loader == 'forge':
            build = """plugins { id 'java'; id 'net.minecraftforge.gradle' version '6.0.54' }
minecraft {
    mappings channel: 'official', version: '1.20.1'
    runs {
        server { workingDirectory project.file('run'); args '--nogui'; mods { metallurgy { source sourceSets.main } } }
        gameTestServer { workingDirectory project.file('run'); property 'forge.enabledGameTestNamespaces', 'metallurgy'; mods { metallurgy { source sourceSets.main } } }
    }
}
repositories { mavenCentral() }
dependencies { minecraft 'net.minecraftforge:forge:1.20.1-47.4.10' }
tasks.named('jar').configure { finalizedBy 'reobfJar' }
""" + common
            metadata = '''modLoader="javafml"
loaderVersion="[47,)"
license="GPL-3.0-only"
[[mods]]
modId="metallurgy"
version="1.4.1-port.1"
displayName="Metallurgy 4: Reforged"
authors="Davoleo, Matpac, Metallurgy contributors; Metallurgy Chests by KreloX; modern port by Miles"
description="Modern port with integrated Metallurgy Chests."
[[dependencies.metallurgy]]
modId="forge"
mandatory=true
versionRange="[47.4.10,48)"
ordering="NONE"
side="BOTH"
[[dependencies.metallurgy]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1]"
ordering="NONE"
side="BOTH"
'''
            entry = '''package it.hurts.metallurgy_reforged;
import net.minecraftforge.fml.common.Mod;
@Mod("metallurgy")
public final class Metallurgy { public Metallurgy() {} }
'''
        else:
            mc = target.split('-', 1)[1]
            build = """plugins { id 'java-library'; id 'net.neoforged.moddev' version '2.0.148' }
neoForge {
    version = '@NEO@'
    runs {
        server { server(); programArgument '--nogui' }
        gameTestServer { type = 'gameTestServer'; systemProperty 'neoforge.enabledGameTestNamespaces', 'metallurgy' }
    }
    mods { metallurgy { sourceSet sourceSets.main } }
}
repositories { mavenCentral() }
""".replace('@NEO@', version) + common
            metadata = '''modLoader="javafml"
loaderVersion="[4,)"
license="GPL-3.0-only"
[[mods]]
modId="metallurgy"
version="1.4.1-port.1"
displayName="Metallurgy 4: Reforged"
authors="Davoleo, Matpac, Metallurgy contributors; Metallurgy Chests by KreloX; modern port by Miles"
description="Modern port with integrated Metallurgy Chests."
[[dependencies.metallurgy]]
modId="neoforge"
type="required"
versionRange="[@NEO@]"
ordering="NONE"
side="BOTH"
[[dependencies.metallurgy]]
modId="minecraft"
type="required"
versionRange="[@MC@]"
ordering="NONE"
side="BOTH"
'''.replace('@NEO@', version).replace('@MC@', mc)
            entry = '''package it.hurts.metallurgy_reforged;
import net.neoforged.fml.common.Mod;
@Mod("metallurgy")
public final class Metallurgy { public Metallurgy() {} }
'''
        (out / 'build.gradle').write_text(build, encoding='utf-8')
        path = out / 'src/main/resources/META-INF' / ('mods.toml' if loader == 'forge' else 'neoforge.mods.toml')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(metadata, encoding='utf-8')
        path = out / 'src/main/java/it/hurts/metallurgy_reforged/Metallurgy.java'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(entry, encoding='utf-8')
    shutil.copy2(ref / 'upstream/LICENSE', ROOT / 'LICENSE')
    (ROOT / 'NOTICE.md').write_text('''# Original project credits

Metallurgy4: Reforged by Davoleo, Matpac and contributors; based on Metallurgy4 by ShadowClaimer and its team. Original project: https://github.com/Davoleo/Metallurgy-4-Reforged, GPLv3. Source revision and supplied artifact identities are recorded in docs/source-baseline.json.

Metallurgy Chests by KreloX: https://github.com/KreloX/metallurgychests, GPLv3. The five chest tiers are integrated into Metallurgy by this requested port. Original artwork and attribution are retained.

This local port is not an official release by the original authors. The upstream README expressly states its authors' plans against future-version ports; this port does not imply their endorsement. No publication or launcher installation was requested or performed.
''', encoding='utf-8')


if __name__ == '__main__':
    main()
