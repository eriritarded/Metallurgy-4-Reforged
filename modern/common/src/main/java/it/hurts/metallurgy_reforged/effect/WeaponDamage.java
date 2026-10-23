package it.hurts.metallurgy_reforged.effect;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** Reviewed1.4.1 weapon damage/death equations. Delayed and activated effects
 * have a separate lifecycle, rather than being replaced with generic buffs. */
public final class WeaponDamage {
    private static final Random RANDOM=new Random();
    public static float beforeArmor(LivingEntity target,DamageSource source,float amount) {
        if(EquipmentEffects.weapon(target).equals("black_steel")) {
            float absorbed=amount*0.25F;
            VersionBridge.damageHeld(target,(int)(absorbed*2));
            amount-=absorbed;
            VersionBridge.sound(target,"block.anvil.place",0.7F,0.7F);
            it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,"black_steel",10,3);
        }
        LivingEntity owner=source.getEntity() instanceof LivingEntity living?living:null;
        LivingEntity direct=source.getDirectEntity() instanceof LivingEntity living?living:null;
        if(owner!=null) {
            String metal=EquipmentEffects.weapon(owner);
            switch(metal) {
                case "angmallen" -> {
                    boolean armor=EquipmentEffects.ARMOR_SLOTS.stream().anyMatch(slot->VersionBridge.isArmor(target.getItemBySlot(slot)));
                    if(armor) { amount*=1.75F;it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,10,1.5F); }
                }
                case "deep_iron" -> {
                    if(owner.isInWater()) { amount+=6;it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,5,0.6F); }
                }
                case "eximite" -> {
                    if(target.level().dimension()==Level.END) { amount*=1.5F;it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,15,2); }
                }
                case "ceruclase" -> {
                    if(owner.getRandom().nextInt(100)<75) { VersionBridge.buff(target,"slowness",60,255);VersionBridge.award(owner,"challenges/low_temperatures"); }
                }
                case "carmot" -> { if(owner instanceof Player player) WeaponLifecycle.hurt(player,target,amount); }
                case "desichalkos" -> {
                    VersionBridge.directDamage(target,source,amount);it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,30,2);amount=0;
                }
            }
        }
        if(direct!=null) {
            String metal=EquipmentEffects.weapon(direct);
            switch(metal) {
                case "astral_silver" -> {
                    if(target.level().dimension()!=Level.OVERWORLD) { amount*=1.45F;it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,10,1.5F); }
                }
                case "haderoth" -> {
                    try(State data=VersionBridge.stack(direct.getMainHandItem())) {
                        int kills=data.integer("kill_count");
                        if(kills>0 && data.string("killed_type").equals(VersionBridge.entityId(target))) {
                            amount=Math.min(amount+kills,12);it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,15,2);
                        }
                    }
                }
                case "hepatizon" -> {
                    if(target.getRandom().nextInt(8)==0) {
                        List<EquipmentSlot> slots=new ArrayList<>();
                        for(EquipmentSlot slot:EquipmentSlot.values()) if(!target.getItemBySlot(slot).isEmpty()) slots.add(slot);
                        if(!slots.isEmpty()) {
                            EquipmentSlot slot=slots.get(target.getRandom().nextInt(slots.size()));
                            ItemStack dropped=target.getItemBySlot(slot);target.setItemSlot(slot,ItemStack.EMPTY);
                            // Nondamageable mod equipment cannot supply nextInt(0).
                            if(dropped.getMaxDamage()>=2) dropped.setDamageValue(dropped.getMaxDamage()-(target.getRandom().nextInt(dropped.getMaxDamage()/2)+1));
                            VersionBridge.drop(target,dropped);it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,10,2.5F);
                        }
                    }
                }
                case "krik" -> {
                    if(!target.onGround()) { amount*=2;it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,8,2.5F); }
                    VersionBridge.buff(target,"levitation",25,0);
                }
                case "oureclase" -> {
                    if(target.getHealth()==target.getMaxHealth()) { amount*=2;it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,10,3); }
                }
                case "platinum" -> {
                    if(VersionBridge.undead(target)) { amount+=4;VersionBridge.burn(target,4);if(target.level() instanceof net.minecraft.server.level.ServerLevel level) it.hurts.metallurgy_reforged.visual.EffectParticles.point(level,target.getEyePosition(),metal,20,1.3F,true,6,net.minecraft.world.phys.Vec3.ZERO,new net.minecraft.world.phys.Vec3(.0625,.0625,.0625)); }
                }
                case "sanguinite" -> {
                    if(direct instanceof Player player) { amount=Math.min(amount+20-player.getFoodData().getFoodLevel(),20);it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,10,2); }
                }
                case "shadow_iron" -> {
                    if(RANDOM.nextInt(6)==0) { amount*=(float)(1.2+(it.hurts.metallurgy_reforged.config.EffectTweaksConfig.shadowIronWeaponCritUpperBound-1.2)*Math.random());it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,20,1.5F); }
                }
                case "shadow_steel" -> amount=WeaponLifecycle.shadowSteel(direct,amount);
            }
        }
        return amount;
    }

    public record FinalDamage(float amount,boolean cancelled) {}
    public static FinalDamage afterArmor(LivingEntity target,DamageSource source,float amount) {
        LivingEntity owner=source.getEntity() instanceof LivingEntity living?living:null;
        LivingEntity direct=source.getDirectEntity() instanceof LivingEntity living?living:null;
        if(owner!=null && EquipmentEffects.weapon(owner).equals("celenegil")) {
            ItemStack weapon=owner.getMainHandItem();
            if(owner instanceof Player player && VersionBridge.cooldown(player,weapon)>0) return new FinalDamage(0,true);
            try(State data=VersionBridge.stack(weapon)) {
                if(data.flag("glory_seeker")) {
                    amount*=1.35F;
                    if(target.getHealth()-amount<=0) { VersionBridge.buff(owner,"speed",160,0);VersionBridge.buff(owner,"strength",160,0); }
                    else if(owner instanceof Player player) VersionBridge.cooldown(player,weapon,100);
                }
            }
        }
        if(direct!=null) {
            String metal=EquipmentEffects.weapon(direct);
            if(direct instanceof Player player) WeaponLifecycle.damaged(player);
            if(metal.equals("vulcanite")) WeaponLifecycle.primeHit(direct,target);
            if(metal.equals("etherium") && RANDOM.nextInt(3)<1) {
                int augment=2+RANDOM.nextInt(3);amount+=augment;
                if(direct.getHealth()<direct.getMaxHealth()) direct.heal(augment);
                else if(VersionBridge.amplifier(direct,"absorption")<0) VersionBridge.buff(direct,"absorption",1200,0);
                it.hurts.metallurgy_reforged.visual.EffectParticles.etherium(target,direct);
            } else if(metal.equals("vyroxeres")) {
                int previous=VersionBridge.amplifier(target,"poison");
                VersionBridge.buff(target,"poison",60,previous+1);
                it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,metal,10,previous*0.5F);
            }
        }
        return new FinalDamage(amount,false);
    }

    public static void death(LivingEntity target,DamageSource source) {
        if(!(source.getDirectEntity() instanceof LivingEntity owner)) return;
        String metal=EquipmentEffects.weapon(owner);
        if(metal.equals("haderoth")) {
            try(State data=VersionBridge.stack(owner.getMainHandItem())) {
                if(!data.flag("reborn")) return;
                String type=VersionBridge.entityId(target);
                data.integer("kill_count",data.string("killed_type").equals(type)?data.integer("kill_count")+1:1);
                data.string("killed_type",type);
            }
        } else if(metal.equals("sanguinite") && owner instanceof Player player) {
            player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel()+Math.round(target.getMaxHealth()/5));
            VersionBridge.buff(player,"hunger",80,0);
        }
    }

    public static int experience(LivingEntity target,Player attacker,int original) {
        if(!(target instanceof Player) && attacker!=null && EquipmentEffects.weapon(attacker).equals("damascus_steel")) {
            it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,"damascus_steel",6,2);return original*3+2;
        }
        return original;
    }
    private WeaponDamage() {}
}
