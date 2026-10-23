"""Optional Patchouli handbook from maintained defaults and recipes; no Patchouli Java linkage."""
from pathlib import Path
import json
import re

ROOT=Path(__file__).resolve().parents[1]
COMMON=ROOT/'common/src/main/resources'
JAVA=ROOT/'common/src/main/java/it/hurts/metallurgy_reforged'
BOOK='handbook'
BOOK_ID='metallurgy:'+BOOK
TARGETS=['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']

def load(path): return json.loads(path.read_text(encoding='utf-8'))
def dump(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def title(name): return name.removeprefix('@').replace('_',' ').title()
def split_text(text,limit=330):
    text=text.replace('<PC>','%').replace('<NL>','$(br2)').replace('`','')
    words=text.split(); pages=[]; current=[]
    for word in words:
        if current and len(' '.join(current+[word]))>limit:
            pages.append(' '.join(current));current=[]
        current.append(word)
    if current: pages.append(' '.join(current))
    return pages
def compact_heading(heading):
    return {
        "The Metallurgist's Handbook":"Metallurgist's Guide",
        'Moving Items and Experience':'Items and Experience',
        'Repair, Rebirth, and Enchanting':'Equipment Care',
        'Armor: Weight-Controlled Flight':'Weight-Based Flight',
        'Equipment: Photosynthesis':'Photosynthesis',
        'Equipment: Umbral-synthesis':'Umbral-synthesis',
        'Equipment: Paragon absorb':'Paragon Absorption',
        'Astral Silver Block Batch':'Astral Silver Batch'
    }.get(heading,heading)

def text_pages(text,heading=None):
    return [{'type':'patchouli:text',**({'title':compact_heading(heading)} if heading and i==0 else {}),'text':part} for i,part in enumerate(split_text(text))]
def spotlight(item,text,heading=None):
    # Spotlight leaves forty pixels for the icon; keep its first text page shorter.
    parts=split_text(text,250)
    return [{'type':'patchouli:spotlight','item':item,**({'title':compact_heading(heading)} if heading else {}),'text':parts[0]}]+[{'type':'patchouli:text','text':part} for part in parts[1:]]

def material_entries(catalog,lang):
    profiles={p['name']:p for p in load(COMMON/'assets/metallurgy/legacy_worldgen.json')['ores']}
    roster=(JAVA/'config/EffectsRosterConfig.java').read_text(encoding='utf-8')
    effects={}
    for camel,category in re.findall(r'boolean (\w+)Effect(\w+) = true',roster):
        name=re.sub(r'([a-z])([A-Z])',r'\1_\2',camel).lower()
        effects.setdefault(name,[]).append(category.lower())
    # Read the exact stock alloy equations, including the separate Angmallen forms.
    alloys={}
    source=(JAVA/'machine/MachineRecipes.java').read_text(encoding='utf-8')
    for a,ac,b,bc,result,count in re.findall(r'new Alloy\("([^"]+)",(\d+),"([^"]+)",(\d+),"([^"]+)",(\d+),',source):
        alloys.setdefault(result,[]).append(f'{ac} {title(a)} + {bc} {title(b)} = {count} {title(result)}')
    entries=[]
    for material in catalog['materials']:
        name=material['name']; label=title(name); icon=f'metallurgy:{name}_ingot'; pages=[]
        if name in profiles:
            ore=profiles[name]; realm={'overworld':'Overworld','nether':'Nether','end':'End'}[ore['dimension']]
            origin=f'Default deposits: {realm}, Y {ore["min_y"]} to {ore["max_y"]}. Veins target up to {ore["vein_size"]} blocks, with {ore["attempts"]} placement attempts per chunk. Mining requirement: level {material["ore_harvest_level"]}.'
            if ore.get('biomes'): origin+=' Some deposits also have biome restrictions.'
            pages+=spotlight(f'metallurgy:{name}_ore',origin,'Finding '+label)
            pages+=text_pages(f'Smelt {label} Ore or {label} Dust into ingots. Crushing the ore first normally yields two dusts. Ingots can be stored as metal blocks or converted into the material\'s other crafting forms.','Working '+label)
        elif name in alloys:
            pages+=spotlight(icon,'Make this alloy in an Alloyer:$(br2)'+'$(br2)'.join(alloys[name]),'Making '+label)
            pages+=text_pages('Supply furnace fuel and the complete ingredient amounts. Most alloy inputs accept ingots or dusts; the Angmallen fallback uses matching forms. Loaded recipes can change these stock equations.','Alloying Notes')
        else:
            pages+=spotlight(icon,f'{label} is one of Metallurgy\'s source materials. Use the current recipe display for its available acquisition and processing routes. Material and world settings can change which routes are enabled.',label)
        tool=material.get('tool_stats')
        if tool:
            text=f'Durability: {tool["durability"]} uses.$(br)Harvest level: {tool["harvest_level"]}.$(br)Mining efficiency: {tool["efficiency"]}.$(br)Material damage bonus: {tool["damage"]}.$(br)Enchantability: {tool["enchantability"]}.$(br2)These are the supplied material values. The item type, enchantments, and active traits affect final performance.'
            pages+=spotlight(f'metallurgy:{name}_pickaxe',text,'Default Tool Stats')
        armor=material.get('armor_stats')
        if armor:
            boots,legs,chest,helmet=armor['damage_reduction']
            text=f'Protection: helmet {helmet}, chestplate {chest}, leggings {legs}, boots {boots}.$(br2)Toughness per piece: {armor.get("toughness",0)}.$(br)Enchantability: {armor["enchantability"]}.'
            if armor.get('max_health'): text+=f'$(br)Extra full-set health: {armor["max_health"]} HP.'
            pages+=spotlight(f'metallurgy:{name}_chestplate',text,'Default Armor Stats')
        for category in effects.get(name,[]):
            key=f'tooltip.metallurgy.effect.{name}_{category}'
            if key not in lang or key+'.tooltip' not in lang: raise ValueError('Missing active effect text '+key)
            description=lang[key+'.tooltip']
            # Clarify the direct-melee rule added for issue388, not a generic held-item bonus.
            if (name,category)==('amordrine','weapon'):
                description+=' Only a direct melee attack qualifies; arrows, projectiles, and spells do not inherit this bonus from a held weapon.'
            if (name,category)==('platinum','all'):
                description='Tools become unbreakable when equipped in the main hand. This trait does not make Platinum armor unbreakable.'
            if (name,category)==('tartarite','all'):
                description='Use the Paragon cross recipe to absorb a suitable material. The top metal block selects it. Tartarite armor counts twice for eligible absorbed armor powers, up to an effective level of four.'
            heading=('Equipment' if category=='all' else category.title())+': '+lang[key]
            pages+=text_pages(description,heading)
        entry={'id':'materials/'+name,'name':label,'category':'materials','icon':icon,'pages':pages}
        entries.append(entry)
    return entries

def main():
    topics=load(ROOT/'content/handbook-topics.json'); catalog=load(ROOT/'content/legacy-catalog.json')
    lang=load(COMMON/'assets/metallurgy/lang/en_us.json')
    materials=material_entries(catalog,lang)
    common_assets=COMMON/'assets/metallurgy'
    # Reuse the original pixel-art book cover; this is not a replacement wiki item.
    dump(common_assets/'models/item/handbook.json',{'parent':'minecraft:item/generated','textures':{'layer0':'metallurgy:items/wiki'}})
    results=[]
    for target in TARGETS:
        old=target=='forge-1.20.1'; latest=target=='neoforge-26.1.2'; loader='forge' if old else 'neoforge'
        base=ROOT/target/'src/main/resources'; recipes=base/'data/metallurgy'/('recipes' if old else 'recipe')
        assets=base/'assets/metallurgy/patchouli_books'/BOOK/'en_us'
        book={'name':"The Metallurgist's Handbook",'landing_text':'A practical guide to metals, machines, equipment, and the integrations installed in this world. Craft a copy with a Book and a Minecraft Copper Ingot.',
            'version':1,'use_resource_pack':True,'model':'metallurgy:handbook','creative_tab':'metallurgy:metallurgy',
            'show_progress':False,'show_toasts':False,'use_blocky_font':True,'text_overflow_mode':'resize',
            'book_texture':'patchouli:textures/gui/book_brown.png','header_color':'713D27','link_color':'704427','link_hover_color':'B36B27'}
        dump(base/'data/metallurgy/patchouli_books'/BOOK/'book.json',book)
        if latest: dump(base/'assets/metallurgy/items/handbook.json',{'model':{'type':'minecraft:model','model':'metallurgy:item/handbook'}})
        # Conditions are evaluated before an absent Patchouli item/serializer is decoded.
        recipe={loader+':conditions':[{'type':loader+':mod_loaded','modid':'patchouli'}]}
        if old:
            recipe.update(type='patchouli:shapeless_book_recipe',ingredients=[{'item':'minecraft:book'},{'item':'minecraft:copper_ingot'}],book=BOOK_ID)
        else:
            recipe.update(type='minecraft:crafting_shapeless',category='misc',ingredients=['minecraft:book','minecraft:copper_ingot'] if latest else [{'item':'minecraft:book'},{'item':'minecraft:copper_ingot'}],
                result={'id':'patchouli:guide_book','count':1,'components':{'patchouli:book':BOOK_ID}})
        dump(recipes/'handbook.json',recipe)
        integrations=[i for i in topics['integrations'] if target in i['targets']]
        for index,category in enumerate(topics['categories']):
            dump(assets/'categories'/(category['id']+'.json'),{k:v for k,v in category.items() if k!='id'}|{'sortnum':index})
        dump(assets/'categories/integrations.json',{'name':'Installed Integrations','description':'How the installed companion mods work with Metallurgy.','icon':'metallurgy:electrum_ingot','sortnum':7,
            'flag':'|'+','.join('mod:'+i['mod'] for i in integrations)})
        # Only native crafting recipes are passed to Patchouli's crafting page type.
        crafting={}
        for file in sorted(recipes.rglob('*.json')):
            value=load(file)
            if not value.get('type','').startswith('minecraft:crafting_'): continue
            result=value.get('result',{})
            item=result if isinstance(result,str) else result.get('id',result.get('item'))
            if item: crafting.setdefault(item,[]).append('metallurgy:'+file.relative_to(recipes).with_suffix('').as_posix())
        entries=list(topics['entries'])+materials
        for metal,slots in catalog['chests'].items():
            entries.append({'id':'storage/'+metal,'name':title(metal)+' Chest','category':'storage','icon':'metallurgy:'+metal+'_chest',
                'text':[f'This tier holds {slots} slots. Its full inventory is available to normal chest interactions, hoppers, and compatible item pipes. Use the recipe below to build this storage tier.'], 'craft_item':'metallurgy:'+metal+'_chest'})
        for integration in integrations:
            entries.append({'id':'integrations/'+integration['id'],'name':integration['name'],'category':'integrations','icon':integration['icon'],
                'text':integration['text'],'flag':'mod:'+integration['mod']})
        count=0; page_count=0
        for item in entries:
            pages=list(item.get('pages',[]))
            for i,paragraph in enumerate(item.get('text',[])):
                pages+=(spotlight(item['icon'],paragraph,item['name']) if i==0 else text_pages(paragraph))
            if item.get('append_chamber_batches'):
                for file in sorted((recipes/'processing/chamber').glob('*.json')):
                    value=load(file); ingredient=value['ingredients'][0]
                    if isinstance(ingredient,dict): ingredient=ingredient.get('tag',ingredient.get('item',''))
                    ingredient_id=ingredient.removeprefix('#'); material=ingredient_id.split(':')[-1].split('/')[-1]
                    label=title(material)
                    effect=value['effect'].split(':')[-1].replace('_',' ').title(); amount=value.get('amounts',[1])[0]
                    icon=ingredient_id if not ingredient.startswith('#') else 'metallurgy:'+material
                    pages+=spotlight(icon,f'{amount} {label} supplies {effect} I for {value["duration"]/20:g} seconds per batch. The chamber applies level I auras, following its original behavior. Current server recipes take priority.',label+' Batch')
            wanted=item.get('craft_items',[])+([item['craft_item']] if 'craft_item' in item else [])
            mapping={}
            for output in wanted:
                recipe_ids=['metallurgy:handbook'] if output=='patchouli:guide_book' else crafting.get(output,[])[:2]
                if recipe_ids:
                    # A bare guide_book key would also identify other mods' books.
                    if output!='patchouli:guide_book':mapping[output]=len(pages)
                    page={'type':'patchouli:crafting','recipe':recipe_ids[0]}
                    if len(recipe_ids)>1:page['recipe2']=recipe_ids[1]
                    pages.append(page)
            entry={'name':item['name'],'category':'metallurgy:'+item['category'],'icon':item['icon'],'pages':pages}
            if 'flag' in item:entry['flag']=item['flag']
            if mapping:entry['extra_recipe_mappings']=mapping
            dump(assets/'entries'/(item['id']+'.json'),entry); count+=1; page_count+=len(pages)
        results.append({'target':target,'entries':count,'pages':page_count,'materials':len(materials),'integrations':[i['mod'] for i in integrations],
            'optionalPatchouli':True,'recipe':'metallurgy:handbook','book':BOOK_ID})
    dump(ROOT/'docs/handbook-content.json',{'source':'content/handbook-topics.json + maintained material/worldgen/effect/recipe defaults','targets':results,'renderingObserved':False})
    print(json.dumps(results))

if __name__=='__main__':main()
