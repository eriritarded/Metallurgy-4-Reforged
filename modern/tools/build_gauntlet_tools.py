"""Compile the three native GadgetItem adapters only; no game or tests."""
from pathlib import Path
from datetime import datetime,timezone
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import zipfile
from build_report_fixes import ROOT,dependencies,OLD_NATIVE,RENAMER,sha,read,dump,FIELD

RELATIVE='src/main/java/it/hurts/metallurgy_reforged/gadget/GadgetItem.java'
CLASS='it/hurts/metallurgy_reforged/gadget/GadgetItem.class'
TARGETS={'forge-1.20.1':17,'neoforge-1.21.1':21,'neoforge-26.1.2':25}

def owned_work(path):
    if not Path('H:/').is_dir() or shutil.disk_usage('H:/').free<20*1024**3:raise RuntimeError('H: unavailable or below20 GiB free')
    path=path.resolve(strict=True)
    allowed=Path('H:/CodexScratch/terraria_modding/.test-runtime').resolve(strict=True)
    if path.parent!=allowed or not path.name.startswith('metallurgy-gauntlet-tools-') or path.is_symlink():raise RuntimeError('Invalid owned scratch')
    return path

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scratch',type=Path,required=True)
    work=owned_work(parser.parse_args().scratch)
    commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT.parent,text=True).strip()
    manifest=read(ROOT/'artifacts/manifest.json')
    temp=work/'process-temp';temp.mkdir(exist_ok=True)
    env=os.environ.copy();env.update(TEMP=str(temp),TMP=str(temp),TMPDIR=str(temp),JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir='+str(temp))
    records=[]
    for target,java in TARGETS.items():
        source=ROOT/target/RELATIVE
        committed=subprocess.check_output(['git','show',commit+':'+source.relative_to(ROOT.parent).as_posix()],cwd=ROOT.parent)
        if committed!=source.read_bytes():raise RuntimeError('Commit exact production source first')
        baseline=next(a for a in manifest['artifacts'] if a['target']==target and a['kind']=='playable-mod')
        archive=ROOT/'artifacts'/baseline['file']
        if sha(archive)!=baseline['sha256']:raise RuntimeError('Baseline identity mismatch')
        if java==25:
            reference=read(ROOT/'evidence/bees-login-compile-inputs.json')
            cp=[Path(r['path']) for r in reference['classpath'] if '/mods/' not in Path(r['path']).as_posix() and Path(r['path']).parent!=ROOT/'artifacts']
        else:
            cp=dependencies(target)
            if java==17:
                cp=[OLD_NATIVE]+[p for p in cp if p.name!='forge-1.20.1-47.4.28.jar']
        if any(not p.is_file() for p in cp):raise RuntimeError('Missing pinned compiler platform')
        cp.append(archive)
        classes=work/target/'classes';classes.mkdir(parents=True,exist_ok=True)
        empty=work/target/'empty-sourcepath';empty.mkdir(exist_ok=True)
        javac=Path(f'C:/Program Files/Java/jdk-{java}/bin/javac.exe')
        command=[str(javac),'--release',str(java),'-encoding','UTF-8','-proc:none','-implicit:none','--source-path',str(empty),'-cp',os.pathsep.join(map(str,cp)),'-d',str(classes),str(source)]
        proc=subprocess.run(command,capture_output=True,text=True,encoding='utf-8',errors='replace',env=env)
        log=work/target/'compile.log';log.write_text(proc.stdout+proc.stderr,encoding='utf-8')
        if proc.returncode:print(proc.stdout+proc.stderr);raise SystemExit(proc.returncode)
        expected={CLASS} if java==25 else {CLASS,CLASS.replace('.class','$1.class')}
        actual={p.relative_to(classes).as_posix() for p in classes.rglob('*.class')}
        if actual!=expected:raise RuntimeError('Unexpected compiler output: '+str(actual))
        record=dict(target=target,sourceCommit=commit,source=source.relative_to(ROOT).as_posix(),sourceSha256=sha(source),
                    baseline=baseline,java=java,exitCode=0,annotationProcessing=False,implicitCompilation=False,
                    command=command,compileLogSha256=sha(log),dependencies=[dict(path=str(p),sha256=sha(p)) for p in cp])
        if java==17:
            named=work/target/'named.jar';mapped=work/target/'reobfuscated.jar'
            with zipfile.ZipFile(named,'w') as jar:
                for name in sorted(actual):jar.write(classes/name,name)
            mapping=FIELD/target/'build/moddev/artifacts/namedToIntermediate.tsrg'
            remap=[str(javac.with_name('java.exe')),'-jar',str(RENAMER),'--input',str(named),'--output',str(mapped),'--map',str(mapping)]
            for library in cp:remap+=['--lib',str(library)]
            proc=subprocess.run(remap,capture_output=True,text=True,encoding='utf-8',errors='replace',env=env)
            remap_log=work/target/'reobfuscation.log';remap_log.write_text(proc.stdout+proc.stderr,encoding='utf-8')
            if proc.returncode:print(proc.stdout+proc.stderr);raise SystemExit(proc.returncode)
            with zipfile.ZipFile(mapped) as jar:outputs={n:jar.read(n) for n in actual}
            record.update(reobfuscationCommand=remap,mappingSha256=sha(mapping),renamerSha256=sha(RENAMER),reobfuscationLogSha256=sha(remap_log))
        else:outputs={n:(classes/n).read_bytes() for n in actual}
        production=work/target/'production';production.mkdir(exist_ok=True)
        for name,content in outputs.items():
            path=production/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(content)
        record['classes']=[dict(path=n,sha256=hashlib.sha256(b).hexdigest(),classFileMajor=int.from_bytes(b[6:8],'big')) for n,b in sorted(outputs.items())]
        records.append(record)
        print(json.dumps(dict(target=target,compiledClasses=len(outputs),exitCode=0)))
    dump(ROOT/'evidence/gauntlet-tools-compile.json',dict(recordedAt=datetime.now(timezone.utc).isoformat(),sourceCommit=commit,records=records,gameLaunched=False,testsRun=False,runtimeConfirmed=False))

if __name__=='__main__':main()
