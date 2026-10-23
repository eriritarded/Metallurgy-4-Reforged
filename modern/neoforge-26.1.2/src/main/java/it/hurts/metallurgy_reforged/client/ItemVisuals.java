package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import it.hurts.metallurgy_reforged.material.MaterialItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.minecraft.client.gui.screens.Screen;
@EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class ItemVisuals {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {MaterialTooltips.append(event.getItemStack(),event.getToolTip(),com.mojang.blaze3d.platform.InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT),com.mojang.blaze3d.platform.InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL));GadgetTooltips.append(event.getItemStack(),event.getToolTip());}
    @SubscribeEvent public static void color(RenderTooltipEvent.Texture event) {
        var material=MaterialItems.material(event.getItemStack());TooltipTint.color(0xffffffff);
        if(material!=null && GeneralConfig.metalColoredTooltipBorders) {TooltipTint.color(0xff000000|material.color());event.setTexture(net.minecraft.resources.Identifier.fromNamespaceAndPath("metallurgy","metal"));}
    }
    private ItemVisuals() {}
}
