package it.hurts.metallurgy_reforged.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import java.util.function.Consumer;
import org.joml.Vector3fc;
@EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class KnucklesRenderer implements SpecialModelRenderer<KnucklesRenderer.State> {
    public record State(Identifier skin,boolean slim,float swing) {}
    private static final Identifier TEXTURE=Identifier.fromNamespaceAndPath("metallurgy","textures/items/gadgets/brass_knuckles.png");
    private final KnucklesParts wide,slim;
    private final boolean first;
    private KnucklesRenderer(boolean first,boolean left) {this.first=first;wide=new KnucklesParts(left,false);slim=new KnucklesParts(left,true);}
    @Override public State extractArgument(ItemStack stack) {
        var mc=Minecraft.getInstance();var player=mc.player;var skin=player==null?DefaultPlayerSkin.getDefaultSkin():player.getSkin();
        float swing=first && player!=null?(float)Math.sin(player.getAttackAnim(mc.isPaused()?1:mc.getDeltaTracker().getGameTimeDeltaPartialTick(false))*Math.PI):0;
        return new State(skin.body().texturePath(),skin.model()==PlayerModelType.SLIM,swing);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline) {
        if(state==null) return;var parts=state.slim()?slim:wide;
        pose.pushPose();pose.translate(state.swing()*0.8F,0,0);pose.mulPose(Axis.ZP.rotationDegrees(state.swing()*-30));pose.scale(2.4F,2.4F,2.4F);
        if(first) {
            var skin=RenderTypes.entityCutout(state.skin());
            collector.submitModelPart(parts.arm,pose,skin,light,overlay,null,false,false,-1,null,outline);
            collector.submitModelPart(parts.sleeve,pose,skin,light,overlay,null,false,false,-1,null,outline);
        }
        collector.submitModelPart(parts.knuckles,pose,RenderTypes.entityCutout(TEXTURE),light,overlay,null,false,foil,-1,null,outline);pose.popPose();
    }
    @Override public void getExtents(Consumer<Vector3fc> output) {
        var pose=new PoseStack();pose.scale(2.4F,2.4F,2.4F);wide.knuckles.getExtentsForGui(pose,output);
        if(first) {wide.arm.getExtentsForGui(pose,output);wide.sleeve.getExtentsForGui(pose,output);}
    }
    public record Unbaked(boolean first,boolean left) implements SpecialModelRenderer.Unbaked<State> {
        public static final MapCodec<Unbaked> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.BOOL.optionalFieldOf("first_person",false).forGetter(Unbaked::first),Codec.BOOL.optionalFieldOf("left",false).forGetter(Unbaked::left)).apply(i,Unbaked::new));
        @Override public SpecialModelRenderer<State> bake(SpecialModelRenderer.BakingContext context) {return new KnucklesRenderer(first,left);}
        @Override public MapCodec<Unbaked> type() {return CODEC;}
    }
    @SubscribeEvent public static void register(RegisterSpecialModelRendererEvent event) {event.register(Identifier.fromNamespaceAndPath("metallurgy","knuckles"),Unbaked.CODEC);}
}
