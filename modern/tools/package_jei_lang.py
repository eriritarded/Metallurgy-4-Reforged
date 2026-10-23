"""Package a resource-only title correction, retaining the prior compiled classes."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,zipfile
from package_release import inspect

ROOT=Path(__file__).resolve().parents[1]
TARGETS={'forge-1.20.1':('1.4.1-port.6',61),'neoforge-1.21.1':('1.4.1-port.6',65),'neoforge-26.1.2':('1.4.1-port.12',69)}
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def dump(p,v):p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()

def main():
    receipt=ROOT/'evidence/jei-lang-release.json'
    if receipt.exists():raise RuntimeError('Preserve the existing release receipt')
    work=Path((ROOT/'evidence/current-jei-lang-scratch.txt').read_text(encoding='utf-8').strip()).resolve()
    if work.parent!=Path('H:/CodexScratch/terraria_modding/.test-runtime').resolve() or not work.name.startswith('metallurgy-jei-lang-'):raise RuntimeError('Invalid owned H: package directory')
    stage=work/'packages';stage.mkdir(exist_ok=True)
    manifest=read(ROOT/'artifacts/manifest.json');artifacts=[];records=[]
    translations={'assets/metallurgy/lang/'+p.name:p.read_bytes() for p in (ROOT/'common/src/main/resources/assets/metallurgy/lang').glob('*.json')}
    for target,(version,major) in TARGETS.items():
        old_artifacts=[a for a in manifest['artifacts'] if a.get('target')==target]
        if len(old_artifacts)!=2:raise RuntimeError('Missing prior binary/source pair')
        record={'target':target,'version':version,'compilationRun':False,'classFilesByteIdentical':0,'archives':[]}
        for old in old_artifacts:
            original=ROOT/'artifacts'/old['file']
            if sha(original)!=old['sha256']:raise RuntimeError('Baseline hash mismatch')
            name=old['file'].replace(old['version'],version);out=stage/name
            metadata='META-INF/mods.toml' if major==61 else 'META-INF/neoforge.mods.toml'
            replacements={**translations,metadata:(ROOT/target/'src/main/resources'/metadata).read_bytes()}
            with zipfile.ZipFile(original) as src,zipfile.ZipFile(out,'w') as dst:
                if any(n.upper().endswith(('.SF','.RSA','.DSA')) and n.startswith('META-INF/') for n in src.namelist()):raise RuntimeError('Signed input cannot be repackaged')
                if not replacements.keys()<=set(src.namelist()):raise RuntimeError('Missing source resources')
                for info in src.infolist():dst.writestr(info,replacements.get(info.filename,src.read(info.filename)))
            with zipfile.ZipFile(original) as src,zipfile.ZipFile(out) as dst:
                if src.namelist()!=dst.namelist():raise RuntimeError('Archive entries changed')
                changed=[n for n in src.namelist() if src.read(n)!=dst.read(n)]
                if set(changed)!=set(replacements):raise RuntimeError('Unexpected resource delta: '+str(changed))
                classes=[n for n in src.namelist() if n.endswith('.class')]
                if any(src.read(n)!=dst.read(n) for n in classes):raise RuntimeError('Compiled classes changed')
                for path in translations:
                    before=json.loads(src.read(path));after=json.loads(dst.read(path))
                    if after!={**before,'container.chamber':after['container.chamber']}:raise RuntimeError('Unrelated language edit')
                if old['kind']=='sources':
                    for n in dst.namelist():
                        if n.endswith('.java'):
                            p=ROOT/target/'src/main/java'/n
                            if not p.is_file():p=ROOT/'common/src/main/java'/n
                            if dst.read(n)!=p.read_bytes():raise RuntimeError('Stale maintained Java')
                record['archives'].append({'kind':old['kind'],'previous':old,'changedEntries':changed,'classFilesByteIdentical':len(classes)})
                if old['kind']=='playable-mod':record['classFilesByteIdentical']=len(classes)
            inspection=inspect(out,major) if old['kind']=='playable-mod' else {}
            entry={**old,**inspection,'file':name,'version':version,'sha256':sha(out),'size':out.stat().st_size}
            destination=ROOT/'artifacts'/name
            if destination.exists():raise RuntimeError('Deliverable already exists')
            shutil.copy2(out,destination)
            if sha(destination)!=entry['sha256']:raise RuntimeError('Copy hash mismatch')
            artifacts.append(entry)
        records.append(record)
    result={'recordedAt':datetime.now(timezone.utc).isoformat(),'scratch':str(work),'packagingSucceeded':True,'buildSucceeded':True,'buildMethod':'Resource-only repackaging of prior inspected/reobfuscated production and source archives','javaCompilationRun':False,'artifacts':artifacts,'targets':records,'sourceReview':'evidence/jei-lang-source-review.json','installed':False,'scratchCleanupComplete':False,'gameLaunched':False,'testsRun':False,'jeiTitleRuntimeObserved':False}
    dump(receipt,result)
    superseded=manifest.setdefault('supersededArtifacts',[]);known={a['file'] for a in superseded}
    for a in manifest['artifacts']:
        if a['file'] not in known:superseded.append(a)
    manifest.update(artifacts=artifacts,targetVersions={t:v[0] for t,v in TARGETS.items()},state='JEI chamber title resources packaged and inspected; installation pending. Compiled classes unchanged; no game/tests.',updatedAt=result['recordedAt'],currentReleaseEvidence='evidence/jei-lang-release.json',currentReleaseInstalled=False,currentReleaseGameLaunched=False,currentReleaseTestsRun=False,currentReleaseRuntimeConfirmed=False,currentReleaseScratchRemoved=False)
    dump(ROOT/'artifacts/manifest.json',manifest)
    (ROOT/'artifacts/SHA256SUMS.txt').write_text(''.join(a['sha256']+'  '+a['file']+'\n' for a in artifacts),encoding='utf-8')
    print(json.dumps([{'target':r['target'],'version':r['version'],'classFilesUnchanged':r['classFilesByteIdentical']} for r in records]))

if __name__=='__main__':main()
