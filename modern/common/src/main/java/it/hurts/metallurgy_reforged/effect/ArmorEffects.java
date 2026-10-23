package it.hurts.metallurgy_reforged.effect;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

/** Passive, damage-response and targeting armor callbacks. Activated/phasing,
 * companion and corpse lifecycles are implemented separately. */
public final class ArmorEffects {
    private static int level(LivingEntity entity,String metal) { return EquipmentEffects.armorLevel(entity,metal); }
    private static ItemStack representative(LivingEntity entity,String metal) {
        for(var slot:EquipmentEffects.ARMOR_SLOTS) { ItemStack stack=entity.getItemBySlot(slot);if(EquipmentEffects.material(stack).equals(metal)) return stack; }
        return ItemStack.EMPTY;
    }
    public static boolean incoming(LivingEntity entity,DamageSource source,float amount) {
        int celenegil=level(entity,"celenegil");
        if(celenegil>0 && entity instanceof Player) {
            try(State state=VersionBridge.entity(entity)) {
                int hits=state.integer("celenegil_hits");
                if(hits>4) {
                    entity.removeAllEffects();VersionBridge.buff(entity,"regeneration",20+20*celenegil,1);
                    BlockPos pos=entity.blockPosition();
                    for(Entity other:entity.level().getEntities(entity,new AABB(Vec3.atLowerCornerOf(pos.offset(-5,-1,-5)),Vec3.atLowerCornerOf(pos.offset(5,1,5))))) {
                        Vec3 push=new Vec3(other.getX()-entity.getX(),0.6,other.getZ()-entity.getZ()).normalize();
                        other.setDeltaMovement(other.getDeltaMovement().add(push.multiply(1.5,1,1.5)));other.hurtMarked=true;
                    }
                    state.integer("celenegil_hits",0);if(entity.level() instanceof ServerLevel server) it.hurts.metallurgy_reforged.visual.EffectParticles.block(server,entity.blockPosition(),"celenegil",60,4,true,6,new Vec3(-.1,.01,-.1),new Vec3(.8,0,.8));VersionBridge.award(entity,"challenges/loyal_friends");
                } else state.integer("celenegil_hits",hits+1);
                // The original bundle's inactive timer is never started: its
                // active flag is never set. Keep the observed sixth-hit cycle.
            }
        }
        int count=level(entity,"shadow_iron");
        if(count>0 && Math.random()<count/10F) {
            List<ItemStack> pieces=new ArrayList<>();
            for(var slot:EquipmentEffects.ARMOR_SLOTS) { ItemStack piece=entity.getItemBySlot(slot);if(EquipmentEffects.material(piece).equals("shadow_iron")) pieces.add(piece); }
            if(!pieces.isEmpty()) {
                ItemStack piece=pieces.get(entity.getRandom().nextInt(pieces.size()));
                VersionBridge.damageStack(entity,piece,Math.round(amount/200F*piece.getMaxDamage()));
                VersionBridge.sound(entity,"item.shield.block",1,1);it.hurts.metallurgy_reforged.visual.EffectParticles.front(entity,"shadow_iron",10,1.25F,true,2);VersionBridge.award(entity,"challenges/loyal_friends");return true;
            }
        }
        count=level(entity,"black_steel");
        if(count>0 && entity.getRandom().nextBoolean()) {
            int amplifier=VersionBridge.amplifier(entity,"resistance");
            if(count>amplifier && amplifier<=2) {
                VersionBridge.buff(entity,"slowness",200,amplifier+1);VersionBridge.buff(entity,"resistance",200,amplifier+1);
                VersionBridge.sound(entity,"block.anvil.place",0.7F,0.7F);it.hurts.metallurgy_reforged.visual.EffectParticles.entity(entity,"black_steel",10,3,true,9);
            }
        }
        count=level(entity,"electrum");
        if(count>0 && entity instanceof Player player) VersionBridge.chargeInventory(player,(1-(player.getHealth()-amount)/player.getMaxHealth())*(7000+(count-1)*3000));
        return false;
    }
    public static void damaged(LivingEntity entity,DamageSource source) {
        CompanionArmor.damaged(entity,source);
        if(level(entity,"kalendrite")>0) {
            ItemStack piece=representative(entity,"kalendrite");
            try(State state=VersionBridge.stack(piece)) {
                if(state.has("damage_timestamp")) VersionBridge.removeBuff(entity,"regeneration");
                state.longValue("damage_timestamp",entity.level().getGameTime());
            }
        }
        int count=level(entity,"osmium");
        if(count>0 && source.getDirectEntity() instanceof LivingEntity attacker) {
            double x=entity.getX()-attacker.getX(),z=entity.getZ()-attacker.getZ();
            while(x*x+z*z<1.0E-4) { z=(Math.random()-Math.random())*0.01;x=(Math.random()-Math.random())*0.01; }
            attacker.knockback(0.3F*count,x,z);
        }
    }
    public static boolean preventDeath(LivingEntity entity) {
        if(level(entity,"adamantine")==0) return false;
        var slot=EquipmentEffects.ARMOR_SLOTS.get(entity.getRandom().nextInt(4));ItemStack piece=entity.getItemBySlot(slot);
        if(entity instanceof Player player && VersionBridge.cooldown(player,piece)!=0) return false;
        if(!EquipmentEffects.material(piece).equals("adamantine")) return false;
        entity.setItemSlot(slot,ItemStack.EMPTY);entity.setHealth(1);
        VersionBridge.sound(entity,"item.totem.use",1,1);VersionBridge.buff(entity,"regeneration",500,2);VersionBridge.buff(entity,"absorption",60,1);
        if(entity instanceof Player player) for(String form:List.of("helmet","chestplate","leggings","boots")) VersionBridge.cooldown(player,Content.stack("adamantine_"+form),100);
        VersionBridge.award(entity,"challenges/loyal_friends");VersionBridge.totem(entity,piece);
        return true;
    }
    public static void death(LivingEntity victim,DamageSource source) {
        if(!(source.getEntity() instanceof LivingEntity owner) || !(victim instanceof Mob dead) || dead.getTarget()!=owner) return;
        int count=level(owner,"orichalcum");if(count==0) return;
        int radius=2+count*3;
        int attackers=owner.level().getEntitiesOfClass(Mob.class,new AABB(owner.blockPosition()).inflate(radius),mob->mob!=owner && mob.isAlive() && mob.getTarget()==owner).size();
        if(attackers>0) VersionBridge.buff(owner,"strength",count*60,Math.min(5,attackers-1));
    }
    public static LivingEntity target(Mob mob,LivingEntity target) {
        if(!(mob instanceof Shulker) || target==null || level(target,"eximite")<4) return target;
        LivingEntity result=mob.level().getEntitiesOfClass(Player.class,mob.getBoundingBox().inflate(16),player->player.isAlive() && !player.isSpectator() && level(player,"eximite")<4 && mob.canAttack(player) && mob.getSensing().hasLineOfSight(player))
            .stream().min(Comparator.comparingDouble(player->player.distanceToSqr(mob))).orElse(null);
        it.hurts.metallurgy_reforged.visual.EffectParticles.entity(mob,"eximite",20,4,false,3);return result;
    }
    public static void useTick(LivingEntity entity,ItemStack stack,int duration,java.util.function.IntConsumer setter) {
        int count=level(entity,"carmot");
        if(count>0) it.hurts.metallurgy_reforged.visual.EffectParticles.front(entity,"carmot",2,.4F,true,5);
        if(count>0 && (duration+1)%4<count) { VersionBridge.useTick(entity,stack,duration);setter.accept(duration-1); }
    }
    public static void potions(LivingEntity entity,MobEffectInstance added,MobEffectInstance removed) {
        int count=level(entity,"vyroxeres");
        if(count==0) { VersionBridge.removeAttribute(entity,"armor","vyroxeres_armor");return; }
        List<MobEffectInstance> effects=new ArrayList<>(entity.getActiveEffects());
        if(removed!=null) effects.removeIf(effect->effect.getEffect().equals(removed.getEffect()));
        if(added!=null) { effects.removeIf(effect->effect.getEffect().equals(added.getEffect()));effects.add(added); }
        int stacks=effects.stream().mapToInt(effect->effect.getAmplifier()+1).sum();
        if(stacks>0) VersionBridge.attribute(entity,"armor","vyroxeres_armor",stacks*(count<=2?count:count-0.5),0);
        else VersionBridge.removeAttribute(entity,"armor","vyroxeres_armor");
    }
    public static void tick(LivingEntity entity) {
        if(!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        ArmorLifecycle.livingTick(entity);
        if(level(entity,"atlarus")>0) for(Entity other:level.getEntities(entity,entity.getBoundingBox().inflate(3))) {
            double distance=other.position().distanceTo(new Vec3(other.xo,other.yo,other.zo));
            if(other.getBoundingBox().intersects(entity.getBoundingBox().inflate(1)) || !(other instanceof net.minecraft.world.entity.projectile.Projectile) && distance<0.6) continue;
            Vec3 motion=other.getDeltaMovement();
            if(motion.lengthSqr()<1E-12 || !other.getBoundingBox().expandTowards(motion).inflate(1).intersects(entity.getBoundingBox())) continue;
            Vec3 reversed=motion.reverse().normalize();
            var random=level.getRandom();
            Vec3 result=reversed.add((1-random.nextGaussian()*2)*0.252,(1-random.nextGaussian()*2)*0.252,(1-random.nextGaussian()*2)*0.252).scale(0.5);
            other.setDeltaMovement(result);other.hurtMarked=true;
            other.setYRot((float)(Math.atan2(result.x,result.z)*180/Math.PI));other.setXRot((float)(Math.atan2(result.y,result.horizontalDistance())*180/Math.PI));
            other.yRotO=other.getYRot();other.xRotO=other.getXRot();
            double x=other.getX()+(random.nextDouble()-0.5)*other.getBbWidth(),y=other.getY()+random.nextDouble()*other.getBbHeight(),z=other.getZ()+(random.nextDouble()-0.5)*other.getBbWidth();
            for(int i=0;i<5;i++) level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,x,y,z,0,result.x,result.y,result.z,1);
        }
        int ceruclase=level(entity,"ceruclase");
        if(ceruclase>0) {
            BlockPos pos=entity.blockPosition();
            for(LivingEntity other:level.getEntitiesOfClass(LivingEntity.class,new AABB(Vec3.atLowerCornerOf(pos.offset(-8,-4,-8)),Vec3.atLowerCornerOf(pos.offset(8,4,8))),other->entity instanceof Player?other instanceof Mob:other instanceof Player)) {
                VersionBridge.buff(other,"weakness",10,Math.max(ceruclase-2,0));VersionBridge.buff(other,"mining_fatigue",10,Math.max(ceruclase-2,0));it.hurts.metallurgy_reforged.visual.EffectParticles.entity(other,"ceruclase",1,0.85F,false,5);
                DamageSource last=other.getLastDamageSource();
                if(last!=null && (last.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) || last.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)) && entity.getRandom().nextBoolean()) {
                    other.clearFire();VersionBridge.sound(other,"entity.generic.extinguish_fire",1,1);
                }
            }
        }
        int count=level(entity,"deep_iron");
        if(count>0 && entity.isInWater()) {
            if(entity.tickCount%30==0) {
                VersionBridge.buff(entity,"water_breathing",230,0,false,false);
                if(count>=2) VersionBridge.buff(entity,"night_vision",230,count-3,false,false);
            }
            VersionBridge.attribute(entity,"swim_speed","deep_iron_swim",count,0);
            if(entity.getDeltaMovement().y<0) entity.setDeltaMovement(entity.getDeltaMovement().add(0,-0.025*count,0));
        } else VersionBridge.removeAttribute(entity,"swim_speed","deep_iron_swim");
        count=level(entity,"astral_silver");
        if(count>0 && entity.tickCount%80==0 && level.canSeeSky(BlockPos.containing(entity.getEyePosition())) && (level.dimension()==Level.END || !VersionBridge.day(entity)&&level.dimensionType().hasSkyLight())) {
            VersionBridge.buff(entity,"night_vision",300,0);
            if(count>1) VersionBridge.buff(entity,"speed",100,count-2);
        }
        if(level(entity,"carmot")>=4 && entity.tickCount%40==0) VersionBridge.buff(entity,"haste",100,2);
        count=level(entity,"kalendrite");
        if(count>0 && entity.tickCount%4==0) {
            try(State state=VersionBridge.stack(representative(entity,"kalendrite"))) {
                if(state.has("damage_timestamp") && level.getGameTime()-state.longValue("damage_timestamp")>=(20<<5-count) && VersionBridge.duration(entity,"regeneration")<=200) VersionBridge.buff(entity,"regeneration",410,count-1);
            }
        }
        for(var slot:EquipmentEffects.ARMOR_SLOTS) {
            ItemStack piece=entity.getItemBySlot(slot);
            if(!EquipmentEffects.material(piece).equals("haderoth")) continue;
            try(State state=VersionBridge.stack(piece)) {
                if(!state.flag("reborn")) continue;
                switch(slot) {
                    case HEAD->VersionBridge.removeBuff(entity,"hunger");case LEGS->VersionBridge.removeBuff(entity,"slowness");case FEET->VersionBridge.removeBuff(entity,"levitation");
                    case CHEST->{if(entity.tickCount%40==0) VersionBridge.buff(entity,"fire_resistance",60,0,false,false);}
                }
            }
        }
        count=level(entity,"platinum");
        if(count>0) for(var pair:Map.of("poison","regeneration","slowness","speed","weakness","strength").entrySet()) {
            int amp=VersionBridge.amplifier(entity,pair.getKey());
            if(amp>=0) {
                int duration=(int)(count/2F*VersionBridge.duration(entity,pair.getKey()));VersionBridge.removeBuff(entity,pair.getKey());VersionBridge.buff(entity,pair.getValue(),duration,amp);it.hurts.metallurgy_reforged.visual.EffectParticles.front(entity,"platinum",2,1,true,5);it.hurts.metallurgy_reforged.visual.EffectParticles.entity(entity,"platinum",5,1,true,5);
            }
        }
        count=level(entity,"oureclase");
        if(count>0 && entity.tickCount%10==0 && entity.isSprinting()) {
            Vec3 origin=Vec3.atLowerCornerOf(entity.blockPosition());
            var nearby=level.getEntities(entity,new AABB(origin,origin).inflate(3));
            for(Entity other:nearby) if(other instanceof LivingEntity target) {
                VersionBridge.hit(target,target.damageSources().flyIntoWall(),count*2-2);
                Vec3 direction=entity.position().subtract(target.position()).normalize();target.knockback(0.4F+0.4F/3*(count-1),direction.x,direction.z);
            }
            if(!nearby.isEmpty()) VersionBridge.modSound(entity,"crusher_impact",0.4F,1);
        }
        count=level(entity,"angmallen");
        if(entity instanceof Player player && count>0 && entity.tickCount%20==0 && entity.getRandom().nextFloat()<count*0.1F) {
            BlockPos origin=entity.blockPosition(),nearest=null;
            for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-4,-4,-4),origin.offset(3,3,3))) {
                if(!level.hasChunkAt(pos)) continue;
                String id=VersionBridge.blockId(level.getBlockState(pos));
                if(!id.startsWith("metallurgy:") || !id.endsWith("_ore")) continue;
                var material=Materials.BY_NAME.get(id.substring(11,id.length()-4));
                if(material!=null && material.oreLevel()>3 && (nearest==null || pos.distSqr(origin)<nearest.distSqr(origin))) nearest=pos.immutable();
            }
            if(nearest!=null) VersionBridge.privateSound(player,nearest.getCenter(),"block.note_block.bell",0.75F,1);
        }
        if(level(entity,"vyroxeres")==0) VersionBridge.removeAttribute(entity,"armor","vyroxeres_armor");
    }
    public static double visibility(LivingEntity entity) { return 1-level(entity,"hepatizon")/5D; }
    private ArmorEffects() {}
}
