package it.hurts.metallurgy_reforged.effect;

import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.*;
import java.util.Comparator;

/** Server-owned delays from ProgressiveEffectsHandler; progress survives saves.
 * A synchronous callback retains its step until it returns, preventing its
 * own follow-up hit from scheduling another copy of the same ability. */
public final class WeaponLifecycle {
    private record Timer(String id,int steps,int delay) {}
    private static final Timer[] TIMERS={new Timer("carmot",3,10),new Timer("inolashite",1,10),
        new Timer("mithril",1,20),new Timer("quicksilver",11,1),new Timer("shadow_steel",2,20),new Timer("vulcanite",6,20)};
    private static String key(String id,String field) { return "weapon."+id+"."+field; }
    public static boolean active(Player player,String id) {
        try(State state=VersionBridge.entity(player)) { return state.integer(key(id,"step"))>0; }
    }
    private static void begin(Player player,String id,boolean increment) {
        try(State state=VersionBridge.entity(player)) {
            String stepKey=key(id,"step");int step=state.integer(stepKey);
            if(step>0 && !increment) return;
            int max=3;
            for(Timer timer:TIMERS) if(timer.id.equals(id)) max=timer.steps;
            if(step>=max) { state.remove(stepKey);state.remove(key(id,"at"));return; }
            state.integer(stepKey,step+1);
            if(step==0) state.longValue(key(id,"at"),player.level().getGameTime());
        }
    }
    private static void clear(Player player,String id) {
        try(State state=VersionBridge.entity(player)) { state.remove(key(id,"step"));state.remove(key(id,"at")); }
    }
    public static void hurt(Player player,LivingEntity target,float damage) {
        String metal=EquipmentEffects.weapon(player);
        if(metal.equals("carmot")) {
            try(State state=VersionBridge.entity(player)) {
                state.integer(key(metal,"target"),target.getId());state.decimal(key(metal,"damage"),damage);
            }
            begin(player,metal,true);
        }
    }
    public static void damaged(Player player) {
        String metal=EquipmentEffects.weapon(player);
        if(metal.equals("mithril")) {
            if(active(player,metal)) return;
            String base=EquipmentEffects.baseMetal(player.getMainHandItem());
            try(State state=VersionBridge.entity(player)) { state.string(key(metal,"material"),base); }
            begin(player,metal,false);
        } else if(metal.equals("inolashite") || metal.equals("quicksilver")) begin(player,metal,false);
    }
    public static float shadowSteel(LivingEntity attacker,float amount) {
        BlockPos pos=attacker.blockPosition();
        int sky=VersionBridge.day(attacker)?Math.min(14,attacker.level().getBrightness(LightLayer.SKY,pos)):0;
        int block=Math.min(14,attacker.level().getBrightness(LightLayer.BLOCK,pos));
        float darkness=15-Math.max(sky,block)*15F/14;
        if(darkness>=8 && attacker instanceof Player player) {
            if(!active(player,"shadow_steel")) {
                VersionBridge.attribute(player,"attack_speed","shadow_steel_weapon",Math.round(darkness)-8,0);
                begin(player,"shadow_steel",false);
            }
            player.resetAttackStrengthTicker();
        } else if(darkness<=6) amount+=6-darkness;
        return amount;
    }
    public static void primeHit(LivingEntity attacker,LivingEntity target) {
        try(State state=VersionBridge.stack(attacker.getMainHandItem())) {
            if(!state.flag("primed")) return;
            state.flag("primed",false);
        }
        if(attacker instanceof Player player) clear(player,"vulcanite");
        float cooldown=attacker instanceof Player player?VersionBridge.cooldown(player,attacker.getMainHandItem()):100;
        VersionBridge.explode(attacker,target.position(),0.32F+(1-cooldown)*1.1F);
        target.knockback(2,attacker.getX()-target.getX(),attacker.getZ()-target.getZ());
    }
    public static boolean rightClick(Player player,InteractionHand hand) {
        if(!(player.level() instanceof ServerLevel level)) return false;
        String metal=EquipmentEffects.weapon(player);
        ItemStack weapon=player.getMainHandItem();
        // Main-hand ownership makes two-hand interaction packets deterministic.
        if(hand!=InteractionHand.MAIN_HAND) return false;
        if(metal.equals("celenegil")) {
            try(State state=VersionBridge.stack(weapon)) {
                boolean enabled=!state.flag("glory_seeker");state.flag("glory_seeker",enabled);
                VersionBridge.sound(player,"entity.item.pickup",1,enabled?1:0.5F);
            }
            return true;
        }
        if(VersionBridge.cooldown(player,weapon)>0) return false;
        if(metal.equals("vulcanite")) {
            VersionBridge.sound(player,"entity.tnt.primed",0.75F,1.25F);
            try(State state=VersionBridge.stack(weapon)) { state.flag("primed",true); }
            // The old null-player increment left the initial timestamp at -1.
            begin(player,metal,true);
            try(State state=VersionBridge.entity(player)) { state.longValue(key(metal,"at"),-1); }
            VersionBridge.cooldown(player,weapon,50);return true;
        }
        if(metal.equals("atlarus")) {
            Vec3 p=player.position();
            for(Entity target:level.getEntities(player,new AABB(p,p).inflate(5))) {
                Vec3 direction=new Vec3(target.getX()-p.x,0.6,target.getZ()-p.z).normalize();
                target.setDeltaMovement(target.getDeltaMovement().add(direction.x*1.5,direction.y,direction.z*1.5));
                VersionBridge.velocity(target);
                for(int i=0;i<4;i++) {var random=level.getRandom();level.sendParticles(ParticleTypes.CLOUD,target.getX()+(random.nextDouble()-.5)*target.getBbWidth(),target.getY()+random.nextDouble()*target.getBbHeight(),target.getZ()+(random.nextDouble()-.5)*target.getBbWidth(),0,direction.x,0,direction.z,1);}
            }
            BlockPos origin=player.blockPosition();
            for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-5,-5,-5),origin.offset(5,5,5))) {
                var state=level.getBlockState(pos);
                if(state.is(BlockTags.LEAVES) || state.getBlock() instanceof net.minecraft.world.level.block.VineBlock) VersionBridge.breakBlock(player,pos);
            }
            for(int i=0;i<60;i++) level.sendParticles(ParticleTypes.CLOUD,p.x,p.y+0.4,p.z,0,.7-level.getRandom().nextDouble()*1.4,.1,.7-level.getRandom().nextDouble()*1.4,1);
            VersionBridge.sound(player,"entity.blaze.shoot",1,0.75F);
            VersionBridge.cooldown(player,weapon,40);player.swing(hand);return true;
        }
        if(metal.equals("electrum")) {
            VersionBridge.sound(player,"entity.shulker.open",1,1);
            Vec3 look=player.getLookAngle(),eye=player.getEyePosition(),end=eye.add(look.scale(8));
            Entity target=level.getEntities(player,player.getBoundingBox().expandTowards(look.scale(8)),e->!e.isSpectator() && e.isAlive() && e.isPickable())
                .stream().filter(e->e.getBoundingBox().inflate(0.30000001192092896).clip(eye,end).isPresent())
                .min(Comparator.comparingDouble(e->e.distanceToSqr(player))).orElse(null);
            if(target!=null) {
                Vec3 offset=target.getEyePosition().subtract(eye);
                target.setDeltaMovement(look.scale(-offset.length()*0.25));VersionBridge.velocity(target);
                int count=(int)(offset.length()*5);
                for(int i=0;i<count;i++) it.hurts.metallurgy_reforged.visual.EffectParticles.point(level,eye.add(offset.scale(i/(double)count)),metal,1,1.5F,true,3,null);
                VersionBridge.cooldown(player,weapon,120);
            }
            return true;
        }
        return false;
    }
    public static void tick(Player player) {
        if(!(player.level() instanceof ServerLevel level)) return;
        for(Timer timer:TIMERS) {
            int step;long at;
            try(State state=VersionBridge.entity(player)) { step=state.integer(key(timer.id,"step"));at=state.longValue(key(timer.id,"at")); }
            if(step==0 || level.getGameTime()<at+timer.delay) continue;
            step(player,timer,step);
            try(State state=VersionBridge.entity(player)) {
                if(state.integer(key(timer.id,"step"))==0) continue;
                if(step>=timer.steps) { state.remove(key(timer.id,"step"));state.remove(key(timer.id,"at")); }
                else { state.integer(key(timer.id,"step"),step+1);state.longValue(key(timer.id,"at"),level.getGameTime()); }
            }
        }
    }
    private static void step(Player player,Timer timer,int step) {
        LivingEntity target=VersionBridge.lastTarget(player);
        switch(timer.id) {
            case "carmot" -> {
                int targetId;float amount;
                try(State state=VersionBridge.entity(player)) { targetId=state.integer(key(timer.id,"target"));amount=(int)state.decimal(key(timer.id,"damage")); }
                if(player.level().getEntity(targetId) instanceof LivingEntity anchor) {
                    Vec3 pos=anchor.position();
                    for(LivingEntity other:player.level().getEntitiesOfClass(LivingEntity.class,new AABB(pos,pos).inflate(2),e->e!=anchor && anchor.getClass().isInstance(e))) {
                        VersionBridge.hit(other,other.damageSources().generic(),amount);it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(other,"carmot",5,1.5F);
                    }
                }
            }
            case "mithril" -> {
                String material;
                try(State state=VersionBridge.entity(player)) { material=state.string(key(timer.id,"material")); }
                var stats=Materials.BY_NAME.get(material);
                if(target!=null && target.isAlive() && stats!=null && stats.hasTools()) {
                    VersionBridge.hit(target,target.damageSources().magic(),Math.max(1,stats.tool("damage",0)*0.3F));it.hurts.metallurgy_reforged.visual.EffectParticles.point((ServerLevel)player.level(),target.position().add(0,target.getBbHeight()*.6,0),"mithril",10,2,true,6,Vec3.ZERO,new Vec3(.075,.075,.075));
                }
            }
            case "inolashite" -> {
                var speed=player.getAttribute(Attributes.ATTACK_SPEED);
                if(speed==null) return;
                double previous=speed.getBaseValue();
                try {
                    speed.setBaseValue(1000);
                    if(target!=null && target.isAlive()) { target.invulnerableTime=0;player.attack(target); }
                    player.swing(InteractionHand.MAIN_HAND);
                } finally { speed.setBaseValue(previous); }
            }
            case "quicksilver" -> {
                if(step>=timer.steps) { restoreDash(player,true);return; }
                if(target==null || !target.isAlive()) return;
                try(State state=VersionBridge.entity(player)) {
                    if(step==1) {
                        Vec3 velocity=target.position().subtract(player.position()).scale(0.6);
                        state.decimal(key(timer.id,"vx"),(float)velocity.x);state.decimal(key(timer.id,"vy"),(float)velocity.y);state.decimal(key(timer.id,"vz"),(float)velocity.z);
                    }
                    player.setDeltaMovement(state.decimal(key(timer.id,"vx")),state.decimal(key(timer.id,"vy")),state.decimal(key(timer.id,"vz")));
                    if(!state.flag(key(timer.id,"owns_invulnerability"))) {
                        state.flag(key(timer.id,"previous_invulnerability"),player.isInvulnerable());
                        state.flag(key(timer.id,"owns_invulnerability"),true);
                    }
                }
                VersionBridge.velocity(player);
                if(target.getBoundingBox().intersects(player.getBoundingBox())) {
                    target.invulnerableTime=0;VersionBridge.hit(target,target.damageSources().playerAttack(player),4);it.hurts.metallurgy_reforged.visual.EffectParticles.weapon(target,"quicksilver",10,2);
                }
                player.setInvulnerable(true);
            }
            case "shadow_steel" -> { if(step==timer.steps) VersionBridge.removeAttribute(player,"attack_speed","shadow_steel_weapon"); }
            case "vulcanite" -> {
                if(step==timer.steps) {
                    VersionBridge.explode(null,player,player.position().add(0,0.65,0),0.8F);
                    try(State state=VersionBridge.stack(player.getMainHandItem())) { state.flag("primed",false); }
                }
            }
        }
    }
    private static void restoreDash(Player player,boolean stop) {
        try(State state=VersionBridge.entity(player)) {
            if(state.flag(key("quicksilver","owns_invulnerability"))) {
                player.setInvulnerable(state.flag(key("quicksilver","previous_invulnerability")));
                state.remove(key("quicksilver","owns_invulnerability"));state.remove(key("quicksilver","previous_invulnerability"));
            }
        }
        if(stop) { player.setDeltaMovement(Vec3.ZERO);VersionBridge.velocity(player); }
    }
    public static void cleanup(Player player) {
        restoreDash(player,false);
        VersionBridge.removeAttribute(player,"attack_speed","shadow_steel_weapon");
        for(Timer timer:TIMERS) clear(player,timer.id);
    }
    private WeaponLifecycle() {}
}
