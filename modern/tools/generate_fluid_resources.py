"""Maintained fluid resources using each target's actual bucket model schema."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]


def dump(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')


def main():
    materials=json.loads((ROOT/'content/legacy-catalog.json').read_text(encoding='utf-8'))['materials']
    fluids=[(m['name'],'molten_'+m['name'],'Molten '+m['name'].replace('_',' ').title()) for m in materials]
    fluids += [('molten_tar','molten_tar','Molten Tar'),('molten_thermite','molten_thermite','Molten Thermite')]
    for target in ['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']:
        base=ROOT/target/'src/main/resources';old=target.startswith('forge');latest=target.endswith('26.1.2')
        ns='forge' if old else 'neoforge'
        loader='forge' if old else 'neoforge'
        for folder,value in [('worldgen/configured_feature',{'type':'metallurgy:tar_lake','config':{}}),
                             ('worldgen/placed_feature',{'feature':'metallurgy:tar_lake','placement':[{'type':'minecraft:biome'}]}),
                             (loader+'/biome_modifier',{'type':'metallurgy:tar_dimensions','feature':'metallurgy:tar_lake'})]:
            dump(base/'data/metallurgy'/folder/'tar_lake.json',value)
        atlas=base/'assets/minecraft/atlases/blocks.json'
        data=json.loads(atlas.read_text(encoding='utf-8')) if atlas.exists() else {'sources':[]}
        for texture in ['molten_metal_still','molten_metal_flow']:
            entry={'type':'minecraft:single','resource':'metallurgy:blocks/'+texture}
            if entry not in data['sources']: data['sources'].append(entry)
        dump(atlas,data)
        for fluid,block,title in fluids:
            dump(base/'assets/metallurgy/blockstates'/(block+'.json'),{'variants':{'':{'model':'metallurgy:block/'+block}}})
            dump(base/'assets/metallurgy/models/block'/(block+'.json'),{'textures':{'particle':'metallurgy:blocks/molten_metal_still'}})
            bucket=fluid+'_bucket'
            if latest:
                dump(base/'assets/metallurgy/items'/(bucket+'.json'),{'model':{'type':'neoforge:fluid_container','fluid':'metallurgy:'+fluid,
                     'textures':{'base':'minecraft:item/bucket','fluid':'neoforge:item/mask/bucket_fluid'},'apply_fluid_luminosity':True}})
            else:
                dump(base/'assets/metallurgy/models/item'/(bucket+'.json'),{'parent':ns+':item/bucket','loader':ns+':fluid_container','fluid':'metallurgy:'+fluid})
        fluid_dir='fluids' if old else 'fluid'
        for tag,values in [('molten_metals',[key for key,_,_ in fluids if not key.startswith('molten_')]),('all_molten',[key for key,_,_ in fluids])]:
            dump(base/'data/metallurgy/tags'/fluid_dir/(tag+'.json'),{'replace':False,'values':['metallurgy:'+prefix+key for key in values for prefix in ['','flowing_']]})
        item_dir='items' if old else 'item'
        dump(base/'data'/('forge' if old else 'c')/'tags'/item_dir/'buckets.json',{'replace':False,'values':['metallurgy:'+key+'_bucket' for key,_,_ in fluids]})
    lang_path=ROOT/'common/src/main/resources/assets/metallurgy/lang/en_us.json'
    lang=json.loads(lang_path.read_text(encoding='utf-8'))
    for key,block,title in fluids:
        lang['fluid_type.metallurgy.'+key]=title
        lang['fluid.metallurgy.'+key]=title
        lang['block.metallurgy.'+block]=title
        lang['item.metallurgy.'+key+'_bucket']=title+' Bucket'
    dump(lang_path,lang)
    print('Generated51 native fluid/bucket definitions per target using original artwork.')


if __name__=='__main__':main()
