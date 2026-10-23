package it.hurts.metallurgy_reforged.mixin;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BlockModelRenderState.class)
public interface BlockModelRenderAccess {
    @Accessor("renderType") void metallurgy$renderType(RenderType type);
}
