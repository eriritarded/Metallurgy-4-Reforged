"""Package the new native26 subscriber over the hash-pinned port.13 baseline."""
from pathlib import Path
from datetime import datetime, timezone
import argparse
import json
import shutil
import zipfile
from build_gauntlet_arm import ROOT, RELATIVE, CLASS, owned_work, sha
from package_release import inspect

VERSION = '1.4.1-port.14'
TARGET = 'neoforge-26.1.2'
METADATA = 'META-INF/neoforge.mods.toml'
PROVENANCE = 'META-INF/metallurgy-build.json'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scratch', type=Path, required=True)
    args = parser.parse_args()
    work = owned_work(args.scratch)
    compile_record = json.loads((ROOT / 'evidence/gauntlet-arm-compile.json').read_text(encoding='utf-8-sig'))
    source = ROOT / RELATIVE
    compiled = work / 'classes' / CLASS
    if compile_record['exitCode'] != 0 or sha(source) != compile_record['sourceSha256'] or sha(compiled) != compile_record['classSha256']:
        raise RuntimeError('Changed source/compiler output since the recorded successful compilation')
    receipt = ROOT / 'evidence/gauntlet-arm-release.json'
    if receipt.exists():
        raise RuntimeError('Preserve the completed release receipt')
    destination = ROOT / 'artifacts'
    manifest = json.loads((destination / 'manifest.json').read_text(encoding='utf-8-sig'))
    originals = [a for a in manifest['artifacts'] if a.get('target') == TARGET]
    if len(originals) != 2 or any(a['version'] != '1.4.1-port.13' for a in originals):
        raise RuntimeError('Expected the reviewed port.13 binary/source pair')
    stage = work / 'packages'
    stage.mkdir(exist_ok=True)
    provenance = {'schema': 1, 'modId': 'metallurgy', 'modVersion': VERSION, 'minecraft': '26.1.2',
                  'neoforge': '26.1.2.122', 'sourceCommit': compile_record['sourceCommit'],
                  'buildMode': 'incremental: one new client subscriber; prior compiled classes/resources preserved',
                  'changedSource': RELATIVE, 'changedSourceSha256': sha(source),
                  'changedClass': CLASS, 'changedClassSha256': sha(compiled),
                  'baselineArtifact': compile_record['baselineArtifact'],
                  'baselineArtifactSha256': compile_record['baselineArtifactSha256'],
                  'baselineSourceCommit': None, 'baselineSourceLinkage': 'legacy unknown; not retroactively assigned',
                  'testsRun': False, 'renderingObserved': False}
    provenance_bytes = (json.dumps(provenance, indent=2) + '\n').encode('utf-8')
    new_metadata = (ROOT / TARGET / 'src/main/resources' / METADATA).read_bytes()
    if ('version="' + VERSION + '"').encode() not in new_metadata:
        raise RuntimeError('Native mod metadata version mismatch')
    records = []
    for original in originals:
        prior = destination / original['file']
        if sha(prior) != original['sha256']:
            raise RuntimeError('Prior archive hash mismatch')
        name = original['file'].replace(original['version'], VERSION)
        output = stage / name
        insertion = {'it/hurts/metallurgy_reforged/client/GauntletHandRenderer.java': source.read_bytes()} if original['kind'] == 'sources' else {CLASS: compiled.read_bytes()}
        insertion[PROVENANCE] = provenance_bytes
        with zipfile.ZipFile(prior) as old, zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as jar:
            if any(p in old.namelist() for p in insertion):
                raise RuntimeError('Refuse to overwrite an existing subscriber/provenance entry')
            for info in old.infolist():
                jar.writestr(info, new_metadata if info.filename == METADATA else old.read(info.filename))
            for path, data in insertion.items():
                jar.writestr(path, data)
        with zipfile.ZipFile(prior) as old, zipfile.ZipFile(output) as jar:
            old_names, new_names = set(old.namelist()), set(jar.namelist())
            changed = sorted(p for p in old_names & new_names if old.read(p) != jar.read(p))
            added = sorted(new_names - old_names)
            if changed != [METADATA] or set(added) != set(insertion) or old_names - new_names:
                raise RuntimeError('Unexpected archive delta')
            preserved_classes = sum(p.endswith('.class') and old.read(p) == jar.read(p) for p in old_names)
            preserved_pngs = sum(p.endswith('.png') and old.read(p) == jar.read(p) for p in old_names)
        identity = {'file': name, 'target': TARGET, 'version': VERSION, 'kind': original['kind'],
                    'minecraft': '26.1.2', 'loader': 'NeoForge26.1.2.122', 'java': '25',
                    'size': output.stat().st_size, 'sha256': sha(output),
                    'sourceCommit': compile_record['sourceCommit'], 'buildMode': provenance['buildMode']}
        if original['kind'] == 'playable-mod':
            identity.update(inspect(output, 69))
        final = destination / name
        if final.exists():
            raise RuntimeError('Refuse to overwrite a release artifact')
        shutil.copy2(output, final)
        if sha(final) != identity['sha256']:
            raise RuntimeError('Deliverable copy hash mismatch')
        records.append({'artifact': identity, 'baseline': original, 'changed': changed, 'added': added,
                        'preservedCompiledClasses': preserved_classes, 'preservedPngs': preserved_pngs})
    manifest['artifacts'] = [a for a in manifest['artifacts'] if a.get('target') != TARGET] + [r['artifact'] for r in records]
    if manifest.get('currentInstallationEvidence'):
        manifest['previousInstallationEvidence'] = manifest.pop('currentInstallationEvidence')
    if manifest.get('installationReceipt'):
        manifest['previousInstallationReceipt'] = manifest.pop('installationReceipt')
    manifest.update(state='Native26 port.14 gauntlet arm correction packaged; port.13 remains the last recorded main installation. Other target packages unchanged.',
                    updatedAt=datetime.now(timezone.utc).isoformat(), currentSourceCommit=compile_record['sourceCommit'],
                    latestPackagingReceipt='evidence/gauntlet-arm-release.json', currentReleaseInstalled=False,
                    gameLaunched=False, testsRun=False, mainProfileModified=False)
    (destination / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    (destination / 'SHA256SUMS.txt').write_text(''.join(a['sha256'] + '  ' + a['file'] + '\n' for a in manifest['artifacts']), encoding='utf-8')
    result = {'recordedAt': datetime.now(timezone.utc).isoformat(), 'target': TARGET, 'version': VERSION,
              'sourceCommit': compile_record['sourceCommit'], 'buildMode': provenance['buildMode'],
              'compiled': True, 'packaged': True, 'installed': False, 'renderingObserved': False,
              'gameLaunched': False, 'testsRun': False, 'mainProfileModified': False, 'records': records,
              'limits': ['No client render or gameplay run.', 'Only native26 is changed; older target packages/source render paths are unchanged.',
                         'Baseline source commit is unknown; inherited compiled classes are preserved and identified by the prior archive SHA-256.']}
    receipt.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'version': VERSION, 'artifacts': [r['artifact']['file'] for r in records], 'installed': False}))


if __name__ == '__main__':
    main()
