package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.visual.ExplosionVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
/** Original Vulcanite flame/ore choice and outward velocity, using the native blast's block sample. */
public final class ExplosionParticles {
    public static void burst(ExplosionVisual data) {
        var mc=Minecraft.getInstance();if(mc.level==null || !data.valid()) return;var random=new java.util.Random(data.seed());
        for(var pos:data.blocks()) {
            double x=pos.getX()+random.nextFloat(),y=pos.getY()+random.nextFloat(),z=pos.getZ()+random.nextFloat();
            double dx=x-data.x(),dy=y-data.y(),dz=z-data.z(),distance=Math.sqrt(dx*dx+dy*dy+dz*dz);if(distance<1.0E-8) continue;
            double speed=.5/(distance/6+.1)*(random.nextFloat()*random.nextFloat()+.3)/distance;dx*=speed;dy*=speed;dz*=speed;
            if(random.nextInt(3)==0) mc.level.addParticle(ParticleTypes.FLAME,(x+data.x())/2,(y+data.y())/2,(z+data.z())/2,dx,dy,dz);
            else mc.particleEngine.add(new OreParticle(mc.level,x,y,z,dx,dy,dz,3,data.color(),true,random.nextInt(2)+6));
        }
    }
    private ExplosionParticles() {}
}
