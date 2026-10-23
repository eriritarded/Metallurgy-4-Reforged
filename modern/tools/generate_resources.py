"""Version-specific resource definitions over unchanged original artwork."""
from pathlib import Path
import json
import shutil
import re

ROOT = Path(__file__).resolve().parents[1]


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def tag_base(base,kind,name):
    if kind in ('block','blocks') and re.fullmatch(r'needs_(?:level_\d|stone_tool|iron_tool|diamond_tool)|incorrect_for_(?:level_\d|wooden_tool|gold_tool|stone_tool|iron_tool|diamond_tool|netherite_tool)',name):
        base=base/'resourcepacks/core_tiers'
    return base


def tag(base, namespace, kind, name, values):
    base=tag_base(base,kind,name)
    dump(base / 'data' / namespace / 'tags' / kind / (name + '.json'), dict(replace=False, values=values))


def main():
    catalog = json.loads((ROOT / 'content/legacy-catalog.json').read_text(encoding='utf-8'))
    metals = catalog['materials']
    common = ROOT / 'common/src/main/resources'
    own = common / 'assets/metallurgy'
    dump(own/'blockstates/bimetal_structure_block.json',{'variants':{'':{'model':'metallurgy:block/bimetal_block'}}})
    dump(own/'models/item/bimetal_structure_block.json',{'parent':'metallurgy:block/bimetal_block'})
    frame=own/'models/block/bimetal_block.json';frame_data=json.loads(frame.read_text(encoding='utf-8'));frame_data['render_type']='minecraft:cutout';dump(frame,frame_data)
    # Only Minecraft's resource paths changed. Mod texture paths deliberately
    # retain their original plural directories and identical PNG bytes.
    for path in (own / 'models').rglob('*.json'):
        text = path.read_text(encoding='utf-8').replace('minecraft:blocks/', 'minecraft:block/').replace('minecraft:items/', 'minecraft:item/')
        path.write_text(text, encoding='utf-8')
    for metal in catalog['chests']:
        name = metal + '_chest'
        dump(own / 'blockstates' / (name + '.json'), {'variants': {'': {'model': 'metallurgy:block/' + name}}})
        dump(own / 'models/block' / (name + '.json'), {'textures': {'particle': 'metallurgy:blocks/' + metal + '_block'}})
        dump(own / 'models/item' / (name + '.json'), {'parent': 'minecraft:item/chest'})
    # A core slice may be verified before the machines and gadgets are enabled.
    # The complete legacy recipe conversion is tracked separately, not silently
    # substituted with an easier crafting table alloy recipe.
    current_blocks = [m['name'] + '_' + f for m in metals for f in ['block','engraved_block','large_bricks','bricks','crystals','hazard_block','reinforced_glass']]
    current_blocks += [m['name'] + '_ore' for m in metals if m['ore_harvest_level'] >= 0]
    current_blocks += [m + '_chest' for m in catalog['chests']]
    current_blocks += ['bimetal_structure_block','crusher','alloyer','sublimation_chamber','sulfur_ore','phosphorite_ore','tar_ore','potash_ore','bitumen_block','charcoal_block','sulfur_block']
    current_blocks += ['phosphorus_lamp','road_block','striped_road_block','vulcanite_tnt']
    current_blocks += [m+'_'+f for m in ['iron','gold'] for f in ['engraved_block','large_bricks','bricks','crystals','hazard_block','reinforced_glass']]
    for path in (own/'models/block').rglob('*.json'):
        if 'reinforced_glass' in path.stem or path.stem=='bimetal_structure_block':
            value=json.loads(path.read_text(encoding='utf-8'));value['render_type']='minecraft:cutout';dump(path,value)
    for target in ['forge-1.20.1', 'neoforge-1.21.1', 'neoforge-26.1.2']:
        base = ROOT / target / 'src/main/resources'
        old = target == 'forge-1.20.1'
        latest = target == 'neoforge-26.1.2'
        mixin={'required':True,'minVersion':'0.8','package':'it.hurts.metallurgy_reforged.mixin','compatibilityLevel':'JAVA_17' if old else 'JAVA_25' if latest else 'JAVA_21','mixins':['LiquidBlockMixin','LivingArmorMixin','LivingAccess']+(['BlockLootMixin','VulcaniteExplosionMixin','TinkerArmorMenuMixin'] if old else []),'injectors':{'defaultRequire':1}}
        if old: mixin['refmap']='metallurgy.refmap.json'
        dump(base/'metallurgy.mixins.json',mixin)
        block_dir, item_dir = ('blocks','items') if old else ('block','item')
        recipe_dir, loot_dir = ('recipes','loot_tables') if old else ('recipe','loot_table')
        dump(base/'data/metallurgy/damage_type/water_damage.json',{'message_id':'metallurgy.water_damage','exhaustion':0.1,'scaling':'never'})
        tag(base,'minecraft','damage_type','bypasses_armor',['metallurgy:water_damage'])
        for form in ['ingots','dusts','nuggets']:
            suffix = {'ingots':'ingot','dusts':'dust','nuggets':'nugget'}[form]
            for metal in metals:
                name = metal['name']; values = ['metallurgy:' + name + '_' + suffix]
                for namespace in ['metallurgy', 'forge' if old else 'c']:
                    tag(base, namespace, item_dir, form + '/' + name, values)
        for level in range(8):
            denied = ['metallurgy:' + m['name'] + '_ore' for m in metals if m['ore_harvest_level'] > level]
            vanilla = ['wooden','stone','iron','diamond','netherite','netherite','netherite','netherite'][level]
            if not old:
                tag(base,'metallurgy',block_dir,'incorrect_for_level_'+str(level),[*(('#minecraft:needs_stone_tool',) if level<1 else ()),*(('#minecraft:needs_iron_tool',) if level<2 else ()),*(('#minecraft:needs_diamond_tool',) if level<3 else ()),*denied])
        tag(base,'minecraft',block_dir,'mineable/pickaxe',['metallurgy:'+b for b in current_blocks])
        tag(base,'minecraft',block_dir,'needs_stone_tool',['metallurgy:'+m+'_chest' for m in catalog['chests']])
        tag(base,'minecraft',block_dir,'needs_iron_tool',['metallurgy:'+m['name']+'_'+f for m in metals for f in ['block','engraved_block','large_bricks','bricks','crystals','hazard_block','reinforced_glass']])
        if not old:
            for tool,level in [('wooden',0),('gold',0),('stone',1),('iron',2),('diamond',3),('netherite',4)]:
                tag(base,'minecraft',block_dir,'incorrect_for_'+tool+'_tool',['metallurgy:'+m['name']+'_ore' for m in metals if m['ore_harvest_level']>level])
        for required, name in [(1,'needs_stone_tool'),(2,'needs_iron_tool'),(3,'needs_diamond_tool')]:
            path=tag_base(base,block_dir,name)/'data/minecraft/tags'/block_dir/(name+'.json')
            prior=json.loads(path.read_text(encoding='utf-8'))['values'] if path.exists() else []
            values=prior+['metallurgy:'+m['name']+'_ore' for m in metals if m['ore_harvest_level']==required or required==3 and m['ore_harvest_level']>3]
            tag(base,'minecraft',block_dir,name,sorted(set(values)))
        for block in current_blocks:
            dump(base/'data/metallurgy'/loot_dir/'blocks'/(block+'.json'),{
                'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'metallurgy:'+block}],
                    'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
        tag(base,'minecraft',block_dir,'needs_stone_tool',['metallurgy:'+b for b in ['crusher','alloyer','sublimation_chamber','sulfur_ore','phosphorite_ore','tar_ore','potash_ore','bitumen_block','charcoal_block','sulfur_block']]+['metallurgy:'+m+'_chest' for m in catalog['chests']])
        for ore,drops in {'sulfur_ore':[('sulfur_dust',4,1)],'phosphorite_ore':[('phosphorus',3,1)],'tar_ore':[('tar',2,1),('bitumen',2,0.5)],'potash_ore':[('potash',3,1)]}.items():
            pools=[]
            for item,count,chance in drops:
                conditions=[{'condition':'minecraft:survives_explosion'}]
                if chance<1: conditions.append({'condition':'minecraft:random_chance','chance':chance})
                pools.append({'rolls':1,'entries':[{'type':'minecraft:item','name':'metallurgy:'+item,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':count}}]}],'conditions':conditions})
            dump(base/'data/metallurgy'/loot_dir/'blocks'/(ore+'.json'),{'type':'minecraft:block','pools':pools})
        for metal in catalog['chests']:
            def ingredient(value):
                return value if latest else ({'tag':value[1:]} if value.startswith('#') else {'item':value})
            previous={'brass':'wooden','silver':'brass','electrum':'silver','rubracium':'electrum','platinum':'rubracium'}[metal]
            namespace='forge' if old else 'c'
            tag(base,namespace,item_dir,'chests/'+metal,['metallurgy:'+metal+'_chest'])
            dump(base/'data/metallurgy'/recipe_dir/(metal+'_chest.json'),{
                'type':'minecraft:crafting_shaped','pattern':['mmm','mbm','mcm'],
                'key':{'m':ingredient('#metallurgy:ingots/'+metal),'b':ingredient('metallurgy:bimetal_structure_block'),'c':ingredient('#'+namespace+':chests/'+previous)},
                'result':{'item' if old else 'id':'metallurgy:'+metal+'_chest','count':1}})
        def ingot(name):
            value='#metallurgy:ingots/'+name
            return value if latest else {'tag':value[1:]}
        dump(base/'data/metallurgy'/recipe_dir/'scaffolding.json',{
            'type':'minecraft:crafting_shaped','pattern':['TZT','Z Z','TZT'],
            'key':{'T':ingot('tin'),'Z':ingot('zinc')},
            'result':{'item' if old else 'id':'metallurgy:bimetal_structure_block','count':1}})
        if old:
            dump(base/'pack.mcmeta',{'pack':{'description':'Metallurgy modern port','pack_format':15}})
        if latest:
            for path in (own/'models/item').glob('*.json'):
                # These are parent templates, not registered items with their own texture.
                if path.stem in {'extended', 'extended_handheld'}:
                    continue
                dump(base/'assets/metallurgy/items'/path.name,{'model':{'type':'minecraft:model','model':'metallurgy:item/'+path.stem}})
            for metal in catalog['chests']:
                name=metal+'_chest'
                dest=base/'assets/metallurgy/textures/entity/chest'/(name+'.png')
                dest.parent.mkdir(parents=True,exist_ok=True)
                shutil.copy2(common/'assets/metallurgychests/textures/blocks'/(name+'.png'),dest)
                dump(base/'assets/metallurgy/items'/(name+'.json'),{
                    'model':{'type':'minecraft:special','base':'minecraft:item/chest','model':{'type':'minecraft:chest','texture':'metallurgy:'+name}}})
            dump(base/'assets/minecraft/atlases/chests.json',{'sources':[{'type':'minecraft:single','resource':'metallurgy:entity/chest/'+m+'_chest'} for m in catalog['chests']]})
            for metal in metals:
                if 'armor_stats' not in metal: continue
                name=metal['name']
                dump(base/'assets/metallurgy/equipment'/(name+'.json'),{
                    'layers':{'humanoid':[{'texture':'metallurgy:'+name}],'humanoid_leggings':[{'texture':'metallurgy:'+name}]}})
                for layer, index in [('humanoid',1),('humanoid_leggings',2)]:
                    dest=base/'assets/metallurgy/textures/entity/equipment'/layer/(name+'.png')
                    dest.parent.mkdir(parents=True,exist_ok=True)
                    shutil.copy2(own/'textures/models/armor'/(name+'_layer_'+str(index)+'.png'),dest)
    print('Generated native tags, core loot, chest recipes and26.1.2 item/equipment definitions.')
    # Regenerate optional material resources with their own native schemas.
    import runpy
    runpy.run_path(str(ROOT/'tools/generate_gadget_resources.py'),run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_modular_compat.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_fluid_resources.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_config_resources.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_client_resources.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_optional_recipes.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_processing_resources.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_projecte_resources.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_core_tier_resources.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_tconstruct_resources.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_spartan_resources.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_ore_resources.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/fix_model_resources.py'), run_name='__main__')
    runpy.run_path(str(ROOT / 'tools/generate_handbook.py'), run_name='__main__')


if __name__=='__main__':
    main()
