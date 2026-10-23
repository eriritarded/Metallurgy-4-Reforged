package it.hurts.metallurgy_reforged.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ModelEvent;
import java.util.List;
/** Stateless context selection; no global last-perspective field or per-frame model bake. */
@Mod.EventBusSubscriber(modid="metallurgy",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class KnucklesModel implements BakedModel {
    private final BakedModel flat;
    private KnucklesModel(BakedModel flat) {this.flat=flat;}
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event) {
        var key=new ModelResourceLocation(new ResourceLocation("metallurgy","brass_knuckles"),"inventory");
        event.getModels().computeIfPresent(key,(id,model)->new KnucklesModel(model));
    }
    @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left) {
        flat.applyTransform(context,pose,left);
        return context==ItemDisplayContext.GUI || context==ItemDisplayContext.GROUND || context==ItemDisplayContext.FIXED?flat:this;
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random) {return flat.getQuads(state,side,random);}
    @Override public boolean useAmbientOcclusion() {return flat.useAmbientOcclusion();}
    @Override public boolean isGui3d() {return flat.isGui3d();}
    @Override public boolean usesBlockLight() {return flat.usesBlockLight();}
    @Override public boolean isCustomRenderer() {return true;}
    @Override public TextureAtlasSprite getParticleIcon() {return flat.getParticleIcon();}
    @Override public ItemOverrides getOverrides() {return flat.getOverrides();}
}
