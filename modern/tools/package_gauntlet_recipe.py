"""Package the modern gauntlet output correction without compiling or launching."""
from pathlib import Path
from datetime import datetime, timezone
import argparse
import hashlib
import json
import shutil
import subprocess
import zipfile
from package_release import inspect

ROOT=Path(__file__).resolve().parents[1]
TARGETS={'neoforge-1.21.1':('1.4.1-port.9',65),'neoforge-26.1.2':('1.4.1-port.16',69)}
RECIPE='data/metallurgy/recipe/legacy/gadget/rubracium_gauntlet.json'
METADATA='META-INF/neoforge.mods.toml'
PROVENANCE='META-INF/metallurgy-build.json'

def read(path):return json.loads(path.read_text(encoding='utf-8-sig'))
def dump(path,data):path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scratch',type=Path,required=True)
    args=parser.parse_args()
    if not Path('H:/').is_dir() or shutil.disk_usage('H:/').free<20*1024**3:raise RuntimeError('H: unavailable or below20 GiB free')
    work=args.scratch.resolve(strict=True)
    allowed=Path('H:/CodexScratch/terraria_modding/.test-runtime').resolve(strict=True)
    if work.parent!=allowed or not work.name.startswith('metallurgy-gauntlet-recipe-') or work.is_symlink():raise RuntimeError('Invalid owned scratch')
    receipt=ROOT/'evidence/gauntlet-recipe-release.json'
    if receipt.exists():raise RuntimeError('Preserve the existing release receipt')
    commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT.parent,text=True).strip()
    manifest=read(ROOT/'artifacts/manifest.json')
    originals=[a for a in manifest['artifacts'] if a['target'] in TARGETS]
    if len(originals)!=4:raise RuntimeError('Expected two production/source pairs')
    stage=work/'packages';stage.mkdir()
    artifacts=[];records=[]
    for old in originals:
        target=old['target'];version,major=TARGETS[target]
        source=ROOT/'artifacts'/old['file']
        if sha(source)!=old['sha256']:raise RuntimeError('Baseline archive identity changed')
        resource_root=ROOT/target/'src/main/resources'
        resources={rel:(resource_root/rel).read_bytes() for rel in [RECIPE,METADATA]}
        for rel in resources:
            path=(resource_root/rel).relative_to(ROOT.parent).as_posix()
            committed=subprocess.check_output(['git','show',commit+':'+path],cwd=ROOT.parent)
            if committed!=resources[rel]:raise RuntimeError('Commit exact resource bytes before packaging')
        name=old['file'].replace(old['version'],version)
        package=stage/name
        provenance=dict(schema=1,modId='metallurgy',modVersion=version,target=target,sourceCommit=commit,
                        buildMode='resource-only: one gauntlet per craft',baselineArtifact=old['file'],
                        baselineArtifactSha256=old['sha256'],baselineSourceCommit=old.get('sourceCommit'),
                        changedSourceHashes={rel:hashlib.sha256(data).hexdigest() for rel,data in resources.items()})
        replacements={**resources,PROVENANCE:(json.dumps(provenance,indent=2)+'\n').encode('utf-8')}
        with zipfile.ZipFile(source) as src,zipfile.ZipFile(package,'w') as dst:
            if any(n.startswith('META-INF/') and n.upper().endswith(('.SF','.RSA','.DSA')) for n in src.namelist()):raise RuntimeError('Signed baseline')
            if not set(replacements)<=set(src.namelist()):raise RuntimeError('Missing baseline resource/provenance')
            before=json.loads(src.read(RECIPE));after=json.loads(resources[RECIPE])
            if before['result']['id']!='metallurgy:rubracium_gauntlet' or before['result']['count']!=2 or after['result']['count']!=1:raise RuntimeError('Unexpected recipe output')
            before['result']['count']=1
            if before!=after:raise RuntimeError('Recipe changed beyond the output count')
            for info in src.infolist():dst.writestr(info,replacements.get(info.filename,src.read(info.filename)))
        with zipfile.ZipFile(source) as src,zipfile.ZipFile(package) as dst:
            if src.namelist()!=dst.namelist():raise RuntimeError('Archive entry set changed')
            changed=[n for n in src.namelist() if src.read(n)!=dst.read(n)]
            if set(changed)!=set(replacements):raise RuntimeError('Unexpected archive delta')
            classes=[n for n in src.namelist() if n.endswith('.class')]
            pngs=[n for n in src.namelist() if n.endswith('.png')]
            other_recipes=[n for n in src.namelist() if '/recipe/' in n and n.endswith('.json') and n!=RECIPE]
            if any(src.read(n)!=dst.read(n) for n in classes+pngs+other_recipes):raise RuntimeError('Unrelated class, PNG or recipe change')
            for n in dst.namelist():
                if n.endswith('.json'):json.loads(dst.read(n))
        inspection=inspect(package,major) if old['kind']=='playable-mod' else {}
        artifact={**old,**inspection,'file':name,'version':version,'sha256':sha(package),'size':package.stat().st_size,
                  'sourceCommit':commit,'buildMode':provenance['buildMode']}
        destination=ROOT/'artifacts'/name
        if destination.exists():raise RuntimeError('Preserve existing deliverable')
        shutil.copy2(package,destination)
        if sha(destination)!=artifact['sha256']:raise RuntimeError('Deliverable copy mismatch')
        artifacts.append(artifact)
        records.append(dict(target=target,artifact=artifact,baseline=old,changedEntries=changed,
                            unchangedClassCount=len(classes),unchangedPngCount=len(pngs),unchangedOtherRecipeCount=len(other_recipes)))
    result=dict(recordedAt=datetime.now(timezone.utc).isoformat(),sourceCommit=commit,packaged=True,
                javaCompilationRun=False,installed=False,gameLaunched=False,testsRun=False,runtimeConfirmed=False,
                records=records,scratchCleanupComplete=False,recipe=RECIPE,previousOutputCount=2,outputCount=1,
                ingredientsUnchanged=True,patternUnchanged=True,forge1201PackageUnchanged=True)
    dump(receipt,result)
    known={a['file'] for a in manifest.setdefault('supersededArtifacts',[])}
    manifest['supersededArtifacts'] += [a for a in originals if a['file'] not in known]
    manifest['artifacts']=[a for a in manifest['artifacts'] if a['target'] not in TARGETS]+artifacts
    manifest.update(targetVersions={a['target']:a['version'] for a in manifest['artifacts']},
                    state='Modern gauntlet recipe output packaged; new packages uninstalled; no Java compilation/game/tests.',
                    updatedAt=result['recordedAt'],currentSourceCommit=commit,latestPackagingReceipt='evidence/gauntlet-recipe-release.json',
                    currentReleaseInstalled=False,currentReleaseInstalledTargets=[],currentReleaseGameLaunched=False,
                    currentReleaseTestsRun=False,currentReleaseRuntimeConfirmed=False,currentReleaseScratchRemoved=False)
    dump(ROOT/'artifacts/manifest.json',manifest)
    (ROOT/'artifacts/SHA256SUMS.txt').write_text(''.join(a['sha256']+'  '+a['file']+'\n' for a in manifest['artifacts']),encoding='utf-8')
    print(json.dumps([dict(file=r['artifact']['file'],sha256=r['artifact']['sha256'],unchangedClasses=r['unchangedClassCount']) for r in records if r['artifact']['kind']=='playable-mod']))

if __name__=='__main__':main()
