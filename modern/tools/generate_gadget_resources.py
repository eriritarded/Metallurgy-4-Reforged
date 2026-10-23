"""Native gadget resources; retained source artwork and authored model geometry."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
SHIELDS=['lemurite_shield','zinc_buckler','vulcanite_buckler','ceruclase_shield','orichalcum_buckler']
def dump(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def model(name): return {'type':'minecraft:model','model':'metallurgy:item/'+name}
def enlarge_buckler_icons():
    """Inventory transforms only; preserve Matpac's geometry and pixel art."""
    common=ROOT/'common/src/main/resources/assets/metallurgy'
    scales={}
    for name in ['zinc_buckler','vulcanite_buckler','orichalcum_buckler']:
        for folder in ['', 'gadget/']:
            for suffix in ['', '_blocking']:
                path=common/'models/item'/(folder+name+suffix+'.json')
                value=json.loads(path.read_text(encoding='utf-8'))
                # Increase the existing authored icon uniformly by eight percent.
                scale=1.08
                value.setdefault('display',{}).setdefault('gui',{})['scale']=[scale]*3
                dump(path,value);scales[folder+name+suffix]=scale
    return scales

def main():
    common=ROOT/'common/src/main/resources/assets/metallurgy'
    enlarge_buckler_icons()
    # The old TEISR base is not a valid native inventory model. Keep the source
    # GUI icon while its first-person skin-arm animation is ported separately.
    transforms={
        'firstperson_righthand':{'rotation':[0,280,50],'translation':[2.4,4.8,-4.8],'scale':[0.4,0.5,0.4]},
        'firstperson_lefthand':{'rotation':[0,100,-50],'translation':[2.4,4.8,-4.8],'scale':[0.4,0.5,0.4]},
        'thirdperson_righthand':{'rotation':[-90,0,0],'translation':[1.28,-1.76,-2.08],'scale':[0.375]*3},
        'thirdperson_lefthand':{'rotation':[-90,180,0],'translation':[0.96,-2.08,-2.08],'scale':[0.375]*3}}
    dump(common/'models/item/brass_knuckles.json',{'parent':'metallurgy:item/gadget/brass_knuckles_item','display':transforms})
    # Native special rendering owns the geometry; its base supplies only transforms and particles.
    # Minecraft 26 no longer supplies the old builtin/entity parent.
    dump(common/'models/item/gadget/brass_knuckles_hand.json',{'textures':{'particle':'metallurgy:items/gadgets/brass_knuckles'},'elements':[],'display':transforms})
    dump(common/'models/item/gadget/brass_knuckles.json',{'textures':{'particle':'metallurgy:items/gadgets/brass_knuckles'},'elements':[]})
    dump(common/'blockstates/ice_shield.json',{'variants':{'age='+str(i):{'model':'metallurgy:block/ice_shield_'+str(i)} for i in range(4)}})
    for i in range(4):dump(common/'models/block'/('ice_shield_'+str(i)+'.json'),{'parent':'minecraft:block/frosted_ice_'+str(i),'render_type':'minecraft:translucent'})
    lamp=json.loads((common/'blockstates/phosphorus_lamp.json').read_text(encoding='utf-8'));lamp['variants'].pop('inventory',None);dump(common/'blockstates/phosphorus_lamp.json',lamp)
    dump(common/'models/item/phosphorus_lamp.json',{'parent':'metallurgy:block/lamp_ground'})
    for name in ['lamp_ground','lamp_hanging','lamp_wall']:
        path=common/'models/block'/(name+'.json');value=json.loads(path.read_text(encoding='utf-8'));value['render_type']='minecraft:cutout';dump(path,value)
    for name in ['road_block','striped_road_block']:
        dump(common/'blockstates'/(name+'.json'),{'variants':{'facing='+facing:{'model':'metallurgy:block/'+name,**({'y':90} if facing in ['east','west'] else {})} for facing in ['north','south','east','west']}})
        path=common/'models/block'/(name+'.json');value=json.loads(path.read_text(encoding='utf-8'));value['parent']='minecraft:block/cube';dump(path,value)
    for target in ['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']:
        base=ROOT/target/'src/main/resources';old=target.startswith('forge');latest=target.endswith('26.1.2')
        loot='loot_tables' if old else 'loot_table'
        dump(base/'data/metallurgy'/loot/'blocks/ice_shield.json',{'type':'minecraft:block','pools':[]})
        tags='blocks' if old else 'block'
        path=base/'resourcepacks/core_tiers/data/minecraft/tags'/tags/'needs_stone_tool.json';value=json.loads(path.read_text(encoding='utf-8'));value['values']=sorted(set(value['values']+['metallurgy:phosphorus_lamp']));dump(path,value)
        native_rock=['base_stone_overworld','base_stone_nether','stone_ore_replaceables','stone_bricks','stone_crafting_materials','coal_ores','iron_ores','copper_ores','gold_ores','lapis_ores','redstone_ores','diamond_ores','emerald_ores','terracotta']
        values=[{'id':'#minecraft:'+name,'required':False} for name in native_rock]
        values+=['minecraft:'+name for name in ['end_stone','end_stone_bricks','obsidian','crying_obsidian','cobblestone','mossy_cobblestone','bricks','nether_bricks','red_nether_bricks','netherrack','nether_quartz_ore','nether_gold_ore','smooth_stone','chiseled_stone_bricks','polished_granite','polished_diorite','polished_andesite','sandstone','cut_sandstone','chiseled_sandstone','red_sandstone','cut_red_sandstone','chiseled_red_sandstone','blackstone','polished_blackstone','polished_blackstone_bricks','basalt','polished_basalt']]
        catalog=json.loads((ROOT/'content/legacy-catalog.json').read_text(encoding='utf-8'))
        values+=['metallurgy:'+metal['name']+'_ore' for metal in catalog['materials'] if metal['ore_harvest_level']>=0]
        values+=['metallurgy:'+name for name in ['sulfur_ore','phosphorite_ore','tar_ore','potash_ore','road_block','striped_road_block','bitumen_block','sulfur_block','charcoal_block']]
        dump(base/'data/metallurgy/tags'/tags/'vulcanite_softened.json',{'replace':False,'values':values})
        if latest:
            for equipment in ['celenegil_axe','celenegil_sword','electrum_axe','electrum_pickaxe','electrum_shovel','tartarite_axe','tartarite_pickaxe','tartarite_shovel','tartarite_sword']:
                source=json.loads((common/'models/item'/(equipment+'.json')).read_text(encoding='utf-8'))
                active={'type':'minecraft:model','model':source['overrides'][0]['model']}
                def flag(value,on_false):return {'type':'minecraft:condition','property':'minecraft:component','predicate':'minecraft:custom_data','value':value,'on_true':active,'on_false':on_false}
                value=flag('{voltage_control:true}',model(equipment)) if equipment.startswith('electrum_') else flag('{glory_seeker:true}',model(equipment))
                if equipment.startswith('tartarite_'):value=flag('{paragon:"celenegil",glory_seeker:true}',flag('{paragon:"electrum",voltage_control:true}',model(equipment)))
                dump(base/'assets/metallurgy/items'/(equipment+'.json'),{'model':value})
            def knuckles(first,left): return {'type':'minecraft:special','base':'metallurgy:item/gadget/brass_knuckles_hand','model':{'type':'metallurgy:knuckles','first_person':first,'left':left}}
            dump(base/'assets/metallurgy/items/brass_knuckles.json',{'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[
                {'when':['gui','ground','fixed'],'model':model('gadget/brass_knuckles_item')},
                {'when':'firstperson_righthand','model':knuckles(True,False)},
                {'when':'firstperson_lefthand','model':knuckles(True,True)},
                {'when':'thirdperson_lefthand','model':knuckles(False,True)}], 'fallback':knuckles(False,False)}})
            dump(base/'assets/metallurgy/items/ore_detector.json',{'model':{**model('ore_detector'),'tints':[{'type':'metallurgy:detector','index':i} for i in range(3)]}})
            for shield in SHIELDS:
                dump(base/'assets/metallurgy/items'/(shield+'.json'),{'model':{'type':'minecraft:condition','property':'minecraft:using_item','on_true':model('gadget/'+shield+'_blocking'),'on_false':model(shield)}})
            active=model('gadget/electrum_magnet_active')
            dump(base/'assets/metallurgy/items/electrum_magnet.json',{'model':{'type':'minecraft:condition','property':'minecraft:component','predicate':'minecraft:custom_data','value':'{status:1}','on_true':active,'on_false':{'type':'minecraft:condition','property':'minecraft:component','predicate':'minecraft:custom_data','value':'{status:2}','on_true':active,'on_false':model('electrum_magnet')}}})
    from fix_held_gadget_models import generate
    generate()
    print('Generated source-art gadget model selectors and temporary ice resources.')
if __name__=='__main__':main()
