package it.hurts.metallurgy_reforged.world;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;
import java.util.function.Supplier;
public final class RetrogenEvents {
    private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,"metallurgy");
    private static final Supplier<AttachmentType<Boolean>> GENERATED=TYPES.register("ore_generated",()->AttachmentType.builder(()->false).serialize(Codec.BOOL.fieldOf("generated")).build());
    public static void register(net.neoforged.bus.api.IEventBus bus) {TYPES.register(bus);}
    @SubscribeEvent public static void load(ChunkEvent.Load event) {
        if(!(event.getLevel() instanceof ServerLevel level)) return;
        if(event.isNewChunk()) generated(event.getChunk());
        else if(!event.getChunk().getData(GENERATED.get())) Retrogen.loaded(level,event.getChunk().getPos());
    }
    @SubscribeEvent public static void unload(ChunkEvent.Unload event) {if(event.getLevel() instanceof ServerLevel level) Retrogen.unloaded(level,event.getChunk().getPos());}
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {if(event.getLevel() instanceof ServerLevel level) Retrogen.tick(level);}
    public static void generated(ChunkAccess chunk) {chunk.setData(GENERATED.get(),true);chunk.markUnsaved();}
    private RetrogenEvents() {}
}
