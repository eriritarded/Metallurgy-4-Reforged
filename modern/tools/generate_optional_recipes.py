"""Source-derived optional machine recipes, using each dependency's native JSON.

No game, tests, user configuration, launcher or installation is accessed.
Primary dependency refs and exact JSON examples are pinned in the research record.
"""
from pathlib import Path
import json,re

ROOT=Path(__file__).resolve().parents[1]

def dump(path,value):
    text=json.dumps(value,indent=2)+'\n'
    path.parent.mkdir(parents=True,exist_ok=True)
    if not path.exists() or path.read_text(encoding='utf-8')!=text:path.write_text(text,encoding='utf-8')

def main():
    source=json.loads((ROOT/'content/optional-legacy.json').read_text(encoding='utf-8'))
    materials=json.loads((ROOT/'content/legacy-catalog.json').read_text(encoding='utf-8'))['materials']
    nether=['nether_wastes','crimson_forest','warped_forest','soul_sand_valley','basalt_deltas']
    end=['the_end','end_highlands','end_midlands','small_end_islands','end_barrens']
    colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
    def biome_ids(value):
        out=[]
        for key in value.split(','):
            key=key.strip()
            if not key:continue
            out+=['minecraft:'+p for p in nether] if key=='minecraft:hell' else ['minecraft:'+p for p in end] if key=='minecraft:sky' else [key]
        return list(dict.fromkeys(out))
    counts={}
    for target in ['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']:
        old=target.startswith('forge-');latest=target.endswith('26.1.2')
        base=ROOT/target/'src/main/resources';root=base/'data/metallurgy'/('recipes' if old else 'recipe')/'compat'
        metadata={'description':'Metallurgy native Ender IO alloys'}
        metadata.update({'pack_format':15 if old else 48} if not latest else {'min_format':[101,1],'max_format':[101,1]})
        dump(base/'resourcepacks/enderio/pack.mcmeta',{'pack':metadata})
        ns='forge' if old else 'neoforge';common='forge' if old else 'c';written={}
        def write(module,name,recipe,content=()):
            conditions=[{'type':ns+':mod_loaded','modid':module},{'type':'metallurgy:integration_enabled','integration':module}]
            conditions += [{'type':'metallurgy:content_enabled','content':item} for item in dict.fromkeys(content) if item.startswith('metallurgy:')]
            recipe['conditions' if old else 'neoforge:conditions']=conditions
            destination=root/module/(name+'.json')
            if module=='enderio':destination=base/'resourcepacks/enderio'/destination.relative_to(base)
            dump(destination,recipe);written[module]=written.get(module,0)+1
        ingredient={'ingredient':{'item':'metallurgy:lutetium_ore'}} if old else {'count':1,'ingredient':'metallurgy:lutetium_ore'} if latest else {'count':1,'item':'metallurgy:lutetium_ore'}
        recipe={'type':'mekanism:enriching','input':ingredient,'output':{'item' if old else 'id':'metallurgy:lutetium_dust','count':2}}
        write('mekanism','lutetium_enriching',recipe,['metallurgy:lutetium_ore','metallurgy:lutetium_dust'])
        for alloy in source['enderio']:
            inputs=[]
            for entry in alloy['inputs']:
                tag=common+':'+('ingots' if entry['form']=='ingot' else 'dusts')+'/'+entry['metal']
                item={'ingredient':{'tag':tag},'count':entry['count']} if old else {'ingredient':'#'+tag,'count':entry['count']} if latest else {'tag':tag,'count':entry['count']}
                inputs.append(item)
            recipe={'type':'enderio:alloy_smelting','energy':alloy['energy'],'experience':alloy['experience'],'inputs':inputs,
                    'result' if old else 'output':{'item' if old else 'id':alloy['result'],'count':alloy['count']}}
            write('enderio',alloy['result'].split(':')[1],recipe,[alloy['result']])
        # No compatible26 IF API/data release has been reviewed. Do not emit a
        # guessed foreign serializer for that target.
        if not latest:
            for ore in source['laser']:
                name=re.sub(r'(?<!^)(?=[A-Z])','_',ore['item'][3:]).lower();output='metallurgy:'+name+'_ore'
                rarity=[]
                for row in ore['rarity']:
                    white,black=biome_ids(row['whitelist']),biome_ids(row['blacklist'])
                    entry={k:row[k] for k in ['depth_min','depth_max','weight']}
                    if old:
                        entry.update(whitelist={'type':'minecraft:worldgen/biome','values':white} if white else {},blacklist={'type':'minecraft:worldgen/biome','values':black} if black else {})
                    else:entry.update(biome_filter={'whitelist':white,'blacklist':black},dimension_filter={'whitelist':[],'blacklist':[]})
                    rarity.append(entry)
                lens='industrialforegoing:laser_lens'+str(ore['color']) if old else 'industrialforegoing:'+colors[ore['color']]+'_laser_lens'
                recipe={'type':'industrialforegoing:laser_drill_ore','catalyst':{'item':lens},'output':{'item':output,'count':1},'rarity':rarity}
                if old:recipe['pointer']=0
                write('industrialforegoing',name,recipe,[output])
        if old:
            # Native TConstruct3 defines90mB per ingot. Sharing one metal unit
            # across machines avoids144/90 cross-integration metal duplication.
            ingot,nugget,block=90,10,810
            for material in materials:
                metal=material['name'];fluid='metallurgy:'+metal
                forms=[('nugget',nugget,500),('ingot',ingot,4000),('block',block,36000)]
                if material['ore_harvest_level']>=0:forms.append(('ore',ingot*2,8000))
                for form,amount,energy in forms:
                    item='metallurgy:'+metal+'_'+form
                    write('thermal_expansion','melting/'+metal+'_'+form,{'type':'thermal:crucible','ingredient':{'item':item},'result':[{'fluid':fluid,'amount':amount}],'energy':energy},[item])
                equipment=[]
                if 'tool_stats' in material:equipment += [(form,1,10) for form in ['sword','axe','pickaxe','shovel','hoe']]
                if 'armor_stats' in material:equipment += [('helmet',2,15),('chestplate',4,25),('leggings',3,20),('boots',1,15)]
                for form,amount,chance in equipment:
                    item='metallurgy:'+metal+'_'+form;output='metallurgy:'+metal+'_ingot'
                    write('thermal_expansion','recycling/'+metal+'_'+form,{'type':'thermal:smelter','ingredients':[{'item':'minecraft:sand'},{'item':item}],'result':[{'item':output,'count':amount},{'item':'thermal:slag','chance':chance/100}],'energy':6000},[item,output])
            for item,metal,amount,chance in [('rubracium_gauntlet','rubracium',2,10),('lemurite_shield','lemurite',3,25)]:
                write('thermal_expansion','recycling/'+item,{'type':'thermal:smelter','ingredients':[{'item':'minecraft:sand'},{'item':'metallurgy:'+item}],'result':[{'item':'metallurgy:'+metal+'_ingot','count':amount},{'item':'thermal:slag','chance':chance/100}],'energy':6000},['metallurgy:'+item,'metallurgy:'+metal+'_ingot'])
            write('thermal_expansion','potash_fertilizer',{'type':'thermal:pulverizer','ingredient':{'item':'metallurgy:potash'},'result':[{'item':'metallurgy:potash_fertilizer'}],'energy':4000},['metallurgy:potash_fertilizer'])
            write('thermal_expansion','thermite_fuel',{'type':'thermal:magmatic_fuel','ingredient':{'fluid':'metallurgy:molten_thermite','amount':1000},'energy':200000})
            table=(ROOT/'common/src/main/java/it/hurts/metallurgy_reforged/machine/MachineRecipes.java').read_text(encoding='utf-8')
            rows=re.findall(r'new Alloy\("([@a-z_]+)",(\d+),"([@a-z_]+)",(\d+),"([a-z_]+)",(\d+),([0-9.]+)F\)',table);seen=set()
            def concrete(name):return ('metallurgy:' if name.endswith('_dust') else 'minecraft:')+name[1:] if name.startswith('@') else ('minecraft:' if name in ['iron','gold'] else 'metallurgy:')+name+'_ingot'
            for first,a,second,b,result,count,xp in rows:
                if result in seen:continue
                seen.add(result);a,b,count=int(a),int(b),int(count);item='metallurgy:'+result+'_ingot';outputs=[{'item':concrete(first),'count':a},{'item':concrete(second),'count':b}]
                write('thermal_expansion','separating/'+result,{'type':'thermal:centrifuge','ingredient':{'item':item,'count':count},'result':outputs,'energy':max(a,b)*2000+4000},[item]+[o['item'] for o in outputs])
        counts[target]=written
    dump(ROOT/'docs/optional-recipe-coverage.json',{'sourceRevision':source['sourceRevision'],'state':'Generated native resources, not dependency-decoded or runtime-tested.','targets':counts,'nativeMetalFluidAmounts':{'ingot':90,'nugget':10,'block':810},'limits':['No reviewed IF26 or Thermal21/26 release: those recipes are not fabricated.','Source Ender IO XML editing uses opt-in per-target native JSON folders; see ENDERIO-CUSTOMIZATION.md. Legacy XML is preserved but not parsed.']})
    print(json.dumps(counts))

if __name__=='__main__':main()
