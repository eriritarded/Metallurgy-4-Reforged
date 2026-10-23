package it.hurts.metallurgy_reforged.gadget;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
public final class BlockGadgetEvents {
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void explosion(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
        if(event.getLevel() instanceof ServerLevel level && event.getExplosion().getDirectSourceEntity() instanceof it.hurts.metallurgy_reforged.entity.VulcanitePrimedTnt tnt) {
            var sample=new java.util.ArrayList<net.minecraft.core.BlockPos>();var blocks=event.getAffectedBlocks();int count=Math.min(blocks.size(),it.hurts.metallurgy_reforged.visual.ExplosionVisual.LIMIT);
            for(int i=0;i<count;i++) sample.add(blocks.get((int)((long)i*blocks.size()/count)).immutable());
            it.hurts.metallurgy_reforged.network.VisualNetwork.explosion(level,new it.hurts.metallurgy_reforged.visual.ExplosionVisual(tnt.getX(),tnt.getY(),tnt.getZ(),it.hurts.metallurgy_reforged.material.Materials.BY_NAME.get("vulcanite").color(),level.getRandom().nextLong(),sample));
        }
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {if(event.getLevel() instanceof ServerLevel level) LampRange.loaded(level,event.getChunk());}
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event) {if(event.getLevel() instanceof ServerLevel level) LampRange.unloaded(level,event.getChunk());}
    @SubscribeEvent public static void spawn(MobSpawnEvent.PositionCheck event) {if(LampRange.near(event.getLevel().getLevel(),event.getEntity().blockPosition())) event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);}
    private BlockGadgetEvents() {}
}
