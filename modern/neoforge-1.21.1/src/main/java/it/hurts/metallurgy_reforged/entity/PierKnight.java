package it.hurts.metallurgy_reforged.entity;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Source Pier Knight: owner response, thirty-second idle life and40-tick fades. */
public final class PierKnight extends PathfinderMob {
    private static final EntityDataAccessor<Integer> THICKNESS=SynchedEntityData.defineId(PierKnight.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> WIDE=SynchedEntityData.defineId(PierKnight.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> VANISH=SynchedEntityData.defineId(PierKnight.class,EntityDataSerializers.INT);
    private UUID owner;
    private int remaining=20,ownerHurtAt,ownerAttackAt;
    public PierKnight(EntityType<? extends PierKnight> type,Level level) { super(type,level);setPersistenceRequired();setDropChance(EquipmentSlot.MAINHAND,0); }
    public static AttributeSupplier.Builder attributes() { return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED,0.3).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.MAX_HEALTH,20); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder);builder.define(THICKNESS,1);builder.define(WIDE,false);builder.define(VANISH,40); }
    public void summon(LivingEntity owner,LivingEntity attacker,int level) {
        this.owner=owner.getUUID();setTarget(attacker);remaining=600;thickness(level);setItemSlot(EquipmentSlot.MAINHAND,Content.stack("damascus_steel_sword"));
        boolean wide=false;for(var slot:EquipmentEffects.ARMOR_SLOTS) if(owner.getItemBySlot(slot).getHoverName().getString().toLowerCase(Locale.ROOT).contains("putin")) wide=true;
        entityData.set(WIDE,wide);
    }
    public LivingEntity owner() { return level() instanceof ServerLevel server && owner!=null && server.getEntity(owner) instanceof LivingEntity living?living:null; }
    public int thickness() { return entityData.get(THICKNESS); }
    public void thickness(int value) { int count=Math.max(1,Math.min(4,value));entityData.set(THICKNESS,count);getAttribute(Attributes.MAX_HEALTH).setBaseValue(10*count);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2*count); }
    public boolean wide() { return entityData.get(WIDE); }
    public int vanish() { return entityData.get(VANISH); }
    @Override public boolean isNoAi() { return vanish()>0 || super.isNoAi(); }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canBeLeashed() { return false; }
    @Override protected boolean canAddPassenger(Entity passenger) { return false; }
    @Override public void setTarget(LivingEntity target) { remaining=600;super.setTarget(target); }
    @Override public boolean doHurtTarget(Entity target) { return VersionBridge.knightStrike(this,target); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(5,new MeleeAttackGoal(this,1,true));goalSelector.addGoal(6,new Follow(this));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(7,new RandomLookAroundGoal(this));goalSelector.addGoal(8,new RandomStrollGoal(this,1));
        targetSelector.addGoal(1,new OwnerTarget(this,true));targetSelector.addGoal(2,new OwnerTarget(this,false));targetSelector.addGoal(3,new HurtByTargetGoal(this).setAlertOthers());
    }
    @Override public void aiStep() {
        super.aiStep();
        if(!VersionBridge.client(this)) {
            LivingEntity owner=owner();
            boolean matches=false;
            if(owner!=null) try(State state=VersionBridge.entity(owner)) { matches=state.string("pier_uuid").equals(getUUID().toString()); }
            if(vanish()==0 && getTarget()==null) remaining--;
            if(isAlive() && (remaining<=0 || owner==null || !owner.isAlive() || EquipmentEffects.armorLevel(owner,"damascus_steel")==0 || !matches)) VersionBridge.hit(this,damageSources().generic(),Integer.MAX_VALUE);
            if(vanish()>0) { smoke(this,true);if(owner!=null) smoke(owner,false);entityData.set(VANISH,vanish()-1); }
        }
    }
    private void smoke(Entity entity,boolean inward) {
        if(!(level() instanceof ServerLevel server)) return;
        Vec3 center=entity.getBoundingBox().getCenter();
        for(int i=0;i<8;i++) {
            Vec3 point=new Vec3(entity.getX()+(random.nextDouble()-0.5)*entity.getBbWidth(),entity.getY()+random.nextDouble()*entity.getBbHeight(),entity.getZ()+(random.nextDouble()-0.5)*entity.getBbWidth());
            Vec3 motion=inward?center.subtract(point).scale(0.1):point.subtract(center).scale(0.2);
            server.sendParticles(ParticleTypes.SMOKE,point.x,point.y,point.z,0,motion.x,motion.y,motion.z,1);
        }
    }
    @Override protected void tickDeath() { deathTime++;smoke(this,false);LivingEntity owner=owner();if(owner!=null) smoke(owner,true);if(deathTime>=40 && !VersionBridge.client(this)) discard(); }
    @Override public void die(DamageSource source) { super.die(source);CompanionArmor.released(this,owner()); }
    @Override protected void dropAllDeathLoot(ServerLevel level,DamageSource source) { /* Summoned equipment and knight itself drop nothing. */ }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);if(owner!=null) tag.putString("OwnerUUID",owner.toString());tag.putInt("PierThickness",thickness());tag.putInt("PierLifespan",remaining);tag.putBoolean("WidePutin",wide());tag.putInt("VanishTime",vanish());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);try { owner=UUID.fromString(tag.getString("OwnerUUID")); }catch(IllegalArgumentException ignored) { owner=null; }
        thickness(tag.getInt("PierThickness"));remaining=tag.contains("PierLifespan")?tag.getInt("PierLifespan"):20;entityData.set(WIDE,tag.getBoolean("WidePutin"));entityData.set(VANISH,Math.max(0,Math.min(40,tag.getInt("VanishTime"))));
    }
    private static final class OwnerTarget extends Goal {
        private final PierKnight knight;private final boolean hurt;private LivingEntity target;
        OwnerTarget(PierKnight knight,boolean hurt) { this.knight=knight;this.hurt=hurt;setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            LivingEntity owner=knight.owner();if(owner==null) return false;
            target=hurt?owner.getLastHurtByMob():owner.getLastHurtMob();int stamp=hurt?owner.getLastHurtByMobTimestamp():owner.getLastHurtMobTimestamp();
            return stamp!=(hurt?knight.ownerHurtAt:knight.ownerAttackAt) && target!=null && target!=owner && target.isAlive() && knight.canAttack(target);
        }
        @Override public void start() {
            knight.setTarget(target);LivingEntity owner=knight.owner();if(owner!=null) { if(hurt) knight.ownerHurtAt=owner.getLastHurtByMobTimestamp();else knight.ownerAttackAt=owner.getLastHurtMobTimestamp(); }
        }
        @Override public boolean canContinueToUse() { return false; }
    }
    private static final class Follow extends Goal {
        private final PierKnight knight;private LivingEntity owner;private int recalc;private float waterCost;
        Follow(PierKnight knight) { this.knight=knight;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() { owner=knight.owner();return owner!=null && !(owner instanceof Player player && player.isSpectator()) && knight.getTarget()==null && knight.distanceToSqr(owner)>=100; }
        @Override public boolean canContinueToUse() { return owner!=null && !knight.getNavigation().isDone() && knight.distanceToSqr(owner)>4; }
        @Override public void start() { recalc=0;waterCost=knight.getPathfindingMalus(PathType.WATER);knight.setPathfindingMalus(PathType.WATER,0); }
        @Override public void stop() { owner=null;knight.getNavigation().stop();knight.setPathfindingMalus(PathType.WATER,waterCost); }
        @Override public void tick() {
            if(owner==null) return;knight.getLookControl().setLookAt(owner,10,knight.getMaxHeadXRot());
            if(--recalc>0) return;recalc=10;
            if(knight.getNavigation().moveTo(owner,1.1) || knight.distanceToSqr(owner)<144) return;
            BlockPos corner=owner.blockPosition().offset(-2,-1,-2);
            for(int x=0;x<=4;x++) for(int z=0;z<=4;z++) {
                if(x>0 && x<4 && z>0 && z<4) continue;BlockPos floor=corner.offset(x,0,z);
                if(!knight.level().hasChunkAt(floor)) continue;var state=knight.level().getBlockState(floor);
                if(state.isFaceSturdy(knight.level(),floor,Direction.DOWN) && state.isValidSpawn(knight.level(),floor,knight.getType()) && knight.level().isEmptyBlock(floor.above()) && knight.level().isEmptyBlock(floor.above(2))) {
                    knight.moveTo(floor.getX()+0.5,floor.getY()+1,floor.getZ()+0.5,knight.getYRot(),knight.getXRot());knight.getNavigation().stop();return;
                }
            }
        }
    }
}
