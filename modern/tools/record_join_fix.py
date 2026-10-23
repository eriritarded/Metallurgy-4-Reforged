"""Record port.6 compilation and static serializer evidence without loading game classes."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, os, re, subprocess, zipfile
from package_release import inspect

ROOT=Path(__file__).resolve().parents[1]
SCRATCH=Path('H:/CodexScratch/terraria_modding/.test-runtime/metallurgy-join-fix-8e45e0cf4a1e42bb9d55d4af537a410f')
NATIVE=Path('C:/Users/USER/AppData/Roaming/PrismLauncher/libraries/net/neoforged/minecraft-client-patched/26.1.2.122/minecraft-client-patched-26.1.2.122.jar')
NAME='Metallurgy-Reforged-MC-26.1.2-neoforge-'
VERSION='1.4.1-port.6'

def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()

def read(name):return json.loads((ROOT/name).read_text(encoding='utf-8-sig'))

def javap(path,name):
    temporary=str(SCRATCH/'process-temp');env=dict(os.environ)
    env.update(TEMP=temporary,TMP=temporary,TMPDIR=temporary,JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir='+temporary)
    return subprocess.run(['C:/Program Files/Java/jdk-25/bin/javap.exe','-classpath',str(path),'-p','-c',name],env=env,capture_output=True,text=True,encoding='utf-8',check=True).stdout

def main():
    receipt=ROOT/'evidence/singleplayer-join-fix-26.1.2.json'
    if receipt.exists() and read('evidence/singleplayer-join-fix-26.1.2.json').get('installed'):
        raise RuntimeError('Preserve the completed historical installation receipt.')
    log=SCRATCH/'join-fix-neoforge-26.1.2.log';text=log.read_text(encoding='utf-8',errors='replace')
    assert 'BUILD SUCCESSFUL' in text and not re.search(r'(?m)^.*error:',text)
    assert not read('evidence/singleplayer-join-fix-source-26.1.2.json')['mismatches']
    binary=ROOT/'artifacts'/(NAME+VERSION+'.jar');previous=ROOT/'artifacts'/(NAME+'1.4.1-port.5.jar')
    artifacts=[]
    for suffix,kind in [('.jar','playable-mod'),('-sources.jar','sources')]:
        file=ROOT/'artifacts'/(NAME+VERSION+suffix)
        assert sha(file)==sha(SCRATCH/'builds/neoforge-26.1.2/neoforge-26.1.2/build/libs'/file.name)
        artifacts.append(dict(file=file.name,target='neoforge-26.1.2',version=VERSION,kind=kind,minecraft='26.1.2',loader='NeoForge26.1.2.122',java='25',size=file.stat().st_size,sha256=sha(file),**(inspect(file,69) if kind=='playable-mod' else {})))
    with zipfile.ZipFile(binary) as new,zipfile.ZipFile(previous) as old:
        new_names,old_names=set(new.namelist()),set(old.namelist())
        assert new_names==old_names
        changed=sorted(n for n in new_names if new.read(n)!=old.read(n))
        assert changed==['META-INF/neoforge.mods.toml','it/hurts/metallurgy_reforged/compat/JeiPlugin.class','it/hurts/metallurgy_reforged/content/Content.class'],changed
        assert all(new.read(n)==old.read(n) for n in new_names if n.startswith(('assets/','data/','resourcepacks/')))
    with zipfile.ZipFile(ROOT/'artifacts'/(NAME+VERSION+'-sources.jar')) as source:
        names=source.namelist();assert len(names)==len(set(names))
        for name in ['content/Content.java','compat/JeiPlugin.java']:
            full='it/hurts/metallurgy_reforged/'+name
            assert source.read(full)==(ROOT/'neoforge-26.1.2/src/main/java'/full).read_bytes()
    content=javap(binary,'it.hurts.metallurgy_reforged.content.Content')
    signature='statelessRecipe(T);';assert signature in content
    helper=content[content.index(signature):].split('\n  public static void register',1)[0]
    assert 'MapCodec.unit' in helper and 'StreamCodec.unit' in helper and helper.count('aload_0')==2
    native=javap(NATIVE,'net.minecraft.network.codec.StreamCodec$3')
    assert 'java/lang/Object.equals' in native and 'IllegalStateException' in native
    plugin=javap(binary,'it.hurts.metallurgy_reforged.compat.JeiPlugin')
    assert 'String lemurite_shield' in plugin and 'String invisibility_shield' not in plugin
    slots=read('evidence/join-model-slots-26.1.2.json');listeners=read('evidence/join-subscriber-after-26.1.2.json')
    assert slots['artifactSha256']==sha(binary) and not slots['failures']
    assert listeners['sha256']==sha(binary) and not listeners['invalidOrUnresolved']
    r=dict(recordedAt=datetime.now(timezone.utc).isoformat(),version=VERSION,replacementVersion=VERSION,minecraft='26.1.2',neoforge='26.1.2.122',scratch=str(SCRATCH),buildSucceeded=True,installed=False,buildTasks=['jar','sourcesJar'],buildLogSha256=sha(log),compilerErrors=0,compilerWarnings=len(re.findall(r'(?m)^.*warning:',text)),artifacts=artifacts,changedJarEntries=changed,addedJarEntries=[],removedJarEntries=[],allAssetRecipeAndPackBytesUnchanged=True,sourceArchiveMatchesMaintainedFixes=True,nativeJarSha256=sha(NATIVE),nativeUnitCodecChecksEquality=True,serializerHelperUsesSameArgumentForBothCodecs=True,serializerHelperDisassembly=helper,affectedSerializerIds=['metallurgy:ore_detector_shapeless','metallurgy:tartarite_paragon_shaped'],jeiShieldId='metallurgy:lemurite_shield',sourceEvidence='evidence/singleplayer-join-fix-source-26.1.2.json',userLogEvidence='evidence/singleplayer-disconnect-log-26.1.2.json',listenerAudit='evidence/join-subscriber-after-26.1.2.json',packagedModelAudit='evidence/join-model-slots-26.1.2.json',listenerCount=listeners['listenerCount'],abstractOrUnresolvedListeners=0,gameLaunched=False,testsRun=False,runtimeConfirmed=False,scratchCleanupComplete=False,maximumMinecraftGameWindows=1,backgroundCompilersAllowed=True)
    receipt.write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:r[k] for k in ['buildSucceeded','artifacts','changedJarEntries','serializerHelperUsesSameArgumentForBothCodecs']},indent=2))

if __name__=='__main__':main()
