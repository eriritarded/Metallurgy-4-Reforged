package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
public final class OreParticles {
    private static final class Emission {
        final it.hurts.metallurgy_reforged.visual.ParticleBurst data;final ClientLevel level;int age;
        Emission(it.hurts.metallurgy_reforged.visual.ParticleBurst data,ClientLevel level) {this.data=data;this.level=level;}
    }
    private static final java.util.List<Emission> EMISSIONS=new java.util.ArrayList<>();
    public static void tick() {
        var mc=Minecraft.getInstance();if(mc.level==null) {EMISSIONS.clear();return;}if(mc.isPaused()) return;
        var iterator=EMISSIONS.iterator();while(iterator.hasNext()) {var emission=iterator.next();if(emission.level!=mc.level) {iterator.remove();continue;}emit(emission.data,emission.age++);if(emission.age>=emission.data.emissionTicks()) iterator.remove();}
    }
    public static void burst(it.hurts.metallurgy_reforged.visual.ParticleBurst data) {
        var mc=Minecraft.getInstance();if(mc.level==null || !data.valid()) return;
        if(data.emissionTicks()>1) {if(EMISSIONS.size()>=64) EMISSIONS.remove(0);EMISSIONS.add(new Emission(data,mc.level));}else emit(data,0);
    }
    private static void emit(it.hurts.metallurgy_reforged.visual.ParticleBurst data,int age) {
        var mc=Minecraft.getInstance();if(mc.level==null) return;
        var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(data.seed()+age);
        for(int i=0;i<data.count();i++) {
            double x=data.x()+(random.nextDouble()*2-1)*data.radiusX(),y=data.y()+(random.nextDouble()*2-1)*data.radiusY(),z=data.z()+(random.nextDouble()*2-1)*data.radiusZ();
            float scale=data.scale()*(data.varyingScale()?random.nextFloat()+0.5F:1);
            mc.particleEngine.add(data.explicitMotion()?new OreParticle(mc.level,x,y,z,data.motionX()+(random.nextDouble()*2-1)*data.jitterX(),data.motionY()+(random.nextDouble()*2-1)*data.jitterY(),data.motionZ()+(random.nextDouble()*2-1)*data.jitterZ(),scale,data.color(),data.dynamic(),data.detail()):new OreParticle(mc.level,x,y,z,scale,data.color(),data.dynamic(),data.detail()));
        }
    }
    public static void block(Level world,BlockPos pos,RandomSource random,Materials.Definition material) {
        if(!GeneralConfig.enableOreParticles || material.oreLevel()<=1 || !(world instanceof ClientLevel level)) return;
        for(int face=0;face<6;face++) {
            double x=pos.getX()+random.nextFloat(),y=pos.getY()+random.nextFloat(),z=pos.getZ()+random.nextFloat();
            var direction=switch(face) {case 0->net.minecraft.core.Direction.UP;case 1->net.minecraft.core.Direction.DOWN;case 2->net.minecraft.core.Direction.SOUTH;case 3->net.minecraft.core.Direction.NORTH;case 4->net.minecraft.core.Direction.EAST;default->net.minecraft.core.Direction.WEST;};
            if(!world.getBlockState(pos.relative(direction)).isSolidRender(world,pos.relative(direction))) {
                switch(face) {case 0->y=pos.getY()+1.0625;case 1->y=pos.getY()-0.0625;case 2->z=pos.getZ()+1.0625;case 3->z=pos.getZ()-0.0625;case 4->x=pos.getX()+1.0625;case 5->x=pos.getX()-0.0625;}
                Minecraft.getInstance().particleEngine.add(new OreParticle(level,x,y,z,1.5F,material.color(),true,(int)((material.oreLevel()-2F)/5F*9)));
            }
        }
    }
    private OreParticles() {}
}
