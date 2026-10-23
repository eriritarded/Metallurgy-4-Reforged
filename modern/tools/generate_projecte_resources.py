"""Native20/21 custom EMC conversion packs. No absent26 API is invented."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
def dump(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
def main():
    data=json.loads((ROOT/'content/projecte-values.json').read_text(encoding='utf-8'))
    materials=json.loads((ROOT/'content/legacy-catalog.json').read_text(encoding='utf-8'))['materials']
    values={'metallurgy:'+name:value for name,value in data['extras'].items()};conversions=[]
    for metal in materials:
        name=metal['name'];value=data['metals'][name]
        for form,amount in [('ingot',value),('dust',value),('nugget',value//9),('block',value*9)]:values['metallurgy:'+name+'_'+form]=amount
        if 'armor_stats' in metal:
            for form,count in data['armorMetalCounts'].items():values['metallurgy:'+name+'_'+form]=count*value
        if 'tool_stats' in metal:
            for form,count in data['toolMetalCounts'].items():conversions.append({'ingredients':{'metallurgy:'+name+'_ingot':count,'minecraft:stick':data['sticksPerTool']},'output':'metallurgy:'+name+'_'+form})
    for target,format in [('forge-1.20.1',15),('neoforge-1.21.1',48)]:
        old=target.startswith('forge-');base=ROOT/target/'src/main/resources/resourcepacks/projecte'
        dump(base/'pack.mcmeta',{'pack':{'description':'Metallurgy original ProjectE EMC values','pack_format':format}})
        native={'before':values,'conversion':conversions} if old else {'before':[{'type':'projecte:item','id':id,'emc_value':value} for id,value in values.items()],'conversion':[{'ingredients':[{'type':'projecte:item','id':id,'amount':count} for id,count in c['ingredients'].items()],'output':{'type':'projecte:item','id':c['output']}} for c in conversions]}
        dump(base/'data/metallurgy/pe_custom_conversions/metallurgy.json',{'comment':'Original Metallurgy1.4.1 EMC values. Tools use the current native stick EMC; see PROJECTE.md.','values':native})
    dump(ROOT/'docs/projecte-coverage.json',{'implementedTargets':['forge-1.20.1','neoforge-1.21.1'],'unreviewedTarget':'neoforge-26.1.2','materialBases':len(data['metals']),'fixedValues':len(values),'toolConversions':len(conversions),'dependencyDecodingChecked':False,'configuration':'Dependency/config-gated built-in pack, filtered by runtime content availability'})
    print(f'Generated ProjectE packs: {len(values)} fixed values, {len(conversions)} tool conversions per supported target')
if __name__=='__main__':main()
