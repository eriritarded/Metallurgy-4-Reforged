package it.hurts.metallurgy_reforged.gadget;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
public final class PotashFertilizerItem extends Item {
    public PotashFertilizerItem(Properties properties) {super(properties);}
    @Override public InteractionResult useOn(UseOnContext context) {
        var world=context.getLevel();var pos=context.getClickedPos();
        if(!BoneMealItem.applyBonemeal(context.getItemInHand(),world,pos,context.getPlayer())) return InteractionResult.FAIL;
        if(world instanceof ServerLevel server) for(int i=0;i<50;i++) server.sendParticles(net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA,false,false,pos.getX()+world.getRandom().nextDouble(),pos.getY()+world.getRandom().nextDouble(),pos.getZ()+world.getRandom().nextDouble(),0,0,0,0,1);
        return InteractionResult.SUCCESS;
    }
}
