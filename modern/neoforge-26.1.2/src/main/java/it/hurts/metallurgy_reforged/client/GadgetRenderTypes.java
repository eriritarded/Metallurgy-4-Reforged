package it.hurts.metallurgy_reforged.client;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
public final class GadgetRenderTypes {
    public static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("metallurgy","pipeline/monocle"))
        .withShaderDefine("ALPHA_CUTOUT",0.1F).withDepthStencilState(java.util.Optional.empty()).withCull(false).build();
    public static final RenderType REDSTONE=RenderType.create("metallurgy_monocle",RenderSetup.builder(PIPELINE)
        .withTexture("Sampler0",TextureAtlas.LOCATION_BLOCKS).useLightmap().createRenderSetup());
    private GadgetRenderTypes() {}
}
