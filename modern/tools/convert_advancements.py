"""Port the pinned source advancement tree, keeping criteria and requirements.

This writes maintained datapack sources only. It does not load Minecraft.
"""
from pathlib import Path
import argparse,json,re,hashlib
ROOT=Path(__file__).resolve().parents[1]
def dump(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True,type=Path);args=parser.parse_args()
    source=args.source.resolve()
    if source.drive.lower()!='h:':raise ValueError('Original references must remain on H:')
    records=[]
    for target in ('forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2'):
        old=target.startswith('forge-');namespace='forge' if old else 'c'
        def ore_tag(value):
            if value=='cobblestone':return namespace+':'+('cobblestone' if old else 'cobblestones')
            for prefix,kind in [('ingot','ingots'),('dust','dusts'),('ore','ores'),('block','storage_blocks')]:
                if value.startswith(prefix):return namespace+':'+kind+'/'+re.sub(r'(?<!^)(?=[A-Z])','_',value[len(prefix):]).lower()
            raise ValueError('Unmapped source advancement ore key '+value)
        def predicate(value):
            value=dict(value)
            if value.get('type')=='forge:ore_dict':
                tag=ore_tag(value.pop('ore'));value.pop('type')
                value['tag' if old else 'items']=tag if old else '#'+tag
            if 'item' in value:value['items']=[value.pop('item')]
            if not old and 'nbt' in value:value.setdefault('predicates',{})['minecraft:custom_data']=value.pop('nbt')
            return value
        for path in sorted(source.rglob('*.json')):
            value=json.loads(path.read_text(encoding='utf-8'));relative=path.relative_to(source)
            display=value.get('display',{})
            if display.get('icon',{}).get('item')=='metallurgy:wiki':
                replacement={'main/root.json':'metallurgy:copper_ingot','challenges/root.json':'metallurgy:tartarite_ingot'}.get(relative.as_posix())
                if replacement is None:raise ValueError('Unmapped removed wiki icon in '+str(relative))
                display['icon']['item']=replacement
            if not old and 'icon' in display:
                icon=display['icon'];icon['id']=icon.pop('item')
            if 'icon' in display and display['icon'].pop('data',0)!=0:raise ValueError('Unmapped icon metadata in '+str(relative))
            for criterion in value['criteria'].values():
                conditions=criterion.get('conditions',{})
                if criterion['trigger']=='minecraft:inventory_changed':conditions['items']=[predicate(v) for v in conditions['items']]
                if criterion['trigger']=='minecraft:item_durability_changed':conditions['item']=predicate(conditions['item'])
            destination=ROOT/target/'src/main/resources/data/metallurgy'/('advancements' if old else 'advancement')/relative
            dump(destination,value)
            records.append({'source':relative.as_posix(),'sourceSha256':hashlib.sha256(path.read_bytes()).hexdigest(),'target':target,'output':destination.relative_to(ROOT).as_posix(),'state':'adapted; native resource loading unverified'})
    dump(ROOT/'docs/advancement-coverage.json',{'complete':False,'rows':records,'limits':'Source adaptation only; no game/resource loading or progression checks.'})
    print('Adapted',len(records)//3,'source advancement files per target')
if __name__=='__main__':main()
