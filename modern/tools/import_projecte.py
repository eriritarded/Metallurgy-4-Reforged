"""Recover the original49 EMC bases; source is read-only and never extracted here."""
from pathlib import Path
import argparse,json,re,hashlib
ROOT=Path(__file__).resolve().parents[1]
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,required=True);args=parser.parse_args()
    raw=args.source.read_bytes();text=raw.decode('utf-8')
    metals={name:int(value) for name,value in re.findall(r'emcMap.put\("(\w+)",\s*(\d+)L\)',text)}
    data={'sourceRevision':'ef94cb94cfc08ccebe8a0df307ce6b390dcd4704','source':'integration/IntegrationProjectE.java','sourceSha256':hashlib.sha256(raw).hexdigest(),'metals':metals,'extras':{'iron_dust':256,'gold_dust':2048,'potash':128,'potash_fertilizer':256,'phosphorus':128,'bitumen':256,'tar':128},'toolMetalCounts':{'axe':3,'hoe':2,'pickaxe':3,'shovel':1,'sword':2},'sticksPerTool':2,'armorMetalCounts':{'helmet':5,'chestplate':8,'leggings':7,'boots':4}}
    (ROOT/'content/projecte-values.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    print(f'Recovered {len(metals)} ProjectE material base values')
if __name__=='__main__':main()
