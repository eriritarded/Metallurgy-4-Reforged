package it.hurts.metallurgy_reforged.world;
import it.hurts.metallurgy_reforged.config.WorldGenerationConfig;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import java.util.*;

/** One already-loaded chunk per dimension/tick. Never request an unloaded neighbor. */
public final class Retrogen {
    private static final Map<ServerLevel,LinkedHashSet<ChunkPos>> PENDING=new WeakHashMap<>();
    public static synchronized void loaded(ServerLevel level,ChunkPos pos) {
        if(!WorldGenerationConfig.retrogen) return;
        if(PENDING.computeIfAbsent(level,key->new LinkedHashSet<>()).add(pos) && WorldGenerationConfig.verbose_retrogen)
            org.slf4j.LoggerFactory.getLogger("metallurgy").info("Queued ore retrogen at {} in {}",pos,VersionBridge.dimensionId(level));
    }
    public static synchronized void unloaded(ServerLevel level,ChunkPos pos) {var queue=PENDING.get(level);if(queue!=null) {queue.remove(pos);if(queue.isEmpty()) PENDING.remove(level);}}
    public static synchronized void tick(ServerLevel level) {
        var queue=PENDING.get(level);if(queue==null || queue.isEmpty()) return;
        if(!WorldGenerationConfig.retrogen) {PENDING.remove(level);return;}
        // Bounded rotation prevents an incomplete border from starving other chunks.
        for(int scan=0,limit=Math.min(4,queue.size());scan<limit;scan++) {
            var iterator=queue.iterator();ChunkPos pos=iterator.next();iterator.remove();
            int chunkX=pos.getMinBlockX()>>4,chunkZ=pos.getMinBlockZ()>>4;
            var chunk=level.getChunkSource().getChunkNow(chunkX,chunkZ);
            if(chunk==null) continue;
            boolean ready=true;
            for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) if(level.getChunkSource().getChunkNow(chunkX+x,chunkZ+z)==null) ready=false;
            if(!ready) {queue.add(pos);continue;}
            String realm=VersionBridge.oreRealm(level,"overworld")?"overworld":VersionBridge.oreRealm(level,"nether")?"nether":VersionBridge.oreRealm(level,"end")?"end":"";
            if(!realm.isEmpty()) {
                java.util.Random seeds=new java.util.Random(level.getSeed());long xSeed=seeds.nextLong()>>3,zSeed=seeds.nextLong()>>3;
                var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(xSeed*chunkX+zSeed*chunkZ^level.getSeed());
                Content.ORE_BATCH.get().place(new OreBatchConfiguration(realm),level,level.getChunkSource().getGenerator(),random,new BlockPos(pos.getMinBlockX(),0,pos.getMinBlockZ()));
            }
            RetrogenEvents.generated(chunk);
            if(WorldGenerationConfig.verbose_retrogen) org.slf4j.LoggerFactory.getLogger("metallurgy").info("Completed ore retrogen at {} in {}",pos,VersionBridge.dimensionId(level));
            break;
        }
        if(queue.isEmpty()) PENDING.remove(level);
    }
    private Retrogen() {}
}
