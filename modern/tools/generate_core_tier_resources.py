"""Metadata for the maintained core mining-tag pack. Native settings adapt in memory."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
def main():
    materials=json.loads((ROOT/'common/src/main/resources/assets/metallurgy/materials.json').read_text(encoding='utf-8'))
    for target,format in [('forge-1.20.1',15),('neoforge-1.21.1',48),('neoforge-26.1.2',None)]:
        path=ROOT/target/'src/main/resources/resourcepacks/core_tiers/pack.mcmeta';path.parent.mkdir(parents=True,exist_ok=True)
        value={'description':'Metallurgy configured mining tiers'}
        value.update({'pack_format':format} if format else {'min_format':[101,1],'max_format':[101,1]})
        path.write_text(json.dumps({'pack':value},indent=2)+'\n',encoding='utf-8')
        base=path.parent;kind='blocks' if target.startswith('forge') else 'block'
        beacon=base/'data/minecraft/tags'/kind/'beacon_base_blocks.json';beacon.parent.mkdir(parents=True,exist_ok=True)
        beacon.write_text(json.dumps({'replace':False,'values':['metallurgy:'+m['name']+'_block' for m in materials if (m['ore_harvest_level'] if m['ore_harvest_level']>=0 else m.get('tool_stats',{}).get('harvest_level',0))>1]},indent=2)+'\n',encoding='utf-8')
        resource=ROOT/target/'src/main/resources';item_kind='items' if target.startswith('forge') else 'item';namespace='forge' if target.startswith('forge') else 'c'
        families={'slimeballs' if target.startswith('forge') else 'slime_balls':['metallurgy:tar'],'dusts/gold':['metallurgy:gold_dust'],'dusts/iron':['metallurgy:iron_dust'],'dusts/bitumen':['metallurgy:bitumen'],'dusts/potash':['metallurgy:potash'],'dusts/sulfur':['metallurgy:sulfur_dust'],'dusts/thermite':['metallurgy:thermite_dust'],'dusts/phosphorus':['metallurgy:phosphorus'],'gobs/tar':['metallurgy:tar']}
        for name,values in families.items():
            file=resource/'data'/namespace/'tags'/item_kind/(name+'.json');file.parent.mkdir(parents=True,exist_ok=True)
            file.write_text(json.dumps({'replace':False,'values':values},indent=2)+'\n',encoding='utf-8')
if __name__=='__main__':main()
