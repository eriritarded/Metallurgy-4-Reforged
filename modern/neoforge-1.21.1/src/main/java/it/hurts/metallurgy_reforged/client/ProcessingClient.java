package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.recipe.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
import java.util.List;
@EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class ProcessingClient {
    private static List<ProcessingAccess.Entry> recipes=List.of();
    static {ProcessingAccess.clientEntries=()->recipes;}
    @SubscribeEvent public static void received(RecipesUpdatedEvent event) {ProcessingAccess.invalidate();recipes=ProcessingAccess.read(event.getRecipeManager());}
    @SubscribeEvent public static void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {recipes=List.of();ProcessingAccess.invalidate();}
    private ProcessingClient() {}
}
