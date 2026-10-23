package it.hurts.metallurgy_reforged.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
/** Original32-tick particle, integer32/7 frame steps and final30% shrink. */
public final class OreParticle extends TextureSheetParticle {
    private final int detail;
    private final boolean dynamic;
    private final float initialSize;
    public OreParticle(ClientLevel level,double x,double y,double z,float scale,int color,boolean dynamic,int detail) {
        super(level,x,y,z,0,0,0);this.detail=Math.max(0,Math.min(9,detail));this.dynamic=dynamic;
        lifetime=32;float brightness=random.nextFloat()*0.4F+0.6F;
        setColor((random.nextFloat()*0.2F+0.8F)*(color>>16&255)/255F*brightness,(random.nextFloat()*0.2F+0.8F)*(color>>8&255)/255F*brightness,(random.nextFloat()*0.2F+0.8F)*(color&255)/255F*brightness);
        quadSize*=0.75F*scale;initialSize=quadSize;xd*=0.1;yd*=0.1;zd*=0.1;setSprite(frame(dynamic?0:this.detail));
    }
    public OreParticle(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,float scale,int color,boolean dynamic,int detail) {this(level,x,y,z,scale,color,dynamic,detail);xd=vx;yd=vy;zd=vz;}
    private static TextureAtlasSprite frame(int index) {return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(new ResourceLocation("metallurgy","particles/ore_particle_"+(index+1)));}
    @Override public ParticleRenderType getRenderType() {return ParticleRenderType.TERRAIN_SHEET;}
    @Override public float getQuadSize(float partial) {return initialSize*Math.max(0,Math.min(1,(lifetime-age-partial)/(lifetime*0.3F)));}
    @Override public void tick() {
        xo=x;yo=y;zo=z;if(age++>=lifetime) {remove();return;}move(xd,yd,zd);
        if(onGround) {xd*=0.7;zd*=0.7;}
        if(dynamic) setSprite(frame((age/(lifetime/7))%(detail+1)));
    }
}
