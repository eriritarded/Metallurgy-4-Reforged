"""Package ordinary handbook/icon/bucket builds and record static file evidence only."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, re, shutil, zipfile
from package_release import inspect

ROOT=Path(__file__).resolve().parents[1]
TARGETS={
    'forge-1.20.1':('1.4.1-port.2',61,'1.20.1','forge','Forge47.4.10','17','20'),
    'neoforge-1.21.1':('1.4.1-port.2',65,'1.21.1','neoforge','NeoForge21.1.262','21','21'),
    'neoforge-26.1.2':('1.4.1-port.8',69,'26.1.2','neoforge','NeoForge26.1.2.122','25','26')}

def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
def read(path):return json.loads(path.read_text(encoding='utf-8-sig'))
def dump(path,value):path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')

def main():
    receipt_path=ROOT/'evidence/handbook-release.json'
    if receipt_path.exists() and read(receipt_path).get('installed'):
        raise RuntimeError('Preserve the completed installation receipt.')
    work=Path((ROOT/'evidence/current-bucket-color-scratch.txt').read_text().strip()).resolve()
    if work.parent!=Path('H:/CodexScratch/terraria_modding/.test-runtime').resolve() or not work.name.startswith('metallurgy-bucket-color-'):
        raise RuntimeError('Not the exact owned H: scratch child')
    previous=read(ROOT/'artifacts/manifest.json')
    coverage=read(ROOT/'docs/handbook-content.json');known=set(read(ROOT/'content/current-registry-ids.json'))
    artifacts=[];records=[]
    for target,(version,major,mc,loader_name,loader,java,log_suffix) in TARGETS.items():
        stage=work/'builds'/target/target
        log=work/('handbook-build-'+log_suffix+'.log');text=log.read_text(encoding='utf-8',errors='replace')
        marks=re.findall(r'BUILD (SUCCESSFUL|FAILED)',text)
        if not marks or marks[-1]!='SUCCESSFUL' or re.search(r'(?m)^.*error:',text):raise RuntimeError('Build did not succeed: '+target)
        filename=f'Metallurgy-Reforged-MC-{mc}-{loader_name}-{version}'
        binary=stage/'build/libs'/(filename+'.jar');source=stage/'build/libs'/(filename+'-sources.jar')
        archive=inspect(binary,major)
        old=next(a for a in previous['artifacts'] if a.get('target')==target and a.get('kind')=='playable-mod')
        old_path=ROOT/'artifacts'/old['file']
        if sha(old_path)!=old['sha256']:raise RuntimeError('Previous artifact identity mismatch')
        with zipfile.ZipFile(binary) as jar,zipfile.ZipFile(old_path) as prior,zipfile.ZipFile(source) as sources:
            names=set(jar.namelist());old_names=set(prior.namelist())
            changed=sorted(n for n in names&old_names if jar.read(n)!=prior.read(n))
            added=sorted(names-old_names);removed=sorted(old_names-names)
            pngs=[n for n in old_names if n.endswith('.png')]
            if any(n not in names or jar.read(n)!=prior.read(n) for n in pngs):raise RuntimeError('Original pixel art changed')
            root='assets/metallurgy/patchouli_books/handbook/en_us/'
            entries=[n for n in names if n.startswith(root+'entries/') and n.endswith('.json')]
            expected=next(c for c in coverage['targets'] if c['target']==target)
            if len(entries)!=expected['entries']:raise RuntimeError('Missing book entries')
            book=json.loads(jar.read('data/metallurgy/patchouli_books/handbook/book.json'))
            if not book['use_resource_pack'] or book['model']!='metallurgy:handbook' or book['creative_tab']!='metallurgy:metallurgy':raise RuntimeError('Invalid book root')
            categories={n[len(root+'categories/'):-5] for n in names if n.startswith(root+'categories/') and n.endswith('.json')}
            pages=0;integrations=[];recipe_references=0
            recipe_dir='data/metallurgy/'+('recipes' if major==61 else 'recipe')+'/'
            for name in entries:
                entry=json.loads(jar.read(name));pages+=len(entry['pages'])
                if entry['category'].removeprefix('metallurgy:') not in categories:raise RuntimeError('Missing category: '+name)
                if '/integrations/' in name:
                    if not entry.get('flag','').startswith('mod:'):raise RuntimeError('Ungated integration: '+name)
                    integrations.append(entry['flag'][4:])
                for icon in [entry['icon']]+[p['item'] for p in entry['pages'] if 'item' in p]:
                    if icon.startswith('metallurgy:') and icon not in known:raise RuntimeError('Unknown book item: '+icon)
                for page in entry['pages']:
                    for key in ['recipe','recipe2']:
                        if key in page:
                            path=recipe_dir+page[key].removeprefix('metallurgy:')+'.json'
                            if path not in names:raise RuntimeError('Missing crafting recipe: '+path)
                            recipe_references+=1
            if pages!=expected['pages'] or sorted(integrations)!=sorted(expected['integrations']):raise RuntimeError('Book coverage mismatch')
            recipe=json.loads(jar.read(recipe_dir+'handbook.json'));condition=('forge' if major==61 else 'neoforge')+':conditions'
            if recipe[condition]!=[{'type':condition.split(':')[0]+':mod_loaded','modid':'patchouli'}]:raise RuntimeError('Missing Patchouli condition')
            if major!=61 and recipe['result']!={'id':'patchouli:guide_book','count':1,'components':{'patchouli:book':'metallurgy:handbook'}}:raise RuntimeError('Wrong book output')
            obsolete=['assets/metallurgy/models/item/wiki.json','assets/metallurgy/items/wiki.json',recipe_dir+'legacy/gadget/wiki.json']
            if any(n in names for n in obsolete):raise RuntimeError('Obsolete wiki resource remains')
            for buckler in ['zinc_buckler','vulcanite_buckler','orichalcum_buckler']:
                for folder in ['', 'gadget/']:
                    for suffix in ['', '_blocking']:
                        path='assets/metallurgy/models/item/'+folder+buckler+suffix+'.json'
                        value=json.loads(jar.read(path));before=json.loads(prior.read(path))
                        if value['display']['gui']['scale']!=[1.08]*3:raise RuntimeError('Buckler scale mismatch')
                        value['display']['gui'].pop('scale');before['display']['gui'].pop('scale',None)
                        if value!=before:raise RuntimeError('Unrequested buckler model change')
            matched=0
            for name in sources.namelist():
                if name.endswith('.java'):
                    candidates=[ROOT/target/'src/main/java'/name,ROOT/'common/src/main/java'/name]
                    maintained=next((p for p in candidates if p.is_file()),None)
                    if maintained is None or sources.read(name)!=maintained.read_bytes():raise RuntimeError('Stale/unowned Java source: '+name)
                    matched+=1
            content=sources.read('it/hurts/metallurgy_reforged/content/Content.java')
            if b'"wiki"' in content:raise RuntimeError('Wiki still registered')
            if major==69:
                code=jar.read('it/hurts/metallurgy_reforged/Metallurgy.class')
                if b'BucketResourceHandler' not in code or b'Capabilities$Fluid' not in code:raise RuntimeError('Missing native bucket registration')
            record=dict(target=target,version=version,previous=old,buildTasks=['jar','sourcesJar'],buildSucceeded=True,
                buildLogSha256=sha(log),compilerErrors=0,compilerWarnings=len(re.findall(r'(?m)^.*warning:',text)),
                entries=len(entries),pages=pages,categories=len(categories),materials=49,integrationMods=sorted(integrations),craftingRecipeReferences=recipe_references,
                originalPngsByteIdentical=len(pngs),sourceArchiveMatchesMaintainedJava=True,matchedJavaSources=matched,
                changedJarEntries=changed,addedJarEntries=added,removedJarEntries=removed)
            initial=work/('handbook-initial-build-'+log_suffix+'.log')
            if initial.exists():
                earlier=initial.read_text(encoding='utf-8',errors='replace')
                record['earlierBuildLogSha256']=sha(initial)
                record['earlierCompilerWarnings']=len(re.findall(r'(?m)^.*warning:',earlier))
                record['packagingRetry']='A build helper exited unexpectedly; the final successful ordinary package is recorded separately.'
        for file,kind in [(binary,'playable-mod'),(source,'sources')]:
            destination=ROOT/'artifacts'/file.name
            if destination.exists() and sha(destination)!=sha(file):raise RuntimeError('Refusing to replace a different released artifact')
            shutil.copy2(file,destination)
            if sha(destination)!=sha(file):raise RuntimeError('Deliverable copy mismatch')
            artifacts.append(dict(file=file.name,target=target,version=version,kind=kind,minecraft=mc,loader=loader,java=java,
                size=file.stat().st_size,sha256=sha(file),**(archive if kind=='playable-mod' else {})))
        records.append(record)
    native=Path('C:/Users/USER/AppData/Roaming/PrismLauncher/libraries/net/neoforged/neoforge/26.1.2.122/neoforge-26.1.2.122-universal.jar')
    patchouli=Path('C:/Users/USER/AppData/Roaming/PrismLauncher/instances/Simply NeoForged/minecraft/mods/patchouli-neoforge-26.1-94.jar')
    result=dict(recordedAt=datetime.now(timezone.utc).isoformat(),scratch=str(work),buildSucceeded=True,installed=False,
        artifacts=artifacts,targets=records,book='metallurgy:handbook',recipeInputs=['minecraft:book','minecraft:copper_ingot'],
        wikiItemRemoved=True,bucklerGuiScale=1.08,native26BucketCapabilities=51,nativeNeoForgeSha256=sha(native),reviewedPatchouliSha256=sha(patchouli),
        gameLaunched=False,testsRun=False,renderingObserved=False,runtimeConfirmed=False,scratchCleanupComplete=False)
    dump(receipt_path,result)
    previous['version']='target-specific';previous['targetVersions']={t:v[0] for t,v in TARGETS.items()};previous['artifacts']=artifacts
    previous['updatedAt']=result['recordedAt'];previous['state']='All three handbook/icon packages compiled and inspected; native26 includes bucket color correction. Installation pending; no new runtime check.'
    previous['currentReleaseEvidence']='evidence/handbook-release.json';previous['currentReleaseGameLaunched']=False;previous['currentReleaseTestsRun']=False
    dump(ROOT/'artifacts/manifest.json',previous)
    (ROOT/'artifacts/SHA256SUMS.txt').write_text(''.join(a['sha256']+'  '+a['file']+'\n' for a in artifacts),encoding='utf-8')
    dump(ROOT/'evidence/bucket-color-fix-26.1.2.json',dict(recordedAt=result['recordedAt'],version='1.4.1-port.8',capabilities=51,
        source='neoforge-26.1.2/src/main/java/it/hurts/metallurgy_reforged/Metallurgy.java::capabilities',nativeNeoForgeSha256=result['nativeNeoForgeSha256'],
        implementation='Explicit native Capabilities.Fluid.ITEM and BucketResourceHandler for the custom MetalBucketItem subclass; retained native FluidContentsTint.',
        buildSucceeded=True,installed=False,releaseReceipt='evidence/handbook-release.json',gameLaunched=False,testsRun=False,renderingObserved=False))
    print(json.dumps([{'target':r['target'],'entries':r['entries'],'pages':r['pages'],'originalPngsByteIdentical':r['originalPngsByteIdentical']} for r in records]))

if __name__=='__main__':main()
