"""Native dimension-aware ore injection and extensible block targets."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
def dump(path,value):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def main():
    for target in ['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']:
        old=target.startswith('forge-');base=ROOT/target/'src/main/resources';loader='forge' if old else 'neoforge'
        for realm in ['overworld','nether','end']:
            dump(base/'data/metallurgy'/(loader+'/biome_modifier')/(realm+'_ores.json'),{'type':'metallurgy:ore_realms','feature':'metallurgy:'+realm+'_ores'})
            dump(base/'data/metallurgy/tags'/('blocks' if old else 'block')/'ore_replaceables'/(realm+'.json'),{'replace':False,'values':[]})
    print('Generated native all-biome ore realm injection and optional terrain target tags')
if __name__=='__main__':main()
