package it.hurts.metallurgy_reforged.compat.tconstruct;

import it.hurts.metallurgy_reforged.compat.TinkerData;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.*;
import slimeknights.tconstruct.library.modifiers.hook.behavior.*;
import slimeknights.tconstruct.library.modifiers.hook.build.*;
import slimeknights.tconstruct.library.modifiers.hook.combat.*;
import slimeknights.tconstruct.library.modifiers.hook.mining.*;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.*;
import slimeknights.tconstruct.library.tools.context.*;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.nbt.*;
import slimeknights.tconstruct.library.tools.stat.*;
import slimeknights.tconstruct.tools.modules.AutosmeltModule;
import java.util.*;

/** Original Metallurgy/TCon2 callbacks expressed as a native TCon3 data module. */
public record LegacyTraitModule(String effect) implements ModifierModule,MeleeHitModifierHook,MeleeDamageModifierHook,
    ToolDamageModifierHook,BreakSpeedModifierHook,BlockBreakModifierHook,ProcessLootModifierHook,
    ToolStatsModifierHook,ValidateModifierHook {
    public static final RecordLoadable<LegacyTraitModule> LOADER=RecordLoadable.create(
        StringLoadable.DEFAULT.requiredField("effect",LegacyTraitModule::effect),LegacyTraitModule::new);
    private static final AutosmeltModule SMELT=new AutosmeltModule(0,RecipeType.SMELTING);
    @SuppressWarnings("deprecation") public static void clearRecipes() {SMELT.clearCache();}
    private static final MobEffect[] DICE={MobEffects.BLINDNESS,MobEffects.LEVITATION,MobEffects.HUNGER,MobEffects.HARM,
        MobEffects.CONFUSION,MobEffects.NIGHT_VISION,MobEffects.POISON,MobEffects.MOVEMENT_SLOWDOWN,MobEffects.REGENERATION};
    @Override public RecordLoadable<LegacyTraitModule> getLoader() {return LOADER;}
    @Override public List<ModuleHook<?>> getDefaultHooks() {
        return HookProvider.<LegacyTraitModule>defaultHooks(ModifierHooks.MELEE_HIT,ModifierHooks.MELEE_DAMAGE,
            ModifierHooks.TOOL_DAMAGE,ModifierHooks.BREAK_SPEED,ModifierHooks.BLOCK_BREAK,
            ModifierHooks.PROCESS_LOOT,ModifierHooks.TOOL_STATS,ModifierHooks.VALIDATE);
    }
    private static boolean active() {return GeneralConfig.tiCIntegration;}
    @Override public void afterMeleeHit(IToolStackView tool,ModifierEntry modifier,ToolAttackContext context,float damage) {
        var target=context.getLivingTarget();if(!active() || target==null || context.getLevel().isClientSide || damage<=0) return;
        var random=context.getAttacker().getRandom();
        switch(effect) {
            case "obscure"->target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,30,2,false,true));
            case "vulcan"->target.setSecondsOnFire(vulcanSeconds(tool));
            case "poisonous"->{if(target.isAlive()) target.addEffect(new MobEffectInstance(MobEffects.POISON,101));}
            case "wither"->{if(random.nextInt(100)<=20) target.addEffect(new MobEffectInstance(MobEffects.WITHER,30,1));}
            case "king_dice"->target.addEffect(new MobEffectInstance(DICE[random.nextInt(DICE.length)],80));
            case "lifesteal"->{var player=context.getPlayerAttacker();if(player!=null && player.getHealth()<player.getMaxHealth() && random.nextFloat()<.20F+Math.round(player.getLuck())*.20F) player.heal(damage*.40F);}
        }
    }
    @Override public float getMeleeDamage(IToolStackView tool,ModifierEntry modifier,ToolAttackContext context,float baseDamage,float damage) {
        return active() && effect.equals("superheat") && context.getTarget().isOnFire()?damage+baseDamage*.35F:damage;
    }
    @Override public int onDamageTool(IToolStackView tool,ModifierEntry modifier,int amount,LivingEntity holder) {
        if(!active() || amount<=0 || holder!=null && holder.level().isClientSide) return amount;
        var random=holder==null?net.minecraft.util.RandomSource.create():holder.getRandom();
        if(effect.equals("duritos")) {float roll=random.nextFloat();return roll<.1?Math.min(Integer.MAX_VALUE/2,amount)*2:roll<.5?0:amount;}
        if(effect.equals("dense")) {double ratio=tool.getDamage()/(double)Math.max(1,tool.getStats().getInt(ToolStats.DURABILITY));if(random.nextFloat()<Math.pow(.75*ratio,3)) return Math.max(0,amount-Math.max(amount/2,1));}
        return amount;
    }
    @Override public void onBreakSpeed(IToolStackView tool,ModifierEntry modifier,BreakSpeed event,Direction side,boolean effective,float multiplier) {
        if(!active()) return;
        if(effect.equals("opistognathus") && event.getEntity().isUnderWater()) event.setNewSpeed(event.getNewSpeed()*3);
        if(effect.equals("unnatural") && effective) {
            int required=net.minecraftforge.common.TierSortingRegistry.getSortedTiers().stream().filter(t->t.getTag()!=null && event.getState().is(t.getTag())).mapToInt(net.minecraft.world.item.Tier::getLevel).max().orElse(0);
            int level=tool.getStats().get(ToolStats.HARVEST_TIER).getLevel();
            if(level>required) event.setNewSpeed(event.getNewSpeed()+level-required);
        }
    }
    @Override public void afterBlockBreak(IToolStackView tool,ModifierEntry modifier,ToolHarvestContext context) {
        if(!active()) return;var pos=context.getPos();var level=context.getWorld();
        if(effect.equals("obscure")) level.sendParticles(ParticleTypes.LARGE_SMOKE,pos.getX()+.5,pos.getY(),pos.getZ()+.5,100,.5,0,.5,.05);
        if(effect.equals("vulcan") && context.isEffective()) level.sendParticles(ParticleTypes.FLAME,pos.getX()+.5,pos.getY(),pos.getZ()+.5,3,.5,0,.5,.05);
    }
    @Override public void processLoot(IToolStackView tool,ModifierEntry modifier,List<ItemStack> loot,LootContext context) {
        if(!active()) return;
        if(effect.equals("duplication") && GeneralConfig.enableDuplicationTrait) {
            var target=context.getParamOrNull(LootContextParams.THIS_ENTITY);
            var killer=context.getParamOrNull(LootContextParams.KILLER_ENTITY);
            if(target instanceof LivingEntity living && !(target instanceof Player) && !living.isAlive() && killer instanceof LivingEntity && context.getRandom().nextFloat()<.5F) {
                var copies=new ArrayList<ItemStack>();for(var stack:loot) copies.add(stack.copy());loot.addAll(copies);
            }
        }
        if(effect.equals("vulcan") && context.hasParam(LootContextParams.BLOCK_STATE) && compatible(tool)) {
            var state=context.getParam(LootContextParams.BLOCK_STATE);
            if(IsEffectiveToolHook.isEffective(tool,state)) SMELT.processLoot(tool,modifier,loot,context);
        }
    }
    @Override public Component validate(IToolStackView tool,ModifierEntry modifier) {
        return effect.equals("vulcan") && !compatible(tool)?Component.translatable("modifier.metallurgy.vulcan.conflict"):null;
    }
    private static boolean compatible(IToolStackView tool) {
        for(var entry:tool.getModifierList()) if(Set.of("tconstruct:silky","tconstruct:squeaky").contains(entry.getId().toString())) return false;
        return true;
    }
    private static int vulcanSeconds(IToolContext tool) {
        int seconds=3;var parts=ToolMaterialHook.stats(tool.getDefinition());
        for(int i=0;i<parts.size();i++) if(Set.of("head","limb").contains(parts.get(i).getPath()) && tool.getMaterial(i).getId().toString().equals("metallurgy:vulcanite")) seconds=5;
        return seconds;
    }
    @Override public void addToolStats(IToolContext tool,ModifierEntry modifier,ModifierStatsBuilder builder) {
        if(!active() || !(effect.equals("legacy_parts") || effect.equals("armor_legacy"))) return;
        var definition=tool.getDefinition();var materials=ToolMaterialHook.stats(definition);
        for(int i=0;i<materials.size();i++) {
            var id=tool.getMaterial(i).getId();if(!id.getNamespace().equals("metallurgy") || !TinkerData.enabled(id.getPath())) continue;
            var material=Materials.BY_NAME.get(id.getPath());String kind=materials.get(i).getPath();
            String legacy=switch(kind) {case "handle","grip"->"handle";case "binding"->"extra";default->null;};
            float scale=definition.getHook(ToolHooks.TOOL_MATERIALS).scaleStats(definition,i);
            if(effect.equals("armor_legacy")) {
                if(GeneralConfig.conarmIntegration && kind.equals("maille") && material.hasArmor()) {
                    double d=material.armor("durability",1),plate=d*.75,multiplier=Math.sqrt(plate)*.094;
                    ToolStats.DURABILITY.add(builder,(plate+d/6)*scale);
                    ToolStats.DURABILITY.percent(builder,((multiplier>2?multiplier*.5:multiplier)-1)*scale);
                    ToolStats.ARMOR_TOUGHNESS.add(builder,material.armor("toughness",0)*scale);
                }
                continue;
            }
            if(legacy!=null) {var part=TinkerData.part(material,legacy);if(part!=null) ToolStats.DURABILITY.add(builder,part.get("durability").getAsFloat()*scale);if(kind.equals("grip")) {var extra=TinkerData.part(material,"extra");if(extra!=null) ToolStats.DURABILITY.add(builder,extra.get("durability").getAsFloat()*scale);}}
            if(kind.equals("limb")) {var bow=TinkerData.part(material,"bow");if(bow!=null) ToolStats.PROJECTILE_DAMAGE.add(builder,bow.get("bonus_damage").getAsFloat()*scale);}
        }
    }
}
