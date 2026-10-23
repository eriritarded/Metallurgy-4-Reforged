"""Record which recipes and tool materials Hephaestus and Hephaestus Tools already ship.

Hephaestus (modid hephaestus, com.titammods) is a separate NeoForge26.1.2 smeltery mod,
not a Tinkers fork: its recipe types, fluids, casts and tool-material files have their own
ids. generate_hephaestus_resources.py reads the snapshot written here so that Metallurgy
never duplicates a native recipe or material, and only fills gaps a native file leaves
behind its own mod_loaded conditions. This reads jar data only; it never executes a game.
"""
from pathlib import Path
import argparse,hashlib,json,re,zipfile
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'content/hephaestus-native.json'
# HephaestusFluids$Material entries without a required-mod argument (checked against the
# decompiled 1.0.17 enum). Only these molten fluids exist in every Hephaestus install.
UNCONDITIONAL_FLUIDS=['aluminum','brass','bronze','chromium','constantan','copper','electrum','enderium','gold','invar',
    'iridium','iron','lead','lumium','moonstone','netherite','nickel','osmium','peridot','platinum','prismarine','ruby',
    'sapphire','signalum','silver','steel','tin','titanium','topaz','uranium','wrought_iron','zinc','onyx','garnet']

def key(value):return value if isinstance(value,str) else json.dumps(value,sort_keys=True)

def read(jar):
    with zipfile.ZipFile(jar) as archive:
        for name in sorted(archive.namelist()):
            if name.startswith('data/') and name.endswith('.json'):
                try:yield name,json.loads(archive.read(name).decode('utf-8'))
                except ValueError:continue
            elif name=='META-INF/neoforge.mods.toml':
                yield name,archive.read(name).decode('utf-8')

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--hephaestus',type=Path,required=True);parser.add_argument('--tools',type=Path,required=True)
    args=parser.parse_args()
    data={'jars':{},'fluids':[],'unconditionalFluids':UNCONDITIONAL_FLUIDS,'melting':{},'casting':{},'alloys':{},'materials':{}}
    def add(table,name,conditions):table.setdefault(name,[]).append(conditions or [])
    for jar in [args.hephaestus,args.tools]:
        for name,value in read(jar):
            if name.endswith('.toml'):
                mod=re.search(r'modId="(\w+)"',value)[1];version=re.search(r'version="([^"]+)"',value)[1]
                data['jars'][mod]={'file':jar.name,'version':version,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest()}
                continue
            if not isinstance(value,dict):continue
            conditions=value.get('neoforge:conditions');kind=value.get('type')
            if '/hephaestus_tools/materials/' in name:add(data['materials'],value['id'],conditions)
            elif kind=='hephaestus:melting':add(data['melting'],key(value['ingredient']),conditions)
            elif kind in ['hephaestus:casting_table','hephaestus:casting_basin']:
                add(data['casting'],'|'.join([kind,value['fluid']['id'],key(value.get('cast',''))]),conditions)
            elif kind=='hephaestus:alloy':add(data['alloys'],value['output']['id'],conditions)
    with zipfile.ZipFile(args.hephaestus) as archive:
        lang=json.loads(archive.read('assets/hephaestus/lang/en_us.json').decode('utf-8'))
    data['fluids']=sorted(k.split('.molten_',1)[1] for k in lang if k.startswith('fluid_type.hephaestus.molten_'))
    missing=[f for f in UNCONDITIONAL_FLUIDS if f not in data['fluids']]
    if missing:raise SystemExit('Hephaestus no longer lists fluids '+', '.join(missing))
    OUT.write_text(json.dumps(data,indent=1,sort_keys=True)+'\n',encoding='utf-8')
    print(f"Recorded {len(data['melting'])} melting, {len(data['casting'])} casting, {len(data['alloys'])} alloy and {len(data['materials'])} material keys from "+', '.join(f"{k} {v['version']}" for k,v in data['jars'].items()))

if __name__=='__main__':main()
