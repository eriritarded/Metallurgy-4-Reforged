package it.hurts.metallurgy_reforged.world;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.*;
import net.minecraftforge.event.TickEvent;
import java.util.*;
public final class RetrogenEvents {
    private static final String KEY="MetallurgyOreGeneration";
    private static final Map<ChunkAccess,Boolean> GENERATED=Collections.synchronizedMap(new WeakHashMap<>());
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {}
    @SubscribeEvent public static void read(ChunkDataEvent.Load event) {GENERATED.put(event.getChunk(),event.getData().getCompound(KEY).getBoolean("generated"));}
    @SubscribeEvent public static void save(ChunkDataEvent.Save event) {var tag=new CompoundTag();tag.putBoolean("generated",GENERATED.getOrDefault(event.getChunk(),true));event.getData().put(KEY,tag);}
    @SubscribeEvent public static void load(ChunkEvent.Load event) {
        if(!(event.getLevel() instanceof ServerLevel level)) return;
        if(event.isNewChunk()) generated(event.getChunk());
        else if(!GENERATED.getOrDefault(event.getChunk(),false)) Retrogen.loaded(level,event.getChunk().getPos());
    }
    @SubscribeEvent public static void unload(ChunkEvent.Unload event) {if(event.getLevel() instanceof ServerLevel level) Retrogen.unloaded(level,event.getChunk().getPos());}
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {if(event.phase==TickEvent.Phase.END && event.level instanceof ServerLevel level) Retrogen.tick(level);}
    public static void generated(ChunkAccess chunk) {GENERATED.put(chunk,true);chunk.setUnsaved(true);}
    private RetrogenEvents() {}
}
