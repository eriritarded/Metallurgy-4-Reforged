"""Write source-defined Hephaestus trait markers/tooltips; no builds or deletions."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'common/src/main/resources/assets/metallurgy/hephaestus_traits.json'
DESCRIPTIONS={
    'writable':('Writable','Unlocks one additional upgrade slot early, once per tool, up to the native five-slot limit.'),
    'duplication':('Duplication','Successful direct melee kills have a 50% chance to duplicate non-player item drops. Controlled by the duplication setting.'),
    'vulcan':('Vulcan','Ignatius heads ignite targets for 3 seconds; Vulcanite heads for 5. Effective block drops autosmelt. Silk Touch takes priority.'),
    'superheat':('Superheat','Deals 35% additional incoming attack damage to burning targets on direct melee hits.'),
    'enderference':('Enderference','Successful direct melee hits prevent Endermen from teleporting for 5 seconds.'),
    'poisonous':('Poisonous','Successful hits poison surviving targets for 101 ticks.'),
    'dense':('Dense (Tinkers)','As the tool wears down, a cubic chance reduces durability loss. This is the original Tinkers trait, not Hephaestus bonus maximum durability.'),
    'duritos':('Duritos','Each wear operation has a 10% chance of extra wear, 40% chance of canceled wear and 50% chance of normal wear.'),
    'obscure':('Obscure','Hits inflict Blindness III for 30 ticks. Breaking blocks releases smoke.'),
    'lifesteal':('Lifesteal','20% chance, plus 20% per rounded luck point, to heal 40% of health damage dealt.'),
    'wither':('Wither','Successful hits have a 21% chance to inflict Wither II for 30 ticks.'),
    'unnatural':('Unnatural','Effective mining gains 1 speed for each harvest level above the block requirement.'),
    'king_dice':('King Dice','Successful hits apply one of nine original effects for 80 ticks. Not every roll helps you.'),
    'opistognathus':('Opistognathus','While in water, the selected tool mines at 3 times speed and grants +2.7 attack speed.')
}

def write(path,value):
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def main():
    assignments=json.loads(SOURCE.read_text(encoding='utf-8'))['materials']
    folder=ROOT/'neoforge-26.1.2/src/main/resources/resourcepacks/hephaestus/data/metallurgy/hephaestus_tools/materials'
    changed=[]
    for name,roles in assignments.items():
        path=folder/(name+'.json')
        if not path.is_file():continue
        data=json.loads(path.read_text(encoding='utf-8'))
        for key,effects in zip(['head_stats','handle_stats','binding_stats'],roles):
            if key in data:
                data[key].pop('trait',None)
                if effects:data[key]['trait']='metallurgy:'+name
        write(path,data);changed.append(name)
    language=ROOT/'common/src/main/resources/assets/metallurgy/lang/en_us.json'
    data=json.loads(language.read_text(encoding='utf-8'))
    for key,(name,description) in DESCRIPTIONS.items():
        data['trait.metallurgy.hephaestus.'+key]=name
        data['trait.metallurgy.hephaestus.'+key+'.desc']=description
    data['effect.metallurgy.enderference']='Enderference'
    write(language,data)
    print(json.dumps({'materialDefinitionsMarked':changed,'distinctTraits':len(DESCRIPTIONS)}))

if __name__=='__main__':main()
