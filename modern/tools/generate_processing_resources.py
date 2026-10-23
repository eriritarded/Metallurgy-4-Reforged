"""Materialize the source machine tables as removable native data recipes."""
from pathlib import Path
import json,re

ROOT=Path(__file__).resolve().parents[1]

def dump(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')

def main():
    java=ROOT/'common/src/main/java/it/hurts/metallurgy_reforged/machine'
    alloy=re.findall(r'new Alloy\("([@\w]+)",(\d+),"([@\w]+)",(\d+),"(\w+)",(\d+),([.\d]+)F\)',(java/'MachineRecipes.java').read_text(encoding='utf-8'))
    chamber=re.findall(r'new Recipe\("(\w+)",(\d+),"(\w+)",(\d+),(\d+)\)',(java/'SublimationRecipes.java').read_text(encoding='utf-8'))
    materials=json.loads((ROOT/'content/legacy-catalog.json').read_text(encoding='utf-8'))['materials']
    counts={}
    for target in ['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']:
        old=target.startswith('forge-');latest=target.endswith('26.1.2');loader='forge' if old else 'neoforge'
        base=ROOT/target/'src/main/resources/data/metallurgy';out=base/('recipes' if old else 'recipe')/'processing';tags=base/'tags'/('items' if old else 'item')/'processing'
        def optional(value):return {'id':value,'required':False}
        def tag(name,values):
            dump(tags/(name+'.json'),{'replace':False,'values':values})
            return '#metallurgy:processing/'+name
        def common(path):return [optional('#'+ns+':'+path) for ns in ['forge','c']]
        def ingredient(value):return value if latest else {'tag':value[1:]} if value.startswith('#') else {'item':value}
        def write(name,kind,inputs,amounts,result='',count=1,xp=0,**extra):
            data={'type':'metallurgy:processing','kind':kind,'ingredients':[ingredient(i) for i in inputs],'amounts':amounts}
            if result:data.update(result={'item' if old else 'id':result,'count':count},experience=xp)
            data.update(extra)
            conditions=[]
            if result.startswith('metallurgy:'):conditions.append({'type':'metallurgy:content_enabled','content':result})
            for value in inputs:
                if value.startswith('#'):conditions.append({'type':loader+':not','value':{'type':loader+':tag_empty','tag':value[1:]}})
            if conditions:data['conditions' if old else 'neoforge:conditions']=conditions
            dump(out/(name+'.json'),data)
        for m in materials:
            name=m['name'];own='metallurgy:'+name
            ingots=tag('crusher_ingots/'+name,[own+'_ingot',*common('ingots/'+name)])
            raw=tag('crusher_raw/'+name,[*([optional('minecraft:raw_'+name)] if name in ['iron','gold','copper'] else []),*common('raw_materials/'+name)])
            write('crusher/'+name+'_from_ingot','crusher',[ingots],[1],own+'_dust',1,0)
            write('crusher/'+name+'_from_raw','crusher',[raw],[1],own+'_dust',2,.75)
            if m['ore_harvest_level']>=0:
                ores=tag('crusher_ores/'+name,[own+'_ore',*common('ores/'+name)])
                write('crusher/'+name+'_from_ore','crusher',[ores],[1],own+'_dust',2,.75)
            tag('alloyer/'+name,[own+'_ingot',own+'_dust',*common('ingots/'+name),*common('dusts/'+name)])
        for name in ['iron','gold']:
            own='metallurgy:'+name
            tag('alloyer/'+name,['minecraft:'+name+'_ingot',own+'_dust'])
            for form,items,count,xp in [('ingot',['minecraft:'+name+'_ingot',*common('ingots/'+name)],1,0),('raw',['minecraft:raw_'+name,*common('raw_materials/'+name)],2,.75),('ore',['minecraft:'+name+'_ore','minecraft:deepslate_'+name+'_ore',*common('ores/'+name)],2,.75)]:
                value=tag('crusher_'+form+'s/'+name,items)
                write('crusher/'+name+'_from_'+form,'crusher',[value],[1],own+'_dust',count,xp)
        for index,(first,a,second,b,result,count,xp) in enumerate(alloy):
            def input_value(value):
                if value.startswith('@'):
                    item=value[1:];return ('metallurgy:' if item.endswith('_dust') else 'minecraft:')+item
                return '#metallurgy:processing/alloyer/'+value
            write('alloyer/'+result+('_dust' if first.endswith('_dust') else ''),'alloyer',[input_value(first),input_value(second)],[int(a),int(b)],'metallurgy:'+result+'_ingot',int(count),float(xp))
        for metal,amount,effect,duration,amplifier in chamber:
            write('chamber/'+metal,'chamber',['metallurgy:'+metal+'_block'],[int(amount)],effect='minecraft:'+effect,duration=int(duration),amplifier=int(amplifier))
        for input,output,count,xp in [('tar_ore','tar',4,.75),('phosphorite_ore','phosphorus',4,.75),('potash_ore','potash',4,.75),('sulfur_ore','sulfur_dust',4,.75),('potash','potash_fertilizer',1,1)]:
            write('crusher/'+input,'crusher',['metallurgy:'+input],[1],'metallurgy:'+output,count,xp)
        write('crusher/generic','generic_crusher',[],[])
        counts[target]=len(list(out.rglob('*.json')))
    dump(ROOT/'docs/processing-recipe-coverage.json',{'schema':'metallurgy:processing','targets':counts,'defaultAlloyEquations':len(alloy),'defaultChamberRecipes':len(chamber),'genericFallback':'Single removable recipe; explicit default metals excluded','decodingOrRuntimeChecked':False})
    print('Generated native machine recipes: '+str(counts))

if __name__=='__main__':main()
