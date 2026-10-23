"""Static native-signature and production-archive inspection; executes no mod code."""
from pathlib import Path
import argparse,hashlib,json,os,re,struct,subprocess,tomllib,zipfile
from datetime import datetime,timezone

ROOT=Path(__file__).resolve().parents[1]

def digest(data):return hashlib.sha256(data).hexdigest()

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--scratch',type=Path,required=True)
    p.add_argument('--baseline',type=Path,required=True)
    p.add_argument('--hephaestus-tools',type=Path,required=True)
    p.add_argument('--javap',type=Path,required=True)
    args=p.parse_args()
    work=args.scratch.resolve(strict=True)
    if work.parent!=Path('H:/CodexScratch/terraria_modding/.test-runtime').resolve() or not work.name.startswith('metallurgy-hephaestus-traits-') or work.is_symlink():raise ValueError('Invalid owned scratch')
    build=json.loads((ROOT/'docs/hephaestus-traits-build.json').read_text())
    mod=ROOT/'artifacts'/build['artifacts'][0]['file']
    if digest(mod.read_bytes())!=build['artifacts'][0]['sha256']:raise ValueError('Production artifact changed')
    if digest(args.baseline.read_bytes())!=build['baseline']['sha256']:raise ValueError('Baseline changed')
    with zipfile.ZipFile(args.baseline) as z:before={n:z.read(n) for n in z.namelist() if not n.endswith('/')}
    with zipfile.ZipFile(mod) as z:
        names=z.namelist()
        if len(names)!=len(set(names)):raise ValueError('Duplicate archive entry')
        after={n:z.read(n) for n in names}
    classes=[n for n in after if n.endswith('.class')]
    if any(not n.startswith('it/hurts/metallurgy_reforged/') for n in classes):raise ValueError('Foreign dependency class bundled')
    if any(n.endswith('.jar') for n in names):raise ValueError('Embedded JAR dependency')
    if any(any(word in n.lower() for word in ['gametest','coreverification','test_hooks']) for n in classes):raise ValueError('Verification class in production')
    majors=sorted({struct.unpack('>H',after[n][6:8])[0] for n in classes})
    if majors!=[69]:raise ValueError('Wrong Java bytecode target')
    for row in build['compiledClasses']:
        if digest(after[row['path']])!=row['sha256']:raise ValueError('Compiled/shipped class mismatch')
    pngs=[n for n in before if n.endswith('.png')]
    recipes=[n for n in before if n.endswith('.json') and ('/recipe/' in n or '/recipes/' in n)]
    if any(before[n]!=after.get(n) for n in pngs+recipes):raise ValueError('Unexpected art or recipe change')
    for name in ['LICENSE','NOTICE.md']:
        if after[name]!=(ROOT/name).read_bytes():raise ValueError('Current licensing/credits missing')
    meta=tomllib.loads(after['META-INF/neoforge.mods.toml'].decode())
    config=json.loads(after['metallurgy.hephaestus.mixins.json'])
    if meta['mods'][0]['version']!='1.4.1-port.18':raise ValueError('Wrong package version')
    if 'metallurgy.hephaestus.mixins.json' not in [x['config'] for x in meta['mixins']]:raise ValueError('Optional mixin configuration not linked')
    for name in config['mixins']:
        path=config['package'].replace('.','/')+'/'+name+'.class'
        if b'Lorg/spongepowered/asm/mixin/Pseudo;' not in after[path]:raise ValueError('Optional mixin is not pseudo')
    for n,b in after.items():
        if n.endswith('.json'):json.loads(b.decode('utf-8-sig'))
    expected={
        'com.titammods.hephaestus_tools.tools.helper.ToolDurability':('hurt','(Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V'),
        'com.titammods.hephaestus_tools.table.ToolUpgrades':('unlockedSlots','(Lnet/minecraft/world/item/ItemStack;)I'),
        'com.titammods.hephaestus_tools.tools.helper.ToolTooltipBuilder':('partStats','(Lcom/titammods/hephaestus_tools/materials/MaterialId;I)Ljava/util/List;')}
    env=os.environ.copy();env.update(TEMP=str(work),TMP=str(work),JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir='+str(work))
    inspected=[]
    for target,(method,descriptor) in expected.items():
        raw=subprocess.check_output([str(args.javap),'-cp',str(args.hephaestus_tools),'-p','-s',target],env=env,text=True,encoding='utf-8')
        pattern=r'\b'+method+r'\([^\n]*\);\s+descriptor: '+re.escape(descriptor)
        if not re.search(pattern,raw):raise ValueError('Native mixin target signature changed: '+target)
        (work/(target.rsplit('.',1)[-1]+'.signatures.txt')).write_text(raw,encoding='utf-8')
        inspected.append(dict(target=target,method=method,descriptor=descriptor,disassemblySha256=digest(raw.encode())))
    source=ROOT/'artifacts'/build['artifacts'][1]['file']
    if digest(source.read_bytes())!=build['artifacts'][1]['sha256']:raise ValueError('Source package changed')
    record=dict(schema=1,recordedAt=datetime.now(timezone.utc).isoformat(),sourceCommit=build['sourceCommit'],
                artifacts=build['artifacts'],classCount=len(classes),classFileMajors=majors,compiledClassBytesMatch=True,
                unchangedPngCount=len(pngs),unchangedRecipeJsonCount=len(recipes),foreignImplementationClasses=0,
                optionalMixinTargetSignatures=inspected,pseudoMixins=3,currentLicenseAndNoticeMatch=True,
                testsRun=False,gameLaunched=False,installed=False,runtimeConfirmed=False,
                scope='Archive contents, JSON/TOML syntax and javap native descriptors only; no mixin transformation or gameplay execution')
    (ROOT/'docs/hephaestus-traits-inspection.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:record[k] for k in ['classCount','unchangedPngCount','unchangedRecipeJsonCount','pseudoMixins','currentLicenseAndNoticeMatch']}))

if __name__=='__main__':main()
