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
    private static long generation;
    private static int expected,parts,bytes;
    private static java.util.Map<String,String> crafting=java.util.Map.of(),pendingCrafting=java.util.Map.of();
    public static String craftingRule(String kind) {return crafting.getOrDefault(kind,"");}
    private static final java.util.ArrayList<ProcessingAccess.Entry> pending=new java.util.ArrayList<>();
    private static void clearPending() {generation=0;expected=0;parts=0;bytes=0;pending.clear();pendingCrafting=java.util.Map.of();}
    public static void part(it.hurts.metallurgy_reforged.network.ProcessingNetwork.Part part) {
        if(part.index()==0) {clearPending();generation=part.generation();parts=part.total();pendingCrafting=part.crafting();}
        if(generation!=part.generation() || expected!=part.index() || parts!=part.total() || !pendingCrafting.equals(part.crafting())) {clearPending();return;}
        bytes+=part.bytes();pending.addAll(part.entries());expected++;
        if(bytes>it.hurts.metallurgy_reforged.network.ProcessingNetwork.MAX_TOTAL || pending.size()>it.hurts.metallurgy_reforged.network.ProcessingNetwork.MAX_RECIPES) {clearPending();throw new IllegalArgumentException("Oversized processing snapshot");}
        if(expected==parts) {
            var ids=new java.util.HashSet<String>();for(var entry:pending) if(!ids.add(entry.id())) {clearPending();throw new IllegalArgumentException("Duplicate processing recipe ID");}
            recipes=pending.stream().sorted(java.util.Comparator.comparing(ProcessingAccess.Entry::id)).toList();crafting=pendingCrafting;clearPending();
        }
    }
    @SubscribeEvent public static void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {recipes=List.of();crafting=java.util.Map.of();clearPending();ProcessingAccess.invalidate();}
    private ProcessingClient() {}
}
