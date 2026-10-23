package it.hurts.metallurgy_reforged.inventory;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

public final class MetalChestItem extends BlockItem {
    public MetalChestItem(Block block,Properties properties) { super(block,properties); }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return it.hurts.metallurgy_reforged.client.ChestItemRenderer.instance();
            }
        });
    }
}
