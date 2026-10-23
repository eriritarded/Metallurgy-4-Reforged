package it.hurts.metallurgy_reforged.gadget;
import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import java.util.*;

/** Loaded-chunk index, rebuilt from persisted blocks rather than stale saved positions. */
public final class LampRange {
    private static final Map<ServerLevel,Map<Long,Set<BlockPos>>> LEVELS=new WeakHashMap<>();
    private static long key(int x,int z) {return (x&0xffffffffL)|((z&0xffffffffL)<<32);}
    public static synchronized void add(ServerLevel level,BlockPos pos) {
        LEVELS.computeIfAbsent(level,l->new HashMap<>()).computeIfAbsent(key(pos.getX()>>4,pos.getZ()>>4),c->new HashSet<>()).add(pos.immutable());
    }
    public static synchronized void remove(ServerLevel level,BlockPos pos) {
        var chunks=LEVELS.get(level);if(chunks==null) return;long key=key(pos.getX()>>4,pos.getZ()>>4);
        var positions=chunks.get(key);if(positions!=null) {positions.remove(pos);if(positions.isEmpty()) chunks.remove(key);}
    }
    public static synchronized void unloaded(ServerLevel level,ChunkAccess chunk) {var chunks=LEVELS.get(level);if(chunks!=null) chunks.remove(key(chunk.getPos().getMinBlockX()>>4,chunk.getPos().getMinBlockZ()>>4));}
    public static synchronized void loaded(ServerLevel level,ChunkAccess chunk) {
        var lamp=Content.BLOCK_MAP.get("phosphorus_lamp").get();var sections=chunk.getSections();
        for(int i=0;i<sections.length;i++) {
            var section=sections[i];if(section==null || !section.maybeHas(state->state.is(lamp))) continue;
            int bottom=chunk.getSectionYFromSectionIndex(i)<<4;
            for(int x=0;x<16;x++) for(int y=0;y<16;y++) for(int z=0;z<16;z++) if(section.getBlockState(x,y,z).is(lamp))
                add(level,new BlockPos(chunk.getPos().getMinBlockX()+x,bottom+y,chunk.getPos().getMinBlockZ()+z));
        }
    }
    public static synchronized boolean near(ServerLevel level,BlockPos target) {
        var chunks=LEVELS.get(level);if(chunks==null) return false;var lamp=Content.BLOCK_MAP.get("phosphorus_lamp").get();
        for(int x=(target.getX()-8)>>4;x<=(target.getX()+8)>>4;x++) for(int z=(target.getZ()-8)>>4;z<=(target.getZ()+8)>>4;z++) {
            var positions=chunks.get(key(x,z));if(positions==null) continue;
            var it=positions.iterator();while(it.hasNext()) {
                BlockPos pos=it.next();if(!level.hasChunkAt(pos)) continue;
                if(!level.getBlockState(pos).is(lamp)) {it.remove();continue;}
                if(pos.distSqr(target)<=64) return true;
            }
        }
        return false;
    }
    private LampRange() {}
}
