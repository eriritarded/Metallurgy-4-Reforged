package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.entity.PierKnight;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
public final class PierKnightRenderer extends HumanoidMobRenderer<PierKnight,AvatarRenderState,PlayerModel> {
    private static final Identifier SKIN=Identifier.fromNamespaceAndPath("metallurgy","textures/models/pierknight_skin.png");
    private static final class KnightState extends AvatarRenderState { boolean wide;float fade; }
    public PierKnightRenderer(EntityRendererProvider.Context context) { super(context,new PlayerModel(context.bakeLayer(ModelLayers.PLAYER),false),0.5F); }
    @Override public AvatarRenderState createRenderState() { return new KnightState(); }
    @Override public void extractRenderState(PierKnight knight,AvatarRenderState state,float partial) {
        super.extractRenderState(knight,state,partial);var own=(KnightState)state;own.wide=knight.wide();own.fade=Math.max(0,Math.min(1,1-(partial+(knight.deathTime>0?knight.deathTime:knight.vanish()))/40F));
    }
    @Override public Identifier getTextureLocation(AvatarRenderState state) { return SKIN; }
    @Override protected int getModelTint(AvatarRenderState state) { return ((int)(((KnightState)state).fade*255)<<24)|0xffffff; }
    @Override protected float getFlipDegrees() { return 0; }
    @Override protected void scale(AvatarRenderState state,PoseStack pose) { if(((KnightState)state).wide) pose.scale(3,1,1); }
}
