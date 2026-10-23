package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.entity.VulcanitePrimedTnt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
public final class VulcaniteTntRenderer extends EntityRenderer<VulcanitePrimedTnt> {
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blocks;
    public VulcaniteTntRenderer(EntityRendererProvider.Context context) {super(context);blocks=context.getBlockRenderDispatcher();shadowRadius=0.5F;}
    @Override public void render(VulcanitePrimedTnt entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose();pose.translate(0,0.5,0);
        if(entity.getFuse()-partial+1<10) {float scale=Mth.clamp(1-(entity.getFuse()-partial+1)/10,0,1);scale*=scale;scale*=scale;scale=1+scale*0.3F;pose.scale(scale,scale,scale);}
        pose.mulPose(Axis.YP.rotationDegrees(-90));pose.translate(-0.5,-0.5,0.5);pose.mulPose(Axis.YP.rotationDegrees(90));
        TntMinecartRenderer.renderWhiteSolidBlock(blocks,Content.BLOCK_MAP.get("vulcanite_tnt").get().defaultBlockState(),pose,buffers,light,entity.getFuse()/5%2==0);
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(VulcanitePrimedTnt entity) {return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
}
