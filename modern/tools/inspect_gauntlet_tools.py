"""Inspect shipped mining adapters and preserved combat methods; no game loading."""
from datetime import datetime,timezone
from pathlib import Path
import argparse
import hashlib
import os
import re
import subprocess
import zipfile
from build_gauntlet_tools import ROOT,CLASS,owned_work,read,dump,sha

def method(text,name):
    match=re.search(r'    @Override public [^\n]+\b'+re.escape(name)+r'\(',text)
    if not match:return None
    start=text.index('{',match.start());depth=1;end=start+1
    while depth:
        if text[end]=='{':depth+=1
        elif text[end]=='}':depth-=1
        end+=1
    return text[match.start():end].replace('\r\n','\n')

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scratch',type=Path,required=True)
    work=owned_work(parser.parse_args().scratch)
    release=read(ROOT/'evidence/gauntlet-tools-release.json')
    compiler=read(ROOT/'evidence/gauntlet-tools-compile.json')
    env=os.environ.copy();env.update(TEMP=str(work/'process-temp'),TMP=str(work/'process-temp'),
                                  JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir='+str(work/'process-temp'))
    rows=[]
    for compiled in compiler['records']:
        target=compiled['target'];java=compiled['java']
        current=ROOT/compiled['source']
        old_source_row=next(r for r in release['records'] if r['target']==target and r['artifact']['kind']=='sources')
        with zipfile.ZipFile(ROOT/'artifacts'/old_source_row['baseline']['file']) as z:
            before=z.read(CLASS.replace('.class','.java')).decode('utf-8-sig')
        after=current.read_text(encoding='utf-8-sig')
        preserved=[]
        for name in ['initializeClient','use','hurtEnemy','onEntitySwing','getEnchantmentValue','isFoil','isBarVisible','getBarWidth','getBarColor']:
            old,new=method(before,name),method(after,name)
            if old!=new:raise RuntimeError('Unintended method change: '+target+'/'+name)
            if old is not None:preserved.append(name)
        bytecode=subprocess.check_output([f'C:/Program Files/Java/jdk-{java}/bin/javap.exe','-p','-c',str(work/target/'classes'/CLASS)],
                                        text=True,encoding='utf-8',env=env)
        raw=work/target/'tool-bytecode.txt';raw.write_text(bytecode,encoding='utf-8')
        if 'Item.useOn:' not in bytecode or 'IRON_SHOVEL' not in bytecode:raise RuntimeError('Native shovel delegation missing')
        if java==17:
            if not all(token in bytecode for token in ['Tiers.IRON','TierSortingRegistry.isCorrectTierForDrops','DEFAULT_PICKAXE_ACTIONS','DEFAULT_SHOVEL_ACTIONS','GadgetBridge.damage','IRON_PICKAXE','Enchantment.canApplyAtEnchantingTable']):raise RuntimeError('Missing legacy native tool behavior')
        else:
            if bytecode.count('Tool$Rule.minesAndDrops:')!=2 or 'Tool$Rule.deniesDrops:' not in bytecode or 'DataComponents.TOOL:' not in bytecode:raise RuntimeError('Incomplete native tool rules')
            if 'Enchantment.isSupportedItem:' not in bytecode:raise RuntimeError('Native data-defined enchanting support missing')
            if java==21 and 'Tiers.IRON:' not in bytecode:raise RuntimeError('Wrong native iron tier')
            if java==25 and 'ToolMaterial.IRON:' not in bytecode:raise RuntimeError('Wrong native iron material')
        record=next(r for r in release['records'] if r['target']==target and r['artifact']['kind']=='playable-mod')
        package=ROOT/'artifacts'/record['artifact']['file']
        if sha(package)!=record['artifact']['sha256']:raise RuntimeError('Production archive identity changed')
        with zipfile.ZipFile(package) as z:
            for cls in compiled['classes']:
                if hashlib.sha256(z.read(cls['path'])).hexdigest()!=cls['sha256']:raise RuntimeError('Shipped class differs from compilation')
        rows.append(dict(target=target,artifact=record['artifact']['file'],sha256=record['artifact']['sha256'],
                         preservedMethods=preserved,compiledClassBytesMatchPackage=True,nativeShovelUseDelegated=True,
                         namedBytecodeSha256=sha(raw),ironMiningRulesPresent=True,toolEnchantingPathPresent=True))
    dump(ROOT/'evidence/gauntlet-tools-inspection.json',dict(recordedAt=datetime.now(timezone.utc).isoformat(),sourceCommit=release['sourceCommit'],
         records=rows,nativeSourceReview='evidence/gauntlet-tools-source-review.json',gameLaunched=False,testsRun=False,runtimeConfirmed=False,
         scope='Source preservation, native API/data and compiled/shipped archive inspection; not a gameplay or enchantment outcome test'))
    print([dict(target=r['target'],preservedMethods=len(r['preservedMethods'])) for r in rows])

if __name__=='__main__':main()
