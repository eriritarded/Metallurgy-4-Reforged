"""Export committed Metallurgy source to its reviewed upstream fork, without publishing.

No build, game, test or network mutation. Historical evidence remains local;
personal home paths are anonymized in documentation and workstation helpers.
Runtime sources, resources and Gradle inputs must remain byte-identical.
"""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import io
import json
import re
import subprocess
import tarfile

ROOT = Path(__file__).resolve().parents[1]
WORKSPACE = ROOT.parent
FORK = 'https://github.com/eriritarded/Metallurgy-4-Reforged.git'
TARGETS = ('forge-1.20.1', 'neoforge-1.21.1', 'neoforge-26.1.2')
SECRET = re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{25,}|github_pat_[A-Za-z0-9_]{30,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|Bearer [A-Za-z0-9._-]{20,})')
HOME = re.compile(r'''(?i)[A-Z]:[/\\]+Users[/\\]+[^/\\\s"']+''')
EVIDENCE_LINK = re.compile(r'\]\((?:\.\./)?evidence/[^)]+\)')

def git(*args):
    return subprocess.check_output(['git', '-C', str(WORKSPACE), *args])

def digest(data):
    return hashlib.sha256(data).hexdigest()

def dump(path, value):
    path.write_text(json.dumps(value, indent=2)+'\n', encoding='utf-8')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--clone', required=True, type=Path)
    args = parser.parse_args()
    clone = args.clone.resolve(strict=True)
    if not (clone/'.git').is_dir():
        raise ValueError('Use the standalone maintained fork clone')
    remote = subprocess.check_output(['git','-C',str(clone),'remote','get-url','origin']).decode().strip()
    if remote != FORK:
        raise ValueError('Unexpected destination origin')
    dest = clone/'modern'
    if dest.exists():
        raise ValueError('Initial export only: inspect an existing modern source tree before updating')
    if git('status','--porcelain','--',ROOT.name).strip():
        raise ValueError('Commit the selected source before export')
    commit = git('rev-parse','HEAD').decode().strip()
    subtree = git('rev-parse',f'{commit}:{ROOT.name}').decode().strip()
    archive = git('archive','--format=tar',commit,'--',ROOT.name)
    included, omitted, anonymized, runtime = [], [], [], []
    dest.mkdir()
    with tarfile.open(fileobj=io.BytesIO(archive), mode='r:') as tar:
        for entry in tar:
            if not entry.isfile():
                continue
            relative = PurePosixPath(entry.name).relative_to(ROOT.name)
            if '..' in relative.parts or relative.is_absolute():
                raise ValueError('Unsafe archive path')
            name = relative.as_posix()
            data = tar.extractfile(entry).read()
            if SECRET.search(data):
                raise ValueError('Possible credential in '+name)
            if relative.parts[0] == 'evidence' or name == 'AGENTS.md':
                omitted.append(name)
                continue
            original = data
            is_runtime = relative.parts[0] in ('common','content',*TARGETS,'verification')
            if data and b'\0' not in data:
                try:
                    text = data.decode('utf-8-sig')
                except UnicodeDecodeError:
                    text = None
                if text is not None:
                    scrubbed = HOME.sub('C:/Users/USER',text)
                    if scrubbed != text:
                        if is_runtime:
                            raise ValueError('Runtime source needs separate privacy review: '+name)
                        data = scrubbed.encode('utf-8')
                        anonymized.append(name)
                    if relative.suffix == '.md':
                        text = data.decode('utf-8-sig')
                        link = '../SOURCE-PROVENANCE.md#historical-evidence' if relative.parts[0]=='docs' else 'SOURCE-PROVENANCE.md#historical-evidence'
                        text = EVIDENCE_LINK.sub(']('+link+')', text)
                        if '](../artifacts/manifest.json)' in text:
                            text = text.replace('](../artifacts/manifest.json)','](../SOURCE-PROVENANCE.json)')
                        data = text.encode('utf-8')
            if is_runtime:
                if data != original:
                    raise ValueError('Runtime source changed during export: '+name)
                runtime.append((name,digest(data)))
            out = dest/relative
            out.parent.mkdir(parents=True,exist_ok=True)
            out.write_bytes(data)
            if out.suffix == '.json':
                json.loads(data.decode('utf-8-sig'))
            included.append((name,digest(data)))
    def aggregate(rows):
        return digest(''.join(name+'\0'+sha+'\n' for name,sha in sorted(rows)).encode())
    record = dict(schema=1,sourceCommit=commit,sourceProject='MetallurgyModern',sourceTree=subtree,
                  upstreamRepository='Davoleo/Metallurgy-4-Reforged',upstreamRevision='ef94cb94cfc08ccebe8a0df307ce6b390dcd4704',
                  forkRepository='eriritarded/Metallurgy-4-Reforged',branch='codex/modern-ports',
                  exportedFiles=len(included),exportedFilesSha256=aggregate(included),
                  unchangedRuntimeFiles=len(runtime),unchangedRuntimeFilesSha256=aggregate(runtime),
                  anonymizedHomePathFiles=anonymized,omittedLocalFiles=omitted,
                  historicalEvidenceOmitted=True,credentialPatternMatches=0,
                  built=False,testsRun=False,gameLaunched=False,installed=False,
                  scope='Committed source export; unchanged runtime sources/resources/build inputs. No fresh compile or runtime claim.')
    dump(dest/'SOURCE-PROVENANCE.json',record)
    (dest/'SOURCE-PROVENANCE.md').write_text(f'''# Source provenance

This source snapshot comes from the maintained project at commit `{commit}`,
subtree `{subtree}`. The fork retains the original upstream history and adds the
modern port under `modern/`. Original GPL and third-party notices are retained.

All {len(runtime)} gameplay/source/resource/build-input files are byte-identical
to that committed project, including the gauntlet tools and Claude's Hephaestus
integration. `SOURCE-PROVENANCE.json` records aggregate hashes and export edits.
This is a source publication, not a new binary release, build or gameplay check.

## Historical evidence

Workstation build/install receipts remain in the local source history. They
contain local profile paths, unrelated pack inventories and compiler cache paths.
References to them in imported documents point here. Documentation and historical
workstation helper home paths are anonymized. Those helpers describe historical
operations and must not be used as a portable install/release workflow.

Current source builds use the target's checked-in Gradle wrapper and pinned
dependencies. See the repository root README. Imported status documents retain
their original dates; the 2026-10-23 fork record takes precedence for source setup.
Historical hashes are retained as historical identities, not fresh evidence.
''',encoding='utf-8')
    # Fork-specific generated/runtime exclusions, while preserving required wrappers.
    with (dest/'.gitignore').open('a',encoding='utf-8') as stream:
        stream.write('\n# Local receipts, releases and launcher state stay outside source publication.\n/artifacts/\n/evidence/\n/.test-runtime/\n*.class\n*.jar\n!**/gradle/wrapper/gradle-wrapper.jar\n')
    print(json.dumps({k:record[k] for k in ['sourceCommit','sourceTree','exportedFiles','unchangedRuntimeFiles','credentialPatternMatches']}))

if __name__ == '__main__':
    main()
