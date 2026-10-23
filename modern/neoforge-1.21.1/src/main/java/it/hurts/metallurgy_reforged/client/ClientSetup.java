package it.hurts.metallurgy_reforged.client;

import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid="metallurgy",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void setup(RegisterMenuScreensEvent event) { event.register(Content.CHEST_MENU.get(),ChestScreen::new);event.register(Content.MACHINE_MENU.get(),MachineScreen::new); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(Content.CHEST_ENTITY.get(),ChestRenderer::new);event.registerBlockEntityRenderer(Content.CHAMBER_ENTITY.get(),ChamberRenderer::new);event.registerEntityRenderer(Content.PIER_KNIGHT.get(),PierKnightRenderer::new);event.registerEntityRenderer(Content.VULCANITE_TNT_ENTITY.get(),VulcaniteTntRenderer::new); }
}
