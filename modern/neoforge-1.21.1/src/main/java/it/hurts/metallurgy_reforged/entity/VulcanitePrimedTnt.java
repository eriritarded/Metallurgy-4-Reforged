package it.hurts.metallurgy_reforged.entity;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.ToolBridge;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import java.util.UUID;
public final class VulcanitePrimedTnt extends PrimedTnt {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> SOFTENED=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("metallurgy","vulcanite_softened"));
    private UUID ownerId;
    public VulcanitePrimedTnt(EntityType<? extends PrimedTnt> type,Level level) {super(type,level);setBlockState(Content.BLOCK_MAP.get("vulcanite_tnt").get().defaultBlockState());}
    public static VulcanitePrimedTnt create(Level level,BlockPos pos,LivingEntity owner) {
        var entity=new VulcanitePrimedTnt(Content.VULCANITE_TNT_ENTITY.get(),level);
        entity.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);double angle=level.getRandom().nextDouble()*Math.PI*2;
        entity.setDeltaMovement(-Math.sin(angle)*0.02,0.2,-Math.cos(angle)*0.02);entity.setFuse(80);entity.ownerId=owner==null?null:owner.getUUID();
        entity.xo=entity.getX();entity.yo=entity.getY();entity.zo=entity.getZ();return entity;
    }
    @Override public LivingEntity getOwner() {return ownerId!=null && level() instanceof ServerLevel server && server.getEntity(ownerId) instanceof LivingEntity entity?entity:null;}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.putInt("tnt_type",0);if(ownerId!=null) tag.putUUID("metallurgy_owner",ownerId);}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);ownerId=tag.hasUUID("metallurgy_owner")?tag.getUUID("metallurgy_owner"):null;}
    @Override public float getBlockExplosionResistance(Explosion explosion,BlockGetter level,BlockPos pos,BlockState state,FluidState fluid,float resistance) {
        return state.is(SOFTENED)?resistance/7F:resistance;
    }
    @Override public boolean shouldBlockExplode(Explosion explosion,BlockGetter level,BlockPos pos,BlockState state,float power) {
        return ToolBridge.required(state)<=Materials.BY_NAME.get("vulcanite").tool("harvest_level",4);
    }
    @Override protected void explode() {
        var calculator=new EntityBasedExplosionDamageCalculator(this) {
            @Override public float getEntityDamageAmount(Explosion explosion,Entity entity) {return (float)Math.floor((super.getEntityDamageAmount(explosion,entity)+1)/2);}
        };
        level().explode(this,Explosion.getDefaultDamageSource(level(),this),calculator,getX(),getY(),getZ(),6,false,Level.ExplosionInteraction.TNT);
    }
}
