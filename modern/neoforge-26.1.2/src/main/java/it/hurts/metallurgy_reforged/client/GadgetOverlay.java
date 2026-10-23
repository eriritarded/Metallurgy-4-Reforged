package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.mixin.BlockModelRenderAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.util.*;
@EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class GadgetOverlay {
    private record Model(BlockPos pos,BlockModelRenderState state) {}
    private record Frame(List<DetectorDisplay.Ore> ores,List<Model> models) {}
    private static final ContextKey<Frame> FRAME=new ContextKey<>(Identifier.fromNamespaceAndPath("metallurgy","gadget_scan"));
    private static final BlockDisplayContext CONTEXT=BlockDisplayContext.create();
    @SubscribeEvent public static void extract(ExtractLevelRenderStateEvent event) {
        var positions=DetectorDisplay.redstone();
        if(positions.isEmpty()) {
            event.getRenderState().setRenderData(FRAME,new Frame(DetectorDisplay.ores(),List.of()));return;
        }
        var mc=Minecraft.getInstance();var models=new ArrayList<Model>(positions.size());
        for(var pos:positions) {
            var block=event.getLevel().getBlockState(pos);
            if(!DetectorDisplay.redstoneBlock(block.getBlock())) continue;
            var state=new BlockModelRenderState();
            mc.getBlockModelResolver().update(state,block,CONTEXT);
            ((BlockModelRenderAccess)(Object)state).metallurgy$renderType(GadgetRenderTypes.REDSTONE);
            for(var tint:mc.getBlockColors().getTintSources(block)) state.tintLayers().add(tint.colorInWorld(block,event.getLevel(),pos));
            models.add(new Model(pos,state));
        }
        event.getRenderState().setRenderData(FRAME,new Frame(DetectorDisplay.ores(),List.copyOf(models)));
    }
    @SubscribeEvent public static void submit(SubmitCustomGeometryEvent event) {
        if(Minecraft.getInstance().screen!=null) return;
        var state=event.getLevelRenderState();var frame=state.getRenderData(FRAME);if(frame==null) return;
        var camera=state.cameraRenderState.pos;var pose=event.getPoseStack();
        for(var model:frame.models()) {
            pose.pushPose();pose.translate(model.pos().getX()-camera.x,model.pos().getY()-camera.y,model.pos().getZ()-camera.z);
            model.state().submit(pose,event.getSubmitNodeCollector(),LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        }
    }
    @SubscribeEvent public static void ores(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        var frame=event.getLevelRenderState().getRenderData(FRAME);if(frame==null) return;
        try(var collection=event.getLevelRenderer().collectPerFrameGizmos()) {
            for(var ore:frame.ores()) Gizmos.cuboid(ore.pos(),GizmoStyle.stroke(0xff000000|ore.color(),2)).setAlwaysOnTop();
        }
    }
    private GadgetOverlay() {}
}
