"""Compile the optional native26 trait adapter; package only committed matching sources.

Requires explicit local build inputs. Writes all temporary output under an owned
H: scratch directory. No game launch, tests, installation or dependency bundling.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess,zipfile
from datetime import datetime,timezone

ROOT=Path(__file__).resolve().parents[1]
TARGET=ROOT/'neoforge-26.1.2'
PACKAGE='it/hurts/metallurgy_reforged/'
SOURCES=[ROOT/'common/src/main/java'/PACKAGE/'compat/HephaestusData.java',
         ROOT/'common/src/main/java'/PACKAGE/'compat/HephaestusTraitData.java',
         TARGET/'src/main/java'/PACKAGE/'Metallurgy.java']
SOURCES+=sorted((TARGET/'src/main/java'/PACKAGE/'compat/hephaestus').rglob('*.java'))

def sha(path):
    with Path(path).open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()

def git(*args):return subprocess.check_output(['git','-C',str(ROOT),*args]).decode().strip()

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--scratch',type=Path,required=True)
    p.add_argument('--baseline',type=Path,required=True)
    p.add_argument('--hephaestus-tools',type=Path,required=True)
    p.add_argument('--dependency-manifest',type=Path,required=True)
    p.add_argument('--javac',type=Path,required=True)
    p.add_argument('--package',action='store_true')
    args=p.parse_args()
    if not Path('H:/').is_dir() or shutil.disk_usage('H:/').free<20*1024**3:raise RuntimeError('H: unavailable or below 20 GiB free')
    work=args.scratch.resolve(strict=True)
    parent=Path('H:/CodexScratch/terraria_modding/.test-runtime').resolve(strict=True)
    if work.parent!=parent or not work.name.startswith('metallurgy-hephaestus-traits-') or work.is_symlink():raise ValueError('Invalid owned scratch')
    dirty=bool(git('status','--porcelain','--','.'))
    if args.package and dirty:raise ValueError('Commit current source before production packaging')
    if sha(args.baseline)!='a2955f2165cc5591ff5034112c5695ff2651ef38c317f73247d4eb41118f9ddc':raise ValueError('Wrong Hephaestus baseline')
    if sha(args.hephaestus_tools)!='6f41198cf8eb8e2b415bcaa5a6be41fa6718a51132b7a94919373c861e3453ed':raise ValueError('Wrong optional API dependency')
    old=json.loads(args.dependency_manifest.read_text(encoding='utf-8-sig'))
    old=next(row for row in old['records'] if row['target']=='neoforge-26.1.2')
    libraries=[]
    for item in old['dependencies']:
        path=Path(item['path'])
        if 'Metallurgy-Reforged-' in path.name:continue
        if not path.is_file() or sha(path)!=item['sha256']:raise ValueError('Changed compile dependency: '+path.name)
        libraries.append(path)
    libraries+=[args.baseline.resolve(),args.hephaestus_tools.resolve()]
    stage=work/'trait-build'
    stage.mkdir(exist_ok=True)
    out=stage/'classes';out.mkdir(exist_ok=True)
    empty=stage/'empty-sourcepath';empty.mkdir(exist_ok=True)
    env=os.environ.copy();env.update(TEMP=str(work),TMP=str(work),JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir='+str(work))
    command=[str(args.javac),'--release','25','-encoding','UTF-8','-proc:none','-implicit:none','--source-path',str(empty),
             '-classpath',os.pathsep.join(map(str,libraries)),'-d',str(out),*map(str,SOURCES)]
    arguments=stage/'javac.args'
    arguments.write_text('\n'.join(json.dumps(arg) for arg in command[1:])+'\n',encoding='utf-8')
    log=stage/'javac.log'
    with log.open('wb') as stream:
        result=subprocess.run([str(args.javac),'@'+str(arguments)],env=env,stdout=stream,stderr=subprocess.STDOUT)
    if result.returncode:
        print(log.read_text(encoding='utf-8',errors='replace')[-18000:])
        raise SystemExit(result.returncode)
    classes={p.relative_to(out).as_posix():p.read_bytes() for p in out.rglob('*.class')}
    record=dict(schema=1,recordedAt=datetime.now(timezone.utc).isoformat(),sourceCommit=git('rev-parse','HEAD'),sourceDirty=dirty,
                target='NeoForge26.1.2.122',java=25,version='1.4.1-port.18',compilerExitCode=0,
                buildMode='Changed-class compilation against pinned ordinary Hephaestus port.17 and native runtime; not a full clean rebuild',
                baseline=dict(file=args.baseline.name,sha256=sha(args.baseline)),
                optionalDependency=dict(file=args.hephaestus_tools.name,sha256=sha(args.hephaestus_tools)),
                compiledSources=[dict(path=s.relative_to(ROOT).as_posix(),sha256=sha(s)) for s in SOURCES],
                compiledClasses=[dict(path=n,sha256=hashlib.sha256(b).hexdigest()) for n,b in sorted(classes.items())],
                libraries=[dict(file=x.name,sha256=sha(x)) for x in libraries],
                compilerLogSha256=sha(log),annotationProcessing=False,testsRun=False,gameLaunched=False,installed=False,runtimeConfirmed=False,packaged=False)
    if args.package:
        for s in SOURCES:
            relative=s.relative_to(ROOT).as_posix()
            committed=subprocess.check_output(['git','-C',str(ROOT),'show','HEAD:modern/'+relative])
            if committed!=s.read_bytes():raise ValueError('Compiler source bytes differ from Git: '+relative)
        resources={}
        common=ROOT/'common/src/main/resources'
        resources['assets/metallurgy/hephaestus_traits.json']=(common/'assets/metallurgy/hephaestus_traits.json').read_bytes()
        resources['assets/metallurgy/lang/en_us.json']=(common/'assets/metallurgy/lang/en_us.json').read_bytes()
        resources['META-INF/neoforge.mods.toml']=(TARGET/'src/main/resources/META-INF/neoforge.mods.toml').read_bytes()
        resources['metallurgy.hephaestus.mixins.json']=(TARGET/'src/main/resources/metallurgy.hephaestus.mixins.json').read_bytes()
        folder=TARGET/'src/main/resources/resourcepacks/hephaestus/data/metallurgy/hephaestus_tools/materials'
        assignments=json.loads(resources['assets/metallurgy/hephaestus_traits.json'])['materials']
        for metal in assignments:
            file=folder/(metal+'.json');resources[file.relative_to(TARGET/'src/main/resources').as_posix()]=file.read_bytes()
        provenance=dict(sourceCommit=record['sourceCommit'],version=record['version'],target=record['target'],
                        buildMode=record['buildMode'],baseline=record['baseline'],optionalDependency=record['optionalDependency'],
                        inheritedSourceLinkage='The baseline is Claude\'s retained Hephaestus package; inherited metadata is not clean-build evidence.',
                        compiledClasses=record['compiledClasses'],gameLaunched=False,testsRun=False,installed=False)
        resources['META-INF/metallurgy-build.json']=(json.dumps(provenance,indent=2)+'\n').encode()
        artifacts=ROOT/'artifacts';artifacts.mkdir(exist_ok=True)
        binary=artifacts/'Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.18.jar'
        source=binary.with_name(binary.stem+'-sources.jar')
        if binary.exists() or source.exists():raise ValueError('Refusing to overwrite a release artifact')
        with zipfile.ZipFile(args.baseline) as z:before={n:z.read(n) for n in z.namelist() if not n.endswith('/')}
        after={**before,**classes,**resources}
        with zipfile.ZipFile(binary,'w',zipfile.ZIP_DEFLATED) as z:
            for n,b in sorted(after.items()):z.writestr(n,b)
        source_files={}
        for base in [ROOT/'common/src/main/java',ROOT/'common/src/main/resources',TARGET/'src/main/java',TARGET/'src/main/resources']:
            for file in base.rglob('*'):
                if file.is_file():source_files[file.relative_to(base).as_posix()]=file.read_bytes()
        for n in ['LICENSE','NOTICE.md']:source_files[n]=(ROOT/n).read_bytes()
        source_files['META-INF/metallurgy-build.json']=resources['META-INF/metallurgy-build.json']
        with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED) as z:
            for n,b in sorted(source_files.items()):z.writestr(n,b)
        changed=sorted(n for n in before if before[n]!=after[n]);added=sorted(after.keys()-before.keys())
        if any(n.endswith('.png') for n in changed+added):raise ValueError('Unexpected artwork change')
        if any('/recipe/' in n or '/recipes/' in n for n in changed+added):raise ValueError('Unexpected recipe change')
        record.update(packaged=True,artifacts=[dict(file=x.name,sha256=sha(x),bytes=x.stat().st_size) for x in [binary,source]],
                      changedEntries=changed,addedEntries=added,unchangedPngCount=sum(n.endswith('.png') and before[n]==after[n] for n in before),
                      unchangedClassCount=sum(n.endswith('.class') and before[n]==after[n] for n in before))
        (ROOT/'docs/hephaestus-traits-build.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:record[k] for k in ['sourceCommit','sourceDirty','compilerExitCode','packaged']},indent=2))
    print('Compiled classes:',len(classes))
    if args.package:print(json.dumps(record['artifacts'],indent=2))

if __name__=='__main__':main()
