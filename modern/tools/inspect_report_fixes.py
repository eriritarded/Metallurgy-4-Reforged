"""Inspect packaged atlas coverage and changed bytecode without loading a game."""
from datetime import datetime, timezone
from pathlib import Path
import argparse
import collections
import hashlib
import json
import os
import re
import struct
import subprocess
import zipfile
from build_report_fixes import ROOT, FIELD, OLD_NATIVE, PREFIX, owned_work, read, dump, sha

NATIVE26 = FIELD / 'neoforge-26.1.2/build/moddev/artifacts/minecraft-patched-26.1.2.122-merged.jar'

def access_flags(data):
    offset, count = 10, struct.unpack_from('>H', data, 8)[0]
    index = 1
    while index < count:
        tag = data[offset]
        offset += 1
        if tag == 1:
            length = struct.unpack_from('>H', data, offset)[0]
            offset += 2 + length
        else:
            offset += {3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}[tag]
            if tag in (5,6):
                index += 1
        index += 1
    return struct.unpack_from('>H', data, offset)[0]

def atlas_review(package):
    with zipfile.ZipFile(NATIVE26) as native, zipfile.ZipFile(package) as mod:
        texture_files = [n for n in mod.namelist() if n.startswith('assets/metallurgy/textures/') and n.endswith('.png')]
        models = {n.removeprefix('assets/metallurgy/models/').removesuffix('.json'):json.loads(mod.read(n))
                  for n in mod.namelist() if n.startswith('assets/metallurgy/models/') and n.endswith('.json')}
        def model(name):
            ns, path = name.split(':',1) if ':' in name else ('minecraft',name)
            if ns == 'metallurgy':
                return models.get(path,{})
            filename = 'assets/' + ns + '/models/' + path + '.json'
            return json.loads(native.read(filename)) if filename in native.namelist() else {}
        def slots(name, seen=None):
            seen = set() if seen is None else set(seen)
            if name in seen:
                raise RuntimeError('Model inheritance cycle: ' + name)
            seen.add(name)
            value = model(name)
            result = slots(value['parent'],seen) if 'parent' in value else {}
            result.update(value.get('textures',{}))
            return result
        rows = []
        for atlas, domain in [('blocks','block/'),('items','item/')]:
            path = 'assets/minecraft/atlases/' + atlas + '.json'
            vanilla = json.loads(native.read(path))
            additions = json.loads(mod.read(path))
            available = set()
            for source in vanilla['sources'] + additions['sources']:
                if source['type'] == 'minecraft:single':
                    available.add(source.get('sprite',source['resource']))
                elif source['type'] == 'minecraft:directory':
                    folder = 'assets/metallurgy/textures/' + source['source'].rstrip('/') + '/'
                    for n in texture_files:
                        if n.startswith(folder):
                            available.add('metallurgy:' + source['prefix'] + n[len(folder):-4])
            uncovered, missing_files = [], []
            usage = collections.Counter()
            count = 0
            for name in sorted(n for n in models if n.startswith(domain)):
                count += 1
                texture_slots = slots('metallurgy:' + name)
                for key, value in texture_slots.items():
                    visited = set()
                    while isinstance(value,str) and value.startswith('#'):
                        if value in visited:
                            raise RuntimeError('Texture-slot cycle')
                        visited.add(value)
                        value = texture_slots.get(value[1:])
                    if not isinstance(value,str) or not value.startswith('metallurgy:'):
                        continue
                    usage[value.split(':',1)[1].split('/',1)[0]] += 1
                    if value not in available:
                        uncovered.append(dict(model=name,slot=key,texture=value))
                    filename = 'assets/metallurgy/textures/' + value.split(':',1)[1] + '.png'
                    if filename not in texture_files:
                        missing_files.append(dict(model=name,texture=value))
            rows.append(dict(atlas=atlas, modelRoots=count, inheritedOwnTextureUses=dict(usage),
                             includedOwnSprites=len([n for n in available if n.startswith('metallurgy:')]),
                             uncoveredReferences=uncovered, missingTextureFiles=missing_files,
                             vanillaSources=vanilla['sources'], addedSources=additions['sources']))
        if any(r['uncoveredReferences'] or r['missingTextureFiles'] for r in rows):
            raise RuntimeError('Incomplete packaged atlas coverage: ' + json.dumps(rows))
        return dict(nativePath=str(NATIVE26),nativeSha256=sha(NATIVE26),
                    textureFolderCounts=dict(collections.Counter(n.split('/')[3] for n in texture_files)),
                    atlasResults=rows)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scratch',type=Path,required=True)
    work = owned_work(parser.parse_args().scratch)
    release = read(ROOT/'evidence/report-fixes-release.json')
    records = {r['target']:r for r in release['records'] if r['artifact']['kind']=='playable-mod'}
    for row in records.values():
        if sha(ROOT/'artifacts'/row['artifact']['file']) != row['artifact']['sha256']:
            raise RuntimeError('Packaged identity changed')
    atlas = atlas_review(ROOT/'artifacts'/records['neoforge-26.1.2']['artifact']['file'])
    platform21 = FIELD/'neoforge-1.21.1/build/moddev/artifacts/neoforge-21.1.262.jar'
    event_flags = {}
    with zipfile.ZipFile(platform21) as jar:
        for suffix in ['', '$MouseButton$Post', '$Key']:
            name = 'net/neoforged/neoforge/client/event/InputEvent' + suffix
            flags = access_flags(jar.read(name+'.class'))
            event_flags[name] = dict(accessFlags=flags,abstract=bool(flags & 0x400))
    if not event_flags['net/neoforged/neoforge/client/event/InputEvent']['abstract'] or any(v['abstract'] for k,v in event_flags.items() if '$' in k):
        raise RuntimeError('Unexpected native event hierarchy')
    env = os.environ.copy()
    env.update(TEMP=str(work/'process-temp'),TMP=str(work/'process-temp'),
               JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir='+str(work/'process-temp'))
    disassembly = {}
    for target in ['forge-1.20.1','neoforge-1.21.1']:
        java = '17' if target.startswith('forge') else '21'
        output = subprocess.check_output([f'C:/Program Files/Java/jdk-{java}/bin/javap.exe','-p','-v',str(work/target/'production.class')],
                                         text=True,encoding='utf-8',env=env)
        (work/target/'production-bytecode.txt').write_text(output,encoding='utf-8')
        disassembly[target] = output
    forge = disassembly['forge-1.20.1']
    if len(re.findall(r'invokestatic[^\n]+Block\.m_49874_:',forge)) != 2:
        raise RuntimeError('Forge getDrops calls were not correctly reobfuscated')
    if forge.count('org.spongepowered.asm.mixin.injection.Group(') != 2 or forge.count('name="metallurgy$emittedDrops"') != 2:
        raise RuntimeError('Missing grouped injectors')
    neo = disassembly['neoforge-1.21.1']
    if not all(s in neo for s in ['attackMouse(net.neoforged.neoforge.client.event.InputEvent$MouseButton$Post)',
                                  'attackKey(net.neoforged.neoforge.client.event.InputEvent$Key)']):
        raise RuntimeError('Concrete listeners missing')
    if re.search(r'attack\(net\.neoforged\.neoforge\.client\.event\.InputEvent\)',neo):
        raise RuntimeError('Abstract listener remains')
    layouts = []
    for version, native in [('47.4.10',OLD_NATIVE),('47.4.28',FIELD/'forge-1.20.1/build/moddev/artifacts/forge-1.20.1-47.4.28.jar')]:
        text = (work/('forge'+version+'-block.txt')).read_text(encoding='utf-8-sig')
        selected = []
        for section in re.split(r'\n(?=  (?:public|private|protected))',text):
            signature = section.split('\n')[0].strip()
            if 'static void dropResources(' in signature and 'net.minecraft.world.item.ItemStack' in signature:
                selected.append(dict(signature=signature,getDropsInvocations=section.count('Method getDrops:'),
                                     delegationInvocations=section.count('Method dropResources:')))
        if len(selected)!=2 or sum(s['getDropsInvocations'] for s in selected)!=1:
            raise RuntimeError('Unexpected Forge emission layout')
        layouts.append(dict(version=version,path=str(native),sha256=sha(native),methods=selected,
                            disassemblySha256=sha(work/('forge'+version+'-block.txt'))))
    result = dict(recordedAt=datetime.now(timezone.utc).isoformat(),sourceCommit=release['sourceCommit'],
                  userReportedMissingWarningCount=1373,userReportReproduced=False,atlasCoverage=atlas,
                  eventPlatform=dict(path=str(platform21),sha256=sha(platform21),types=event_flags),
                  concreteListeners=['InputEvent.MouseButton.Post','InputEvent.Key'],
                  forgeLayouts=layouts,groupedRedirectCount=2,requiredSuccessfulGroupInjections=1,
                  forgeProductionCallsReobfuscated=True,gameLaunched=False,testsRun=False,
                  clientLogMissingTextureCount=None,renderingObserved=False,startupConfirmed=False,
                  scope='Packaged own texture-file/atlas coverage, exact native class flags and changed bytecode; not native resource loading or runtime.')
    dump(ROOT/'evidence/report-fixes-inspection.json',result)
    print(json.dumps(dict(atlases=[dict(atlas=r['atlas'],modelRoots=r['modelRoots'],uncovered=len(r['uncoveredReferences'])) for r in atlas['atlasResults']],
                          concreteListeners=result['concreteListeners'],forgeOverloadsReviewed=[r['version'] for r in layouts])))

if __name__=='__main__':
    main()
