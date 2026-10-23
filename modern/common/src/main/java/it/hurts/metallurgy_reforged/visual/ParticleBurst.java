package it.hurts.metallurgy_reforged.visual;
import net.minecraft.network.FriendlyByteBuf;
/** One bounded visual-only server payload replaces many individual1.12 packets. */
public record ParticleBurst(double x,double y,double z,float radiusX,float radiusY,float radiusZ,int color,float scale,int count,int detail,boolean dynamic,long seed,
                            float motionX,float motionY,float motionZ,boolean explicitMotion,int emissionTicks,boolean varyingScale,
                            float jitterX,float jitterY,float jitterZ) {
    public ParticleBurst(double x,double y,double z,float radiusX,float radiusY,float radiusZ,int color,float scale,int count,int detail,boolean dynamic,long seed,float motionX,float motionY,float motionZ,boolean explicitMotion,int emissionTicks,boolean varyingScale) {this(x,y,z,radiusX,radiusY,radiusZ,color,scale,count,detail,dynamic,seed,motionX,motionY,motionZ,explicitMotion,emissionTicks,varyingScale,0,0,0);}
    public ParticleBurst(double x,double y,double z,float radiusX,float radiusY,float radiusZ,int color,float scale,int count,int detail,boolean dynamic,long seed) {this(x,y,z,radiusX,radiusY,radiusZ,color,scale,count,detail,dynamic,seed,0,0,0,false,1,false);}
    public ParticleBurst {
        radiusX=radius(radiusX);radiusY=radius(radiusY);radiusZ=radius(radiusZ);
        scale=Float.isFinite(scale)?Math.max(0,Math.min(16,scale)):0;
        count=Math.max(0,Math.min(128,count));detail=Math.max(0,Math.min(9,detail));
        motionX=motion(motionX);motionY=motion(motionY);motionZ=motion(motionZ);emissionTicks=Math.max(1,Math.min(100,emissionTicks));
        jitterX=radius(jitterX);jitterY=radius(jitterY);jitterZ=radius(jitterZ);
    }
    private static float motion(float value) {return Float.isFinite(value)?Math.max(-16,Math.min(16,value)):0;}
    private static float radius(float value) {return Float.isFinite(value)?Math.max(0,Math.min(64,value)):0;}
    public boolean valid() {return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && count>0 && scale>0;}
    public void write(FriendlyByteBuf buffer) {buffer.writeDouble(x);buffer.writeDouble(y);buffer.writeDouble(z);buffer.writeFloat(radiusX);buffer.writeFloat(radiusY);buffer.writeFloat(radiusZ);buffer.writeInt(color);buffer.writeFloat(scale);buffer.writeVarInt(count);buffer.writeByte(detail);buffer.writeBoolean(dynamic);buffer.writeLong(seed);buffer.writeFloat(motionX);buffer.writeFloat(motionY);buffer.writeFloat(motionZ);buffer.writeBoolean(explicitMotion);buffer.writeVarInt(emissionTicks);buffer.writeBoolean(varyingScale);buffer.writeFloat(jitterX);buffer.writeFloat(jitterY);buffer.writeFloat(jitterZ);}
    public static ParticleBurst read(FriendlyByteBuf buffer) {return new ParticleBurst(buffer.readDouble(),buffer.readDouble(),buffer.readDouble(),buffer.readFloat(),buffer.readFloat(),buffer.readFloat(),buffer.readInt(),buffer.readFloat(),buffer.readVarInt(),buffer.readUnsignedByte(),buffer.readBoolean(),buffer.readLong(),buffer.readFloat(),buffer.readFloat(),buffer.readFloat(),buffer.readBoolean(),buffer.readVarInt(),buffer.readBoolean(),buffer.readFloat(),buffer.readFloat(),buffer.readFloat());}
}
