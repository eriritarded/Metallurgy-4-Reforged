package it.hurts.metallurgy_reforged.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
@EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class GadgetOverlay {
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc=Minecraft.getInstance();if(mc.level==null || mc.player==null) return;
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        var vertices=buffers.getBuffer(GadgetRenderTypes.ORES);
        for(var ore:DetectorDisplay.ores()) {int rgb=ore.color();LevelRenderer.renderLineBox(pose,vertices,new AABB(ore.pos()),(rgb>>16&255)/255F,(rgb>>8&255)/255F,(rgb&255)/255F,1);}
        buffers.endBatch(GadgetRenderTypes.ORES);
        for(var pos:DetectorDisplay.redstone()) {
            var block=mc.level.getBlockState(pos);if(!DetectorDisplay.redstoneBlock(block.getBlock())) continue;
            pose.pushPose();pose.translate(pos.getX(),pos.getY(),pos.getZ());
            mc.getBlockRenderer().renderSingleBlock(block,pose,type->buffers.getBuffer(GadgetRenderTypes.REDSTONE),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffers.endBatch(GadgetRenderTypes.REDSTONE);pose.popPose();
    }
    private GadgetOverlay() {}
}
