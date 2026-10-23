"""Package a Metallurgy advancement correction and the Genetic Industry serializer fix, retaining the prior compiled classes."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,zipfile
from package_release import inspect

ROOT=Path(__file__).resolve().parents[1]
TARGETS={'forge-1.20.1':('1.4.1-port.7',61),'neoforge-1.21.1':('1.4.1-port.7',65),'neoforge-26.1.2':('1.4.1-port.13',69)}
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def dump(p,v):p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()

def main():
    receipt=ROOT/'evidence/bees-login-release.json'
    if receipt.exists():raise RuntimeError('Preserve the existing release receipt')
    work=Path((ROOT/'evidence/current-bees-login-scratch.txt').read_text(encoding='utf-8').strip()).resolve()
    if work.parent!=Path('H:/CodexScratch/terraria_modding/.test-runtime').resolve() or not work.name.startswith('metallurgy-bees-login-'):raise RuntimeError('Invalid owned H: package directory')
    stage=work/'packages';stage.mkdir(exist_ok=True)
    manifest=read(ROOT/'artifacts/manifest.json');artifacts=[];records=[]
    for target,(version,major) in TARGETS.items():
        old_artifacts=[a for a in manifest['artifacts'] if a.get('target')==target]
        if len(old_artifacts)!=2:raise RuntimeError('Missing prior binary/source pair')
        record={'target':target,'version':version,'compilationRun':False,'classFilesByteIdentical':0,'archives':[]}
        for old in old_artifacts:
            original=ROOT/'artifacts'/old['file']
            if sha(original)!=old['sha256']:raise RuntimeError('Baseline hash mismatch')
            name=old['file'].replace(old['version'],version);out=stage/name
            metadata='META-INF/mods.toml' if major==61 else 'META-INF/neoforge.mods.toml'
            folder='advancements' if major==61 else 'advancement'
            resources={'data/metallurgy/'+folder+'/'+rel:(ROOT/target/'src/main/resources/data/metallurgy'/folder/rel).read_bytes() for rel in ['main/root.json','challenges/root.json']}
            replacements={**resources,metadata:(ROOT/target/'src/main/resources'/metadata).read_bytes()}
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
                for path in resources:
                    before=json.loads(src.read(path));after=json.loads(dst.read(path))
                    icon='item' if major==61 else 'id'
                    if before['display']['icon'][icon]!='metallurgy:wiki':raise RuntimeError('Unexpected prior root')
                    before['display']['icon'][icon]='metallurgy:copper_ingot' if '/main/' in path else 'metallurgy:tartarite_ingot'
                    if after!=before:raise RuntimeError('Unrelated advancement edit')
                if any(b'metallurgy:wiki' in dst.read(n) for n in dst.namelist() if n.endswith(('.json','.toml'))):raise RuntimeError('Removed wiki item reference remains')
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
    addon,addon_details=package_genetic_industry(work)
    result={'recordedAt':datetime.now(timezone.utc).isoformat(),'scratch':str(work),'packagingSucceeded':True,'buildSucceeded':True,'buildMethod':'Metallurgy resources repackaged with unchanged production classes; one Genetic Industry registration class compiled against the exact installed26.1.2 runtime and substituted into its prior production archive','javaCompilationRun':True,'metallurgyJavaCompilationRun':False,'artifacts':artifacts+[addon],'targets':records,'geneticIndustry':addon_details,'sourceReview':'evidence/bees-login-source-review.json','installed':False,'scratchCleanupComplete':False,'gameLaunched':False,'testsRun':False,'worldJoinRuntimeObserved':False,'geneticIndustryChangedClassCompiled':True}
    dump(receipt,result)
    superseded=manifest.setdefault('supersededArtifacts',[]);known={a['file'] for a in superseded}
    for a in manifest['artifacts']:
        if a['file'] not in known:superseded.append(a)
    manifest.update(artifacts=artifacts,targetVersions={t:v[0] for t,v in TARGETS.items()},state='Metallurgy root icons resource-packaged and Genetic Industry serializer compiled/packaged; inspection passed; installation pending. No game/tests.',updatedAt=result['recordedAt'],currentReleaseEvidence='evidence/bees-login-release.json',currentReleaseInstalled=False,currentReleaseGameLaunched=False,currentReleaseTestsRun=False,currentReleaseRuntimeConfirmed=False,currentReleaseScratchRemoved=False)
    dump(ROOT/'artifacts/manifest.json',manifest)
    (ROOT/'artifacts/SHA256SUMS.txt').write_text(''.join(a['sha256']+'  '+a['file']+'\n' for a in artifacts),encoding='utf-8')
    print(json.dumps([{'target':r['target'],'version':r['version'],'classFilesUnchanged':r['classFilesByteIdentical']} for r in records]))

def package_genetic_industry(work):
    project=Path('H:/AI_MC_Modding/ComplicatedBees')
    original=project/'dist/neoforge-26.1.2/genindustry_bees-neoforge-1.0.0+26.1.2-port.1.jar'
    expected='cdc1866a820436a2c08bbe45ad1a579059d99e42b295f38758f1d08cda23deb3'
    if sha(original)!=expected:raise RuntimeError('Genetic Industry baseline mismatch')
    version='1.0.0+26.1.2-port.2';name='genindustry_bees-neoforge-'+version+'.jar'
    cls='com/thesmartnoob880/genindustry_bees/recipe/modRecipes.class'
    output=work/'packages'/name
    code=(work/'classes'/cls).read_bytes()
    disassembly=(work/'genindustry-codec-corrected.txt').read_text(encoding='utf-8',errors='replace')
    if 'MapCodec.unit:(Ljava/lang/Object;)' not in disassembly or 'StreamCodec.unit:(Ljava/lang/Object;)' not in disassembly:raise RuntimeError('Missing shared-value codec calls')
    body=disassembly.split('lambda$static$0()',1)[1].split('static {};',1)[0]
    if body.count('aload_0')!=2 or 'astore_0' not in body or body.count('RecipeGeneticTemplate."<init>"')!=1:raise RuntimeError('Unexpected serializer bytecode')
    with zipfile.ZipFile(original) as src,zipfile.ZipFile(output,'w') as dst:
        replacements={cls:code,'META-INF/neoforge.mods.toml':src.read('META-INF/neoforge.mods.toml').replace(b'1.0.0+26.1.2-port.1',version.encode())}
        if any(n.startswith('META-INF/') and n.upper().endswith(('.SF','.RSA','.DSA')) for n in src.namelist()):raise RuntimeError('Signed baseline')
        for info in src.infolist():dst.writestr(info,replacements.get(info.filename,src.read(info.filename)))
    with zipfile.ZipFile(original) as src,zipfile.ZipFile(output) as dst:
        if src.namelist()!=dst.namelist():raise RuntimeError('Genetic Industry archive entries differ')
        changed=[n for n in src.namelist() if src.read(n)!=dst.read(n)]
        if set(changed)!=set(replacements):raise RuntimeError('Unexpected Genetic Industry delta')
        classes=[n for n in dst.namelist() if n.endswith('.class')]
        if {int.from_bytes(dst.read(n)[6:8],'big') for n in classes}!={69}:raise RuntimeError('Wrong Java target')
        if any(not n.startswith('com/thesmartnoob880/genindustry_bees/') for n in classes):raise RuntimeError('Foreign implementation in addon')
        if any('validation' in n.lower() for n in dst.namelist()):raise RuntimeError('Validation payload present')
    destination=project/'dist/neoforge-26.1.2'/name
    if destination.exists():raise RuntimeError('Preserve existing hotfix deliverable')
    shutil.copy2(output,destination)
    if sha(destination)!=sha(output):raise RuntimeError('Addon copy mismatch')
    entry={'file':name,'path':str(destination),'target':'neoforge-26.1.2','modId':'genindustry_bees','kind':'playable-mod','version':version,'minecraft':'26.1.2','loader':'NeoForge26.1.2.122','java':'25','size':destination.stat().st_size,'sha256':sha(destination),'classCount':len(classes),'classFileMajors':[69]}
    details={'sourceProject':str(project),'previousPath':str(original),'previousSha256':expected,'changedEntries':changed,'allOtherEntriesByteIdentical':True,'compiledClass':cls,'compiledClassSha256':sha(work/'classes'/cls),'disassemblySha256':sha(work/'genindustry-codec-corrected.txt'),'compileLogSha256':sha(work/'genindustry-compile.log'),'compileInputs':'evidence/bees-login-compile-inputs.json','sourceReview':'evidence/bees-login-source-review.json','gameLaunched':False,'testsRun':False,'otherTarget26_2':'Shared source corrected; no replacement26.2 package built in this task'}
    return entry,details

if __name__=='__main__':main()
