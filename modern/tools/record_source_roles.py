"""Record reviewed legacy support roles and target replacements, without runtime claims."""
from pathlib import Path
import argparse,hashlib,json,re
ROOT=Path(__file__).resolve().parents[1]
GROUPS=[]
def group(names,role,common,native,limits,targets=None,status='adapted'):
    GROUPS.append(dict(names=set(names.split()),role=role,common=common.split(),native=native.split(),limits=limits,targets=targets,status=status))
group('Metallurgy ModBlocks BlockTypes ModItems ItemTypes RegistrationHandler TileEntityHandler GuiHandler IProxy ServerProxy ClientProxy ModRenderers PacketManager MetallurgyTabs ModChecker SubEvent',
 'Native lifecycle, registry/content identities, menus, client registration and optional-mod gates',
 'config/ContentAvailability.java', 'Metallurgy.java content/Content.java client/ClientSetup.java',
 'Modern mod/event buses replace sided proxies and numeric GUI/packet registration. Base mod does not load optional client/API implementations on dedicated sides. Configured category removals preserve IDs; native resource/data adapters replace1.12 model and OreDictionary registration. Actual loader startup remains unobserved.')
group('BlockMetal BlockOre', 'Native block properties, drops, reinforced glass, beacon eligibility and ore display',
 'material/Materials.java', 'content/Content.java client/OreParticles.java',
 'Native block/tag/loot resources replace BlockMaterial and isBeaconBase callbacks. Ore drops preserve source uniform count/chance. Beacon raw-metal blocks use source tier>1 rule. Glass face culling uses native same-block policy. Native terrain/rendering unobserved.')
group('ConfigPack GeneralConfig GadgetsConfig RegistrationConfig', 'Validated restart-scoped JSON settings and non-destructive config lifecycle',
 'config/ConfigFiles.java config/GeneralConfig.java config/GadgetsConfig.java config/RegistrationConfig.java', 'config/ConfigBootstrap.java',
 'New schema preserves user files rather than resetting an entire1.12 config pack on a version-prefix change. No automatic.cfg migration, in-game config GUI or common-settings network synchronization. CONFIGURATION.md records consumers and restrictions.')
group('BaseMetallurgyEffect EnumEffectCategory IProgressiveEffect MetallurgyEffects EventHandler ProgressiveEffectsHandler',
 'Source effect/category selection and authoritative progressive callbacks',
 'effect/EquipmentEffects.java effect/WeaponLifecycle.java effect/ArmorLifecycle.java effect/ArmorControls.java effect/ToolEffects.java', 'effect/EffectEvents.java',
 'Explicit source-derived dispatch replaces reflective handler wrappers. Native state/cooldown/buff packets replace generic client bundle simulation. The old DroneFakePlayer capability crash bypass is unnecessary because the new dispatcher does not dereference the old player capability. Current multiplayer timing is unverified.')
group('ClientEventsHandler GadgetsHandler', 'Native key intents, corpse rendering, detector/goggles, roads and shield controls',
 'effect/ArmorControls.java effect/CorpseLifecycle.java gadget/RoadMovement.java gadget/ShieldLogic.java', 'client/ArmorInput.java client/DetectorDisplay.java client/GadgetVisuals.java',
 'Server validates equipment/jump/height and actual shield authority. Current native rendering/input APIs replace global GL and input hijacking. See CLIENT-VISUALS.md and GADGETS.md; compilation is not visual evidence.')
group('FuelHandler','Native furnace and machine fuel values',
 'config/GadgetsConfig.java', 'config/GeneralEvents.java',
 'Tar800 ticks, charcoal block16000, configured Thermite/Ignatius fuel values times200. Native furnace event dispatch replaces1.12 subscriber; no live burn check.')
group('OreDictHandler', 'Source metal families and chest aliases through native tags',
 '', '', 'forge tags on20 and c tags on newer targets replace OreDictionary. Metallurgy tags supply deterministic own families; source tar slimeball and metal-chest aliases retained. Generic crusher ownership/removal is documented in PROCESSING-RECIPES.md.')
group('IMetalItem ItemBase ItemExtra ItemMetal IToolEffect EnumTools ItemArmorBase ItemAxeBase ItemHoeBase ItemPickaxeBase ItemShovelBase ItemSwordBase ArmorStats JsonMaterialHelper Metal MetalStats ModMetals ToolStats',
 'Original material catalog, per-form native equipment, modifiers/repair and stats/tooltips',
 'material/Materials.java material/MaterialItems.java effect/EquipmentStats.java effect/EquipmentEffects.java client/MaterialTooltips.java', 'content/Content.java',
 '49 original definitions,39 tool sets and41 armor sets. Source per-slot equations and optional attributes mapped to target APIs. Validated overrides retain IDs and source defaults. Native data components replace custom NBT where required. No callers exist for original default-enchantment preset setters, so none are invented. No1.12 world/save conversion.')
group('Drop MetalSample IngredientMetal IRecipeMetal ModRecipes TartariteParagonRecipe',
 'Counted native loot/ingredients, crafting/smelting resources and nine-form Paragon crafting',
 'machine/MachineRecipes.java gadget/DetectorCrafting.java effect/Paragon.java', 'recipe/ParagonRecipe.java recipe/ProcessingAccess.java',
 'Native tags replace OreDictionary and per-metal virtual recipes. Source top selector and mixed side-block behavior remain; result copies damage/state.897 Spartan recipes are tracked separately.1.12 recipe factories are definitions, not missing recipes. Native recipe decoding unverified.')
group('BlockUtils Constants EventUtils IntBiFunction ItemUtils NBTUtils Utils WorldUtils',
 'Source utility roles distributed to native data, effect, world and UI adapters',
 'material/Materials.java effect/EquipmentEffects.java effect/EquipmentStats.java effect/ToolEffects.java effect/State.java client/MaterialTooltips.java', 'effect/VersionBridge.java effect/ToolBridge.java',
 'RGB/localization/modifier/XP/harvest/material/geometry and persistent-state operations use target APIs. Native recipes replace old shaped parser; tag families replace wildcard metadata. Tunneling uses bounded source-range sampling/protection checks. Unknown mod items cannot spoof base powers by name. Legacy JAR filesystem copying is replaced with classpath streams and preserved user config.')
group('BlockTileEntity ContainerNull', 'Native block-entity lifecycle and no-menu chamber interaction',
 '', 'machine/MachineBlock.java machine/ChamberBlock.java machine/ChamberEntity.java',
 'The chamber uses direct sided insertion/extraction and packed block-item persistence. It does not open the old dummy ContainerNull. Crusher/alloyer use actual menus. No inventory contents are copied into a fake client menu.')
group('BlockInfoDataBundle EffectDataProvider EffectDataStorage ExtraFilledDataBundle PlayerEffectData ProgressiveDataBundle IPunchEffect PunchEffectProvider PunchEffectStorage',
 'Native entity/item state and owned timer/punch lifecycle',
 'effect/State.java effect/WeaponLifecycle.java effect/ArmorLifecycle.java effect/ArmorControls.java gadget/GadgetLogic.java', 'effect/VersionBridge.java',
 'Server state and item components replace the old capability bundle API. Dirty transactions save only mutations; explicit phase/corpse/height packets synchronize needed presentation. Timers and cleanup are defined in EQUIPMENT.md/GADGETS.md. Recall queues and held object identity have deliberate transient lifetimes; no1.12 NBT migration.')
group('PacketAdamantineRenderDeathProtection PacketMetallurgyExplosion PacketSpawnVanillaParticles',
 'Native bounded visual packets and native particle delivery',
 'visual/ParticleBurst.java visual/ExplosionVisual.java visual/EffectParticles.java', 'network/VisualNetwork.java client/ArmorActivation.java client/ExplosionParticles.java',
 'Visual protocol4 covers bounded seed/motion/emitter/sample/activation. Native vanilla particle packets cover cloud/portal/drips. Position/count caps replace unbounded legacy per-particle packet loops. Rendering/network delivery unobserved.')
group('PacketSanguiniteEntityState PacketSyncEffectBundle PacketAmordrineJump PacketKrikEditPlayerLevel',
 'Server-derived armor action and client presentation state',
 'effect/ArmorControls.java effect/CorpseLifecycle.java effect/ArmorPhasing.java', 'network/ArmorNetwork.java network/VisualNetwork.java',
 'Client sends intent only; server derives jump budget and clamps Krik height. The old decoder discarded increase and trusted maxJumps. Generic sync omitted its bundle type; explicit payloads avoid those source defects. No generic client timer authority; current packet delivery unverified.')
group('IntegrationJEI OreDetectorWrapper', 'Native optional JEI machine/crafting/subtype/fluid information',
 '', 'compat/JeiPlugin.java', 'Pinned native optional APIs only. Dynamic detector output/state and9 Paragon forms plus source machine categories/transfer/info. RECIPE-VIEWER.md records exact source revisions and limitations.')
group('CompatAlloyer CompatCrusher CompatSublimationChamber IntegrationCT', 'Native recipe-manager JSON scripting',
 'machine/ProcessingDefinition.java', 'recipe/ProcessingAccess.java recipe/ProcessingRecipe.java',
 'Reviewed CT20/21 addJsonRecipe/removeByName/removeAll replace old Zen classes; chamber removal is by native recipe ID rather than effect object. No linked CT dependency required. No reviewed26 CT API. Source JSON pack editing works on all targets; script execution unverified.', ['forge-1.20.1','neoforge-1.21.1'])
group('ItemPotashFertilizer','Native fertilizer growth and uniform source drips', '', 'gadget/PotashFertilizerItem.java', 'Native bonemeal API, fifty uniform lava drips, permission/consumption delegated to native application. No client/gameplay evidence.')
group('IntegrationEnderIO','Native optional alloys and preserved editable recipe folder',
 'compat/EnderIoFiles.java', 'compat/OptionalPacks.java compat/ConfiguredPack.java',
 '18 native equations and opt-in per-target editable JSON files replace copied XML. Existing XML/user files remain untouched. Deleting managed JSON removes that recipe. No dependency decoding/gameplay observed.')
group('IntegrationIF','Native optional source laser drill entries',
 '', '', 'Source laser weights/depth/biomes mapped to reviewed20/21 formats and source palette. Dependency/config/content gates. No reviewed26 API; no fabricated26 recipe.', ['forge-1.20.1','neoforge-1.21.1'])
group('IntegrationThermal','Native optional source Thermal processing and fuels',
 '', '', '559 data recipes on reviewed20 target, including original intended outputs where source single-output helper accidentally wrote input.90mB native metal unit avoids cross-integration duplication. No reviewed21/26 API.', ['forge-1.20.1'])
OMITTED={
 'integration/IntegrationChisel.java':'Official Chisel-Team/Chisel branch inventory reviewed through1.18/dev; no matching official target API. Original integration only sends add_variation decoration groups. All blocks/ordinary crafting remain available. This does not claim that no unofficial fork exists.',
 'entity/model/MonocleModel.java':'No source references outside its own definition; original goggles only implement head-slot eligibility and overlay, with no model hook. Do not fabricate a missing active armor renderer.',
 'util/MetallurgyTweaks.java':'Deprecated helper has no initialization/install callers in source1.4.1. Do not silently overwrite user CraftTweaker scripts.',
 'network/server/PacketMovePlayer.java':'Registered legacy message has no send sites and unconditionally changes player motion. No active feature uses it. Native authoritative movement replaces active source paths.',
 'capabilities/entity/EntityData.java':'Default capability is registered but initEnderman is called only by retired DesichalkosArmorEffectOld. No active behavior consumes the borrowing fields.',
 'capabilities/entity/EntityDataProvider.java':'Only supplies the dormant original Enderman borrow-state capability. Active replacement Desichalkos effect has no borrowing behavior.',
 'capabilities/entity/EntityDataStorage.java':'Only persists the dormant original Enderman borrow-state fields; no active1.4.1 feature needs this schema.'}
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--scratch',type=Path,required=True);args=parser.parse_args();base=args.scratch.resolve()
 if base.drive.lower()!='h:': raise ValueError('References must remain on H:')
 coverage=json.loads((ROOT/'docs/coverage.json').read_text(encoding='utf-8'));rows=[]
 prior_path=ROOT/'docs/source-role-audit.json';prior={r['source'] for r in json.loads(prior_path.read_text(encoding='utf-8'))['rows']} if prior_path.exists() else set()
 for row in coverage['files']:
  if row['status']!='unverified' and row['source'] not in prior: continue
  source=row['source'];chests=source.startswith('metallurgychests/');rel=source.split('metallurgy_reforged/',1)[-1]
  path=base/('chests/src/main/java/'+source.removeprefix('metallurgychests/') if chests else 'upstream/src/main/java/'+source.removeprefix('metallurgy/'))
  text=path.read_text(encoding='utf-8');name=path.stem
  if chests:g=dict(role='Merged chest lifecycle/tags and client registration',common=[],native=['content/Content.java','inventory/MetalChestEntity.java','inventory/MetalChestBlock.java','inventory/MetalChestMenu.java','client/ChestRenderer.java','client/ChestScreen.java'],limits='Five capacities54/72/81/90/108 and original art. Same Metallurgy namespace now owns chest content. No separate legacy addon dependency; do not load old addon alongside this port. No1.12 block-entity/world migration. Current native rendering unobserved.',targets=None,status='adapted')
  elif rel in OMITTED:g=dict(role='Reviewed omitted legacy support role',common=[],native=[],limits=OMITTED[rel],targets=[],status='omitted')
  else:
   found=[g for g in GROUPS if name in g['names']]
   if len(found)!=1:raise ValueError(f'Unmapped/ambiguous role: {source}')
   g=found[0]
  mapped={}
  for target in row['targets']:
   enabled=g['targets'] is None or target in g['targets'];paths=['common/src/main/java/it/hurts/metallurgy_reforged/'+p for p in g['common']]+[target+'/src/main/java/it/hurts/metallurgy_reforged/'+p for p in g['native']]
   # Some roles use only generated resources and therefore have no Java replacement.
   mapped[target]={'status':g['status'] if enabled else 'omitted','implementation':[p for p in paths if (ROOT/p).exists()] if enabled else []}
  rows.append({'source':source,'sourceSha256':hashlib.sha256(path.read_bytes()).hexdigest(),'role':g['role'],'status':g['status'],'targets':mapped,'differencesAndLimits':[g['limits']],'methodNames':re.findall(r'(?m)^\s*(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?[\w<>?,.\[\] ]+\s+(\w+)\s*\(',text)})
 out={'reviewed':'2026-10-16','sourceRevisions':{'metallurgy':'ef94cb94cfc08ccebe8a0df307ce6b390dcd4704','chests':'f8f0b1853d93471d1abc4dd5436429475f0ef4c0'},'scope':'Support-role inventory plus targeted method-body review. Does not claim every line or every combination was executed. Native replacements/omissions, source hashes and callable names recorded. No games/tests.','rows':rows}
 (ROOT/'docs/source-role-audit.json').write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8');print('Recorded',len(rows),'support roles')
if __name__=='__main__':main()
