package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.inventory.MetalChestEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
/** Native model submission also resets lid pose between distinct chest states. */
public final class ChestRenderer extends net.minecraft.client.renderer.blockentity.ChestRenderer<MetalChestEntity> {
    public ChestRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected SpriteId getCustomSprite(MetalChestEntity chest,ChestRenderState state) {
        String id=BuiltInRegistries.BLOCK.getKey(chest.getBlockState().getBlock()).getPath();
        return Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath("metallurgy",id));
    }
}
