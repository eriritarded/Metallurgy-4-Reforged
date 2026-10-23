package it.hurts.metallurgy_reforged.mixin;
import it.hurts.metallurgy_reforged.effect.ArmorMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
    @Inject(method="getCollisionShape",at=@At("HEAD"),cancellable=true)
    private void metallurgy$liquidSurface(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context,CallbackInfoReturnable<VoxelShape> callback) {
        if(context instanceof EntityCollisionContext entity && entity.getEntity() instanceof Player player && ArmorMovement.liquid(player,state.getFluidState())) callback.setReturnValue(Shapes.block());
    }
}
