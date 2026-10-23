"""Recover original IF laser/Ender IO XML data as maintained neutral catalog."""
from pathlib import Path
import argparse,hashlib,json,re,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
def snake(value):return re.sub(r'(?<!^)(?=[A-Z])','_',value).lower()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True,type=Path);args=parser.parse_args();source=args.source.resolve()
    if source.drive.lower()!='h:':raise ValueError('Original reference must stay on H:')
    laser=source/'industrial_foregoing_laser_stats.json';xml=source/'recipes/metallurgy_enderio_alloys.xml'
    alloys=[]
    for recipe in ET.parse(xml).getroot().findall('recipe'):
        alloy=recipe.find('alloying')
        if alloy is None:continue
        inputs=[]
        for entry in alloy.findall('input'):
            name=entry.get('name');prefix=next((p for p in ['ingot','dust'] if name.startswith(p)),None)
            if prefix is None:raise ValueError(name)
            inputs.append({'form':prefix,'metal':snake(name[len(prefix):]),'count':int(entry.get('amount','1'))})
        output=alloy.find('output');name=output.get('name')
        if not name.startswith('ingot'):raise ValueError(name)
        alloys.append({'name':recipe.get('name'),'energy':int(alloy.get('energy')),'experience':float(alloy.get('exp','0')),'inputs':inputs,'result':'metallurgy:'+snake(name[5:])+'_ingot','count':int(output.get('amount','1'))})
    catalog={'sourceRevision':'ef94cb94cfc08ccebe8a0df307ce6b390dcd4704','files':{p.relative_to(source).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in [laser,xml]},'laser':json.loads(laser.read_text(encoding='utf-8')),'enderio':alloys}
    (ROOT/'content/optional-legacy.json').write_text(json.dumps(catalog,indent=2)+'\n',encoding='utf-8')
    print(f'Imported {len(catalog["laser"])} original laser entries and {len(alloys)} Ender IO alloys.')
if __name__=='__main__':main()
