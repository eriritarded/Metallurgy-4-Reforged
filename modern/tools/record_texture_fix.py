"""Record the port.5 resource-only correction using archive/static evidence, never game execution."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, re, zipfile
from package_release import inspect
from fix_model_resources import MODELS

ROOT = Path(__file__).resolve().parents[1]
SCRATCH = Path('H:/CodexScratch/terraria_modding/.test-runtime/metallurgy-texture-fix-cf9df4005b0d42fcba0b4960b210a67e')
NAME = 'Metallurgy-Reforged-MC-26.1.2-neoforge-'
VERSION = '1.4.1-port.5'

def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def read(name):
    return json.loads((ROOT/name).read_text(encoding='utf-8-sig'))

def main():
    receipt_path = ROOT/'evidence/texture-warnings-fix-26.1.2.json'
    if receipt_path.exists() and read('evidence/texture-warnings-fix-26.1.2.json').get('installed'):
        raise RuntimeError('Preserve the completed historical installation receipt.')
    log = SCRATCH/'texture-fix-neoforge-26.1.2.log'
    text = log.read_text(encoding='utf-8', errors='replace')
    assert 'BUILD SUCCESSFUL' in text and not re.search(r'(?m)^.*error:', text)
    assert not read('evidence/texture-warnings-fix-source-26.1.2.json')['mismatches']
    binary = ROOT/'artifacts'/(NAME+VERSION+'.jar')
    before = ROOT/'artifacts'/(NAME+'1.4.1-port.4.jar')
    with zipfile.ZipFile(binary) as new, zipfile.ZipFile(before) as old:
        new_names, old_names = set(new.namelist()), set(old.namelist())
        changed = sorted(n for n in new_names & old_names if new.read(n) != old.read(n))
        added, removed = sorted(new_names-old_names), sorted(old_names-new_names)
        allowed = {'assets/metallurgy/models/'+name+'.json' for name in MODELS}
        allowed.update({'assets/metallurgy/models/item/gadget/brass_knuckles.json',
                        'assets/metallurgy/models/item/gadget/brass_knuckles_hand.json',
                        'META-INF/neoforge.mods.toml'})
        assert set(changed) == allowed and not added, (changed, added)
        assert removed == ['assets/metallurgy/items/extended.json','assets/metallurgy/items/extended_handheld.json'], removed
        classes = [n for n in new_names if n.endswith('.class')]
        pngs = [n for n in new_names if n.endswith('.png')]
        assert all(new.read(n)==old.read(n) for n in classes+pngs)
        assert len(classes)==265
    source_path = ROOT/'artifacts'/(NAME+VERSION+'-sources.jar')
    with zipfile.ZipFile(source_path) as source:
        names=source.namelist()
        assert len(names)==len(set(names))
        definition='it/hurts/metallurgy_reforged/machine/ProcessingDefinition.java'
        assert names.count(definition)==1
        assert source.read(definition)==(ROOT/'neoforge-26.1.2/src/main/java'/definition).read_bytes()
    artifacts=[]
    for suffix,kind in [('.jar','playable-mod'),('-sources.jar','sources')]:
        file=ROOT/'artifacts'/(NAME+VERSION+suffix)
        stage=SCRATCH/'builds/neoforge-26.1.2/neoforge-26.1.2/build/libs'/file.name
        assert sha(file)==sha(stage)
        artifacts.append(dict(file=file.name,target='neoforge-26.1.2',version=VERSION,kind=kind,
            minecraft='26.1.2',loader='NeoForge26.1.2.122',java='25',size=file.stat().st_size,sha256=sha(file),
            **(inspect(file,69) if kind=='playable-mod' else {})))
    slots=read('evidence/texture-slots-after-26.1.2.json')
    listeners=read('evidence/texture-subscriber-after-26.1.2.json')
    assert slots['artifactSha256']==sha(binary) and not slots['failures'] and slots['abstractTemplatesAreNotRoots']
    assert listeners['sha256']==sha(binary) and not listeners['invalidOrUnresolved']
    report=dict(recordedAt=datetime.now(timezone.utc).isoformat(),version=VERSION,replacementVersion=VERSION,
        minecraft='26.1.2',neoforge='26.1.2.122',scratch=str(SCRATCH),buildSucceeded=True,installed=False,
        buildTasks=['jar','sourcesJar'],buildLogSha256=sha(log),compilerErrors=0,
        compilerWarnings=len(re.findall(r'(?m)^.*warning:',text)),artifacts=artifacts,
        changedJarEntries=changed,addedJarEntries=added,removedJarEntries=removed,
        unchangedClassCount=len(classes),allClassBytesUnchanged=True,unchangedPngCount=len(pngs),allPngBytesUnchanged=True,
        gameplayAndRecipeResourcesUnchanged=True,removedUnassignedFaces=142,
        sourceEvidence='evidence/texture-warnings-fix-source-26.1.2.json',
        modelEditEvidence='evidence/texture-model-edits-26.1.2.json',
        beforeModelAudit='evidence/texture-slots-before-26.1.2.json',packagedModelAudit='evidence/texture-slots-after-26.1.2.json',
        listenerAudit='evidence/texture-subscriber-after-26.1.2.json',listenerCount=listeners['listenerCount'],
        abstractOrUnresolvedListeners=0,modelRootCount=slots['reachableOwnModelRoots'],resolvedTextureUses=slots['resolvedTextureUses'],
        unresolvedTextureSlotsOrFiles=0,gameLaunched=False,testsRun=False,runtimeConfirmed=False,
        scratchCleanupComplete=False,maximumMinecraftGameWindows=1,backgroundCompilersAllowed=True)
    receipt_path.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:report[k] for k in ['buildSucceeded','artifacts','changedJarEntries','removedJarEntries','allClassBytesUnchanged','allPngBytesUnchanged']},indent=2))

if __name__=='__main__':
    main()
