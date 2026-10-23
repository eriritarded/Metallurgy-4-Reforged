package it.hurts.metallurgy_reforged.recipe;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
@Mod.EventBusSubscriber(modid="metallurgy")
public final class ProcessingEvents {
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event) {ProcessingAccess.invalidate();}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {ProcessingAccess.invalidate();}
    private ProcessingEvents() {}
}
