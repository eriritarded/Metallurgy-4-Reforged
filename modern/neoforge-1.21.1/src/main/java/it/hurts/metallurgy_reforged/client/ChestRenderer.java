package it.hurts.metallurgy_reforged.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import it.hurts.metallurgy_reforged.inventory.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ChestRenderer implements BlockEntityRenderer<MetalChestEntity> {
    private final ModelPart bottom,lid,lock;
    public ChestRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root=context.bakeLayer(ModelLayers.CHEST);
        bottom=root.getChild("bottom");lid=root.getChild("lid");lock=root.getChild("lock");
    }
    @Override public void render(MetalChestEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        String name=BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).getPath();
        pose.pushPose();pose.translate(0.5F,0.5F,0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getBlockState().getValue(MetalChestBlock.FACING).toYRot()));
        pose.translate(-0.5F,-0.5F,-0.5F);
        float openness=entity.openness(partial);openness=1-(1-openness)*(1-openness)*(1-openness);
        lid.xRot=-(float)Math.PI/2*openness;lock.xRot=lid.xRot;
        VertexConsumer vertices=buffers.getBuffer(RenderType.entityCutout(ResourceLocation.fromNamespaceAndPath("metallurgychests","textures/blocks/"+name+".png")));
        lid.render(pose,vertices,light,overlay);lock.render(pose,vertices,light,overlay);bottom.render(pose,vertices,light,overlay);
        pose.popPose();
    }
}
