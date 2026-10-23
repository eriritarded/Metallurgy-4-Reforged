"""Package report fixes over pinned production archives, preserving unrelated bytes."""
from datetime import datetime, timezone
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import zipfile
from build_report_fixes import ROOT, PREFIX, owned_work, read, dump, sha
from package_release import inspect

TARGETS = {'forge-1.20.1': ('1.4.1-port.8', 61), 'neoforge-1.21.1': ('1.4.1-port.8', 65),
           'neoforge-26.1.2': ('1.4.1-port.15', 69)}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scratch', type=Path, required=True)
    args = parser.parse_args()
    work = owned_work(args.scratch)
    receipt = ROOT / 'evidence/report-fixes-release.json'
    if receipt.exists():
        raise RuntimeError('Preserve the existing release receipt')
    compiler = read(ROOT / 'evidence/report-fixes-compile.json')
    manifest = read(ROOT / 'artifacts/manifest.json')
    stage = work / 'packages'
    stage.mkdir(exist_ok=True)
    records, artifacts = [], []
    for target, (version, major) in TARGETS.items():
        previous = [a for a in manifest['artifacts'] if a.get('target') == target]
        if len(previous) != 2:
            raise RuntimeError('Missing prior binary/source pair')
        metadata = 'META-INF/' + ('mods.toml' if major == 61 else 'neoforge.mods.toml')
        resources = {metadata: (ROOT / target / 'src/main/resources' / metadata).read_bytes()}
        classes, sources = {}, {}
        compile_row = None
        if major != 69:
            compile_row = next(row for row in compiler['records'] if row['target'] == target)
            classes[compile_row['compiledClass']] = (work / target / 'production.class').read_bytes()
            if hashlib.sha256(next(iter(classes.values()))).hexdigest() != compile_row['productionClassSha256']:
                raise RuntimeError('Compiled output changed')
            source = ROOT / compile_row['source']
            if sha(source) != compile_row['sourceSha256']:
                raise RuntimeError('Source changed since compilation')
            sources[compile_row['compiledClass'].replace('.class', '.java')] = source.read_bytes()
        else:
            for name in ['blocks', 'items']:
                rel = 'assets/minecraft/atlases/' + name + '.json'
                resources[rel] = (ROOT / target / 'src/main/resources' / rel).read_bytes()
        for old in previous:
            original = ROOT / 'artifacts' / old['file']
            if sha(original) != old['sha256']:
                raise RuntimeError('Baseline changed: ' + old['file'])
            playable = old['kind'] == 'playable-mod'
            replacements = {**resources, **(classes if playable else sources)}
            provenance = dict(schema=1, modId='metallurgy', modVersion=version, target=target,
                              sourceCommit=compiler['sourceCommit'], baselineArtifact=old['file'],
                              baselineArtifactSha256=old['sha256'], baselineSourceCommit=old.get('sourceCommit'),
                              buildMode='incremental changed-class compilation' if compile_row else 'resource-only',
                              changedEntries={k: hashlib.sha256(v).hexdigest() for k,v in replacements.items()})
            replacements['META-INF/metallurgy-build.json'] = (json.dumps(provenance, indent=2) + '\n').encode('utf-8')
            name = old['file'].replace(old['version'], version)
            package = stage / name
            with zipfile.ZipFile(original) as src, zipfile.ZipFile(package, 'w') as dst:
                names = src.namelist()
                if any(n.startswith('META-INF/') and n.upper().endswith(('.SF', '.RSA', '.DSA')) for n in names):
                    raise RuntimeError('Signed baseline')
                for entry in src.infolist():
                    dst.writestr(entry, replacements.get(entry.filename, src.read(entry.filename)))
                for rel, data in replacements.items():
                    if rel not in names:
                        dst.writestr(rel, data)
            with zipfile.ZipFile(original) as src, zipfile.ZipFile(package) as dst:
                old_names, new_names = set(src.namelist()), set(dst.namelist())
                removed = sorted(old_names - new_names)
                added = sorted(new_names - old_names)
                changed = sorted(n for n in old_names & new_names if src.read(n) != dst.read(n))
                if removed or set(added + changed) != set(replacements):
                    raise RuntimeError('Unexpected archive changes: ' + str((changed, added, removed)))
                unchanged_classes = [n for n in old_names if n.endswith('.class') and n not in classes]
                pngs = [n for n in old_names if n.endswith('.png')]
                if any(src.read(n) != dst.read(n) for n in unchanged_classes + pngs):
                    raise RuntimeError('Unrelated class or pixel-art change')
                for n in dst.namelist():
                    if n.endswith('.json'):
                        json.loads(dst.read(n))
            inspection = inspect(package, major) if playable else {}
            artifact = {**old, **inspection, 'file': name, 'version': version,
                        'sha256': sha(package), 'size': package.stat().st_size,
                        'sourceCommit': compiler['sourceCommit'], 'buildMode': provenance['buildMode']}
            destination = ROOT / 'artifacts' / name
            if destination.exists():
                raise RuntimeError('Preserve existing deliverable')
            shutil.copy2(package, destination)
            if sha(destination) != artifact['sha256']:
                raise RuntimeError('Deliverable copy mismatch')
            artifacts.append(artifact)
            records.append(dict(target=target, artifact=artifact, baseline=old, changedEntries=changed,
                                addedEntries=added, removedEntries=removed, unchangedClassCount=len(unchanged_classes),
                                unchangedPngCount=len(pngs), compiled=compile_row is not None))
    result = dict(recordedAt=datetime.now(timezone.utc).isoformat(), sourceCommit=compiler['sourceCommit'],
                  packaged=True, installed=False, gameLaunched=False, testsRun=False, runtimeConfirmed=False,
                  records=records, scratchCleanupComplete=False,
                  limits=['No client warning count, rendering, startup or drop-mechanics runtime confirmation.',
                          'Inherited baseline classes remain identified by the previous archive SHA; not a clean rebuild.'])
    dump(receipt, result)
    known = {a['file'] for a in manifest.setdefault('supersededArtifacts', [])}
    manifest['supersededArtifacts'] += [a for a in manifest['artifacts'] if a['file'] not in known]
    manifest.update(artifacts=artifacts, targetVersions={t:v[0] for t,v in TARGETS.items()},
                    state='Report fixes packaged; compilation/archive inspection only; installation and runtime unconfirmed.',
                    updatedAt=result['recordedAt'], currentSourceCommit=result['sourceCommit'],
                    latestPackagingReceipt='evidence/report-fixes-release.json', currentReleaseInstalled=False,
                    currentReleaseGameLaunched=False, currentReleaseTestsRun=False, currentReleaseRuntimeConfirmed=False,
                    currentReleaseScratchRemoved=False)
    dump(ROOT / 'artifacts/manifest.json', manifest)
    (ROOT / 'artifacts/SHA256SUMS.txt').write_text(''.join(a['sha256'] + '  ' + a['file'] + '\n' for a in artifacts), encoding='utf-8')
    print(json.dumps([dict(target=r['target'],file=r['artifact']['file'],sha256=r['artifact']['sha256']) for r in records if r['artifact']['kind']=='playable-mod']))

if __name__ == '__main__':
    main()
