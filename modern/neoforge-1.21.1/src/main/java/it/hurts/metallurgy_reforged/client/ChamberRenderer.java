package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.machine.ChamberEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemDisplayContext;
public final class ChamberRenderer implements BlockEntityRenderer<ChamberEntity> {
    public ChamberRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ChamberEntity chamber,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var stack=chamber.getItem(0);if(stack.isEmpty()) return;
        pose.pushPose();pose.translate(0.5,0.25,0.5);pose.scale(1.7F,1.7F,1.7F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.GROUND,light,overlay,pose,buffers,chamber.getLevel(),0);
        pose.popPose();
    }
}
