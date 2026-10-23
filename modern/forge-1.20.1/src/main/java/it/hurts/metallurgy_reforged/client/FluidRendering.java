package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.fluid.FluidCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/** Original animated fluid artwork, tinted with each original metal color. */
public final class FluidRendering {
    public static IClientFluidTypeExtensions extensions(FluidCatalog.Definition definition) {
        return new IClientFluidTypeExtensions() {
            public ResourceLocation getStillTexture() {return new ResourceLocation("metallurgy","blocks/molten_metal_still");}
            public ResourceLocation getFlowingTexture() {return new ResourceLocation("metallurgy","blocks/molten_metal_flow");}
            public int getTintColor() {return definition.color();}
            public ResourceLocation getRenderOverlayTexture(net.minecraft.client.Minecraft minecraft) {return definition.tar()?new ResourceLocation("metallurgy","textures/effects/fluid_tar.png"):null;}
        };
    }
    private FluidRendering() {}
}
