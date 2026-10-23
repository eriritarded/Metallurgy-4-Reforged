"""Package compiled gauntlet mining adapters and native item tags."""
from pathlib import Path
from datetime import datetime,timezone
import argparse
import hashlib
import json
import shutil
import zipfile
from build_gauntlet_tools import ROOT,CLASS,owned_work,sha,read,dump
from package_release import inspect

TARGETS={'forge-1.20.1':('1.4.1-port.9',61),'neoforge-1.21.1':('1.4.1-port.10',65),'neoforge-26.1.2':('1.4.1-port.17',69)}

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scratch',type=Path,required=True)
    work=owned_work(parser.parse_args().scratch)
    receipt=ROOT/'evidence/gauntlet-tools-release.json'
    if receipt.exists():raise RuntimeError('Preserve existing release receipt')
    compiled=read(ROOT/'evidence/gauntlet-tools-compile.json')
    manifest=read(ROOT/'artifacts/manifest.json')
    stage=work/'packages';stage.mkdir()
    artifacts=[];records=[]
    for old in manifest['artifacts']:
        target=old['target'];version,major=TARGETS[target]
        compiler=next(r for r in compiled['records'] if r['target']==target)
        source=ROOT/compiler['source']
        if sha(source)!=compiler['sourceSha256']:raise RuntimeError('Changed source since compilation')
        baseline=ROOT/'artifacts'/old['file']
        if sha(baseline)!=old['sha256']:raise RuntimeError('Baseline archive identity mismatch')
        metadata='META-INF/'+('mods.toml' if major==61 else 'neoforge.mods.toml')
        tag_folder='items' if major==61 else 'item'
        resource_names=[metadata]+['data/minecraft/tags/'+tag_folder+'/'+t+'.json' for t in ['pickaxes','shovels']]
        resources={n:(ROOT/target/'src/main/resources'/n).read_bytes() for n in resource_names}
        classes={r['path']:(work/target/'production'/r['path']).read_bytes() for r in compiler['classes']}
        if any(hashlib.sha256(classes[r['path']]).hexdigest()!=r['sha256'] for r in compiler['classes']):raise RuntimeError('Compiled output identity changed')
        playable=old['kind']=='playable-mod'
        java_path=CLASS.replace('.class','.java')
        replacements={**resources,**(classes if playable else {java_path:source.read_bytes()})}
        provenance=dict(schema=1,modId='metallurgy',target=target,modVersion=version,sourceCommit=compiled['sourceCommit'],
                        buildMode='incremental: native GadgetItem adapter and item tags',baselineArtifact=old['file'],baselineSha256=old['sha256'],
                        baselineSourceCommit=old.get('sourceCommit'),source=compiler['source'],sourceSha256=compiler['sourceSha256'],
                        changedEntries={n:hashlib.sha256(b).hexdigest() for n,b in replacements.items()})
        replacements['META-INF/metallurgy-build.json']=(json.dumps(provenance,indent=2)+'\n').encode('utf-8')
        name=old['file'].replace(old['version'],version);package=stage/name
        with zipfile.ZipFile(baseline) as src,zipfile.ZipFile(package,'w') as dst:
            names=src.namelist()
            if any(n.startswith('META-INF/') and n.upper().endswith(('.SF','.RSA','.DSA')) for n in names):raise RuntimeError('Signed baseline')
            for entry in src.infolist():dst.writestr(entry,replacements.get(entry.filename,src.read(entry.filename)))
            for n,b in replacements.items():
                if n not in names:dst.writestr(n,b)
        with zipfile.ZipFile(baseline) as src,zipfile.ZipFile(package) as dst:
            old_names,new_names=set(src.namelist()),set(dst.namelist())
            changed=sorted(n for n in old_names & new_names if src.read(n)!=dst.read(n));added=sorted(new_names-old_names)
            if old_names-new_names or set(changed+added)!=set(replacements):raise RuntimeError('Unexpected archive delta')
            preserved=[n for n in old_names if n.endswith('.class') and n not in classes]
            pngs=[n for n in old_names if n.endswith('.png')]
            recipes=[n for n in old_names if '/recipe' in n and n.endswith('.json')]
            if any(src.read(n)!=dst.read(n) for n in preserved+pngs+recipes):raise RuntimeError('Unrelated class/art/recipe change')
            for tag in ['pickaxes','shovels']:
                value=json.loads(dst.read('data/minecraft/tags/'+tag_folder+'/'+tag+'.json'))
                if value.get('replace') or 'metallurgy:rubracium_gauntlet' not in value['values']:raise RuntimeError('Bad gauntlet item tag')
            for n in dst.namelist():
                if n.endswith('.json'):json.loads(dst.read(n))
        inspection=inspect(package,major) if playable else {}
        artifact={**old,**inspection,'file':name,'version':version,'sha256':sha(package),'size':package.stat().st_size,
                  'sourceCommit':compiled['sourceCommit'],'buildMode':provenance['buildMode']}
        destination=ROOT/'artifacts'/name
        if destination.exists():raise RuntimeError('Preserve existing deliverable')
        shutil.copy2(package,destination)
        if sha(destination)!=artifact['sha256']:raise RuntimeError('Deliverable copy mismatch')
        artifacts.append(artifact)
        records.append(dict(target=target,artifact=artifact,baseline=old,changedEntries=changed,addedEntries=added,
                            unchangedClassCount=len(preserved),unchangedPngCount=len(pngs),unchangedRecipeJsonCount=len(recipes)))
    result=dict(recordedAt=datetime.now(timezone.utc).isoformat(),sourceCommit=compiled['sourceCommit'],packaged=True,compiled=True,
                records=records,installed=False,gameLaunched=False,testsRun=False,runtimeConfirmed=False,scratchCleanupComplete=False)
    dump(receipt,result)
    known={a['file'] for a in manifest.setdefault('supersededArtifacts',[])}
    manifest['supersededArtifacts'] += [a for a in manifest['artifacts'] if a['file'] not in known]
    manifest.update(artifacts=artifacts,targetVersions={t:v[0] for t,v in TARGETS.items()},state='Gauntlet iron pickaxe/shovel adapters compiled and packaged; uninstalled and runtime unconfirmed.',
                    updatedAt=result['recordedAt'],currentSourceCommit=compiled['sourceCommit'],latestPackagingReceipt='evidence/gauntlet-tools-release.json',
                    currentReleaseInstalled=False,currentReleaseInstalledTargets=[],currentReleaseGameLaunched=False,currentReleaseTestsRun=False,
                    currentReleaseRuntimeConfirmed=False,currentReleaseScratchRemoved=False)
    dump(ROOT/'artifacts/manifest.json',manifest)
    (ROOT/'artifacts/SHA256SUMS.txt').write_text(''.join(a['sha256']+'  '+a['file']+'\n' for a in artifacts),encoding='utf-8')
    print(json.dumps([dict(target=r['target'],file=r['artifact']['file'],sha256=r['artifact']['sha256']) for r in records if r['artifact']['kind']=='playable-mod']))

if __name__=='__main__':main()
