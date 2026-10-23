package it.hurts.metallurgy_reforged.fluid;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
public final class MetalBucketItem extends BucketItem {
    public MetalBucketItem(net.minecraft.world.level.material.Fluid fluid,Properties properties) {super(fluid,properties);}
    @Override public boolean emptyContents(LivingEntity player,Level level,BlockPos pos,BlockHitResult hit,ItemStack container) {
        return it.hurts.metallurgy_reforged.config.RegistrationConfig.categoryBlocks.enableMetalFluidBlocks && super.emptyContents(player,level,pos,hit,container);
    }
}
