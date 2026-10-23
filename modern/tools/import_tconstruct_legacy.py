"""Recover the original TCon2 part rules and their explicit exceptions.

This reads pinned reference sources only. It does not execute legacy code.
"""
from pathlib import Path
import argparse,hashlib,json,math
ROOT=Path(__file__).resolve().parents[1]
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,required=True);args=parser.parse_args()
    base=args.source
    sources=['integration/tic/SetTinkerTraits.java','integration/tic/IntegrationTIC.java','integration/tic/MetallurgyTinkerFuels.java','integration/tic/material/TinkerMetals.java','integration/tic/material/MetallurgyTiCStats.java','util/Utils.java']
    sources+=sorted(p.relative_to(base).as_posix() for p in (base/'integration/conarm').rglob('*.java'))
    auto={'auto':True}
    def head(durability,speed,attack,level):return {'durability':durability,'mining_speed':speed,'melee_attack':attack,'harvest_level':level}
    def handle(multiplier,durability):return {'multiplier':multiplier,'durability':durability}
    data={'sourceRevision':'ef94cb94cfc08ccebe8a0df307ce6b390dcd4704','sourceHashes':{name:hashlib.sha256((base/name).read_bytes()).hexdigest() for name in sources},
          'excludedNativeMaterials':['copper','bronze','zinc','tin','silver','steel','electrum'],
          'native3ExistingMaterials':['hepatizon','osmium'],
          'defaultParts':['head','handle','extra','bow'],
          'overrides':{
              'alduorite':{'head':head(600,14,5,4),'handle':handle(.55,215),'extra':{'durability':40}},
              'infuscolium':{'head':head(180,25,6,3),'handle':handle(.45,190),'extra':{'durability':15},'bow':{'draw_speed':math.sqrt(2),'range':25/12,'bonus_damage':6/12}},
              'rubracium':{'handle':handle(.75,300),'extra':{'durability':47}},
              'midasium':{'head':head(300,10,4,4),'handle':handle(.60,200),'extra':{'durability':30}},
              'tartarite':{'head':head(400,16,10,7),'handle':auto,'extra':auto},
              'platinum':{'head':auto,'handle':handle(1.11,45),'extra':{'durability':157}}},
          'kingDiceEffects':['blindness','levitation','hunger','instant_damage','nausea','night_vision','poison','slowness','regeneration'],
          'fuel':{'fluid':'metallurgy:molten_thermite','amount':50,'duration':100},
          'legacyDependency':{'coordinate':'slimeknights:TConstruct:1.12.2-2.13.0.184','sourcesSha256':'38f03b5008cb0d6cc89a9299d819a461a208d69259c15ecfc2053033f0527a7c'}}
    path=ROOT/'common/src/main/resources/assets/metallurgy/tconstruct_legacy.json'
    path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    print('Imported six explicit part overrides, seven native exclusions, nine Dice effects and original Thermite fuel ratio')
if __name__=='__main__':main()
