"""Record the port.4 production build by reading files/bytecode only; never launch game code."""
from pathlib import Path
from datetime import datetime, timezone
from collections import Counter
import gzip, hashlib, json, os, re, subprocess, zipfile
from package_release import inspect

ROOT = Path(__file__).resolve().parents[1]
SCRATCH = Path('H:/CodexScratch/terraria_modding/.test-runtime/metallurgy-recipe-fix-131bf881d3d54138950cc43c51ca95de')
VERSION = '1.4.1-port.4'
NAME = 'Metallurgy-Reforged-MC-26.1.2-neoforge-'
INSTANCE = Path('C:/Users/USER/AppData/Roaming/PrismLauncher/instances/Simply NeoForged')

def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def disassemble(artifact, name):
    env = dict(os.environ)
    temporary = str(SCRATCH / 'process-temp')
    env.update(TEMP=temporary, TMP=temporary, TMPDIR=temporary,
               JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir=' + temporary)
    return subprocess.run(['C:/Program Files/Java/jdk-25/bin/javap.exe', '-classpath', str(artifact), '-p', '-c', name],
                          env=env, capture_output=True, text=True, encoding='utf-8', check=True).stdout

def main():
    receipt_path = ROOT / 'evidence/recipe-load-fix-26.1.2.json'
    if receipt_path.exists() and json.loads(receipt_path.read_text(encoding='utf-8-sig')).get('installed'):
        raise RuntimeError('This historical build receipt already records installation; preserve it.')
    log = SCRATCH / 'recipe-fix-neoforge-26.1.2.log'
    text = log.read_text(encoding='utf-8', errors='replace')
    assert 'BUILD SUCCESSFUL' in text and not re.search(r'(?m)^.*error:', text)
    source = json.loads((ROOT / 'evidence/recipe-load-fix-source-26.1.2.json').read_text(encoding='utf-8'))
    assert not source['mismatches']
    artifacts = []
    binary = ROOT / 'artifacts' / (NAME + VERSION + '.jar')
    for suffix, kind in [('.jar', 'playable-mod'), ('-sources.jar', 'sources')]:
        file = ROOT / 'artifacts' / (NAME + VERSION + suffix)
        staged = SCRATCH / 'builds/neoforge-26.1.2/neoforge-26.1.2/build/libs' / file.name
        assert sha(file) == sha(staged)
        artifacts.append(dict(file=file.name, target='neoforge-26.1.2', version=VERSION, kind=kind,
                              minecraft='26.1.2', loader='NeoForge26.1.2.122', java='25', size=file.stat().st_size,
                              sha256=sha(file), **(inspect(file, 69) if kind == 'playable-mod' else {})))
    with zipfile.ZipFile(ROOT / 'artifacts' / (NAME + VERSION + '-sources.jar')) as sources:
        names = sources.namelist()
        assert len(names) == len(set(names))
        definition = 'it/hurts/metallurgy_reforged/machine/ProcessingDefinition.java'
        assert names.count(definition) == 1
        assert sources.read(definition) == (ROOT / 'neoforge-26.1.2/src/main/java' / definition).read_bytes()
    previous = ROOT / 'artifacts' / (NAME + '1.4.1-port.3.jar')
    with zipfile.ZipFile(binary) as new, zipfile.ZipFile(previous) as old:
        new_names, old_names = set(new.namelist()), set(old.namelist())
        changed = sorted(n for n in new_names & old_names if new.read(n) != old.read(n))
        added, removed = sorted(new_names - old_names), sorted(old_names - new_names)
        recipes = {n:json.loads(new.read(n)) for n in new_names if n.endswith('.json') and n.startswith('data/metallurgy/recipe/processing/')}
        assert len(recipes) == 174
        assert all(new.read(n) == old.read(n) for n in recipes)
        for value in recipes.values():
            if value['kind'] in ('crusher', 'alloyer'):
                assert isinstance(value['result']['id'], str) and 1 <= value['result'].get('count', 1) <= 64
        kinds = dict(Counter(v['kind'] for v in recipes.values()))
    serializer = disassemble(binary, 'it.hurts.metallurgy_reforged.recipe.ProcessingRecipe$Serializer')
    stream = disassemble(binary, 'it.hurts.metallurgy_reforged.recipe.ProcessingRecipe$Serializer$1')
    definition = disassemble(binary, 'it.hurts.metallurgy_reforged.machine.ProcessingDefinition')
    assert 'ItemStackTemplate.CODEC' in serializer and 'ItemStack.CODEC' not in serializer
    assert 'ItemStackTemplate.STREAM_CODEC' in stream and 'ItemStack.OPTIONAL_STREAM_CODEC' not in stream
    assert 'ItemStack.getMaxStackSize' not in definition and 'Item.components' not in definition
    audit = json.loads((ROOT / 'evidence/recipe-subscriber-after-26.1.2.json').read_text(encoding='utf-8'))
    assert audit['sha256'] == sha(binary) and not audit['invalidOrUnresolved']
    latest = INSTANCE / 'minecraft/logs/latest.log'
    rotated = INSTANCE / 'minecraft/logs/2026-10-16-1.log.gz'
    with gzip.open(rotated, 'rt', encoding='utf-8', errors='replace') as logfile:
        sound_warnings = [line.strip() for line in logfile if 'Missing sound for event: quiverfall:' in line]
    receipt = dict(recordedAt=datetime.now(timezone.utc).isoformat(), version=VERSION, replacementVersion=VERSION,
        minecraft='26.1.2', neoforge='26.1.2.122', scratch=str(SCRATCH), buildSucceeded=True, installed=False,
        buildTasks=['jar','sourcesJar'], buildLogSha256=sha(log), compilerErrors=0,
        compilerWarnings=len(re.findall(r'(?m)^.*warning:', text)), artifacts=artifacts,
        userObservedFailure=dict(version='1.4.1-port.3', phase='World-creation recipe preparation',
            error='IllegalArgumentException: Invalid processing output', log=str(latest), logSha256=sha(latest)),
        changedJarEntries=changed, addedJarEntries=added, removedJarEntries=removed,
        bundledProcessingRecipes=len(recipes), recipeKinds=kinds, processingJsonBytesUnchanged=True,
        nativeTemplateJsonAndStreamCodecs=True, earlyStackCapacityRead=False, sourceArchiveNativeDefinitionOnly=True,
        sourceEvidence='evidence/recipe-load-fix-source-26.1.2.json', listenerAudit='evidence/recipe-subscriber-after-26.1.2.json',
        listenerCount=audit['listenerCount'], abstractOrUnresolvedListeners=0, processingProtocol=4,
        quiverfallWarningLog=dict(path=str(rotated), sha256=sha(rotated), warnings=sound_warnings),
        gameLaunched=False, testsRun=False, runtimeConfirmed=False, scratchCleanupComplete=False,
        maximumMinecraftGameWindows=1, backgroundCompilersAllowed=True)
    (ROOT / 'evidence/recipe-load-fix-26.1.2.json').write_text(json.dumps(receipt, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k:receipt[k] for k in ['buildSucceeded','artifacts','changedJarEntries','addedJarEntries','recipeKinds']}, indent=2))

if __name__ == '__main__':
    main()
