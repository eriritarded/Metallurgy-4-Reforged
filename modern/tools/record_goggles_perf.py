"""Inspect/package goggles scan performance without executing game classes."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,re,shutil,zipfile
from package_release import inspect

ROOT=Path(__file__).resolve().parents[1]
TARGETS={
 'forge-1.20.1':('1.4.1-port.5',61,'1.20.1','forge','Forge47.4.10','17'),
 'neoforge-1.21.1':('1.4.1-port.5',65,'1.21.1','neoforge','NeoForge21.1.262','21'),
 'neoforge-26.1.2':('1.4.1-port.11',69,'26.1.2','neoforge','NeoForge26.1.2.122','25')}
def read(path):return json.loads(path.read_text(encoding='utf-8-sig'))
def dump(path,value):path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def sha(path):
 with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()

def main():
 receipt=ROOT/'evidence/goggles-perf-release.json'
 if receipt.exists() and read(receipt).get('installed'):raise RuntimeError('Preserve the completed receipt')
 work=Path((ROOT/'evidence/current-goggles-perf-scratch.txt').read_text().strip()).resolve()
 if work.parent!=Path('H:/CodexScratch/terraria_modding/.test-runtime').resolve() or not work.name.startswith('metallurgy-goggles-perf-'):raise RuntimeError('Unexpected scratch')
 manifest=read(ROOT/'artifacts/manifest.json');artifacts=[];records=[]

 for target,(version,major,mc,loader_name,loader,java) in TARGETS.items():
  log=work/(target+'-build.log');text=log.read_text(encoding='utf-8',errors='replace');marks=re.findall(r'BUILD (SUCCESSFUL|FAILED)',text)
  if not marks or marks[-1]!='SUCCESSFUL' or re.search(r'(?m)^.*error:',text):raise RuntimeError('Build failed: '+target)
  name=f'Metallurgy-Reforged-MC-{mc}-{loader_name}-{version}'
  binary=work/'builds'/target/target/'build/libs'/(name+'.jar');source=binary.with_name(name+'-sources.jar')
  inspection=inspect(binary,major)
  previous=next(a for a in manifest['artifacts'] if a.get('target')==target and a.get('kind')=='playable-mod')
  prior_path=ROOT/'artifacts'/previous['file']
  if sha(prior_path)!=previous['sha256']:raise RuntimeError('Previous package identity mismatch')
  with zipfile.ZipFile(binary) as jar,zipfile.ZipFile(prior_path) as old,zipfile.ZipFile(source) as src:
   names=set(jar.namelist());old_names=set(old.namelist())
   changed=sorted(n for n in names&old_names if jar.read(n)!=old.read(n))
   added=sorted(names-old_names);removed=sorted(old_names-names)
   pngs=[n for n in old_names if n.endswith('.png')]
   if any(n not in names or jar.read(n)!=old.read(n) for n in pngs):raise RuntimeError('Original art changed')
   allowed=set()
   resource_changes=[n for n in changed if n.startswith(('assets/','data/','resourcepacks/'))]
   if set(resource_changes)-allowed:raise RuntimeError('Unexpected resource changes: '+str(set(resource_changes)-allowed))
   class_changes=[n for n in changed if n.endswith('.class')]
   required={'it/hurts/metallurgy_reforged/client/DetectorDisplay.class','it/hurts/metallurgy_reforged/client/GadgetOverlay.class'}
   if not required.issubset(class_changes) or set(class_changes)-required-{'it/hurts/metallurgy_reforged/client/DetectorDisplay$Ore.class'}:raise RuntimeError('Unexpected compiled changes: '+str(class_changes))
   if any(n.startswith(('assets/','data/','resourcepacks/')) and not n.endswith('/') for n in added+removed):raise RuntimeError('Unexpected added/removed resources')
   if f'version="{version}"'.encode() not in jar.read(inspection['nativeMetadata']):raise RuntimeError('Wrong native version metadata')
   matched=0
   for n in src.namelist():
    if n.endswith('.java'):
     paths=[ROOT/target/'src/main/java'/n,ROOT/'common/src/main/java'/n];p=next((p for p in paths if p.is_file()),None)
     if p is None or src.read(n)!=p.read_bytes():raise RuntimeError('Stale Java: '+n)
     matched+=1
   if b'tickGoggles' not in jar.read('it/hurts/metallurgy_reforged/client/DetectorDisplay.class'):raise RuntimeError('Missing cached goggles scan')
   detector_source=src.read('it/hurts/metallurgy_reforged/client/DetectorDisplay.java').decode('utf-8')
   if 'redstone(Vec3' in detector_source or 'mc.screen==null' not in detector_source or 'nextRedstoneScan=gogglesTicks+5' not in detector_source:raise RuntimeError('Missing menu gate or scan interval')
   record=dict(target=target,version=version,buildSucceeded=True,buildTasks=['jar','sourcesJar'],buildOptions=['org.gradle.workers.max=2','org.gradle.internal.instrumentation.agent=false'],buildLogSha256=sha(log),compilerErrors=0,compilerWarnings=len(re.findall(r'(?m)^.*warning:',text)),previous=previous,changedJarEntries=changed,addedJarEntries=added,removedJarEntries=removed,changedResources=resource_changes,originalPngsByteIdentical=len(pngs),sourceArchiveMatchesMaintainedJava=True,matchedJavaSources=matched)
  for p,kind in [(binary,'playable-mod'),(source,'sources')]:
   out=ROOT/'artifacts'/p.name
   if out.exists() and sha(out)!=sha(p):raise RuntimeError('Released artifact already differs')
   shutil.copy2(p,out)
   if sha(out)!=sha(p):raise RuntimeError('Copy mismatch')
   artifacts.append(dict(file=p.name,target=target,version=version,kind=kind,minecraft=mc,loader=loader,java=java,size=p.stat().st_size,sha256=sha(p),**(inspection if kind=='playable-mod' else {})))
  records.append(record)
 result=dict(recordedAt=datetime.now(timezone.utc).isoformat(),scratch=str(work),buildSucceeded=True,artifacts=artifacts,targets=records,sourceReview='evidence/goggles-perf-source-review.json',installed=False,scratchCleanupComplete=False,gameLaunched=False,testsRun=False,renderingObserved=False,inventoryPerformanceRuntimeObserved=False)
 dump(receipt,result)
 superseded=manifest.setdefault('supersededArtifacts',[]);known={a['file'] for a in superseded}
 for a in manifest['artifacts']:
  if a['file'] not in known:superseded.append(a)
 manifest.update(artifacts=artifacts,targetVersions={t:v[0] for t,v in TARGETS.items()},updatedAt=result['recordedAt'],state='Goggles performance packages built and inspected; installation pending. No game/runtime check.',currentReleaseEvidence='evidence/goggles-perf-release.json',currentReleaseGameLaunched=False,currentReleaseTestsRun=False,currentReleaseRuntimeConfirmed=False,currentReleaseScratchRemoved=False,currentReleaseInstalled=False)
 dump(ROOT/'artifacts/manifest.json',manifest)
 (ROOT/'artifacts/SHA256SUMS.txt').write_text(''.join(a['sha256']+'  '+a['file']+'\n' for a in artifacts),encoding='utf-8')
 print(json.dumps([{'target':r['target'],'version':r['version'],'originalPngsUnchanged':r['originalPngsByteIdentical'],'changedResources':r['changedResources']} for r in records]))

if __name__=='__main__':main()
