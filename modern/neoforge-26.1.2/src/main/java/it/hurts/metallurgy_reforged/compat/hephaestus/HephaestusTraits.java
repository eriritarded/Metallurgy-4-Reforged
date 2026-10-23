package it.hurts.metallurgy_reforged.compat.hephaestus;

import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.table.ToolUpgrades;
import com.titammods.hephaestus_tools.table.ToolXp;
import com.titammods.hephaestus_tools.tools.helper.ToolBuildHandler;
import com.titammods.hephaestus_tools.tools.helper.ToolCombat;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import com.titammods.hephaestus_tools.tools.stat.HarvestTier;
import it.hurts.metallurgy_reforged.compat.HephaestusTraitData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.*;

/** Optional adapter for Hephaestus Tools 1.0.8. Never loaded when that mod is absent. */
public final class HephaestusTraits {
    private static final Identifier WATER_SPEED=Identifier.fromNamespaceAndPath("metallurgy","hephaestus_opistognathus");
    private static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,"metallurgy");
    private static final DeferredHolder<MobEffect,MobEffect> ENDERFERENCE=EFFECTS.register("enderference",()->new MobEffect(MobEffectCategory.HARMFUL,0x21985f) {});
    private static final List<net.minecraft.core.Holder<MobEffect>> DICE=List.of(MobEffects.BLINDNESS,MobEffects.LEVITATION,
        MobEffects.HUNGER,MobEffects.INSTANT_DAMAGE,MobEffects.NAUSEA,MobEffects.NIGHT_VISION,MobEffects.POISON,MobEffects.SLOWNESS,MobEffects.REGENERATION);
    public static void register(IEventBus bus) {EFFECTS.register(bus);NeoForge.EVENT_BUS.register(HephaestusTraits.class);}

    public static Set<String> part(MaterialId material,int role) {
        var stats=MaterialManager.getInstance().getStatsForSlot(material,role);
        if(stats==null || !stats.traitId().startsWith("metallurgy:")) return Set.of();
        return HephaestusTraitData.effects(stats.traitId().substring(11),role);
    }
    private static Set<String> traits(ItemStack stack,boolean usable) {
        if(usable?!ToolStack.isUsable(stack):!ToolStack.isInitialized(stack)) return Set.of();
        var materials=ToolStack.getMaterials(stack);
        var parts=ToolBuildHandler.getToolParts(stack.getItem());
        var result=new LinkedHashSet<String>();
        for(int i=0;i<materials.size();i++) {
            int role=i<parts.size() && parts.get(i) instanceof ToolPartItem p?p.getPartSlot():i==0?0:i==2?2:1;
            result.addAll(part(materials.get(i),role));
        }
        return result;
    }
    /** Native slot limit/UI stays five; Writable unlocks the next slot early once per tool. */
    public static int writableSlots(ItemStack tool,int original) {
        if(!traits(tool,false).contains("writable")) return original;
        int level=ToolXp.getLevel(tool);
        int progression=(int)Arrays.stream(ToolUpgrades.SLOT_UNLOCK_LEVEL).filter(required->level>=required).count();
        // Native original already includes used slots. Incrementing it would
        // repeatedly unlock another slot after each installed upgrade.
        return Math.max(original,Math.min(5,progression+1));
    }
    /** Run before native damageItem and Unbreaking, preserving its normal broken-tool handling. */
    public static int wear(ItemStack tool,int amount,ServerLevel level,LivingEntity owner) {
        if(amount<=0 || !tool.isDamageableItem() || owner!=null && owner.hasInfiniteMaterials()) return amount;
        var traits=traits(tool,true);
        if(traits.isEmpty()) return amount;
        var random=owner==null?level.getRandom():owner.getRandom();
        int changed=amount;
        if(traits.contains("dense")) {
            double ratio=ToolStack.getCurrentDamage(tool)/(double)Math.max(1,ToolStack.getDurability(tool));
            if(random.nextFloat()<Math.pow(.75*ratio,3)) changed-=Math.max(amount/2,1);
        }
        if(traits.contains("duritos")) {
            float roll=random.nextFloat();
            if(roll<.1F) changed=(int)Math.min(Integer.MAX_VALUE,(long)changed+amount);
            else if(roll<.5F) changed-=amount;
        }
        return Math.max(0,changed);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void mining(PlayerEvent.BreakSpeed event) {
        var player=event.getEntity();var tool=player.getMainHandItem();var traits=traits(tool,true);
        float speed=event.getNewSpeed();
        if(traits.contains("opistognathus") && player.isInWater()) speed*=3;
        if(traits.contains("unnatural") && tool.isCorrectToolForDrops(event.getState())) {
            int needed=Arrays.stream(HarvestTier.values()).filter(t->t.canHarvest(event.getState())).mapToInt(HarvestTier::miningLevel).min().orElse(6);
            speed+=Math.max(0,ToolStack.getHarvestTier(tool).miningLevel()-needed);
        }
        event.setNewSpeed(speed);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void incoming(LivingIncomingDamageEvent event) {
        if(event.getEntity().level().isClientSide()) return;
        var tool=ToolCombat.tool(event.getEntity(),event.getSource());
        if(traits(tool,true).contains("superheat") && event.getEntity().isOnFire())
            event.setAmount(event.getAmount()+event.getOriginalAmount()*.35F);
    }
    @SubscribeEvent
    public static void hit(LivingDamageEvent.Post event) {
        var target=event.getEntity();
        if(!(target.level() instanceof ServerLevel) || !(event.getSource().getEntity() instanceof Player player) || event.getInflictedDamage()<=0) return;
        var tool=ToolCombat.tool(target,event.getSource());var traits=traits(tool,true);var random=player.getRandom();
        if(traits.contains("obscure")) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,30,2));
        if(traits.contains("vulcan_hot")) target.igniteForSeconds(5);
        else if(traits.contains("vulcan")) target.igniteForSeconds(3);
        if(traits.contains("poisonous") && target.isAlive()) target.addEffect(new MobEffectInstance(MobEffects.POISON,101));
        if(traits.contains("wither") && random.nextInt(100)<=20) target.addEffect(new MobEffectInstance(MobEffects.WITHER,30,1));
        if(traits.contains("king_dice")) target.addEffect(new MobEffectInstance(DICE.get(random.nextInt(DICE.size())),80));
        if(traits.contains("lifesteal") && player.getHealth()<player.getMaxHealth() && random.nextFloat()<.2F+Math.round(player.getLuck())*.2F)
            player.heal(event.getHealthDamage()*.4F);
        if(traits.contains("enderference") && target instanceof EnderMan) target.addEffect(new MobEffectInstance(ENDERFERENCE,100,1,false,true,false));
    }
    @SubscribeEvent public static void enderTeleport(EntityTeleportEvent.EnderEntity event) {
        if(event.getEntityLiving().hasEffect(ENDERFERENCE)) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void loot(LivingDropsEvent event) {
        var target=event.getEntity();
        if(!(target.level() instanceof ServerLevel level) || target instanceof Player) return;
        var tool=ToolCombat.tool(target,event.getSource());
        if(!traits(tool,true).contains("duplication") || level.getRandom().nextFloat()>=.5F) return;
        var extra=new ArrayList<ItemEntity>();
        for(var drop:event.getDrops()) {
            var copy=new ItemEntity(level,drop.getX(),drop.getY(),drop.getZ(),drop.getItem().copy());
            copy.setDefaultPickUpDelay();extra.add(copy);
        }
        event.getDrops().addAll(extra);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void blocks(BlockDropsEvent event) {
        if(!(event.getBreaker() instanceof Player)) return;
        var tool=event.getTool();var traits=traits(tool,true);var level=event.getLevel();var pos=event.getPos();
        if(traits.contains("obscure")) level.sendParticles(ParticleTypes.LARGE_SMOKE,pos.getX()+.5,pos.getY(),pos.getZ()+.5,100,.5,0,.5,.05);
        if(!traits.contains("vulcan") && !traits.contains("vulcan_hot") || !tool.isCorrectToolForDrops(event.getState())) return;
        // Silk Touch takes priority over autosmelting if a command/datapack combines them.
        if(tool.getEnchantments().keySet().stream().anyMatch(h->h.is(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH))
            || ToolStack.getModifiers(tool).stream().anyMatch(m->m.id().getPath().equals("silk_touch") || m.id().getPath().equals("squeaky"))) return;
        var replacement=new ArrayList<ItemEntity>();int xp=event.getDroppedExperience();
        for(var drop:event.getDrops()) {
            var input=new SingleRecipeInput(drop.getItem());
            var recipe=level.recipeAccess().getRecipeFor(RecipeType.SMELTING,input,level);
            if(recipe.isEmpty()) {replacement.add(drop);continue;}
            var result=recipe.get().value().assemble(input);
            if(result.isEmpty()) {replacement.add(drop);continue;}
            long remaining=(long)result.getCount()*drop.getItem().getCount();
            while(remaining>0) {
                int size=(int)Math.min(remaining,result.getMaxStackSize());remaining-=size;
                replacement.add(new ItemEntity(level,drop.getX(),drop.getY(),drop.getZ(),result.copyWithCount(size)));
            }
            // The original awards cooking XP once per generated drop stack, not once per item.
            float exp=recipe.get().value().experience();
            int extra=exp>=1?(int)exp:level.getRandom().nextFloat()<exp?1:0;
            xp=(int)Math.min(Integer.MAX_VALUE,(long)xp+extra);
        }
        event.getDrops().clear();event.getDrops().addAll(replacement);event.setDroppedExperience(xp);
        level.sendParticles(ParticleTypes.FLAME,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,.5,.5,.5,.05);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        var player=event.getEntity();if(!(player instanceof ServerPlayer)) return;
        var attribute=player.getAttribute(Attributes.ATTACK_SPEED);if(attribute==null) return;
        boolean active=player.isAlive() && player.isInWater() && traits(player.getMainHandItem(),true).contains("opistognathus");
        if(active && !attribute.hasModifier(WATER_SPEED)) attribute.addTransientModifier(new AttributeModifier(WATER_SPEED,2.7,AttributeModifier.Operation.ADD_VALUE));
        else if(!active) attribute.removeModifier(WATER_SPEED);
    }
    private static void clean(Player player) {var attr=player.getAttribute(Attributes.ATTACK_SPEED);if(attr!=null) attr.removeModifier(WATER_SPEED);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {clean(event.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {clean(event.getEntity());}
    @SubscribeEvent public static void death(LivingDeathEvent event) {if(event.getEntity() instanceof Player player) clean(player);}
    private static void addLines(List<Component> lines,Set<String> traits,boolean details) {
        var names=new LinkedHashSet<String>();
        for(var effect:traits) {
            String name=effect.equals("vulcan_hot")?"vulcan":effect;
            if(!names.add(name)) continue;
            var label=Component.translatable("trait.metallurgy.hephaestus."+name).withStyle(ChatFormatting.DARK_AQUA);
            if(lines.stream().anyMatch(line->line.getString().equals(label.getString()))) continue;
            lines.add(label);
            if(details) lines.add(Component.literal("  ").append(Component.translatable("trait.metallurgy.hephaestus."+name+".desc").withStyle(ChatFormatting.DARK_GRAY)));
        }
    }
    public static List<Component> partLines(MaterialId material,int role) {var out=new ArrayList<Component>();addLines(out,part(material,role),true);return out;}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var tool=event.getItemStack();Set<String> traits;
        if(tool.getItem() instanceof ToolPartItem part) traits=part(part.getMaterial(tool),part.getPartSlot());
        else traits=traits(tool,false);
        addLines(event.getToolTip(),traits,event.getFlags().hasShiftDown());
    }
    private HephaestusTraits() {}
}
