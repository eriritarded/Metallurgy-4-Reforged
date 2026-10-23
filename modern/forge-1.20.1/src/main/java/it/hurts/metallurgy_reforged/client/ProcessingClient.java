package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.recipe.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.*;
import java.util.List;
@Mod.EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class ProcessingClient {
    private static List<ProcessingAccess.Entry> recipes=List.of();
    static {ProcessingAccess.clientEntries=()->recipes;}
    @SubscribeEvent public static void received(RecipesUpdatedEvent event) {ProcessingAccess.invalidate();recipes=ProcessingAccess.read(event.getRecipeManager());}
    @SubscribeEvent public static void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {recipes=List.of();ProcessingAccess.invalidate();}
    private ProcessingClient() {}
}
