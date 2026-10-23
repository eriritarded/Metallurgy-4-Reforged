package it.hurts.metallurgy_reforged.compat.spartan;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
/** Client-only class, entered only from the loaded optional integration. */
public final class SpartanClient {
    // Native API-created item constructors already register their own model
    // predicates on clients. Only our legacy layered palette is additional.
    public static void register(IEventBus bus) {bus.addListener(SpartanClient::colors);}
    private static void colors(RegisterColorHandlersEvent.Item event) {
        for(var entry:SpartanIntegration.ENTRIES) event.register((stack,index)->index==1 || index==2?entry.metal().color():0xffffff,entry.item().get());
    }
    private SpartanClient() {}
}
