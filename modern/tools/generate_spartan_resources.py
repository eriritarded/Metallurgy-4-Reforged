"""Native API12 models/recipes over the original layered pixel art."""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'forge-1.20.1/src/main/resources'

def dump(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def main():
    data=json.loads((ROOT/'content/spartan-native-models.json').read_text(encoding='utf-8'))
    forms=data['forms'];metals=[m for m in json.loads((ROOT/'content/legacy-catalog.json').read_text(encoding='utf-8'))['materials'] if 'tool_stats' in m]
    reverse={v:k for k,v in forms.items()}
    legacy_models=json.loads((ROOT/'content/spartan-legacy-models.json').read_text(encoding='utf-8'))
    def model_name(value):
        name=value.split('/')[-1].removesuffix('.json').removeprefix('iron_')
        for native in sorted(reverse,key=len,reverse=True):
            if name==native or name.startswith(native+'_'):return reverse[native]+name[len(native):]
        raise ValueError('Unmapped native model '+value)
    for path,entry in data['models'].items():
        name=model_name(path);native=json.loads(json.dumps(entry['model']))
        for override in native.get('overrides',[]):
            value=override['model']
            if value.startswith('spartanweaponry:item/iron_'):override['model']='metallurgy:item/spartanweaponry/templates/'+model_name(value)
        base=next(key for key in sorted(forms,key=len,reverse=True) if name==key or name.startswith(key+'_'))
        suffix=name[len(base):]
        old_name=base+('_custom'+suffix if base in ['longbow','crossbow'] and suffix else '')
        original=legacy_models[old_name]
        # Base/pose transforms and oil-coating loader remain native. Replace
        # only visible layers with the exact original three-layer sprite set.
        textures=native.setdefault('textures',{})
        for layer,value in original.items():
            if not layer.startswith('layer'):continue
            file=value.split(':',1)[1]
            if not (BASE/'assets/metallurgy/textures/spartanweaponry'/(file+'.png')).exists():raise ValueError('Missing original texture '+file)
            textures[layer]='metallurgy:spartanweaponry/'+file
        dump(BASE/'assets/metallurgy/models/item/spartanweaponry/templates'/(name+'.json'),native)
    lang_path=ROOT/'common/src/main/resources/assets/metallurgy/lang/en_us.json';lang=json.loads(lang_path.read_text(encoding='utf-8'))
    for metal in metals:
        name=metal['name'];title=lang.get('material.metallurgy.'+name+'.name',name.replace('_',' ').title());lang['material.metallurgy.'+name]=title
        dump(BASE/'data/metallurgy/tags/spartanweaponry/weapon_traits/materials'/(name+'.json'),{'replace':False,'values':[]})
        for form in forms:
            key=form+'_'+name
            dump(BASE/'assets/metallurgy/models/item'/(key+'.json'),{'parent':'metallurgy:item/spartanweaponry/templates/'+form})
            lang['item.metallurgy.'+key]=title+' '+form.replace('_',' ').title()
    rows=json.loads((ROOT/'content/spartan-legacy-recipes.json').read_text(encoding='utf-8'))
    for row in rows:
        source=row['recipe'];key={}
        for symbol,value in source['key'].items():
            if 'ore' in value:
                if value['ore'] in ['plankWood','logWood']:
                    key[symbol]={'tag':'minecraft:'+{'plankWood':'planks','logWood':'logs'}[value['ore']]};continue
                if value['ore'] in ['stickWood','string']:
                    key[symbol]={'item':'minecraft:'+{'stickWood':'stick','string':'string'}[value['ore']]};continue
                match=re.fullmatch(r'ingot(.+)',value['ore'])
                if not match:raise ValueError('Unmapped original ore ingredient '+str(value))
                name=re.sub(r'(?<!^)(?=[A-Z])','_',match[1]).lower();key[symbol]={'tag':'forge:ingots/'+name}
            elif value['item']=='spartanweaponry:material':key[symbol]={'item':'spartanweaponry:'+{0:'handle',1:'pole'}[value.get('data',0)]}
            elif value['item']=='minecraft:bow':key[symbol]={'item':'minecraft:bow'}
            else:raise ValueError('Unmapped original item ingredient '+str(value))
        result=source['result'];name=result['item'].split(':')[1]
        recipe={'type':'minecraft:crafting_shaped','pattern':source['pattern'],'key':key,
                'result':{'item':result['item'],'count':result.get('count',1)},
                'conditions':[{'type':'forge:mod_loaded','modid':'spartanweaponry'},{'type':'metallurgy:integration_enabled','integration':'spartanweaponry'}]}
        dump(BASE/'data/metallurgy/recipes/compat/spartanweaponry'/(name+'.json'),recipe)
    dump(lang_path,lang)
    dump(ROOT/'docs/spartan-coverage.json',{'target':'forge-1.20.1','dependency':'SpartanWeaponry1.20.1-forge3.2.1/API12','materials':len(metals),'weaponForms':forms,
          'itemAndRecipeCount':len(metals)*len(forms),'recipeSources':len(rows),'nativeModelsAdapted':len(data['models']),
          'nativeDecodingOrClientBehaviorObserved':False,'unsupportedTargets':['neoforge-1.21.1','neoforge-26.1.2'],
          'effects':'Original Spartan items do not implement Metallurgy IToolEffect/IMetalItem; no extra base-equipment powers are invented.'})
    print('Generated'+str(len(rows))+' native Spartan recipes/items and'+str(len(data['models']))+' native layered pose models')

if __name__=='__main__':main()
