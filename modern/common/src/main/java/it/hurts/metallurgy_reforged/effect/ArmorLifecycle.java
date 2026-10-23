package it.hurts.metallurgy_reforged.effect;

import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.phys.*;
import java.util.*;

/** Original timed armor callbacks, with server-owned state and owned cleanup. */
public final class ArmorLifecycle {
    private static int level(LivingEntity entity,String metal) { return EquipmentEffects.armorLevel(entity,metal); }
    private static ItemStack piece(LivingEntity entity,String metal) {
        for(var slot:EquipmentEffects.ARMOR_SLOTS) { var stack=entity.getItemBySlot(slot);if(EquipmentEffects.material(stack).equals(metal)) return stack; }
        return ItemStack.EMPTY;
    }
    private static void cooldown(Player player,String metal,int ticks) {
        for(String form:List.of("helmet","chestplate","leggings","boots")) VersionBridge.cooldown(player,Content.stack(metal+"_"+form),ticks);
    }
    public static boolean attack(LivingEntity entity,DamageSource source,float amount) {
        if(ArmorMovement.attack(entity,source)) return true;
        int count=level(entity,"ignatius");if(count==0) return false;
        boolean lava=source.is(DamageTypes.LAVA);
        if(!lava && !source.is(DamageTypes.IN_FIRE) && !source.is(DamageTypes.ON_FIRE)) return false;
        if(lava && entity instanceof Player player) {
            try(State state=VersionBridge.entity(player)) {
                if(!state.flag("ignatius_active")) {
                    if(VersionBridge.cooldown(player,piece(player,"ignatius"))>0) return false;
                    state.flag("ignatius_active",true);state.integer("ignatius_step",1);state.longValue("ignatius_at",player.level().getGameTime());
                }
            }
        }
        if(lava && entity.tickCount%25-count*5==0) entity.heal(amount*(count/6F));
        return true;
    }
    public static boolean hurt(LivingEntity entity,DamageSource source,float amount) {
        if(entity instanceof Player player) {
            boolean absorbed=false;int absorbDetail=0;
            try(State state=VersionBridge.entity(player)) {
                int shields=state.integer("desichalkos_shields");
                if(shields>0) {absorbDetail=Math.min(9,shields*2);state.integer("desichalkos_shields",shields-1);absorbed=true; }
                state.integer("desichalkos_quiet",0);
            }
            if(absorbed) {
                it.hurts.metallurgy_reforged.visual.EffectParticles.entity(entity,"desichalkos",30,3,true,absorbDetail);VersionBridge.sound(entity,"entity.enderman.teleport",1.5F,0.3F);VersionBridge.award(entity,"challenges/loyal_friends");return true;
            }
        }
        int shadow=level(entity,"shadow_steel");
        if(shadow>0 && (!(entity instanceof Player player) || VersionBridge.cooldown(player,piece(player,"shadow_steel"))==0)) {
            if(entity instanceof Player) {
                try(State state=VersionBridge.entity(entity)) { if(state.has("shadow_armor_until")) return true; }
            } else if(VersionBridge.amplifier(entity,"speed")>=0) { VersionBridge.removeBuff(entity,"speed");return true; }
            BlockPos pos=entity.blockPosition();
            int sky=VersionBridge.day(entity)?Math.min(14,entity.level().getBrightness(LightLayer.SKY,pos)):0;
            int block=Math.min(14,entity.level().getBrightness(LightLayer.BLOCK,pos));
            if(1-Math.max(sky,block)/14F>0.5F) {
                if(entity instanceof Player) {
                    VersionBridge.attribute(entity,"movement_speed","shadow_steel_armor",0.20000000298023224*shadow,2);
                    try(State state=VersionBridge.entity(entity)) { state.longValue("shadow_armor_until",entity.level().getGameTime()+100); }
                } else if(entity.getRandom().nextBoolean()) VersionBridge.buff(entity,"speed",60,shadow-1);
            } else for(var other:entity.level().getEntitiesOfClass(LivingEntity.class,new AABB(entity.getX()-3,entity.getY()-1,entity.getZ()-3,entity.getX()+3,entity.getY()+1,entity.getZ()+3),other->other!=entity)) {
                VersionBridge.hit(other,other.damageSources().inFire(),3*shadow);VersionBridge.buff(other,"blindness",20+10*shadow,0);
            }
        }
        int vulcanite=level(entity,"vulcanite");
        if(vulcanite>0 && entity instanceof Player player) {
            boolean detonate=false;
            try(State state=VersionBridge.entity(entity)) {
                int hits=state.integer("vulcanite_armor_hits");detonate=hits>4;state.integer("vulcanite_armor_hits",detonate?0:hits+1);
            }
            if(detonate) for(Entity other:entity.level().getEntities(entity,new AABB(entity.getX()-5,entity.getY()-1,entity.getZ()-5,entity.getX()+5,entity.getY()+1,entity.getZ()+5))) {
                VersionBridge.armorExplosion(player,other.position().add(0,1.1,0),0.7F+vulcanite*0.1F,vulcanite>2);
                if(vulcanite>2) VersionBridge.buff(player,"fire_resistance",200,0);
                it.hurts.metallurgy_reforged.visual.EffectParticles.entity(other,"vulcanite",10,2.5F,true,3);
            }
        }
        return false;
    }
    public static void drank(LivingEntity entity,ItemStack stack) {
        if(it.hurts.metallurgy_reforged.config.EffectTweaksConfig.enableIgnatiusDrinkDamage && level(entity,"ignatius")>0 && VersionBridge.drink(stack)) {
            VersionBridge.hit(entity,VersionBridge.waterDamage(entity),2);VersionBridge.sound(entity,"block.fire.extinguish",0.5F,1);
        }
    }
    public static void livingTick(LivingEntity entity) {
        if(level(entity,"ignatius")>0 && entity.isInWaterOrRain() && VersionBridge.amplifier(entity,"fire_resistance")==-1) {
            if(entity.tickCount%10==0) { VersionBridge.hit(entity,VersionBridge.waterDamage(entity),1);VersionBridge.sound(entity,"block.fire.extinguish",0.5F,1); }
            VersionBridge.particles(entity,"ignatius",1,5);
        }
    }
    public static void tick(Player player) {
        if(!(player.level() instanceof net.minecraft.server.level.ServerLevel world)) return;
        long now=world.getGameTime();int desichalkos=level(player,"desichalkos");
        try(State state=VersionBridge.entity(player)) {
            if(desichalkos>0) {
                int shields=Math.min(state.integer("desichalkos_shields"),desichalkos),quiet=state.integer("desichalkos_quiet");
                if(quiet<200) state.integer("desichalkos_quiet",quiet+1);
                else if(player.tickCount%140==0 && shields<desichalkos) {
                    shields++;VersionBridge.sound(player,"entity.enderman.teleport",1.5F,1.3F);it.hurts.metallurgy_reforged.visual.EffectParticles.entity(player,"desichalkos",30,3,true,2);
                }
                state.integer("desichalkos_shields",shields);
            }
            if(state.flag("ignatius_active") && now-state.longValue("ignatius_at")>=10) {
                int step=state.integer("ignatius_step");
                if(!player.isInLava() || level(player,"ignatius")==0 || step>=level(player,"ignatius")*4) {
                    state.flag("ignatius_active",false);state.remove("ignatius_step");
                    if(step>=level(player,"ignatius")*4 && level(player,"ignatius")>0) cooldown(player,"ignatius",200);
                } else { state.integer("ignatius_step",step+1);state.longValue("ignatius_at",now); }
            }
            if(state.has("shadow_armor_until") && (now>=state.longValue("shadow_armor_until") || level(player,"shadow_steel")==0)) {
                VersionBridge.removeAttribute(player,"movement_speed","shadow_steel_armor");state.remove("shadow_armor_until");cooldown(player,"shadow_steel",20*(30-5*level(player,"shadow_steel")));
            }
        }
        prometheum(player,now);
        ArmorControls.tick(player);
    }
    private static void prometheum(Player player,long now) {
        int count=level(player,"prometheum");int step=0;boolean grow=false;
        try(State state=VersionBridge.entity(player)) {
            step=state.integer("prometheum_armor_step");
            if(step==0 && count>0 && player.isShiftKeyDown() && VersionBridge.cooldown(player,piece(player,"prometheum"))==0) {
                state.integer("prometheum_armor_step",1);state.longValue("prometheum_armor_at",now);state.flag("prometheum_crouch",true);return;
            }
            if(step==0 || now-state.longValue("prometheum_armor_at")<10) return;
            if(step%2==1) state.flag("prometheum_crouch",player.isShiftKeyDown());
            else grow=state.flag("prometheum_crouch")!=player.isShiftKeyDown();
            state.integer("prometheum_armor_step",step<4?step+1:0);state.longValue("prometheum_armor_at",now);
        }
        if(grow && count>0) {
            List<BlockPos> positions=new ArrayList<>();BlockPos pos=player.blockPosition();
            for(BlockPos test:BlockPos.betweenClosed(pos.offset(-3,-1,-3),pos.offset(3,1,3))) if(player.level().hasChunkAt(test) && player.level().getBlockState(test).getBlock() instanceof BonemealableBlock) positions.add(test.immutable());
            Collections.shuffle(positions);boolean grass=false;
            for(BlockPos test:positions) if(player.getRandom().nextFloat()<count*0.25F*0.75F) {
                boolean grassy=player.level().getBlockState(test).is(Blocks.GRASS_BLOCK);
                if(grass && grassy) continue;
                if(VersionBridge.bonemeal(player,test)) {
                    player.level().levelEvent(2005,test,0);
                    for(var slot:EquipmentEffects.ARMOR_SLOTS) { var stack=player.getItemBySlot(slot);if(EquipmentEffects.material(stack).equals("prometheum")) VersionBridge.damageStack(player,stack,2); }
                }
                if(player.level().getBlockState(test).is(Blocks.GRASS_BLOCK)) grass=true;
            }
        }
        if(step==4) cooldown(player,"prometheum",180);
    }
    public static void cleanup(Player player) {
        CompanionArmor.cleanup(player);
        ArmorMovement.cleanup(player);ArmorPhasing.cleanup(player);
        ArmorControls.cleanup(player);
        VersionBridge.removeAttribute(player,"movement_speed","shadow_steel_armor");
        try(State state=VersionBridge.entity(player)) {
            state.remove("shadow_armor_until");state.remove("ignatius_active");state.remove("ignatius_step");state.remove("prometheum_armor_step");
        }
    }
    private ArmorLifecycle() {}
}
