package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.entity.PierKnight;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class PierKnightRenderer extends HumanoidMobRenderer<PierKnight,PierKnightRenderer.FadeModel> {
    private static final ResourceLocation SKIN=new ResourceLocation("metallurgy","textures/models/pierknight_skin.png");
    public PierKnightRenderer(EntityRendererProvider.Context context) { super(context,new FadeModel(context),0.5F); }
    @Override public ResourceLocation getTextureLocation(PierKnight knight) { return SKIN; }
    @Override protected float getFlipDegrees(PierKnight knight) { return 0; }
    @Override protected void scale(PierKnight knight,PoseStack pose,float partial) { if(knight.wide()) pose.scale(3,1,1); }
    public static final class FadeModel extends PlayerModel<PierKnight> {
        private float fade=1;
        FadeModel(EntityRendererProvider.Context context) { super(context.bakeLayer(ModelLayers.PLAYER),false); }
        @Override public void prepareMobModel(PierKnight knight,float limb,float speed,float partial) {
            super.prepareMobModel(knight,limb,speed,partial);fade=Math.max(0,Math.min(1,1-(partial+(knight.deathTime>0?knight.deathTime:knight.vanish()))/40F));
        }
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,float red,float green,float blue,float alpha) { super.renderToBuffer(pose,vertices,light,overlay,red,green,blue,alpha*fade); }
    }
}
