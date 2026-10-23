"""Record source types superseded before1.4.1, rather than resurrecting them.

This is a lexical reference inventory, not a proof of arbitrary reflective use.
The reviewed candidates are internal types without automatic subscriber annotations.
"""
from pathlib import Path
import argparse,json,re,hashlib
ROOT=Path(__file__).resolve().parents[1]
CANDIDATES={
 'effect/armor/DesichalkosArmorEffectOld.java','effect/armor/EximiteArmorEffectOld.java',
 'effect/armor/HaderothArmorEffectOld.java','effect/armor/ShadowSteelArmorEffectOld.java',
 'effect/armor/VulcaniteArmorEffectOld.java','effect/tool/AtlarusAxeEffect.java',
 'effect/tool/DeepIronShadowSteelWeaponEffect.java','effect/tool/ShadowSteelToolEffect.java',
 'effect/weapon/VulcaniteIgnatiusSwordEffect.java','entity/ai/AIEndermanPlayerSteal.java',
 'item/gadget/shield/ItemSanguiniteShield.java','integration/tic/trait/MetallurgyTraitDecay.java'}
REVIEWED_DORMANT={('effect/armor/DesichalkosArmorEffectOld.java','capabilities/entity/EntityData.java'):
 'EntityData is registered, but its initEnderman helper only reads the retired borrowableBlocks table; initEnderman is called solely by DesichalkosArmorEffectOld. Registered default capability fields do not instantiate that old effect.'}
def uncomment(text):
    pattern=r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|(/\*[\s\S]*?\*/|//[^\n]*)'
    return re.sub(pattern,lambda match:'' if match.group(1) else match.group(0),text)
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--source',required=True,type=Path);args=parser.parse_args()
 source=args.source.resolve()
 if source.drive.lower()!='h:':raise ValueError('Original reference must stay on H:')
 texts={p.relative_to(source).as_posix():uncomment(p.read_text(encoding='utf-8')) for p in source.rglob('*.java')}
 rows=[]
 for relative in sorted(CANDIDATES):
  path=source/relative;name=path.stem;text=texts[relative]
  references=[p for p,t in texts.items() if p!=relative and re.search(r'\b'+name+r'\b',t)]
  dormant={p:REVIEWED_DORMANT[(relative,p)] for p in references if (relative,p) in REVIEWED_DORMANT}
  external=[p for p in references if p not in CANDIDATES and p not in dormant]
  automatic=bool(re.search(r'@(?:Mod\.)?EventBusSubscriber\b',text))
  if external or automatic:raise ValueError(f'Candidate still has active references: {relative}: {external}, automatic={automatic}')
  rows.append({'source':relative,'sourceSha256':hashlib.sha256(path.read_bytes()).hexdigest(),
   'referencesWithinInertGroup':[p for p in references if p in CANDIDATES],'reviewedDormantReferences':dormant,'activeReferencesOutsideGroup':external,'automaticSubscriberAnnotation':automatic,
   'status':'omitted','reason':'Not instantiated by active1.4.1 initialization; no automatic subscriber annotation. Any dormant helper reference is recorded separately. Current replacement effects are ported separately.'})
 out=ROOT/'docs/inert-source-audit.json'
 out.write_text(json.dumps({'baselineRevision':'ef94cb94cfc08ccebe8a0df307ce6b390dcd4704','method':'Lexical Java type-reference inventory, excluding comments; manual active MetallurgyEffects/ModItems initialization review. Not a claim about arbitrary reflection by third-party mods.','rows':rows},indent=2)+'\n',encoding='utf-8')
 print('Recorded',len(rows),'retired source types; no active outside references')
if __name__=='__main__':main()
