package it.hurts.metallurgy_reforged.gadget;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
public final class RoadMovement {
    public static void input(Player player,boolean moving) {
        if(!VersionBridge.client(player) || !moving || !player.onGround()) return;
        var state=player.level().getBlockState(BlockPos.containing(player.getX(),player.getY()-0.5,player.getZ()));
        String id=VersionBridge.blockId(state);
        if(id.equals("metallurgy:road_block") || id.equals("metallurgy:striped_road_block")) {
            double factor=it.hurts.metallurgy_reforged.config.GeneralConfig.roadSpeed;
            var next=player.getDeltaMovement().multiply(factor,1,factor);
            if(Double.isFinite(next.x) && Double.isFinite(next.z)) player.setDeltaMovement(next);
        }
    }
    private RoadMovement() {}
}
