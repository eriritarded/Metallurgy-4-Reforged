package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import it.hurts.metallurgy_reforged.material.MaterialItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraft.client.gui.screens.Screen;
@Mod.EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class ItemVisuals {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {MaterialTooltips.append(event.getItemStack(),event.getToolTip(),Screen.hasShiftDown(),Screen.hasControlDown());GadgetTooltips.append(event.getItemStack(),event.getToolTip());}
    @SubscribeEvent public static void color(RenderTooltipEvent.Color event) {
        var material=MaterialItems.material(event.getItemStack());if(material==null || !GeneralConfig.metalColoredTooltipBorders) return;
        event.setBorderStart(0xff000000|material.color());event.setBorderEnd(0xff000000|material.color());
    }
    private ItemVisuals() {}
}
