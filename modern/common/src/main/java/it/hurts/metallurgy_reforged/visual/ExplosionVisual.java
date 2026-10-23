package it.hurts.metallurgy_reforged.visual;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import java.util.*;
/** Visual-only native detonation sample; never changes the affected block list. */
public record ExplosionVisual(double x,double y,double z,int color,long seed,List<BlockPos> blocks) {
    public static final int LIMIT=4096;
    public ExplosionVisual {blocks=List.copyOf(blocks.subList(0,Math.min(LIMIT,blocks.size())));}
    public boolean valid() {return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && !blocks.isEmpty();}
    public void write(FriendlyByteBuf buffer) {buffer.writeDouble(x);buffer.writeDouble(y);buffer.writeDouble(z);buffer.writeInt(color);buffer.writeLong(seed);buffer.writeVarInt(blocks.size());for(var pos:blocks) buffer.writeBlockPos(pos);}
    public static ExplosionVisual read(FriendlyByteBuf buffer) {
        double x=buffer.readDouble(),y=buffer.readDouble(),z=buffer.readDouble();int color=buffer.readInt();long seed=buffer.readLong();int count=buffer.readVarInt();
        if(count<0 || count>LIMIT) throw new IllegalArgumentException("Invalid Metallurgy explosion sample");
        List<BlockPos> blocks=new ArrayList<>(count);for(int i=0;i<count;i++) blocks.add(buffer.readBlockPos());return new ExplosionVisual(x,y,z,color,seed,blocks);
    }
}
