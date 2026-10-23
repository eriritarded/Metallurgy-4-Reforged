package it.hurts.metallurgy_reforged.network;
import it.hurts.metallurgy_reforged.recipe.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
/** Maintained26-only adapter: never replaces a recipe viewer's global recipe map. */
public final class ProcessingNetwork {
    public static final int MAX_RECIPES=4096,MAX_CHUNK=256*1024,MAX_TOTAL=8*1024*1024,MAX_PARTS=256;
    private static final AtomicLong GENERATION=new AtomicLong();
    private static final StreamCodec<RegistryFriendlyByteBuf,ProcessingRecipe> RECIPES=ProcessingRecipe.Serializer.create().streamCodec();
    public record Part(long generation,int index,int total,List<ProcessingAccess.Entry> entries,int bytes,Map<String,String> crafting) implements CustomPacketPayload {
        public Part {entries=List.copyOf(entries);crafting=Map.copyOf(crafting);}
        public static final Type<Part> TYPE=new Type<>(Identifier.fromNamespaceAndPath("metallurgy","processing_snapshot"));
        @Override public Type<Part> type() {return TYPE;}
    }
    private static RegistryFriendlyByteBuf buffer(io.netty.buffer.ByteBuf bytes,RegistryAccess registries) {return new RegistryFriendlyByteBuf(bytes,registries,ConnectionType.NEOFORGE);}
    private static byte[] encode(List<ProcessingAccess.Entry> entries,RegistryAccess registries) {
        var buffer=buffer(Unpooled.buffer(),registries);
        try {
            buffer.writeVarInt(entries.size());for(var entry:entries) {buffer.writeUtf(entry.id(),256);RECIPES.encode(buffer,new ProcessingRecipe(entry.definition()));}
            byte[] result=new byte[buffer.readableBytes()];buffer.readBytes(result);return result;
        } finally {buffer.release();}
    }
    private static final StreamCodec<RegistryFriendlyByteBuf,Part> CODEC=new StreamCodec<>() {
        @Override public void encode(RegistryFriendlyByteBuf out,Part part) {
            out.writeVarLong(part.generation);out.writeVarInt(part.index);out.writeVarInt(part.total);
            out.writeVarInt(part.crafting.size());for(var rule:part.crafting.entrySet()) {out.writeUtf(rule.getKey(),16);out.writeUtf(rule.getValue(),256);}
            byte[] bytes=ProcessingNetwork.encode(part.entries,out.registryAccess());if(bytes.length>MAX_CHUNK) throw new IllegalArgumentException("Oversized processing recipe chunk");out.writeByteArray(bytes);
        }
        @Override public Part decode(RegistryFriendlyByteBuf in) {
            long generation=in.readVarLong();int index=in.readVarInt(),total=in.readVarInt();
            if(generation<1 || total<1 || total>MAX_PARTS || index<0 || index>=total) throw new IllegalArgumentException("Invalid processing snapshot sequence");
            int rules=in.readVarInt();if(rules<0 || rules>2) throw new IllegalArgumentException("Invalid special crafting rule count");var crafting=new HashMap<String,String>();
            for(int n=0;n<rules;n++) {String kind=in.readUtf(16),id=in.readUtf(256);if(!Set.of("detector","paragon").contains(kind) || Identifier.tryParse(id)==null || crafting.put(kind,id)!=null) throw new IllegalArgumentException("Invalid special crafting rule");}
            byte[] bytes=in.readByteArray(MAX_CHUNK);var buffer=buffer(Unpooled.wrappedBuffer(bytes),in.registryAccess());
            try {
                int count=buffer.readVarInt();if(count<0 || count>32) throw new IllegalArgumentException("Invalid processing chunk count");
                var entries=new ArrayList<ProcessingAccess.Entry>(count);
                for(int n=0;n<count;n++) {String id=buffer.readUtf(256);if(Identifier.tryParse(id)==null) throw new IllegalArgumentException("Invalid processing ID");entries.add(new ProcessingAccess.Entry(id,RECIPES.decode(buffer).definition));}
                if(buffer.isReadable()) throw new IllegalArgumentException("Trailing processing snapshot data");
                return new Part(generation,index,total,List.copyOf(entries),bytes.length,crafting);
            } finally {buffer.release();}
        }
    };
    public static void register(RegisterPayloadHandlersEvent event) {event.registrar("4").playToClient(Part.TYPE,CODEC,(part,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.ProcessingClient.part(part)));}
    public static void sync(OnDatapackSyncEvent event) {
        var players=event.getRelevantPlayers().toList();if(players.isEmpty()) return;
        var source=ProcessingAccess.entries(players.getFirst().level());var registries=players.getFirst().level().registryAccess();
        var crafting=new HashMap<String,String>();
        var recipes=event.getPlayerList().getServer().getRecipeManager().recipeMap().byType(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream().sorted(Comparator.comparing(r->r.id().identifier().toString())).toList();
        for(var recipe:recipes) {String kind=recipe.value() instanceof DetectorRecipe?"detector":recipe.value() instanceof ParagonRecipe?"paragon":"";if(!kind.isEmpty()) crafting.putIfAbsent(kind,recipe.id().identifier().toString());}
        if(source.size()>MAX_RECIPES) throw new IllegalStateException("Metallurgy processing recipe sync exceeds "+MAX_RECIPES+" recipes");
        var chunks=new ArrayList<List<ProcessingAccess.Entry>>();var batch=new ArrayList<ProcessingAccess.Entry>();int totalBytes=0;
        for(var entry:source) {
            if(entry.id().length()>256) throw new IllegalStateException("Metallurgy processing recipe ID exceeds256 characters: "+entry.id());
            var next=new ArrayList<>(batch);next.add(entry);int size=encode(next,registries).length;
            if(!batch.isEmpty() && (next.size()>32 || size>MAX_CHUNK)) {chunks.add(List.copyOf(batch));totalBytes+=encode(batch,registries).length;batch.clear();}
            batch.add(entry);if(encode(batch,registries).length>MAX_CHUNK) throw new IllegalStateException("Metallurgy processing recipe exceeds256KiB: "+entry.id());
        }
        if(!batch.isEmpty() || chunks.isEmpty()) {chunks.add(List.copyOf(batch));totalBytes+=encode(batch,registries).length;}
        if(chunks.size()>MAX_PARTS || totalBytes>MAX_TOTAL) throw new IllegalStateException("Metallurgy processing snapshot exceeds bounded sync limits");
        long generation=GENERATION.incrementAndGet();
        for(int n=0;n<chunks.size();n++) {var entries=chunks.get(n);var part=new Part(generation,n,chunks.size(),entries,encode(entries,registries).length,crafting);for(var player:players) PacketDistributor.sendToPlayer(player,part);}
    }
    private ProcessingNetwork() {}
}
