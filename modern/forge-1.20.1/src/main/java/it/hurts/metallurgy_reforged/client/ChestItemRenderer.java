package it.hurts.metallurgy_reforged.client;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.metallurgy_reforged.inventory.MetalChestEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import java.util.*;

public final class ChestItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static ChestItemRenderer instance;
    private final Map<Item,MetalChestEntity> entities=new IdentityHashMap<>();
    private ChestItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    public static ChestItemRenderer instance() { if(instance==null) instance=new ChestItemRenderer();return instance; }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(stack.getItem() instanceof BlockItem item) {
            MetalChestEntity entity=entities.computeIfAbsent(item,key->new MetalChestEntity(BlockPos.ZERO,item.getBlock().defaultBlockState()));
            Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(entity,pose,buffers,light,overlay);
        }
    }
}
