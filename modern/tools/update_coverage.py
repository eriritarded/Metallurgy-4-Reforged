"""Refresh file-level coverage without turning bounded checks into parity."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
WEAPONS=set('Amordrine Angmallen AstralSilver Atlarus BlackSteel Carmot Celenegil Ceruclase DamascusSteel DeepIron Desichalkos Electrum Etherium Eximite Haderoth Hepatizon Ignatius Inolashite Krik Midasium Mithril Oureclase Platinum Quicksilver Sanguinite ShadowIron ShadowSteel Vulcanite Vyroxeres'.split())
ARMOR=set('Adamantine Amordrine Angmallen AstralSilver Atlarus BlackSteel Carmot Celenegil Ceruclase DamascusSteel DeepIron Desichalkos Electrum Etherium Eximite Haderoth Hepatizon Ignatius Inolashite Kalendrite Krik Orichalcum Osmium Oureclase Platinum Prometheum Quicksilver Sanguinite ShadowIron ShadowSteel Vulcanite Vyroxeres'.split())
ALL=set('Adamantine Amordrine Haderoth Mithril Orichalcum Platinum Prometheum Sanguinite ShadowSteel Tartarite'.split())
TOOLS=set('AstralSilver Carmot Celenegil Ceruclase DeepIron Desichalkos Electrum Ignatius Inolashite Kalendrite Krik Midasium Quicksilver Sanguinite Vulcanite'.split())
PICKS=set('Angmallen DamascusSteel Mithril Oureclase ShadowIron'.split())
HOES=set('Atlarus Midasium Prometheum'.split())
def main():
    path=ROOT/'docs/coverage.json';data=json.loads(path.read_text(encoding='utf-8'));data['complete']=False
    inert={r['source']:r for r in json.loads((ROOT/'docs/inert-source-audit.json').read_text(encoding='utf-8'))['rows']} if (ROOT/'docs/inert-source-audit.json').exists() else {}
    for row in data['files']:
        source=row['source'];name=Path(source).stem;common=[];native=[];limits=[]
        relative=source.split('metallurgy_reforged/',1)[-1]
        if relative not in inert and (relative.startswith(('integration/tic/','integration/conarm/')) or name=='ArmorCustomSlot'):
            row['status']='adapted';row['differencesAndLimits']=['Native TConstruct3 material/stats/traits/smeltery and native plating/maille armor adapt the original roles on Forge1.20.1. Explicit six stat exceptions and source trait semantics retained. Native assembly/data decoding, armor menu injection and gameplay remain unverified. No matching official NeoForge API was found.']
            for target,value in row['targets'].items():
                supported=target.startswith('forge-')
                value.update(status='adapted' if supported else 'omitted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/compat/TinkerData.java','tools/generate_tconstruct_resources.py']+[target+'/src/main/java/it/hurts/metallurgy_reforged/compat/tconstruct/'+p for p in ['TinkerIntegration.java','LegacyTraitModule.java','TinkerArmorEvents.java']] if supported else [],evidence=['docs/TINKERS.md','docs/tconstruct-api-baseline.json','evidence/optional-tools-compile-checkpoint.json'])
            continue
        if name=='IntegrationSW' or relative.startswith('integration/spartanweaponry/'):
            row['status']='adapted';row['differencesAndLimits']=['Native API12 creates the original39 materials and23 weapon forms on Forge1.20.1, with897 source recipes,69 pose models and90 byte-identical licensed original sprites. Native behavior/data/rendering unverified. No matching official NeoForge API was found.']
            for target,value in row['targets'].items():
                supported=target.startswith('forge-')
                value.update(status='adapted' if supported else 'omitted',implementation=[target+'/src/main/java/it/hurts/metallurgy_reforged/compat/spartan/SpartanIntegration.java',target+'/src/main/java/it/hurts/metallurgy_reforged/compat/spartan/SpartanClient.java','tools/generate_spartan_resources.py'] if supported else [],evidence=['docs/SPARTAN.md','docs/spartan-api-baseline.json','evidence/optional-tools-compile-checkpoint.json'])
            continue
        if name=='IntegrationProjectE':
            row['status']='adapted';row['differencesAndLimits']=['Original49 bases,367 fixed values and195 tool equations use native ProjectE custom conversion data on reviewed20/21 targets. Configuration/content-gated packs. Native decoding/EMC outcomes unverified. No reviewed26 API or fabricated resource format.']
            for target,value in row['targets'].items():
                supported=not target.endswith('26.1.2')
                value.update(status='adapted' if supported else 'omitted',implementation=['tools/generate_projecte_resources.py','common/src/main/java/it/hurts/metallurgy_reforged/compat/DynamicPackData.java',target+'/src/main/java/it/hurts/metallurgy_reforged/compat/OptionalPacks.java'] if supported else [],evidence=['docs/PROJECTE.md','docs/projecte-api-baseline.json','evidence/configured-packs-compile-checkpoint.json'])
            continue
        if relative.startswith('integration/jei/') and name in {'MetalRecipeWrapper','AlloyerRecipeCategory','AlloyerRecipeWrapper','ChamberRecipeCategory','ChamberRecipeWrapper','CrusherRecipeCategory','CrusherRecipeWrapper'}:
            row['status']='adapted';row['differencesAndLimits']=['Native JEI machine pages use synchronized counted data recipes, original category art/animations, catalysts, input-only transfer and menu-instance checks. Compilation only; UI, packet transfer and reload behavior unverified. Special crafting wrappers tracked separately.']
            for target,value in row['targets'].items():value.update(status='adapted',implementation=[target+'/src/main/java/it/hurts/metallurgy_reforged/compat/JeiPlugin.java',target+'/src/main/java/it/hurts/metallurgy_reforged/recipe/ProcessingAccess.java'],evidence=['docs/RECIPE-VIEWER.md','docs/recipe-api-baseline.json','evidence/jei-machine-compile-checkpoint.json'])
            continue
        if name in {'BrassKnucklesModel','BrassKnucklesBakedModel','BrassKnucklesTEISR'}:
            row['status']='adapted';row['differencesAndLimits']=['Source arm/knuckles geometry, source skins/slim UVs, swing, original context transforms and flat GUI icon adapted. Native26 special-model submission replaces TEISR. All three compilers passed; poses, foil and runtime model loading unobserved. See CLIENT-VISUALS.md.']
            for target,value in row['targets'].items():value.update(status='adapted',implementation=[target+'/src/main/java/it/hurts/metallurgy_reforged/client/'+p for p in ['KnucklesParts.java','KnucklesRenderer.java']]+([] if target.endswith('26.1.2') else [target+'/src/main/java/it/hurts/metallurgy_reforged/client/KnucklesModel.java'])+['tools/generate_gadget_resources.py'],evidence=['docs/CLIENT-VISUALS.md','evidence/knuckles-confirmed-compile-checkpoint.json'])
            continue
        if name in {'ParticleOreEmitter','PacketAttachEmitter','PacketSpawnOreParticles'}:
            row['status']='adapted';row['differencesAndLimits']=['Bounded native particle payload and source foot-box Amordrine emitter, explicit motion, scale variation and lifetime adapted. Four-tick source frame step restored. Per-effect placements/motion and source-specific activation/explosion follow-up compile; see CLIENT-VISUALS.md. Runtime/rendering unobserved.']
            for target,value in row['targets'].items():value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/visual/ParticleBurst.java','common/src/main/java/it/hurts/metallurgy_reforged/visual/EffectParticles.java']+[target+'/src/main/java/it/hurts/metallurgy_reforged/'+p for p in ['client/OreParticles.java','client/OreParticle.java','network/VisualNetwork.java']],evidence=['docs/CLIENT-VISUALS.md','docs/particle-source-calls.json','evidence/particle-variants-compile-checkpoint.json'])
            continue
        if name in {'ModSounds','SoundHandler','PacketStartStopAmbienceSound'}:
            row['status']='adapted';row['differencesAndLimits']=['Original sound events/assets, server processing clocks and client sound replacement/stop/world-change cleanup adapted. No playback/audibility/runtime proof.']
            for target,value in row['targets'].items():value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/visual/SoundCue.java']+[target+'/src/main/java/it/hurts/metallurgy_reforged/'+p for p in ['content/Content.java','client/MachineAmbience.java','network/VisualNetwork.java','machine/MachineEntity.java','machine/ChamberEntity.java']],evidence=['docs/CLIENT-VISUALS.md','evidence/ambience-confirmed-compile-checkpoint.json'])
            continue
        if name in {'TileEntityChamber','BlockChamber','SublimationChamberTESR'}:
            row['status']='adapted';row['differencesAndLimits']=['Native chamber inventory, fuel-paused clock, authoritative aura, packed item state, HUD/tooltips, original item renderer, vapor and ambiance adapted. Selected historical server fixtures apply only to their snapshots; current visual/runtime behavior unverified.']
            for target,value in row['targets'].items():value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/machine/SublimationRecipes.java']+[target+'/src/main/java/it/hurts/metallurgy_reforged/'+p for p in ['machine/ChamberEntity.java','machine/ChamberBlock.java','machine/ChamberBridge.java','client/ChamberRenderer.java','client/ChamberParticles.java','client/HudRenderer.java','client/GadgetTooltips.java']],evidence=['docs/CLIENT-VISUALS.md','evidence/knuckles-confirmed-compile-checkpoint.json','evidence/ambience-confirmed-compile-checkpoint.json'])
            continue
        if name=='IntegrationMekanism':
            row['status']='adapted';row['differencesAndLimits']=['Original single Lutetium enriching recipe uses each reviewed native Mekanism JSON format and loader/content conditions. Source resource inspection and base compilation only; dependency decoding/runtime unverified.']
            for target,value in row['targets'].items():value.update(status='adapted',implementation=['tools/generate_optional_recipes.py',target+'/src/main/resources/data/metallurgy/'+('recipes' if target.startswith('forge') else 'recipe')+'/compat/mekanism/lutetium_enriching.json'],evidence=['docs/MACHINE-COMPAT.md','docs/optional-compat-research.json','evidence/machine-compat-compile-checkpoint.json'])
            continue
        if relative in inert:
            row['status']='omitted';row['differencesAndLimits']=[inert[relative]['reason'],'Not a removal of an active1.4.1 feature. Exact reference inventory and dormant-helper explanation are retained in inert-source-audit.json.']
            for value in row['targets'].values():value.update(status='omitted',implementation=[],evidence=['docs/inert-source-audit.json'])
            continue
        if '/advancement/' in source:
            row['status']='adapted';row['differencesAndLimits']=['Eight original native triggers,65 converted source advancements and server callbacks implemented. Compilation only; native JSON/predicate decoding, progression/rewards and client tree remain unverified. See ADVANCEMENTS.md.']
            for target,value in row['targets'].items():value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/advancement/AdvancementRules.java',target+'/src/main/java/it/hurts/metallurgy_reforged/advancement/Advancements.java',target+'/src/main/java/it/hurts/metallurgy_reforged/advancement/AdvancementEvents.java','tools/convert_advancements.py'],evidence=['docs/ADVANCEMENTS.md','docs/advancement-coverage.json','evidence/advancements-confirmed-compile-checkpoint.json'])
            continue
        if name in {'EffectTweaksConfig','EffectsRosterConfig','WorldGenerationConfig','WorldTickHandler','SpawnHandler','IsItemRegisteredConditionFactory'}:
            row['status']='adapted';row['differencesAndLimits']=['Original configuration/effect roster, restart values, validated overrides, spawn probabilities, acquisition conditions and native persisted retrogen adapted. General/optional/client consumers remain scoped in CONFIGURATION.md. Compilation only; no game evidence for these changes.']
            for target,value in row['targets'].items():value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/config/ConfigFiles.java','common/src/main/java/it/hurts/metallurgy_reforged/config/ContentAvailability.java','common/src/main/java/it/hurts/metallurgy_reforged/world/Retrogen.java']+[target+'/src/main/java/it/hurts/metallurgy_reforged/'+p for p in ['config/ConfigBootstrap.java','config/GeneralEvents.java','config/ContentCondition.java','world/RetrogenEvents.java','world/TarBiomeModifier.java']],evidence=['docs/CONFIGURATION.md','docs/WORLDGEN.md','evidence/config-confirmed-compile-checkpoint.json'])
            continue
        if name in {'ParticleOre','TooltipRenderHandler','KrikArmorHUD','SublimationChamberHUD','HUDHandler','FontColor','ModFontRenderer'}:
            row['status']='adapted';row['differencesAndLimits']=['Original ore-frame renderer, native RGB tooltips and source HUDs adapted. Native26 uses a client-only tooltip frame mixin. Appearance/runtime injection unverified; effect-specific bursts/emitters and native corpse presentation use the updated adapters. See CLIENT-VISUALS.md.']
            for target,value in row['targets'].items():value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/client/MaterialTooltips.java','common/src/main/java/it/hurts/metallurgy_reforged/material/MaterialItems.java']+[target+'/src/main/java/it/hurts/metallurgy_reforged/'+p for p in ['client/OreParticle.java','client/OreParticles.java','client/ItemVisuals.java','client/HudRenderer.java','network/VisualNetwork.java']]+['tools/generate_client_resources.py'],evidence=['docs/CLIENT-VISUALS.md','evidence/client-ui-confirmed-compile-checkpoint.json'])
            continue
        if name in {'BlockOrientable','BlockPhosphorusLamp','PhosphorusLampSavedData','BlockMetallurgyTNT','MetallurgyPrimedTNT','ExplosiveType','MetallurgyExplosion','MetallurgyTNTRenderer','ItemEtheriumMonocle'}:
            row['status']='adapted'
            row['differencesAndLimits']=['Native lamp/road/TNT source and wearable goggles adapted. Lamp range is rebuilt from loaded chunk palettes; source blast equations, tag-based rock resistance and tier limits are documented in BLOCK-GADGETS.md. Goggles redstone overlay and original flame/ore explosion visual compile. No current game/client evidence.']
            for target,value in row['targets'].items():
                value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/gadget/LampRange.java','common/src/main/java/it/hurts/metallurgy_reforged/gadget/RoadMovement.java']+[target+'/src/main/java/it/hurts/metallurgy_reforged/'+p for p in ['gadget/PhosphorusLampBlock.java','gadget/RoadBlock.java','gadget/BlockGadgetEvents.java','gadget/VulcaniteTntBlock.java','gadget/GogglesItem.java','entity/VulcanitePrimedTnt.java','client/VulcaniteTntRenderer.java']]+([target+'/src/main/java/it/hurts/metallurgy_reforged/mixin/VulcaniteExplosionMixin.java'] if target.startswith('forge') else []),evidence=['docs/BLOCK-GADGETS.md','evidence/blocks-confirmed-compile-checkpoint.json'])
            continue
        if name in {'ItemElectrumMagnet','ItemKnuckles','ItemIgnatiusLighter','ItemVulcaniteLighter','ItemGauntlet','GauntletEffect','GauntletEquipHandler','PunchEffect','ItemShieldBase','ItemBuckler','ItemLemuriteShield','ItemZincBuckler','ItemVulcaniteBuckler','ItemCeruclaseShield','ItemOrichalcumBuckler','BlockIceShield','ItemOreDetector','OreDetectorRecipe'}:
            row['status']='adapted'
            row['differencesAndLimits']=['Native registered item/server mechanics and dynamic detector recipe adapted from source defaults. Client controls, specialized rendering/scan/LEDs and configuration compile; current gameplay/rendering evidence unavailable. See GADGETS.md for exact values and changes to cleanup, protection and bounded block loops.']
            for target,value in row['targets'].items():
                value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/gadget/'+p for p in ['GadgetLogic.java','ShieldLogic.java','DetectorCrafting.java']]+[target+'/src/main/java/it/hurts/metallurgy_reforged/gadget/'+p for p in ['GadgetItem.java','GadgetBridge.java','LighterItem.java','MetallurgyShieldItem.java','IceShieldBlock.java']]+['tools/generate_gadget_resources.py'],evidence=['docs/GADGETS.md','evidence/gadgets-confirmed-compile-checkpoint.json'])
            continue
        if '/integration/silentgems/' in source:
            row['status']='adapted'
            row['differencesAndLimits']=['Modern tools moved from Silent Gems to Silent Gear. Separate native 3.6 and 4.2 data adapters supply41 metals, including39 source tool metals and2 armor-only metals. Source implemented and base adapters compiled; receiving-mod data decoding/runtime/client behavior unobserved. See MODULAR-COMPAT.md.']
            for target,value in row['targets'].items():
                value.update(status='adapted',implementation=['tools/generate_modular_compat.py',target+'/src/main/resources/resourcepacks/silentgear/data/metallurgy/silentgear_materials',target+'/src/main/java/it/hurts/metallurgy_reforged/compat/OptionalPacks.java'],evidence=['docs/modular-compat-baseline.json','docs/modular-compat-coverage.json','evidence/tools-confirmed-compile-checkpoint.json'])
            continue
        if '/fluid/' in source or name=='FluidBlockTar' or name=='ModLakeWorldGen':
            row['status']='adapted'
            row['differencesAndLimits']=['51 native fluid types with original palettes/temperature defaults, source/flowing states and buckets; tar contact/hardening, native rendering resources and tar-lake source implemented. See FLUIDS.md for native-flow/worldgen adaptations. Compilation is not gameplay or client validation; configuration and supported optional native integrations compile; unsupported target APIs are documented separately.']
            for target,value in row['targets'].items():
                value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/fluid/FluidCatalog.java','common/src/main/java/it/hurts/metallurgy_reforged/world/TarLakeFeature.java',target+'/src/main/java/it/hurts/metallurgy_reforged/fluid/FluidContent.java',target+'/src/main/java/it/hurts/metallurgy_reforged/fluid/MetallurgyLiquidBlock.java',target+'/src/main/java/it/hurts/metallurgy_reforged/client/FluidRendering.java','tools/generate_fluid_resources.py'],evidence=['docs/FLUIDS.md','evidence/fluids-confirmed-compile-checkpoint.json'])
            continue
        if ('/effect/tool/' in source and name.removesuffix('ToolEffect') in TOOLS
            or '/effect/pickaxe/' in source and name.removesuffix('PickaxeEffect') in PICKS
            or '/effect/hoe/' in source and name.removesuffix('HoeEffect') in HOES
            or name=='ItemBlockOre'):
            row['status']='adapted'
            row['differencesAndLimits']=['Native tool/hoe/pickaxe callbacks and locked ore item source implemented. See TOOLS.md for exact values, safety adaptations and remaining rendering/config/advancement work. Compilation does not prove runtime drops, protection, timers or client behavior.']
            for target,value in row['targets'].items():
                native=['effect/EffectEvents.java','effect/ToolBridge.java']+(['mixin/BlockLootMixin.java'] if target.startswith('forge') else [])
                value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/effect/ToolEffects.java','common/src/main/java/it/hurts/metallurgy_reforged/content/LockedOreItem.java']+[target+'/src/main/java/it/hurts/metallurgy_reforged/'+p for p in native],evidence=['docs/TOOLS.md','evidence/tools-confirmed-compile-checkpoint.json'])
            continue
        if '/effect/weapon/' in source and name.removesuffix('WeaponEffect') in WEAPONS:
            common=['effect/WeaponDamage.java','effect/WeaponLifecycle.java','effect/WeaponLoot.java','effect/DivineGrace.java'];native=['effect/EffectEvents.java','effect/VersionBridge.java'];limits=['Source effect roster/tweaks, original particles/motion, active models and specialized tooltips are implemented and compile. Only historical selected runtime paths were checked; current rendering/gameplay remains unobserved.']
        elif '/effect/all/' in source and name.removesuffix('Effect') in ALL:
            common=['effect/AllEquipmentEffects.java','effect/Paragon.java','effect/EquipmentStats.java'];native=['effect/EffectEvents.java','effect/VersionBridge.java','recipe/ParagonRecipe.java'];limits=['Selected runtime paths checked; effect configuration, original specialized tooltips and all lifecycle combinations remain unverified.']
        elif '/effect/armor/' in source and name.removesuffix('ArmorEffect') in ARMOR:
            common=['effect/ArmorEffects.java','effect/ArmorLifecycle.java','effect/ArmorControls.java','effect/ArmorMovement.java','effect/ArmorPhasing.java','effect/CorpseLifecycle.java','effect/CompanionArmor.java'];native=['effect/EffectEvents.java','effect/VersionBridge.java','network/ArmorNetwork.java','client/ArmorInput.java','client/ArmorKeys.java'];limits=['Source-derived callbacks implemented; only selected native runtime cases checked and newest changes require fresh evidence. Network controls, client movement, companion renderer and HUD remain unverified. Sanguinite revives mobs, preserving native player respawn. See EQUIPMENT.md for changed cleanup, source quirks and native boundaries.']
        elif name in {'EntityPierKnight','AIPierKnightFollow','AIPierOwnerAttack','AIPierOwnerWasHurt','PierKnightRenderer'}:
            common=['effect/CompanionArmor.java'];native=['entity/PierKnight.java','client/PierKnightRenderer.java'];limits=['Native companion, owner targeting/following, summon lifecycle, original skin, fade and wide-name rendering implemented and compiled. Current runtime/game/client checks are prohibited and not claimed.']
        elif any(source.endswith('/'+n+'.java') for n in ['TileEntitySublimationChamber','BlockSublimationChamber','SublimationRecipes']):
            common=['machine/SublimationRecipes.java'];native=['machine/ChamberEntity.java','machine/ChamberBlock.java','machine/ChamberBridge.java'];limits=['Native server batch, fuel pause, aura and packed block-item state implemented. 17-fixture Neo checkpoint covers selected Chamber paths. Original vapor, item renderer, tooltips, HUD and ambiance compile. Graphical/runtime behavior remains unobserved.']
        elif any(source.endswith('/'+n+'.java') for n in ['TileEntityCrusher','TileEntityAlloyer','BlockCrusher','BlockAlloyer','ContainerCrusher','ContainerAlloyer','GuiCrusher','GuiAlloyer','SlotCrusherOutput','SlotAlloyerOutput','CrusherRecipes','AlloyerRecipes']):
            common=['machine/MachineRecipes.java'];native=['machine/MachineEntity.java','machine/MachineBlock.java','machine/MachineMenu.java','machine/MachineBridge.java','client/MachineScreen.java'];limits=['Historical selected processing checked. Native ambiance, supported external heaters, generic common-tag recipes, managed recipes and per-route saved XP are implemented and compile; current runtime/overflow combinations unobserved.']
        elif '/world/' in source and any(source.endswith('/'+n+'.java') for n in ['ModWorldGen','BaseOreSpawn','NetherOreSpawn','HighChanceYLevelSpawn','HighChanceTemperatureSpawn','NearFortressSpawn','SmallIslandsSpawn','VeinNextToBlockSpawn','IOreSpawn']):
            common=['world/OreBatchFeature.java','world/OreBatchConfiguration.java','world/OreProfiles.java'];native=['effect/VersionBridge.java'];limits=['34 source profiles, retrogen, configured/custom dimension selection and tar lakes implemented and compiled. Historical native feature startup checked; no current terrain distribution proof. Native veins replace deepslate; fortress boost reads prepared nearby starts without generating distant chunks.']
        elif row['source'].startswith('metallurgychests/') or row['source'].startswith('chests/'):
            # Avoid treating every resource/config helper as complete from the
            # inventory core alone. Specific chest source roles are recorded.
            if any(x in name.lower() for x in ['chest','container','inventory','gui','renderer']) and name not in ['MetallurgyChests']:
                native=['inventory/MetalChestEntity.java','inventory/MetalChestBlock.java','inventory/MetalChestMenu.java','client/ChestRenderer.java','client/ChestScreen.java'];limits=['Merged native chest inventory/menu/persistence/automation core checked. Native graphical rendering unobserved; no1.12 world/registry migration. Merged mod IDs and five native capacities are documented in SOURCE-ROLE-AUDIT.md.']
        if common or native:
            row['status']='adapted';row['differencesAndLimits']=limits
            for target,value in row['targets'].items():
                value.update(status='adapted',implementation=['common/src/main/java/it/hurts/metallurgy_reforged/'+p for p in common]+[target+'/src/main/java/it/hurts/metallurgy_reforged/'+p for p in native],evidence=['evidence/armor-ore-forge120-server-pass.json' if target.startswith('forge') else 'evidence/neoforge-native-core-pass.json'])
    role_path=ROOT/'docs/source-role-audit.json'
    if role_path.exists():
        roles={r['source']:r for r in json.loads(role_path.read_text(encoding='utf-8'))['rows']}
        for row in data['files']:
            if row['source'] not in roles:continue
            review=roles[row['source']];row['status']=review['status'];row['differencesAndLimits']=review['differencesAndLimits']
            for target,value in row['targets'].items():value.update(**review['targets'][target],evidence=['docs/source-role-audit.json','docs/SOURCE-ROLE-AUDIT.md','evidence/particle-fidelity-compile-checkpoint.json'])
    path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    print('Source rows adapted:',sum(r['status']=='adapted' for r in data['files']),'of',len(data['files']),'(full port remains incomplete)')
if __name__=='__main__':main()
