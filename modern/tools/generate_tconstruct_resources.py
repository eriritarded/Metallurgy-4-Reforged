"""Maintained native TConstruct3 data for its supported Forge1.20.1 branch.

Old exceptions come from import_tconstruct_legacy.py; this never executes a game.
"""
from pathlib import Path
import json,math,re,colorsys
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'forge-1.20.1/src/main/resources'
PACK=BASE/'resourcepacks/tconstruct'
DATA=PACK/'data/metallurgy'

def dump(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def parts(m,source):
    tool=m.get('tool_stats',{});durability=tool.get('durability',1);speed=tool.get('efficiency',0);attack=tool.get('damage',0)
    multiplier=math.sqrt(durability)*.07
    auto={'head':{'durability':max(1,durability//2),'mining_speed':max(0,speed),'melee_attack':max(0,attack),'harvest_level':tool.get('harvest_level',0)},
          'handle':{'multiplier':multiplier*.5 if multiplier>2 else multiplier,'durability':durability//4},
          'extra':{'durability':durability//4},'bow':{'draw_speed':(27-speed)/12.3,'range':speed/12,'bonus_damage':attack/12}}
    if m['name'] in source['overrides']:
        return {k:auto[k] if v.get('auto') else v for k,v in source['overrides'][m['name']].items()}
    return auto if tool else {}

def tier(level):return 'minecraft:'+('wood','stone','iron','diamond','netherite')[level] if level<=4 else 'metallurgy:level_'+str(level)

def stats(parts,m):
    result={}
    if 'head' in parts:
        head=dict(parts['head']);head['mining_tier']=tier(head.pop('harvest_level'));result['tconstruct:head']=head
    if 'handle' in parts:result['tconstruct:handle']={'durability':parts['handle']['multiplier']-1}
    if 'extra' in parts:result['tconstruct:binding']={}
    if 'bow' in parts and 'head' in parts:
        result['tconstruct:limb']={'durability':max(1,parts['head']['durability']),
                                  'draw_speed':1/max(.05,parts['bow']['draw_speed'])-1,'velocity':parts['bow']['range']-1}
    if 'handle' in parts and 'extra' in parts:result['tconstruct:grip']={'durability':parts['handle']['multiplier']-1}
    if 'armor_stats' in m:
        a=m['armor_stats']
        for i,(slot,factor) in enumerate(zip(['boots','leggings','chestplate','helmet'],[13,15,16,11])):
            result['tconstruct:plating_'+slot]={'durability':max(1,math.floor(a['durability']/3*factor+.5)),'armor':a['damage_reduction'][i]}
        result['tconstruct:maille']={}
    return {'stats':result}

PROFILES={
    'midasium':{'head':['writable2','duplication'],'handle':['writable2','duplication'],'extra':['writable2','duplication']},
    'ignatius':{'head':['vulcan']},'vulcanite':{'head':['vulcan'],'handle':['superheat']},
    'eximite':{'head':['tconstruct:enderference']},
    'vyroxeres':{key:['poisonous'] for key in ['head','handle','extra']},
    'damascus_steel':{'head':['dense','duritos'],'handle':['duritos'],'extra':['duritos']},
    'shadow_iron':{key:['obscure'] for key in ['head','handle','extra']},
    'sanguinite':{'head':['lifesteal'],'handle':['lifesteal']},'tartarite':{'head':['wither']},
    'alduorite':{'head':['unnatural'],'extra':['unnatural']},'desichalkos':{'extra':['king_dice']},
    'deep_iron':{key:['opistognathus'] for key in ['head','handle','extra']}}
DESCRIPTIONS={
    'obscure':('Obscure','Hits blind the target briefly. Broken blocks release smoke.'),
    'vulcan':('Vulcan','Ignatius burns targets for3seconds; Vulcanite for5. Effective block drops are autosmelted. Incompatible with Silky and Squeaky.'),
    'lifesteal':('Lifesteal','Player hits have a20% chance, plus20% per rounded luck point, to restore40% of damage dealt as health.'),
    'wither':('Withering','Successful hits have a21% chance to inflict WitherII for1.5seconds.'),
    'king_dice':('King Dice','Successful hits apply one of the original nine random effects for4seconds.'),
    'duplication':('Duplication','Qualifying weapon kills have a 50% chance to duplicate generated mob drops. Does not duplicate player loot.'),
    'opistognathus':('Opistognathus','Underwater mining speed is tripled. While selected underwater, attack speed increases by2.7.'),
    'poisonous':('Poisonous','Successful hits poison surviving targets for101ticks.'),
    'duritos':('Duritos','Durability damage has a10% chance to double, a40% chance to be canceled, and a50% chance to remain unchanged.'),
    'dense':('Dense','Damage prevention improves as durability is lost, following the original cubic probability.'),
    'superheat':('Superheat','Burning targets take35% of the base attack damage as extra damage.'),
    'unnatural':('Unnatural','Effective mining gains speed equal to positive harvest-tier difference.'),
    'legacy_parts':('Fortified','Adds durability from handles and bindings, and damage from bow limbs.'),
    'writable2':('Writable','Adds one upgrade slot when the tool contains a qualifying Midasium part.')}
ARMOR_PROFILES={'quicksilver':'quickly','amordrine':'strongly','kalendrite':'strongly','astral_silver':'jump_master',
                'celenegil':'resistance','deep_iron':'deeply','vulcanite':'volcano','adamantine':'foodly',
                'platinum':'cat_eyes','prometheum':'prometheum','shadow_steel':'blindness'}
ARMOR_DESCRIPTIONS={
    'quickly':('Quickly','Each worn piece accelerates item use through the original four-tick cadence.'),
    'strongly':('Strongly','While worn, grants Strength II.'),
    'jump_master':('Jump Master','While worn, grants Jump Boost II.'),
    'resistance':('Resistance','While worn, grants Resistance II.'),
    'deeply':('Deeply','In water, grants breathing and vision, sinks the wearer and locks armor slots until leaving the water.'),
    'volcano':('Volcano','Extinguishes the wearer each tick.'),
    'foodly':('Foodly','When hungry, consumes four experience points for one food point and saturation once per second.'),
    'cat_eyes':('Cat Eyes','Plating grants night vision while worn.'),
    'prometheum':('Poison Ward','Maille clears poison while worn.'),
    'blindness':('Clear Sight','Clears blindness while worn.'),
    'legacy':('Reinforced Weave','Maille improves armor durability and toughness.')}
DESCRIPTIONS.update({'armor_'+key:value for key,value in ARMOR_DESCRIPTIONS.items()})

def traits(name,native):
    per={};profile=PROFILES.get(name,{})
    for native_part in native['stats']:
        kind=native_part.split(':')[1]
        if kind.startswith('plating_') or kind=='maille':
            power=ARMOR_PROFILES.get(name)
            entries=[]
            if power and not (power=='cat_eyes' and kind=='maille' or power=='prometheum' and kind!='maille'):
                entries.append({'name':'metallurgy:armor_'+power,'level':1})
            if kind=='maille':entries.append({'name':'metallurgy:armor_legacy','level':1})
            per[native_part]=entries
            continue
        legacy={'head':'head','limb':'head','handle':'handle','grip':'handle','binding':'extra'}[kind]
        entries=[{'name':value if ':' in value else 'metallurgy:'+value,'level':1} for value in profile.get(legacy,[])]
        # A single module scans part roles once; its level never multiplies contributions.
        if kind in ['handle','binding','grip','limb']:entries.append({'name':'metallurgy:legacy_parts','level':1})
        per[native_part]=entries
    return {'default':[],'perStat':per}

def main():
    source=json.loads((ROOT/'common/src/main/resources/assets/metallurgy/tconstruct_legacy.json').read_text(encoding='utf-8'))
    catalog=json.loads((ROOT/'content/legacy-catalog.json').read_text(encoding='utf-8'))['materials']
    delegated=source['excludedNativeMaterials']+source['native3ExistingMaterials']
    metals=[m for m in catalog if m['name'] not in delegated]
    own={m['name'] for m in metals}
    dump(PACK/'pack.mcmeta',{'pack':{'pack_format':15,'description':'Metallurgy: native Tinkers Construct3 materials and smeltery'}})
    part_table={}
    def recipe(path,value):dump(DATA/'recipes/tconstruct'/Path(path+'.json'),value)
    def fluid(name,amount):return {'tag':'forge:molten_'+name,'amount':amount}
    def not_empty(tag):return {'type':'forge:not','value':{'type':'forge:tag_empty','tag':tag}}
    # TCon resolves only the first material-fluid match. Delegate its existing
    # Hepatizon/Osmium materials. Shared metal fluids remain canonical, with
    # separate casting tags as data-pack extension points for our40 materials.
    for m in catalog:
        name=m['name']
        dump(PACK/'data/forge/tags/fluids'/('molten_'+name+'.json'),{'replace':False,'values':['metallurgy:'+name]})
        if name in own:dump(PACK/'data/metallurgy/tags/fluids/molten'/(name+'.json'),{'replace':False,'values':['#forge:molten_'+name]})
    for m in metals:
        name=m['name'];p=parts(m,source);native=stats(p,m);part_table[name]=list(native['stats'])
        dump(DATA/'tinkering/materials/definition'/(name+'.json'),{'craftable':False,'hidden':False,'sortOrder':10,'tier':max(1,min(7,p.get('head',{}).get('harvest_level',m['ore_harvest_level'])))})
        dump(DATA/'tinkering/materials/stats'/(name+'.json'),native)
        dump(DATA/'tinkering/materials/traits'/(name+'.json'),traits(name,native))
        folder='metals/'+name+'/'
        for form,needed,value in [('ingot',1,1),('nugget',9,1),('block',1,9)]:
            tag={'ingot':'ingots/','nugget':'nuggets/','block':'storage_blocks/'}[form]+name
            recipe(folder+'material_'+form,{'type':'tconstruct:material','ingredient':{'tag':'forge:'+tag},'material':'metallurgy:'+name,'needed':needed,'value':value,'conditions':[not_empty('forge:'+tag)]})
        # Fluids use native90mB units. Thermodynamic temperatures are explicit Celsius conversions.
        temperature=next((1000+int(m['ore_harvest_level'])*200-300 for _ in [0] if m['ore_harvest_level']>=0),None)
        if temperature is None:
            # The maintained source FluidCatalog preserves the original alloy tier map.
            java=(ROOT/'common/src/main/java/it/hurts/metallurgy_reforged/fluid/FluidCatalog.java').read_text(encoding='utf-8')
            names=re.search(r'String\[\] names=\{([^}]+)\}',java)[1].replace('"','').split(',')
            tiers=[int(x) for x in re.search(r'int\[\] tiers=\{([^}]+)\}',java)[1].split(',')]
            temperature=1000+dict(zip(names,tiers))[name]*200-300
        recipe(folder+'part_casting',{'type':'tconstruct:material_fluid','fluid':{'tag':'metallurgy:molten/'+name,'amount':90},'output':'metallurgy:'+name,'temperature':temperature})
        recipe(folder+'part_melting',{'type':'tconstruct:material_melting','input':'metallurgy:'+name,'result':fluid(name,90),'temperature':temperature})
        for form,tag,amount in [('ingot','ingots',90),('nugget','nuggets',10),('block','storage_blocks',810),('dust','dusts',90),('plate','plates',90),('gear','gears',360),('raw','raw_materials',90),('ore','ores',180)]:
            tag='forge:'+tag+'/'+name
            value={'type':'tconstruct:ore_melting' if form in ['raw','ore'] else 'tconstruct:melting',
                   'ingredient':{'tag':tag},'result':fluid(name,amount),'temperature':temperature,
                   'time':max(1,math.ceil(65*(amount/90)**(1/3))),'conditions':[not_empty(tag)]}
            if form in ['raw','ore']:value['rate']='metal'
            recipe(folder+'melting_'+form,value)
        for form,amount,cooling in [('ingot',90,65),('nugget',10,20),('block',810,194)]:
            value={'type':'tconstruct:casting_basin' if form=='block' else 'tconstruct:casting_table',
                   'fluid':fluid(name,amount),'result':{'tag':'forge:'+('storage_blocks' if form=='block' else form+'s')+'/'+name},'cooling_time':cooling,
                   'conditions':[not_empty('forge:'+('storage_blocks' if form=='block' else form+'s')+'/'+name)]}
            if form!='block':value['cast']={'tag':'tconstruct:casts/multi_use/'+form}
            recipe(folder+'casting_'+form,value)
            if form!='block':
                value={**value,'cast':{'tag':'tconstruct:casts/single_use/'+form},'cast_consumed':True}
                recipe(folder+'casting_'+form+'_sand',value)
        color=int(m['color'],16);r,g,b=((color>>16)&255)/255,((color>>8)&255)/255,(color&255)/255;h,l,s=colorsys.rgb_to_hls(r,g,b)
        palette={}
        for light,scale in [(0,0),(63,.25),(102,.4),(140,.6),(178,.8),(216,1),(255,1.25)]:
            rgb=colorsys.hls_to_rgb(h,min(.9,l*scale),s);palette[f'{light:03}']='FF'+''.join(f'{round(v*255):02X}' for v in rgb)
        dump(BASE/'assets/metallurgy/tinkering/materials'/(name+'.json'),{'color':f'FF{color:06X}','fallbacks':['metal'],
             'generator':{'supported_stats':list(native['stats'])+['tconstruct:repair_kit'],'transformer':{'type':'tconstruct:recolor_sprite','color_mapping':{'type':'tconstruct:grey_to_color','palette':palette}}}})
    java=(ROOT/'common/src/main/java/it/hurts/metallurgy_reforged/machine/MachineRecipes.java').read_text(encoding='utf-8')
    alloys=re.findall(r'new Alloy\("([@\w]+)",(\d+),"([@\w]+)",(\d+),"(\w+)",(\d+),([.\d]+)F\)',java)
    used=set()
    for first,a,second,b,result,count,_ in alloys:
        if result in ['bronze','electrum'] or result in used:continue
        first=first.removeprefix('@').removesuffix('_ingot').removesuffix('_dust');second=second.removeprefix('@').removesuffix('_ingot').removesuffix('_dust')
        recipe('alloys/'+result,{'type':'tconstruct:alloy','inputs':[fluid(first,int(a)*90),fluid(second,int(b)*90)],'result':fluid(result,int(count)*90),'temperature':1000})
        used.add(result)
    recipe('thermite_melting',{'type':'tconstruct:melting','ingredient':{'item':'metallurgy:thermite_dust'},'result':{'fluid':'metallurgy:molten_thermite','amount':1000},'temperature':100,'time':80})
    recipe('thermite_fuel',{'type':'tconstruct:melting_fuel','fluid':{'fluid':'metallurgy:molten_thermite','amount':50},'duration':100,'rate':10,'temperature':3700})
    for name in DESCRIPTIONS:
        # Exact legacy TraitWritable(2) used suffix2 but levels1; its trait flag
        # applied once per tool, not once per material part. Keep that result.
        modules=[{'type':'tconstruct:modifier_slot','name':'upgrades','flat':1}] if name=='writable2' else [{'type':'metallurgy:legacy_trait','effect':name}]
        dump(DATA/'tinkering/modifiers'/(name+'.json'),{'level_display':'tconstruct:single_level','modules':modules})
    lang_path=ROOT/'common/src/main/resources/assets/metallurgy/lang/en_us.json';lang=json.loads(lang_path.read_text(encoding='utf-8'))
    for m in metals:lang['material.metallurgy.'+m['name']]=lang.get('material.metallurgy.'+m['name']+'.name',m['name'].replace('_',' ').title())
    for name,(title,description) in DESCRIPTIONS.items():
        lang['modifier.metallurgy.'+name]=title;lang['modifier.metallurgy.'+name+'.description']=description;lang['modifier.metallurgy.'+name+'.flavor']=title
    lang['modifier.metallurgy.vulcan.conflict']='Vulcan is incompatible with Silky and Squeaky'
    dump(lang_path,lang)
    dump(ROOT/'docs/tconstruct-coverage.json',{'target':'forge-1.20.1','materialDefinitions':len(metals),'partRoles':part_table,'alloys':len(used),
        'recipes':len(list((DATA/'recipes').rglob('*.json'))),'nativeIngotMilliBuckets':90,'nativeOreBaseMilliBuckets':180,'nativeRawBaseMilliBuckets':90,
        'customTraitFamilies':list(DESCRIPTIONS),'sourceExceptions':source['overrides'],'nativeApiRuntimeChecked':False,
        'delegatedNativeMaterials':delegated,'perMaterialCastingTags':True,'canonicalMetalFluidAliases':True,
        'unsupportedTargets':['neoforge-1.21.1','neoforge-26.1.2'],'armorAdaptation':'Original core to native plating; original plates/trim to native maille, with all ten power families implemented separately'})
    print('Generated TConstruct3 data: '+str(len(metals))+' materials, '+str(len(used))+' alloys; '+str(len(list((DATA/'recipes').rglob('*.json'))))+' recipes')

if __name__=='__main__':main()
