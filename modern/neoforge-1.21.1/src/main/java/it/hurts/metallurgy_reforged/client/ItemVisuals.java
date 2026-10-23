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
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {MaterialTooltips.append(event.getItemStack(),event.getToolTip(),Screen.hasShiftDown(),Screen.hasControlDown());GadgetTooltips.append(event.getItemStack(),event.getToolTip());}
    @SubscribeEvent public static void color(RenderTooltipEvent.Color event) {
        var material=MaterialItems.material(event.getItemStack());if(material==null || !GeneralConfig.metalColoredTooltipBorders) return;
        event.setBorderStart(0xff000000|material.color());event.setBorderEnd(0xff000000|material.color());
    }
    private ItemVisuals() {}
}
