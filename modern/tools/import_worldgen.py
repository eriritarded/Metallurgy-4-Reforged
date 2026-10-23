"""Recover the original default ore order/parameters from the pinned sources."""
from pathlib import Path
import argparse,json,re,hashlib
ROOT=Path(__file__).resolve().parents[1]
def snake(value):return re.sub(r'(?<!^)(?=[A-Z])','_',value).lower()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);args=parser.parse_args()
    source=Path(args.source).resolve()
    if source.drive.lower()!='h:':raise SystemExit('Pinned references must remain on H:')
    config=(source/'config/WorldGenerationConfig.java').read_text(encoding='utf-8');world=(source/'world/ModWorldGen.java').read_text(encoding='utf-8')
    categories={}
    for name,body in re.findall(r'public static class Category(\w+)\s*\{(.*?)(?=\n\tpublic static class|\n})',config,re.S):
        values={k:int(v) for k,v in re.findall(r'public int (minY|maxY|veinSize) = (\d+);',body)}
        biomes=re.search(r'public String\[\] biomes = new String\[\]\s*\{([^}]+)\}',body)
        values['biomes']=re.findall(r'"([^\"]+)"',biomes.group(1)) if biomes else []
        categories[snake(name)]=values
    rarity_values=dict((snake(k.removesuffix('Rarity')).upper(),int(v)) for k,v in re.findall(r'public int (\w+Rarity) = (\d+);',config))
    entries=[]
    for realm,method in [('overworld','generateOverworld'),('nether','generateNether'),('end','generateEnd')]:
        start=world.index('private void '+method);end=world.find('\n\tprivate ',start+1);body=world[start:end]
        calls=re.finditer(r'generateOre\(Mod(Metals|Blocks)\.(\w+), world, random, chunkX, chunkZ,\s*\w+\.veinSize, (\w+),',body)
        for match in calls:
            owner,field,rarity=match.groups()
            metal=field.lower() if owner=='Metals' else snake(field.removeprefix('ore'))
            params=categories[metal]
            special={'rubracium':'lava_adjacent','astral_silver':'high_y','platinum':'warm_chunk','sanguinite':'fortress','eximite':'outer_islands'}.get(metal,'normal')
            entries.append({'name':metal,'block':metal+'_ore','dimension':realm,'min_y':params['minY'],'max_y':params['maxY'],'vein_size':params['veinSize'],'attempts':rarity_values[rarity],'biomes':params['biomes'],'special':special})
    assert len(entries)==34 and len({e['name'] for e in entries})==34
    value={'sourceCommit':'ef94cb94cfc08ccebe8a0df307ce6b390dcd4704','sourceHashes':{p:hashlib.sha256((source/p).read_bytes()).hexdigest() for p in ['world/ModWorldGen.java','config/WorldGenerationConfig.java']},'rarity':rarity_values,'ores':entries}
    path=ROOT/'common/src/main/resources/assets/metallurgy/legacy_worldgen.json';path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
    for target in ['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']:
        loader='forge' if target.startswith('forge') else 'neoforge';base=ROOT/target/'src/main/resources/data/metallurgy'
        for realm in ['overworld','nether','end']:
            for folder,data in [('worldgen/configured_feature',{'type':'metallurgy:ore_batch','config':{'dimension':realm}}),('worldgen/placed_feature',{'feature':'metallurgy:'+realm+'_ores','placement':[{'type':'minecraft:in_square'},{'type':'minecraft:biome'}]}),(loader+'/biome_modifier',{'type':'metallurgy:ore_realms','feature':'metallurgy:'+realm+'_ores'})]:
                out=base/folder/(realm+'_ores.json');out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    print('Recovered',len(entries),'ore profiles:',rarity_values)
if __name__=='__main__':main()
