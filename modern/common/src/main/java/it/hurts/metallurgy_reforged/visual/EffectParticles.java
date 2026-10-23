package it.hurts.metallurgy_reforged.visual;
import it.hurts.metallurgy_reforged.material.Materials;
import it.hurts.metallurgy_reforged.network.VisualNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
/** Source detail/static/motion parameters stay at the effect call site. */
public final class EffectParticles {
    public static void weapon(Entity entity,String metal,int count,float scale) {
        boolean dynamic=!java.util.Set.of("deep_iron","hepatizon","krik","oureclase").contains(metal);
        int detail=switch(metal) {
            case "angmallen","astral_silver","black_steel","damascus_steel","desichalkos"->9;
            case "eximite","mithril","platinum","sanguinite","shadow_iron"->6;
            case "carmot","hepatizon","ignatius"->3;
            case "etherium"->2;case "krik"->4;default->5;
        };
        entity(entity,metal,count,scale,dynamic,detail);
    }
    public static void entity(Entity entity,String metal,int count,float scale,boolean dynamic,int detail) {
        if(entity.level() instanceof ServerLevel level) box(level,entity.getBoundingBox(),metal,count,scale,dynamic,detail,null,1,false);
    }
    public static void point(ServerLevel level,Vec3 pos,String metal,int count,float scale,boolean dynamic,int detail,Vec3 motion) {box(level,new AABB(pos,pos),metal,count,scale,dynamic,detail,motion,1,false);}
    public static void block(ServerLevel level,BlockPos pos,String metal,int count,float scale,boolean dynamic,int detail,Vec3 motion) {
        block(level,pos,metal,count,scale,dynamic,detail,motion,Vec3.ZERO);
    }
    public static void block(ServerLevel level,BlockPos pos,String metal,int count,float scale,boolean dynamic,int detail,Vec3 motion,Vec3 jitter) {
        var shape=level.getBlockState(pos).getShape(level,pos);
        var bounds=shape.isEmpty()?new AABB(pos):shape.bounds().move(pos);box(level,bounds.inflate(0.15),metal,count,scale,dynamic,detail,motion,1,false,jitter);
    }
    public static void point(ServerLevel level,Vec3 pos,String metal,int count,float scale,boolean dynamic,int detail,Vec3 motion,Vec3 jitter) {box(level,new AABB(pos,pos),metal,count,scale,dynamic,detail,motion,1,false,jitter);}
    public static void front(Entity entity,String metal,int count,float scale,boolean dynamic,int detail) {if(entity.level() instanceof ServerLevel level) point(level,entity.position().add(entity.getLookAngle().x*0.5,1.1,entity.getLookAngle().z*0.5),metal,count,scale,dynamic,detail,null);}
    public static void etherium(Entity target,Entity attacker) {
        if(target.level() instanceof ServerLevel level) {var center=target.position().add(0,target.getBbHeight()/2,0);var motion=attacker.position().subtract(target.position()).normalize().scale(0.2);box(level,new AABB(center.add(-0.3,-0.3,-0.3),center.add(0.2,0.2,0.2)),"etherium",15,2,true,2,motion,1,false);}
    }
    public static void jump(Entity entity,int previousJumps) {
        if(entity.level() instanceof ServerLevel level) {
            var b=entity.getBoundingBox();box(level,new AABB(b.minX-0.7,b.minY,b.minZ-0.7,b.maxX+0.7,b.minY,b.maxZ+0.7),"amordrine",16,1,false,4-previousJumps,new Vec3(0,-0.03,0),10,true);
        }
    }
    public static void box(ServerLevel level,AABB box,String metal,int count,float scale,boolean dynamic,int detail,Vec3 motion,int ticks,boolean varyScale) {
        box(level,box,metal,count,scale,dynamic,detail,motion,ticks,varyScale,Vec3.ZERO);
    }
    public static void box(ServerLevel level,AABB box,String metal,int count,float scale,boolean dynamic,int detail,Vec3 motion,int ticks,boolean varyScale,Vec3 jitter) {
        var material=Materials.BY_NAME.get(metal);if(material==null) return;var center=box.getCenter();
        VisualNetwork.burst(level,new ParticleBurst(center.x,center.y,center.z,(float)box.getXsize()/2,(float)box.getYsize()/2,(float)box.getZsize()/2,material.color(),scale,count,detail,dynamic,java.util.concurrent.ThreadLocalRandom.current().nextLong(),motion==null?0:(float)motion.x,motion==null?0:(float)motion.y,motion==null?0:(float)motion.z,motion!=null,ticks,varyScale,(float)jitter.x,(float)jitter.y,(float)jitter.z));
    }
    private EffectParticles() {}
}
