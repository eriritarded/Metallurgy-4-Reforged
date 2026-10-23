"""Compile two report fixes against exact cached platforms; no game or tests."""
from pathlib import Path
from datetime import datetime, timezone
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PREFIX = 'it/hurts/metallurgy_reforged/'
FIELD = Path('H:/AI_MC_Modding/Fieldframe')
OLD_NATIVE = Path('C:/Users/USER/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.4.10_mapped_official_1.20.1/forge-1.20.1-47.4.10_mapped_official_1.20.1.jar')
RENAMER = Path('C:/Users/USER/.gradle/caches/forge_gradle/maven_downloader/net/minecraftforge/renamer/2.0.4/renamer-2.0.4-all.jar')

def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def dump(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')

def owned_work(path):
    if not Path('H:/').is_dir() or shutil.disk_usage('H:/').free < 20 * 1024 ** 3:
        raise RuntimeError('H: unavailable or below 20 GiB free')
    path = path.resolve(strict=True)
    allowed = Path('H:/CodexScratch/terraria_modding/.test-runtime').resolve(strict=True)
    if path.parent != allowed or not path.name.startswith('metallurgy-report-fix-') or path.is_symlink():
        raise RuntimeError('Invalid owned scratch directory')
    return path

def dependencies(target):
    base = FIELD / target / 'build/moddev'
    text = (base / 'clientLegacyClasspath.txt').read_text(encoding='utf-8-sig')
    paths = [Path(p) for p in re.split(r'[;\r\n]+', text) if p.strip()]
    # These module-path dependencies are excluded from the legacy classpath.
    vm = (base / 'clientRunVmArgs.txt').read_text(encoding='utf-8-sig')
    for value in re.findall(r'"([^"\r\n]+)"|([^\r\n]+)', vm):
        for entry in (value[0] or value[1]).strip().split(';'):
            p = Path(entry.strip('" '))
            if p.suffix == '.jar' and p.is_file():
                paths.append(p)
    native = base / 'artifacts' / ('forge-1.20.1-47.4.28.jar' if target.startswith('forge') else 'neoforge-21.1.262.jar')
    paths.insert(0, native)
    result = []
    for p in paths:
        if p.is_file() and p not in result:
            result.append(p)
    return result

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scratch', type=Path, required=True)
    args = parser.parse_args()
    work = owned_work(args.scratch)
    commit = subprocess.check_output(['git', '-C', str(ROOT.parent), 'rev-parse', 'HEAD'], text=True).strip()
    temp = work / 'process-temp'
    temp.mkdir(exist_ok=True)
    env = os.environ.copy()
    env.update(TEMP=str(temp), TMP=str(temp), TMPDIR=str(temp),
               JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir=' + str(temp))
    records = []
    for target, relative, java in [('forge-1.20.1', 'mixin/BlockLootMixin.java', 17),
                                    ('neoforge-1.21.1', 'client/GadgetVisuals.java', 21)]:
        source = ROOT / target / 'src/main/java' / PREFIX / relative
        git_path = source.relative_to(ROOT.parent).as_posix()
        committed = subprocess.check_output(['git', '-C', str(ROOT.parent), 'show', commit + ':' + git_path])
        if committed != source.read_bytes():
            raise RuntimeError('Commit changed production source before compilation')
        baseline = ROOT / 'artifacts' / ('Metallurgy-Reforged-MC-' + ('1.20.1-forge' if java == 17 else '1.21.1-neoforge') + '-1.4.1-port.7.jar')
        manifest = read(ROOT / 'artifacts/manifest.json')
        identity = next(a for a in manifest['artifacts'] if a['file'] == baseline.name)
        if sha(baseline) != identity['sha256']:
            raise RuntimeError('Baseline identity changed')
        cp = dependencies(target) + [baseline]
        classes = work / target / 'classes'
        classes.mkdir(parents=True, exist_ok=True)
        empty = work / target / 'empty-sourcepath'
        empty.mkdir(exist_ok=True)
        javac = Path('C:/Program Files/Java') / ('jdk-' + str(java)) / 'bin/javac.exe'
        command = [str(javac), '--release', str(java), '-encoding', 'UTF-8', '-proc:none', '-implicit:none',
                   '--source-path', str(empty), '-cp', os.pathsep.join(map(str, cp)), '-d', str(classes), str(source)]
        proc = subprocess.run(command, capture_output=True, text=True, encoding='utf-8', errors='replace', env=env)
        log = work / target / 'compile.log'
        log.write_text(proc.stdout + proc.stderr, encoding='utf-8')
        if proc.returncode:
            print(proc.stdout + proc.stderr)
            raise SystemExit(proc.returncode)
        class_name = PREFIX + relative.replace('.java', '.class')
        if [p.relative_to(classes).as_posix() for p in classes.rglob('*.class')] != [class_name]:
            raise RuntimeError('Unexpected compiler output')
        record = dict(target=target, sourceCommit=commit, source=source.relative_to(ROOT).as_posix(),
                      sourceSha256=sha(source), baseline=identity, java=java, exitCode=0,
                      annotationProcessing=False, implicitCompilation=False, compiledClass=class_name,
                      compiledClassSha256=sha(classes / class_name), command=command,
                      dependencies=[dict(path=str(p), sha256=sha(p)) for p in cp], compileLogSha256=sha(log))
        if java == 17:
            raw = work / target / 'named.jar'
            with zipfile.ZipFile(raw, 'w') as jar:
                jar.write(classes / class_name, class_name)
            mapped = work / target / 'reobfuscated.jar'
            mapping = FIELD / target / 'build/moddev/artifacts/namedToIntermediate.tsrg'
            java_exe = javac.with_name('java.exe')
            remap = [str(java_exe), '-jar', str(RENAMER), '--input', str(raw), '--output', str(mapped), '--map', str(mapping)]
            for library in cp:
                remap += ['--lib', str(library)]
            proc = subprocess.run(remap, capture_output=True, text=True, encoding='utf-8', errors='replace', env=env)
            (work / target / 'reobfuscation.log').write_text(proc.stdout + proc.stderr, encoding='utf-8')
            if proc.returncode:
                print(proc.stdout + proc.stderr)
                raise SystemExit(proc.returncode)
            record.update(reobfuscationCommand=remap, mappingSha256=sha(mapping), renamerSha256=sha(RENAMER))
            with zipfile.ZipFile(mapped) as jar:
                binary = jar.read(class_name)
            (work / target / 'production.class').write_bytes(binary)
        else:
            binary = (classes / class_name).read_bytes()
            (work / target / 'production.class').write_bytes(binary)
        record.update(productionClassSha256=hashlib.sha256(binary).hexdigest(), classFileMajor=int.from_bytes(binary[6:8], 'big'))
        records.append(record)
        print(json.dumps({k: record[k] for k in ['target', 'exitCode', 'classFileMajor', 'productionClassSha256']}))
    dump(ROOT / 'evidence/report-fixes-compile.json', dict(recordedAt=datetime.now(timezone.utc).isoformat(),
         sourceCommit=commit, records=records, gameLaunched=False, testsRun=False, runtimeConfirmed=False))

if __name__ == '__main__':
    main()
