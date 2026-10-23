"""Compile only the native26 gauntlet hand subscriber; no tests or game launches."""
from pathlib import Path
from datetime import datetime, timezone
import argparse
import hashlib
import json
import os
import shutil
import struct
import subprocess

ROOT = Path(__file__).resolve().parents[1]
WORKSPACE = ROOT.parent
RELATIVE = 'neoforge-26.1.2/src/main/java/it/hurts/metallurgy_reforged/client/GauntletHandRenderer.java'
CLASS = 'it/hurts/metallurgy_reforged/client/GauntletHandRenderer.class'


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def owned_work(path):
    if not Path('H:/').is_dir() or shutil.disk_usage('H:/').free < 20 * 1024 ** 3:
        raise RuntimeError('H: unavailable or below 20 GiB free; no C: fallback')
    work = path.resolve(strict=True)
    allowed = Path('H:/CodexScratch/terraria_modding/.test-runtime').resolve(strict=True)
    if work.parent != allowed or not work.name.startswith('metallurgy-gauntlet-arm-') or path.is_symlink():
        raise ValueError('Not an owned gauntlet-arm scratch directory')
    return work


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scratch', type=Path, required=True)
    args = parser.parse_args()
    work = owned_work(args.scratch)
    source = ROOT / RELATIVE
    commit = subprocess.check_output(['git', '-C', str(WORKSPACE), 'rev-parse', 'HEAD'], text=True).strip()
    committed = subprocess.check_output(['git', '-C', str(WORKSPACE), 'show', commit + ':MetallurgyModern/' + RELATIVE])
    if committed != source.read_bytes():
        raise RuntimeError('Commit the exact renderer source before the production compilation')
    baseline = ROOT / 'artifacts/Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.13.jar'
    manifest = json.loads((ROOT / 'artifacts/manifest.json').read_text(encoding='utf-8-sig'))
    record = next(a for a in manifest['artifacts'] if a['file'] == baseline.name)
    if sha(baseline) != record['sha256']:
        raise RuntimeError('Baseline artifact identity changed')
    reference = json.loads((ROOT / 'evidence/bees-login-compile-inputs.json').read_text(encoding='utf-8-sig'))
    # Reuse exact-target loader/platform dependencies, without either Bees addon.
    paths = [Path(row['path']) for row in reference['classpath']
             if Path(row['path']).is_file() and '/mods/' not in Path(row['path']).as_posix()]
    paths.append(baseline)
    for required, expected in [('minecraft-client-patched-26.1.2.122.jar', '0621b17307aa4e75340366bf90970bc296ace07b286a5f194f2a78a98aec4346'),
                               ('neoforge-26.1.2.122-universal.jar', '650705f7000994a12b2619fabfcaa3d23b5b3361e658a56a17ce7ae2e9b9c213')]:
        matches = [p for p in paths if p.name == required]
        if len(matches) != 1 or sha(matches[0]) != expected:
            raise RuntimeError('Exact native dependency identity mismatch: ' + required)
    temp = work / 'process-temp'
    temp.mkdir(exist_ok=True)
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    sourcepath = work / 'sourcepath'
    sourcepath.mkdir(exist_ok=True)
    javac = Path('C:/Program Files/Java/jdk-25/bin/javac.exe')
    command = [str(javac), '--release', '25', '-encoding', 'UTF-8', '-proc:none', '-implicit:none',
               '--source-path', str(sourcepath), '-classpath', os.pathsep.join(map(str, paths)),
               '-d', str(classes), str(source)]
    env = os.environ.copy()
    env.update(TEMP=str(temp), TMP=str(temp), TMPDIR=str(temp),
               JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir=' + str(temp))
    result = subprocess.run(command, capture_output=True, text=True, encoding='utf-8', errors='replace', env=env)
    (work / 'compiler.log').write_text(result.stdout + result.stderr, encoding='utf-8')
    if result.returncode:
        print(result.stdout + result.stderr)
        raise SystemExit(result.returncode)
    outputs = sorted(p.relative_to(classes).as_posix() for p in classes.rglob('*.class'))
    if outputs != [CLASS] or struct.unpack('>H', (classes / CLASS).read_bytes()[6:8])[0] != 69:
        raise RuntimeError('Unexpected compiler output')
    receipt = {'recordedAt': datetime.now(timezone.utc).isoformat(), 'target': 'neoforge-26.1.2',
               'minecraft': '26.1.2', 'neoforge': '26.1.2.122', 'java': '25', 'sourceCommit': commit,
               'source': RELATIVE, 'sourceSha256': sha(source), 'baselineArtifact': baseline.name,
               'baselineArtifactSha256': sha(baseline), 'exitCode': result.returncode,
               'annotationProcessing': False, 'implicitCompilation': False, 'class': CLASS,
               'classSha256': sha(classes / CLASS), 'classFileMajor': 69, 'classpathEntries': len(paths),
               'command': command, 'classpath': [{'path': str(p), 'sha256': sha(p)} for p in paths],
               'gameLaunched': False, 'testsRun': False, 'renderingObserved': False}
    (ROOT / 'evidence/gauntlet-arm-compile.json').write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: receipt[k] for k in ['sourceCommit', 'exitCode', 'classFileMajor', 'classpathEntries']}))


if __name__ == '__main__':
    main()
