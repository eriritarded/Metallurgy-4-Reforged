package it.hurts.metallurgy_reforged.recipe;
import it.hurts.metallurgy_reforged.machine.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import java.util.*;
/** Native manager is the authority; reload/sync events invalidate cached lists. */
public final class ProcessingAccess {
    public record Entry(String id,ProcessingDefinition definition) {}
    private static final Map<RecipeManager,List<Entry>> CACHE=new WeakHashMap<>();
    public static synchronized void invalidate() {CACHE.clear();}
    public static java.util.function.Supplier<List<Entry>> clientEntries=List::of;
    public static List<Entry> entries(Level level) {
        if(level==null) return List.of();
        return level instanceof ServerLevel server?read(server.getRecipeManager()):clientEntries.get();
    }
    public static synchronized List<Entry> read(RecipeManager manager) {
        var previous=CACHE.get(manager);if(previous!=null) return previous;
        var entries=manager.getAllRecipesFor(ProcessingRegistration.TYPE.get()).stream().map(recipe->new Entry(recipe.id().toString(),recipe.value().definition)).sorted(Comparator.<Entry,Boolean>comparing(e->e.definition.kind().equals("generic_crusher")).thenComparing(Entry::id)).toList();
        CACHE.put(manager,entries);return entries;
    }
    private static boolean kind(ProcessingDefinition d,String kind) {return d.kind().equals(kind) || kind.equals("crusher") && d.kind().equals("generic_crusher");}
    public static MachineRecipes.Result result(Level level,boolean alloy,ItemStack first,ItemStack second) {
        for(var entry:entries(level)) if(kind(entry.definition,alloy?"alloyer":"crusher")) {var result=entry.definition.result(first,second);if(result!=null) return result;}
        return null;
    }
    public static boolean input(Level level,boolean alloy,ItemStack stack) {
        for(var entry:entries(level)) {var d=entry.definition;if(kind(d,alloy?"alloyer":"crusher") && (d.kind().equals("generic_crusher")?GenericCrushing.find(stack)!=null:d.input(stack))) return true;}
        return false;
    }
    public static boolean pair(Level level,ItemStack first,ItemStack second) {return entries(level).stream().anyMatch(e->e.definition.kind().equals("alloyer") && e.definition.orientation(first,second,false)!=0);}
    public static ProcessingDefinition chamber(Level level,ItemStack stack,boolean exact) {
        for(var entry:entries(level)) {var d=entry.definition;if(d.kind().equals("chamber") && d.orientation(stack,ItemStack.EMPTY,exact)!=0) return d;}
        return null;
    }
    private ProcessingAccess() {}
}
