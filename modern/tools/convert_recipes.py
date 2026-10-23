"""Convert reviewed1.12 crafting resources for content implemented so far.

Missing custom serializers/gadgets/integrations are recorded, never emitted as
invalid recipes. This is not a claim that those features have been ported.
"""
from pathlib import Path
import argparse, json, re
ROOT=Path(__file__).resolve().parents[1]

def dump(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    text=json.dumps(value,indent=2,ensure_ascii=False)+'\n'
    if not path.exists() or path.read_text(encoding='utf-8')!=text:
        path.write_text(text,encoding='utf-8')

def active_ids(materials):
    ids=set()
    for m in materials:
        forms=['ingot','dust','nugget','block','engraved_block','large_bricks','bricks','crystals','hazard_block','reinforced_glass']
        if m['ore_harvest_level']>=0:forms+=['ore']
        if 'tool_stats' in m:forms+=['sword','axe','pickaxe','shovel','hoe']
        if 'armor_stats' in m:forms+=['helmet','chestplate','leggings','boots']
        ids.update('metallurgy:'+m['name']+'_'+f for f in forms)
    ids.update('metallurgy:'+b for b in ['gold_dust','iron_dust','bitumen','tar','potash','sulfur_dust','thermite_dust','ignatius_fuel','phosphorus','potash_fertilizer','crusher','alloyer','sublimation_chamber','sulfur_ore','phosphorite_ore','tar_ore','potash_ore','bitumen_block','charcoal_block','sulfur_block','bimetal_structure_block'])
    ids.update('metallurgy:'+m+'_chest' for m in ['brass','silver','electrum','rubracium','platinum'])
    ids.update('metallurgy:'+m+'_'+f for m in ['iron','gold'] for f in ['engraved_block','large_bricks','bricks','crystals','hazard_block','reinforced_glass'])
    ids.update('metallurgy:'+b for b in ['flint_and_ignatius','flint_and_vulcanite','electrum_magnet','ore_detector','brass_knuckles','rubracium_gauntlet','lemurite_shield','zinc_buckler','vulcanite_buckler','ceruclase_shield','orichalcum_buckler'])
    ids.update('metallurgy:'+m['name']+'_bucket' for m in materials)
    ids.update('metallurgy:molten_'+name+'_bucket' for name in ['tar','thermite'])
    ids.update('metallurgy:'+name for name in ['phosphorus_lamp','road_block','striped_road_block','vulcanite_tnt','etherium_goggles'])
    return ids

def snake(value):return re.sub(r'(?<!^)(?=[A-Z])','_',value).lower()
PREFIX={'ingot':'ingots','dust':'dusts','nugget':'nuggets','ore':'ores','block':'storage_blocks'}

def result_count(result_id,legacy_count,old):
    # Component-era recipe codecs reject output counts above a damageable item's limit.
    return 1 if not old and result_id=='metallurgy:rubracium_gauntlet' else legacy_count

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);args=parser.parse_args()
    source=Path(args.source).resolve()
    if source.drive.lower()!='h:':raise SystemExit('Reference must be on H:')
    materials=json.loads((ROOT/'common/src/main/resources/assets/metallurgy/materials.json').read_text(encoding='utf-8'))
    active=active_ids(materials); rows=[]
    for target in ['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']:
        old=target.startswith('forge');latest=target.endswith('26.1.2');namespace='forge' if old else 'c'
        base=ROOT/target/'src/main/resources';tag_dir='items' if old else 'item';recipe_dir='recipes' if old else 'recipe'
        tags={}
        def tag(path,values): tags.setdefault(path,set()).update(values)
        def ore_key(key):
            special={'gunpowder':'minecraft:gunpowder','sand':'#'+namespace+(':'+ 'sand' if old else ':sands'),'plankWood':'#minecraft:planks','stickWood':'minecraft:stick','string':'minecraft:string','logWood':'#minecraft:logs','dyeWhite':'#'+namespace+':dyes/white'}
            if key in special:return special[key]
            for prefix,modern in PREFIX.items():
                if key.startswith(prefix):return '#'+namespace+':'+modern+'/'+snake(key[len(prefix):])
            raise ValueError('Unmapped OreDictionary '+key)
        def form(value):
            if 'ore' in value:result=ore_key(value['ore'])
            elif value.get('type')=='metallurgy:metal':
                result='#metallurgy:metal_blocks'
            else:
                result=value['item'];data=value.get('data',0)
                result={('minecraft:coal',1):'minecraft:charcoal',('minecraft:dye',4):'minecraft:lapis_lazuli',('minecraft:stonebrick',0):'minecraft:stone_bricks',('minecraft:stonebrick',3):'minecraft:chiseled_stone_bricks'}.get((result,data),result)
                if data not in [0,1,3,4,32767]:raise ValueError('Unmapped metadata '+str(value))
                if result.startswith('metallurgy:') and result not in active:raise ValueError('Unimplemented content '+result)
                if not result.startswith(('minecraft:','metallurgy:')):raise ValueError('Optional external content '+result)
            if latest:return result
            return {'tag':result[1:]} if result.startswith('#') else {'item':result}
        for m in materials:
            for prefix,modern in PREFIX.items():
                if prefix=='ore' and m['ore_harvest_level']<0:continue
                tag(namespace+':'+modern+'/'+m['name'],['metallurgy:'+m['name']+'_'+prefix])
        extras={'storage_blocks/bitumen':'bitumen_block','storage_blocks/charcoal':'charcoal_block','storage_blocks/sulfur':'sulfur_block','dusts/bitumen':'bitumen','dusts/sulfur':'sulfur_dust','dusts/thermite':'thermite_dust','dusts/phosphorus':'phosphorus','dusts/potash':'potash','dusts/iron':'iron_dust','dusts/gold':'gold_dust','ores/phosphorus':'phosphorite_ore','ores/tar':'tar_ore','ores/sulfur':'sulfur_ore','ores/potash':'potash_ore'}
        for path,item in extras.items():tag(namespace+':'+path,['metallurgy:'+item])
        for m in ['iron','gold']:
            for prefix,modern in [('ingot','ingots'),('nugget','nuggets'),('block','storage_blocks')]:tag(namespace+':'+modern+'/'+m,['minecraft:'+m+'_'+prefix])
        tag(namespace+':storage_blocks/redstone',['minecraft:redstone_block'])
        tag('metallurgy:metal_blocks',['metallurgy:'+m['name']+'_block' for m in materials])
        for path in sorted(source.rglob('*.json')):
            original=json.loads(path.read_text(encoding='utf-8'));rel=path.relative_to(source).as_posix();row={'target':target,'source':rel}
            kind=original.get('type')
            if rel=='gadget/wiki.json':
                row.update(status='omitted',reason='Replaced by the optional Patchouli handbook');rows.append(row);continue
            if rel=='_factories.json':
                row.update(status='adapted',note='Legacy factory declarations replaced by native serializer/condition registrations; not a recipe');rows.append(row);continue
            if rel.startswith('generated/item/spartanweaponry/'):
                name=original['result']['item'].split(':',1)[1]
                if old:row.update(status='adapted',output='data/metallurgy/recipes/compat/spartanweaponry/'+name+'.json',note='Native API12 adapter owns generation; tools/generate_spartan_resources.py')
                else:row.update(status='omitted',reason='No matching official native Spartan API for this target; optional weapon integration remains Forge1.20.1')
                rows.append(row);continue
            if kind=='metallurgy:ore_detector_shapeless':
                dump(base/'data/metallurgy'/recipe_dir/'ore_detector_programming.json',{'type':kind,'category':'misc'})
                row.update(status='adapted',output='data/metallurgy/'+recipe_dir+'/ore_detector_programming.json',note='One native dynamic handler covers cleaning and programming with1..3 nonalloy metals');rows.append(row);continue
            if kind=='metallurgy:tartarite_paragon_shaped':
                dump(base/'data/metallurgy'/recipe_dir/'tartarite_paragon.json',{'type':kind,'category':'misc'})
                row.update(status='adapted',output='data/metallurgy/'+recipe_dir+'/tartarite_paragon.json',note='One dynamic native handler covers all9 equipment forms');rows.append(row);continue
            if kind not in ['forge:ore_shaped','forge:ore_shapeless','minecraft:crafting_shaped']:
                row.update(status='pending',reason='Custom serializer or nonrecipe definition: '+str(kind));rows.append(row);continue
            try:
                result=original['result'];result_id=result['item']
                if not result_id.startswith('minecraft:') and result_id not in active:raise ValueError('Unimplemented result '+result_id)
                shaped=kind.endswith('shaped') and not kind.endswith('shapeless')
                converted={'type':'minecraft:crafting_shaped' if shaped else 'minecraft:crafting_shapeless'}
                if shaped:converted.update(pattern=original['pattern'],key={k:form(v) for k,v in original['key'].items()})
                else:converted['ingredients']=[form(v) for v in original['ingredients']]
                count=result_count(result_id,result.get('count',1),old)
                converted['result']={'item' if old else 'id':result_id,'count':count}
                dump(base/'data/metallurgy'/recipe_dir/'legacy'/rel,converted)
                row.update(status='adapted',output='data/metallurgy/'+recipe_dir+'/legacy/'+rel)
                if count!=result.get('count',1):
                    row.update(sourceOutputCount=result.get('count',1),outputCount=count,note='One unstackable gauntlet per craft; modern recipe codecs reject the legacy count of two')
            except (ValueError,KeyError) as error:row.update(status='pending',reason=str(error))
            rows.append(row)
        for key,values in tags.items():
            ns,path=key.split(':',1);dump(base/'data'/ns/'tags'/tag_dir/(path+'.json'),{'replace':False,'values':sorted(values)})
        # Original smelting durations use the native200-tick furnace baseline.
        smelting=[]
        for m in materials:
            if m['ore_harvest_level']>=0:smelting.append((m['name']+'_ore',m['name']+'_ingot',1))
            smelting.append((m['name']+'_dust',m['name']+'_ingot',0.6))
        smelting += [('iron_dust','minecraft:iron_ingot',0.6),('gold_dust','minecraft:gold_ingot',0.6),('tar_ore','tar',0.5),('phosphorite_ore','phosphorus',0.5),('potash_ore','potash',0.5),('sulfur_ore','sulfur_dust',0.5)]
        for input_id,output_id,xp in smelting:
            output_id=output_id if ':' in output_id else 'metallurgy:'+output_id
            dump(base/'data/metallurgy'/recipe_dir/'smelting'/(input_id+'.json'),{'type':'minecraft:smelting','ingredient':form({'item':'metallurgy:'+input_id}),'result':output_id if old else {'id':output_id},'experience':xp,'cookingtime':200})
    dump(ROOT/'docs/crafting-coverage.json',{'sourceRecipeAdaptationComplete':True,'nativeDecodingOrGameplayObserved':False,'unsupportedOptionalTargetsDocumented':True,'rows':rows})
    dump(ROOT/'content/current-registry-ids.json',sorted(active))
    print('Crafting:',{s:sum(r['status']==s for r in rows)//3 for s in ['adapted','pending']},'per target;',len(active),'current item IDs')
    import runpy
    runpy.run_path(str(ROOT/'tools/generate_config_resources.py'),run_name='__main__')
    runpy.run_path(str(ROOT/'tools/generate_handbook.py'),run_name='__main__')

if __name__=='__main__':main()
