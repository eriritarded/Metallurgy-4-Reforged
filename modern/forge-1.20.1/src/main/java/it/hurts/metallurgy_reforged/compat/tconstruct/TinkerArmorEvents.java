package it.hurts.metallurgy_reforged.compat.tconstruct;

import it.hurts.metallurgy_reforged.config.GeneralConfig;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import java.util.*;

/** Source ConArmory powers on TConstruct3's native plating/maille armor. */
public final class TinkerArmorEvents {
    private static final Map<Player,Float> STEPS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Player> EYES=Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
    public static int count(LivingEntity player,String name) {
        if(!GeneralConfig.tiCIntegration || !GeneralConfig.conarmIntegration) return 0;
        int count=0;var id=new ModifierId("metallurgy:armor_"+name);
        for(var stack:player.getArmorSlots()) if(stack.getItem() instanceof ArmorItem && stack.is(TinkerTags.Items.MODIFIABLE)) {
            var tool=ToolStack.from(stack);if(!tool.isBroken() && tool.getModifierLevel(id)>0) count++;
        }
        return count;
    }
    public static boolean locked(Player player) {return player.isAlive() && !player.getAbilities().instabuild && player.isInWater() && count(player,"deeply")>0;}
    public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;var player=event.player;
        boolean deeply=locked(player);
        if(deeply) {
            STEPS.putIfAbsent(player,player.maxUpStep());player.setMaxUpStep(1);
            if(!player.onGround()) player.setDeltaMovement(player.getDeltaMovement().multiply(.5,1,.5));
            player.setDeltaMovement(player.getDeltaMovement().with(net.minecraft.core.Direction.Axis.Y,-.6));
        } else {
            Float previous=STEPS.remove(player);if(previous!=null && Math.abs(player.maxUpStep()-1)<1E-6) player.setMaxUpStep(previous);
        }
        if(player.level().isClientSide) return;
        if(deeply) {buff(player,MobEffects.WATER_BREATHING,230,3);buff(player,MobEffects.NIGHT_VISION,230,1);VersionBridge.attribute(player,"swim_speed","tinker_deeply",7,0);VersionBridge.velocity(player);}
        else VersionBridge.removeAttribute(player,"swim_speed","tinker_deeply");
        if(!player.isAlive()) return;
        // Both old Strongly instances subscribed globally and accepted either
        // material: the effective legacy result is StrengthII for both.
        if(count(player,"strongly")>0) buff(player,MobEffects.DAMAGE_BOOST,40,1);
        if(count(player,"jump_master")>0) buff(player,MobEffects.JUMP,40,1);
        if(count(player,"resistance")>0) buff(player,MobEffects.DAMAGE_RESISTANCE,100,1);
        if(count(player,"volcano")>0) player.clearFire();
        if(count(player,"prometheum")>0) player.removeEffect(MobEffects.POISON);
        if(count(player,"blindness")>0) player.removeEffect(MobEffects.BLINDNESS);
        boolean eyes=count(player,"cat_eyes")>0;
        if(eyes) {buff(player,MobEffects.NIGHT_VISION,220,0);EYES.add(player);}
        else if(EYES.remove(player)) {var effect=player.getEffect(MobEffects.NIGHT_VISION);if(!deeply && effect!=null && effect.getAmplifier()==0 && effect.getDuration()<=220) player.removeEffect(MobEffects.NIGHT_VISION);}
        if(count(player,"foodly")>0 && player instanceof ServerPlayer server && player.tickCount%20==0 && player.canEat(false) && (player.experienceLevel>0 || player.experienceProgress*player.getXpNeededForNextLevel()>=4)) {
            player.giveExperiencePoints(-4);player.getFoodData().eat(1,.5F);
            server.playNotifySound(SoundEvents.GENERIC_EAT,SoundSource.PLAYERS,.3F,1.5F);
        }
    }
    private static void buff(Player player,MobEffect effect,int ticks,int amplifier) {player.addEffect(new MobEffectInstance(effect,ticks,amplifier,false,false));}
    public static void using(LivingEntityUseItemEvent.Tick event) {
        if(!(event.getEntity() instanceof Player player)) return;int count=count(player,"quickly");
        if(count>0 && (event.getDuration()+1)%4<count) {VersionBridge.useTick(player,event.getItem(),event.getDuration());event.setDuration(event.getDuration()-1);}
    }
    private TinkerArmorEvents() {}
}
