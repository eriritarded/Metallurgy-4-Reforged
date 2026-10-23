"""Record port.7 atlas correction from files and bytecode; never load game classes."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, os, re, subprocess, zipfile
from package_release import inspect

ROOT=Path(__file__).resolve().parents[1]
SCRATCH=Path('H:/CodexScratch/terraria_modding/.test-runtime/metallurgy-atlas-fix-45c9de6487e346b0af3c96a81b22b413')
NATIVE=Path('C:/Users/USER/AppData/Roaming/PrismLauncher/libraries/net/neoforged/minecraft-client-patched/26.1.2.122/minecraft-client-patched-26.1.2.122.jar')
NAME='Metallurgy-Reforged-MC-26.1.2-neoforge-'
VERSION='1.4.1-port.7'

def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()

def read(name):return json.loads((ROOT/name).read_text(encoding='utf-8-sig'))

def javap(path,name):
    temporary=str(SCRATCH/'process-temp');env=dict(os.environ)
    env.update(TEMP=temporary,TMP=temporary,TMPDIR=temporary,JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir='+temporary)
    return subprocess.run(['C:/Program Files/Java/jdk-25/bin/javap.exe','-classpath',str(path),'-p','-c',name],env=env,capture_output=True,text=True,encoding='utf-8',check=True).stdout

def main():
    receipt=ROOT/'evidence/ore-atlas-fix-26.1.2.json'
    if receipt.exists() and read('evidence/ore-atlas-fix-26.1.2.json').get('installed'):
        raise RuntimeError('Preserve the completed historical installation receipt.')
    log=SCRATCH/'atlas-fix-neoforge-26.1.2.log';text=log.read_text(encoding='utf-8',errors='replace')
    assert 'BUILD SUCCESSFUL' in text and not re.search(r'(?m)^.*error:',text)
    assert not read('evidence/ore-atlas-fix-source-26.1.2.json')['mismatches']
    binary=ROOT/'artifacts'/(NAME+VERSION+'.jar');previous=ROOT/'artifacts'/(NAME+'1.4.1-port.6.jar')
    artifacts=[]
    for suffix,kind in [('.jar','playable-mod'),('-sources.jar','sources')]:
        file=ROOT/'artifacts'/(NAME+VERSION+suffix)
        assert sha(file)==sha(SCRATCH/'builds/neoforge-26.1.2/neoforge-26.1.2/build/libs'/file.name)
        artifacts.append(dict(file=file.name,target='neoforge-26.1.2',version=VERSION,kind=kind,minecraft='26.1.2',loader='NeoForge26.1.2.122',java='25',size=file.stat().st_size,sha256=sha(file),**(inspect(file,69) if kind=='playable-mod' else {})))
    with zipfile.ZipFile(binary) as new,zipfile.ZipFile(previous) as old:
        names=set(new.namelist());assert names==set(old.namelist())
        changed=sorted(n for n in names if new.read(n)!=old.read(n))
        assert changed==['META-INF/neoforge.mods.toml','it/hurts/metallurgy_reforged/client/OreParticle.class'],changed
        assert all(new.read(n)==old.read(n) for n in names if n.startswith(('assets/','data/','resourcepacks/')))
        sources=json.loads(new.read('assets/minecraft/atlases/blocks.json'))['sources']
        frames=['metallurgy:particles/ore_particle_'+str(i) for i in range(1,11)]
        for frame in frames:
            assert sum(s.get('type')=='minecraft:single' and s.get('resource')==frame for s in sources)==1
            assert 'assets/metallurgy/textures/'+frame.split(':',1)[1]+'.png' in names
    with zipfile.ZipFile(ROOT/'artifacts'/(NAME+VERSION+'-sources.jar')) as source:
        names=source.namelist();assert len(names)==len(set(names))
        full='it/hurts/metallurgy_reforged/client/OreParticle.java'
        assert source.read(full)==(ROOT/'neoforge-26.1.2/src/main/java'/full).read_bytes()
    particle=javap(binary,'it.hurts.metallurgy_reforged.client.OreParticle')
    start=particle.index('private static net.minecraft.client.renderer.texture.TextureAtlasSprite frame(')
    frame_body=particle[start:].split('\n  protected ',1)[0]
    assert 'net/minecraft/data/AtlasIds.BLOCKS' in frame_body and 'AtlasManager.getAtlasOrThrow' in frame_body
    assert 'TextureAtlas.LOCATION_BLOCKS' not in particle and 'Layer.OPAQUE_TERRAIN' in particle
    manager=javap(NATIVE,'net.minecraft.client.resources.model.sprite.AtlasManager')
    lookup=manager[manager.index('public net.minecraft.client.renderer.texture.TextureAtlas getAtlasOrThrow('):].split('\n  public ',1)[0]
    assert 'atlasById:' in lookup
    assert 'AtlasConfig.textureId:' in manager and 'TextureManager.register:' in manager
    assert 'net/minecraft/data/AtlasIds.BLOCKS' in manager and 'TextureAtlas.LOCATION_BLOCKS' in manager
    ids=javap(NATIVE,'net.minecraft.data.AtlasIds')
    assert re.search(r'String blocks[\s\S]{0,400}Field BLOCKS:',ids)
    slots=read('evidence/atlas-model-slots-26.1.2.json');listeners=read('evidence/atlas-subscriber-after-26.1.2.json')
    assert slots['artifactSha256']==sha(binary) and not slots['failures']
    assert listeners['sha256']==sha(binary) and not listeners['invalidOrUnresolved']
    r=dict(recordedAt=datetime.now(timezone.utc).isoformat(),version=VERSION,replacementVersion=VERSION,minecraft='26.1.2',neoforge='26.1.2.122',scratch=str(SCRATCH),buildSucceeded=True,installed=False,buildTasks=['jar','sourcesJar'],buildLogSha256=sha(log),compilerErrors=0,compilerWarnings=len(re.findall(r'(?m)^.*warning:',text)),artifacts=artifacts,changedJarEntries=changed,addedJarEntries=[],removedJarEntries=[],allAssetRecipeAndPackBytesUnchanged=True,sourceArchiveMatchesMaintainedFix=True,nativeJarSha256=sha(NATIVE),atlasId='minecraft:blocks',nativeLookupUsesAtlasById=True,nativeTextureRegistrationUsesTextureId=True,packagedParticleUsesNativeAtlasId=True,frameDisassembly=frame_body,particleFrames=frames,opaqueTerrainLayerPreserved=True,gpuTextureBindingClassBytesUnchanged=True,sourceEvidence='evidence/ore-atlas-fix-source-26.1.2.json',userCrashEvidence='evidence/ore-atlas-crash-26.1.2.json',listenerAudit='evidence/atlas-subscriber-after-26.1.2.json',packagedModelAudit='evidence/atlas-model-slots-26.1.2.json',listenerCount=listeners['listenerCount'],abstractOrUnresolvedListeners=0,gameLaunched=False,testsRun=False,runtimeConfirmed=False,scratchCleanupComplete=False,maximumMinecraftGameWindows=1,backgroundCompilersAllowed=True)
    receipt.write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:r[k] for k in ['buildSucceeded','artifacts','changedJarEntries','packagedParticleUsesNativeAtlasId']},indent=2))

if __name__=='__main__':main()
