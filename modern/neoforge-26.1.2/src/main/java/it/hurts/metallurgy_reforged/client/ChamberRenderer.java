package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.machine.ChamberEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
public final class ChamberRenderer implements BlockEntityRenderer<ChamberEntity,ChamberRenderer.State> {
    private final ItemModelResolver resolver;
    public ChamberRenderer(BlockEntityRendererProvider.Context context) {resolver=context.itemModelResolver();}
    public static final class State extends BlockEntityRenderState {final ItemStackRenderState item=new ItemStackRenderState();}
    @Override public State createRenderState() {return new State();}
    @Override public void extractRenderState(ChamberEntity chamber,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking) {BlockEntityRenderer.super.extractRenderState(chamber,state,partial,camera,breaking);resolver.updateForTopItem(state.item,chamber.getItem(0),ItemDisplayContext.GROUND,chamber.getLevel(),null,0);}
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {if(state.item.isEmpty()) return;pose.pushPose();pose.translate(0.5,0.25,0.5);pose.scale(1.7F,1.7F,1.7F);state.item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
}
