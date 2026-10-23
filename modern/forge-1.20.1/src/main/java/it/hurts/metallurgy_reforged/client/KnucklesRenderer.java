package it.hurts.metallurgy_reforged.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
public final class KnucklesRenderer extends BlockEntityWithoutLevelRenderer {
    private static KnucklesRenderer instance;
    private static final ResourceLocation TEXTURE=new ResourceLocation("metallurgy","textures/items/gadgets/brass_knuckles.png");
    private final KnucklesParts[] models={new KnucklesParts(false,false),new KnucklesParts(false,true),new KnucklesParts(true,false),new KnucklesParts(true,true)};
    private KnucklesRenderer() {super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static KnucklesRenderer instance() {if(instance==null) instance=new KnucklesRenderer();return instance;}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();var player=mc.player;
        boolean left=context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        boolean first=context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context==ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        boolean slim=player!=null && player.getModelName().equals("slim");var parts=models[(left?2:0)+(slim?1:0)];
        pose.pushPose();
        if(first && player!=null) {
            float swing=(float)Math.sin(player.getAttackAnim(mc.isPaused()?1:mc.getFrameTime())*Math.PI);
            pose.translate(swing*0.8F,0,0);pose.mulPose(Axis.ZP.rotationDegrees(swing*-30));
        }
        pose.scale(2.4F,2.4F,2.4F);
        if(first && player!=null) {
            var skin=buffers.getBuffer(RenderType.entityCutoutNoCull(player.getSkinTextureLocation()));
            parts.arm.render(pose,skin,light,overlay);parts.sleeve.render(pose,skin,light,overlay);
        }
        parts.knuckles.render(pose,ItemRenderer.getFoilBufferDirect(buffers,RenderType.entityCutoutNoCull(TEXTURE),false,stack.hasFoil()),light,overlay);
        pose.popPose();
    }
}
