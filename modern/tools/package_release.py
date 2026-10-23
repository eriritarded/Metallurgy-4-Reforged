"""Copy ordinary Gradle deliverables and inspect archive contents without executing them."""
from pathlib import Path
import argparse,hashlib,json,shutil,struct,zipfile
from datetime import datetime,timezone
ROOT=Path(__file__).resolve().parents[1]
TARGETS={'forge-1.20.1':(61,'1.20.1','Forge47.4.10','17'),'neoforge-1.21.1':(65,'1.21.1','NeoForge21.1.262','21'),'neoforge-26.1.2':(69,'26.1.2','NeoForge26.1.2.122','25')}

def digest(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()

def inspect(path,major):
    with zipfile.ZipFile(path) as jar:
        names=jar.namelist();duplicates=sorted({name for name in names if names.count(name)>1})
        if duplicates:raise ValueError('Duplicate archive paths: '+str(duplicates[:8]))
        classes=[name for name in names if name.endswith('.class')];foreign=[name for name in classes if not name.startswith('it/hurts/metallurgy_reforged/')]
        verification=[name for name in names if any(word in name.lower() for word in ['gametest','coreverification','verificationmod','test_hooks'])]
        if foreign or verification:raise ValueError('Unexpected production content: '+str((foreign+verification)[:12]))
        majors=sorted({struct.unpack('>H',jar.read(name)[6:8])[0] for name in classes})
        if majors!=[major]:raise ValueError('Wrong classfile version: '+str(majors))
        for file in ['LICENSE','NOTICE.md']:
            if jar.read(file)!=(ROOT/file).read_bytes():raise ValueError('License/notice mismatch: '+file)
        metadata='META-INF/mods.toml' if major==61 else 'META-INF/neoforge.mods.toml'
        if metadata not in names or 'metallurgy.mixins.json' not in names:raise ValueError('Missing native metadata')
        if any(name.endswith('.jar') for name in names):raise ValueError('Unexpected bundled dependency JAR')
        return {'classCount':len(classes),'classFileMajors':majors,'nativeMetadata':metadata,'foreignImplementationClasses':foreign,'verificationArtifacts':verification,'archivePaths':len(names),'licenseAndNoticeMatch':True}

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--scratch',type=Path,required=True);args=parser.parse_args()
    baseline=json.loads((ROOT/'docs/source-baseline.json').read_text(encoding='utf-8'));work=args.scratch.resolve()
    if work!=Path(baseline['scratch']).resolve() or work.drive.lower()!='h:':raise ValueError('Not the owned H: build directory')
    destination=ROOT/'artifacts';destination.mkdir(exist_ok=True);entries=[]
    for target,(major,mc,loader,java) in TARGETS.items():
        stage=work/'builds'/target/target/'build/libs';jars=sorted(stage.glob('*.jar'));binary=[p for p in jars if not p.name.endswith('-sources.jar')];sources=[p for p in jars if p.name.endswith('-sources.jar')]
        if len(binary)!=1 or len(sources)!=1:raise ValueError('Expected exactly one ordinary and source JAR: '+target)
        inspection=inspect(binary[0],major)
        for source in binary+sources:
            out=destination/source.name
            if out.exists() and digest(out)!=digest(source):
                backup=destination/'backups'/digest(out);backup.mkdir(parents=True,exist_ok=True);shutil.copy2(out,backup/out.name)
            shutil.copy2(source,out)
            if digest(out)!=digest(source):raise ValueError('Deliverable copy mismatch')
            entries.append({'file':out.name,'target':target,'kind':'sources' if source in sources else 'playable-mod','minecraft':mc,'loader':loader,'java':java,'size':out.stat().st_size,'sha256':digest(out),**(inspection if source in binary else {})})
    result={'version':'1.4.1-port.1','createdAt':datetime.now(timezone.utc).isoformat(),'state':'Ordinary production packages compiled and built; no current runtime validation or installation','gameLaunched':False,'testsRun':False,'mainProfileModified':False,'upstreamRevisions':baseline['sourceRevisions'],'artifacts':entries,'evidence':['evidence/production-package-checkpoint.json','docs/static-asset-inventory.json','docs/source-role-audit.json'],'limits':['Native schema decoding, optional dependency loading, runtime mixins, gameplay, rendering, multiplayer and save/reload were not executed.','Optional API support differs by target; see docs/RELEASE.md.','The requested Prism instance remains on26.1.2.117; the26 artifact requires26.1.2.122.']}
    (destination/'manifest.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    (destination/'SHA256SUMS.txt').write_text(''.join(entry['sha256']+'  '+entry['file']+'\n' for entry in entries),encoding='utf-8')
    print(json.dumps([{key:entry[key] for key in ['file','size','sha256']} for entry in entries],indent=2))
if __name__=='__main__':main()
