package it.hurts.metallurgy_reforged.client;

import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="metallurgy",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) { event.enqueueWork(() -> { MenuScreens.register(Content.CHEST_MENU.get(),ChestScreen::new);MenuScreens.register(Content.MACHINE_MENU.get(),MachineScreen::new); }); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(Content.CHEST_ENTITY.get(),ChestRenderer::new);event.registerBlockEntityRenderer(Content.CHAMBER_ENTITY.get(),ChamberRenderer::new);event.registerEntityRenderer(Content.PIER_KNIGHT.get(),PierKnightRenderer::new);event.registerEntityRenderer(Content.VULCANITE_TNT_ENTITY.get(),VulcaniteTntRenderer::new); }
}
