"""Keep concise, exact staged-source evidence before disposable builds are removed."""
from pathlib import Path
import argparse
import hashlib
import json
import re
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--scratch', required=True, type=Path)
parser.add_argument('--prefix', required=True)
parser.add_argument('--receipt', required=True)
parser.add_argument('--scope', required=True)
parser.add_argument('--kind', choices=['fixtures','compile','package'], default='fixtures')
parser.add_argument('--require-maintained-source-match', action='store_true')
args = parser.parse_args()
baseline = json.loads((ROOT / 'docs/source-baseline.json').read_text(encoding='utf-8-sig'))
if args.scratch.resolve() != Path(baseline['scratch']).resolve():
    raise ValueError('Not the owned project scratch directory')
if not re.fullmatch(r'[a-z0-9-]+\.json', args.receipt):
    raise ValueError('Invalid receipt name')
compile_only=args.kind!='fixtures'
hash_cache={}
def hash_files(paths):
    missing=list(dict.fromkeys(p for p in paths if p not in hash_cache))
    def digest(path):
        with path.open('rb') as stream:return path,hashlib.file_digest(stream,'sha256').hexdigest()
    with ThreadPoolExecutor(max_workers=4) as pool:
        hash_cache.update(pool.map(digest,missing))
    return {p:hash_cache[p] for p in paths}
result = {'scope': args.scope, 'recordedAt': datetime.now(timezone.utc).isoformat(), 'targets': {}, 'mainProfileModified': False,
          'gameLaunched': not compile_only, 'testsRun': not compile_only,
          'limits': 'Compilation only; no game, gameplay, material-decoding or client-rendering proof. Incomplete port, no production release.' if compile_only else 'Headless native server fixtures only; no graphical or connected-client proof. Incomplete port, no production release.'}
if args.kind=='package':result['limits']='Ordinary production compilation/packaging and exact staged-source recording only; no game, native data decoding, runtime optional loading, gameplay, networking or client-rendering proof.'
result['buildMode']=args.kind
for target in ['forge-1.20.1', 'neoforge-1.21.1', 'neoforge-26.1.2']:
    log = args.scratch / f'{args.prefix}-{target}.log'
    if not log.exists():
        continue
    raw = log.read_bytes()
    txt = raw.decode('utf-8', errors='replace')
    stage = args.scratch / 'builds' / target
    relevant = [*stage.glob('common/src/main/java/**/*.java'),
                *stage.glob(f'{target}/src/**/*.java'),
                *stage.glob(f'verification/{target}/**/*')]
    passed = re.findall(r'All (\d+) required tests passed', txt)
    errors = re.findall(r'^.*(?:error:|required tests failed|failed!|Caused by:).*$' , txt, re.MULTILINE)
    source_files = [*stage.glob('common/src/**/*'), *stage.glob(f'{target}/src/**/*'),
                    *stage.glob(f'verification/{target}/**/*')]
    source_files += [stage/target/p for p in ['build.gradle','settings.gradle','gradle.properties']]
    source_files += [stage/'tools/no-game-launch.gradle']
    source_files=sorted(set(p for p in source_files if p.is_file()))
    digests=hash_files(source_files)
    source_hashes={p.relative_to(stage).as_posix():digests[p] for p in source_files}
    hashes={p.relative_to(stage).as_posix():digests[p] for p in relevant if p in digests}
    manifest='\n'.join(k+':'+v for k,v in source_hashes.items())
    maintained_files=[*ROOT.glob('common/src/**/*'),*ROOT.glob(f'{target}/src/**/*')]
    maintained_files += [ROOT/target/p for p in ['build.gradle','settings.gradle','gradle.properties']]+[ROOT/'tools/no-game-launch.gradle']
    if not compile_only:maintained_files += [*ROOT.glob(f'verification/{target}/**/*')]
    maintained_files=sorted(set(p for p in maintained_files if p.is_file()))
    maintained_hashes={p.relative_to(ROOT).as_posix():v for p,v in hash_files(maintained_files).items()}
    mismatches=sorted(k for k in set(source_hashes)|set(maintained_hashes) if source_hashes.get(k)!=maintained_hashes.get(k))
    result['targets'][target] = {
        'buildSucceeded': 'BUILD SUCCESSFUL' in txt,
        'requiredTestsPassed': int(passed[-1]) if passed else None,
        'additionalBuiltinTest': 'minecraft:always_pass' if not compile_only and target.endswith('26.1.2') else None,
        'errors': errors[:8], 'logSha256': hashlib.sha256(raw).hexdigest(),
        'compiledJavaFiles' if compile_only else 'testedFiles': hashes, 'stagedSourceManifestSha256': hashlib.sha256(manifest.encode()).hexdigest(),
        'maintainedSourceDifferences':mismatches,
        'stagedSourceFileCount': sum(p.is_file() for p in source_files)}
    if args.require_maintained_source_match and mismatches:raise ValueError(f'{target} staged source differs: {mismatches}')
(ROOT / 'evidence' / args.receipt).write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
print(json.dumps({k:{s:v for s,v in val.items() if s not in ['testedFiles','compiledJavaFiles']} for k,val in result['targets'].items()}, indent=2))
