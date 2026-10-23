package it.hurts.metallurgy_reforged.visual;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
/** Server-only sound intent: -1 stop,0 crusher,1 alloyer,2 chamber. */
public record SoundCue(String dimension,BlockPos pos,int kind) {
    public boolean valid() {return dimension!=null && dimension.length()<=256 && kind>=-1 && kind<=2;}
    public void write(FriendlyByteBuf buffer) {buffer.writeUtf(dimension,256);buffer.writeBlockPos(pos);buffer.writeVarInt(kind);}
    public static SoundCue read(FriendlyByteBuf buffer) {return new SoundCue(buffer.readUtf(256),buffer.readBlockPos(),buffer.readVarInt());}
}
