package it.hurts.metallurgy_reforged.recipe;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
@EventBusSubscriber(modid="metallurgy")
public final class ProcessingEvents {
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event) {ProcessingAccess.invalidate();it.hurts.metallurgy_reforged.network.ProcessingNetwork.sync(event);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {ProcessingAccess.invalidate();}
    private ProcessingEvents() {}
}
