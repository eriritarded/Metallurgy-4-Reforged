package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.fluid.FluidCatalog;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Original animated fluid artwork, tinted with each original metal color. */
@net.neoforged.fml.common.EventBusSubscriber(modid="metallurgy",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class FluidRendering {
    public static IClientFluidTypeExtensions extensions(FluidCatalog.Definition definition) {
        return new IClientFluidTypeExtensions() {
            public Identifier getStillTexture() {return Identifier.fromNamespaceAndPath("metallurgy","blocks/molten_metal_still");}
            public Identifier getFlowingTexture() {return Identifier.fromNamespaceAndPath("metallurgy","blocks/molten_metal_flow");}
            public int getTintColor() {return definition.color();}
            public Identifier getRenderOverlayTexture(net.minecraft.client.Minecraft minecraft) {return definition.tar()?Identifier.fromNamespaceAndPath("metallurgy","textures/effects/fluid_tar.png"):null;}
        };
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void register(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        for(var entry:it.hurts.metallurgy_reforged.fluid.FluidContent.ENTRIES.values()) event.registerFluidType(extensions(entry.definition),entry.type.get());
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void models(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        for(var entry:it.hurts.metallurgy_reforged.fluid.FluidContent.ENTRIES.values()) {
            var still=new net.minecraft.client.resources.model.sprite.Material(Identifier.fromNamespaceAndPath("metallurgy","blocks/molten_metal_still"));
            var flow=new net.minecraft.client.resources.model.sprite.Material(Identifier.fromNamespaceAndPath("metallurgy","blocks/molten_metal_flow"));
            event.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(still,flow,null,net.neoforged.neoforge.client.fluid.FluidTintSources.constant(entry.definition.color())),entry.still,entry.flowing);
        }
    }
    private FluidRendering() {}
}
