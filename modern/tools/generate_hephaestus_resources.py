"""Hephaestus/Hephaestus Tools data for the NeoForge26.1.2 target.

Hephaestus has no Tinkers API: smeltery recipes are plain hephaestus:* JSON and Hephaestus
Tools loads tool materials from data/<namespace>/hephaestus_tools/materials. A cast part takes
the material hephaestus_tools:<fluid path without molten_>, so every Metallurgy fluid
metallurgy:<name> casts parts of material hephaestus_tools:<name>.

The same TCon source rules as generate_tconstruct_resources.py decide which parts each metal
offers; HephaestusData recomputes material stats from the configured Metallurgy stats at load.
Native Hephaestus recipes and materials (content/hephaestus-native.json) always win: an
unconditional native duplicate drops ours, a mod-conditional one makes ours its complement.
This never executes a game.
"""
from pathlib import Path
import json,math
from generate_tconstruct_resources import parts
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'neoforge-26.1.2/src/main/resources'
PACK=BASE/'resourcepacks/hephaestus'
DATA=PACK/'data/metallurgy'
# Hephaestus Tools 1.0.8 iron part costs (mB) and cooling ticks, by stat slot.
HEAD_PARTS={'pick_head':(180,120),'small_axe_head':(180,120),'adze_head':(180,120),'small_blade':(180,120),
            'hammer_head':(720,240),'broad_axe_head':(720,240),'large_blade':(720,240),'large_plate':(360,160)}
HANDLE_PARTS={'tool_handle':(90,100),'tough_handle':(270,140)}
BINDING_PARTS={'tool_binding':(90,100),'tough_binding':(270,140)}
# Hephaestus HarvestTier ordinals: wood, stone, iron, diamond, gold, netherite, adamantite, aurichalcum.
TIERS=[0,1,2,3,5,6,7]
# Original per-role assignments, consumed by the optional adapter through markers.
TRAITS=json.loads((ROOT/'common/src/main/resources/assets/metallurgy/hephaestus_traits.json').read_text(encoding='utf-8'))['materials']
LAVA,BLAZE='minecraft:lava','hephaestus:molten_blaze'

def dump(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def clamp(value,low,high):return round(max(low,min(high,value)),3)

def material(m,p):
    """Mirror of HephaestusData.stats; the runtime copy follows Metallurgy's configured stats."""
    tool=m.get('tool_stats',{});result={}
    if 'head' in p:
        head=p['head'];stats={'durability':max(1,2*head['durability']),'mining_speed':head['mining_speed'],'attack_damage':head['melee_attack'],
            'attack_speed':1.0,'tier':TIERS[max(0,min(6,head['harvest_level']))],'enchantability':tool.get('enchantability',14)}
        result['head_stats']=stats
    if 'handle' in p:
        speed=p.get('head',{}).get('mining_speed',6);attack=p.get('head',{}).get('melee_attack',2)
        result['handle_stats']={'durability_mult':clamp(p['handle']['multiplier']-1,-.5,.4),'speed_mult':clamp((speed-6)*.02,-.2,.45),
            'damage_mult':clamp((attack-2)*.03,-.1,.25),'attack_speed_mult':clamp(tool.get('attack_speed',0)*.1,-.1,.15)}
    if 'extra' in p:result['binding_stats']={'durability':max(1,p['extra']['durability'])}
    for key,effects in zip(['head_stats','handle_stats','binding_stats'],TRAITS.get(m['name'],[[],[],[]])):
        if key in result and effects:result[key]['trait']='metallurgy:'+m['name']
    return result

def main():
    native=json.loads((ROOT/'content/hephaestus-native.json').read_text(encoding='utf-8'))
    source=json.loads((ROOT/'common/src/main/resources/assets/metallurgy/tconstruct_legacy.json').read_text(encoding='utf-8'))
    catalog=json.loads((ROOT/'content/legacy-catalog.json').read_text(encoding='utf-8'))['materials']
    fluids={c['name']:c for c in catalog}
    shared=set(native['unconditionalFluids'])
    def fluid(name):return 'hephaestus:molten_'+name if name in shared else 'metallurgy:'+name
    def tier(name):
        m=fluids[name]
        if m['ore_harvest_level']>=0:return m['ore_harvest_level']
        java=(ROOT/'common/src/main/java/it/hurts/metallurgy_reforged/fluid/FluidCatalog.java').read_text(encoding='utf-8')
        import re
        names=re.search(r'String\[\] names=\{([^}]+)\}',java)[1].replace('"','').split(',')
        tiers=[int(x) for x in re.search(r'int\[\] tiers=\{([^}]+)\}',java)[1].split(',')]
        return dict(zip(names,tiers))[name]
    # Same Celsius conversion as the TCon branch; lava is Hephaestus' 1000-degree fuel.
    def heat(name):
        t=900 if name in ['iron','gold'] else 700+200*tier(name);return t,LAVA if t<=1000 else BLAZE
    def complement(table,key,conditions):
        """None when native data already covers key unconditionally, else extra conditions."""
        extra=[]
        for native_conditions in native[table].get(key,[]):
            if not native_conditions:return None
            extra.append({'type':'neoforge:not','value':{'type':'neoforge:and','values':native_conditions}})
        return conditions+extra
    def not_empty(tag):return {'type':'neoforge:not','value':{'type':'neoforge:tag_empty','tag':tag}}
    def content(item):return {'type':'metallurgy:content_enabled','content':item}
    TOOLS={'type':'neoforge:mod_loaded','modid':'hephaestus_tools'}
    written={'melting':0,'casting':0,'parts':0,'alloys':0,'materials':0,'skipped':0}
    def recipe(path,value,table,key,conditions):
        conditions=complement(table,key,conditions)
        if conditions is None:written['skipped']+=1;return
        value['neoforge:conditions']=conditions;dump(DATA/'recipe/hephaestus'/Path(path+'.json'),value)
    import shutil
    if PACK.exists():shutil.rmtree(PACK)
    dump(PACK/'pack.mcmeta',{'pack':{'description':'Metallurgy native Hephaestus smeltery and Hephaestus Tools materials','min_format':[101,1],'max_format':[101,1]}})
    materials={}
    for m in catalog:
        name=m['name'];t,fuel=heat(name);molten=fluid(name);folder='metals/'+name+'/'
        for form,tag,amount,time in [('ingot','ingots',90,100),('nugget','nuggets',10,33),('block','storage_blocks',810,200),('dust','dusts',90,100),('ore','ores',180,150)]:
            ingredient='#c:'+tag+'/'+name
            recipe(folder+'melting_'+form,{'type':'hephaestus:melting','fuel':{'amount':50,'id':fuel},'ingredient':ingredient,
                'result':{'amount':amount,'id':molten},'temperature':t,'time':time},'melting',ingredient,[not_empty('c:'+tag+'/'+name)])
            written['melting']+=1
        for form,amount,cooling,cast in [('ingot',90,100,'hephaestus:ingot_cast'),('nugget',10,33,'hephaestus:nugget_cast'),('block',810,200,None)]:
            kind='hephaestus:casting_basin' if cast is None else 'hephaestus:casting_table';item='metallurgy:'+name+'_'+form
            value={'type':kind,'cooling_time':cooling,'fluid':{'amount':amount,'id':molten},'result':{'id':item}}
            if cast:value={'type':kind,'cast':cast,**{k:v for k,v in value.items() if k!='type'}}
            recipe(folder+'casting_'+form,value,'casting','|'.join([kind,molten,cast or '']),[content(item)])
            written['casting']+=1
        p=parts(m,source)
        if not p:continue
        mid='hephaestus_tools:'+name
        # A native material needing only our own ingot tag already covers ours.
        if any(c in ([],[not_empty('c:ingots/'+name)]) for c in native['materials'].get(mid,[])):continue
        conditions=complement('materials',mid,[TOOLS,not_empty('c:ingots/'+name)])
        stats=material(m,p)
        dump(DATA/'hephaestus_tools/materials'/(name+'.json'),{'neoforge:conditions':conditions,'id':mid,'ingredient':{'tag':'c:ingots/'+name},'color':int(m['color'],16),**stats})
        materials[name]=sorted(k.split('_')[0] for k in stats);written['materials']+=1
        offered={**(HEAD_PARTS if 'head_stats' in stats else {}),**(HANDLE_PARTS if 'handle_stats' in stats else {}),**(BINDING_PARTS if 'binding_stats' in stats else {})}
        for part,(amount,cooling) in offered.items():
            cast='hephaestus_tools:'+part+'_cast'
            recipe(folder+'part_casting_'+part,{'type':'hephaestus:casting_table','cast':cast,'cast_consumed':False,'cooling_time':cooling,
                'fluid':{'amount':amount,'id':molten},'result':{'count':1,'id':'hephaestus_tools:'+part}},'casting','|'.join(['hephaestus:casting_table',molten,cast]),[TOOLS])
            ingredient={'neoforge:ingredient_type':'neoforge:components','components':{'hephaestus_tools:part_material':mid},'items':'hephaestus_tools:'+part,'strict':False}
            recipe(folder+'part_melting_'+part,{'type':'hephaestus:melting','fuel':{'amount':50,'id':fuel},'ingredient':ingredient,
                'result':{'amount':amount,'id':molten},'temperature':t,'time':cooling},'melting',json.dumps(ingredient,sort_keys=True),[TOOLS])
            written['parts']+=2
    java=(ROOT/'common/src/main/java/it/hurts/metallurgy_reforged/machine/MachineRecipes.java').read_text(encoding='utf-8')
    import re
    used=set()
    for first,a,second,b,result,count,_ in re.findall(r'new Alloy\("([@\w]+)",(\d+),"([@\w]+)",(\d+),"(\w+)",(\d+),([.\d]+)F\)',java):
        if result in used:continue
        used.add(result)
        first=first.removeprefix('@').removesuffix('_ingot').removesuffix('_dust');second=second.removeprefix('@').removesuffix('_ingot').removesuffix('_dust')
        conditions=complement('alloys',fluid(result),[])
        if conditions is None:written['skipped']+=1;continue
        value={'type':'hephaestus:alloy','inputs':[{'amount':int(a)*90,'id':fluid(first)},{'amount':int(b)*90,'id':fluid(second)}],
               'output':{'amount':int(count)*90,'id':fluid(result)},'temperature':max(heat(first)[0],heat(second)[0],heat(result)[0])}
        if conditions:value['neoforge:conditions']=conditions
        dump(DATA/'recipe/hephaestus/alloys'/(result+'.json'),value);written['alloys']+=1
    # Hephaestus derives a fuel's heat from the hottest melting recipe that burns it.
    dump(DATA/'recipe/hephaestus/thermite_melting.json',{'type':'hephaestus:melting','fuel':{'amount':50,'id':LAVA},'ingredient':{'item':'metallurgy:thermite_dust'},
         'result':{'amount':1000,'id':'metallurgy:molten_thermite'},'temperature':100,'time':80,'neoforge:conditions':[content('metallurgy:thermite_dust')]})
    dump(DATA/'recipe/hephaestus/thermite_fuel.json',{'type':'hephaestus:melting','fuel':{'amount':50,'id':'metallurgy:molten_thermite'},'ingredient':{'item':'metallurgy:thermite_dust'},
         'result':{'amount':1000,'id':'metallurgy:molten_thermite'},'temperature':3700,'time':80,'neoforge:conditions':[content('metallurgy:thermite_dust')]})
    # Metallurgy level-6 ores stay above Hephaestus' adamantite tier, as on vanilla netherite.
    for tag,level in [('incorrect_for_adamantite_tool',5),('incorrect_for_aurichalcum_tool',6)]:
        dump(PACK/'data/hephaestus_tools/tags/block'/(tag+'.json'),{'replace':False,'values':[{'id':'metallurgy:'+m['name']+'_ore','required':False} for m in catalog if m['ore_harvest_level']>level]})
    lang_path=ROOT/'common/src/main/resources/assets/metallurgy/lang/en_us.json';lang=json.loads(lang_path.read_text(encoding='utf-8'))
    for name in materials:lang['material.hephaestus_tools.'+name]=lang.get('material.metallurgy.'+name+'.name',name.replace('_',' ').title())
    dump(lang_path,lang)
    dump(ROOT/'docs/hephaestus-coverage.json',{'target':'neoforge-26.1.2','nativeSnapshot':native['jars'],'materials':materials,'sharedHephaestusFluids':sorted(shared&set(fluids)),
        'counts':{**written,'recipeFiles':len(list((DATA/'recipe').rglob('*.json')))},'ingotMilliBuckets':90,'oreMilliBuckets':180,
        'fuel':'lava up to 1000, hephaestus:molten_blaze above','traitsMapped':TRAITS,'customTconTraits':'optional native adapter and role markers: see HEPHAESTUS-TRAITS.md',
        'armor':'not available: Hephaestus Tools has no armor parts','runtimeChecked':False})
    print(f"Generated Hephaestus data: {written['materials']} tool materials, {written['alloys']} alloys, "+str(len(list((DATA/'recipe').rglob('*.json'))))+f" recipes ({written['skipped']} left to native Hephaestus)")

if __name__=='__main__':main()
