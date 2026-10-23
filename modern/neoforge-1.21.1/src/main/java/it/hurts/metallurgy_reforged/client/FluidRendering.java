package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.fluid.FluidCatalog;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Original animated fluid artwork, tinted with each original metal color. */
@net.neoforged.fml.common.EventBusSubscriber(modid="metallurgy",value=net.neoforged.api.distmarker.Dist.CLIENT,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class FluidRendering {
    public static IClientFluidTypeExtensions extensions(FluidCatalog.Definition definition) {
        return new IClientFluidTypeExtensions() {
            public ResourceLocation getStillTexture() {return ResourceLocation.fromNamespaceAndPath("metallurgy","blocks/molten_metal_still");}
            public ResourceLocation getFlowingTexture() {return ResourceLocation.fromNamespaceAndPath("metallurgy","blocks/molten_metal_flow");}
            public int getTintColor() {return definition.color();}
            public ResourceLocation getRenderOverlayTexture(net.minecraft.client.Minecraft minecraft) {return definition.tar()?ResourceLocation.fromNamespaceAndPath("metallurgy","textures/effects/fluid_tar.png"):null;}
        };
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void register(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        for(var entry:it.hurts.metallurgy_reforged.fluid.FluidContent.ENTRIES.values()) event.registerFluidType(extensions(entry.definition),entry.type.get());
    }
    private FluidRendering() {}
}
