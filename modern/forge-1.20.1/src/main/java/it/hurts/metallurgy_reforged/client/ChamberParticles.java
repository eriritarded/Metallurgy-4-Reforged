package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.machine.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
public final class ChamberParticles {
    public static void animate(Level level,BlockPos pos,RandomSource random) {
        if(!(level.getBlockEntity(pos) instanceof ChamberEntity chamber) || chamber.effect.isEmpty() || !chamber.getBlockState().getValue(ChamberBlock.LIT)) return;
        var effect=BuiltInRegistries.MOB_EFFECT.get(it.hurts.metallurgy_reforged.effect.VersionBridge.effectKey(chamber.effect));if(effect==null) return;int color=effect.getColor();
        for(var facing:Direction.Plane.HORIZONTAL) for(int i=0;i<15;i++) {
            double x=pos.getX()+0.3+random.nextFloat()*0.8+facing.getStepX()*0.5,y=pos.getY()+0.1+random.nextFloat()*0.3,z=pos.getZ()+0.3+random.nextFloat()*0.8+facing.getStepZ()*0.5;
            level.addParticle(ParticleTypes.ENTITY_EFFECT,x,y,z,(color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F);
        }
    }
    private ChamberParticles() {}
}
