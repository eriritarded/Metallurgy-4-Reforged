package it.hurts.metallurgy_reforged.effect;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.network.ArmorNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Etherium's collision-paused30x10-tick phase; client receives authority flags. */
public final class ArmorPhasing {
    private static final Map<Player,Boolean> CLIENT=new WeakHashMap<>();
    public static void clientFlag(Player player,boolean flag) { CLIENT.put(player,flag);player.noPhysics=flag||player.isSpectator(); }
    private static boolean ready(Player player) {
        for(var slot:EquipmentEffects.ARMOR_SLOTS) {
            var stack=player.getItemBySlot(slot);if(EquipmentEffects.material(stack).equals("etherium")) return VersionBridge.cooldown(player,stack)==0;
        }
        return false;
    }
    public static void tick(Player player) {
        if(VersionBridge.client(player)) { if(Boolean.TRUE.equals(CLIENT.get(player))) player.noPhysics=true;return; }
        int count=EquipmentEffects.armorLevel(player,"etherium");
        if(count==0 || !player.isAlive() || player.isSpectator()) { cleanup(player);return; }
        if(!player.isShiftKeyDown() || !ready(player)) { disable(player,true);return; }
        boolean collision=player.level().getBlockCollisions(player,player.getBoundingBox().inflate(0.1,0,0.1)).iterator().hasNext();
        if(!collision) { disable(player,false);return; }
        boolean finished=false,starting=false;
        try(State state=VersionBridge.entity(player)) {
            int step=state.integer("etherium_step");long now=player.level().getGameTime();
            if(step==0) { step=1;state.longValue("etherium_at",now); }
            else if(now-state.longValue("etherium_at")>=10) {
                finished=step>=30;step++;state.longValue("etherium_at",now);
            }
            if(!finished) {
                state.integer("etherium_step",step);
                if(!state.flag("etherium_owned")) {
                    state.flag("etherium_prior_physics",player.noPhysics);state.flag("etherium_owned",true);starting=true;
                    if(player.level().noCollision(player,player.getBoundingBox())) { state.flag("etherium_has_exit",true);state.doubleValue("etherium_x",player.getX());state.doubleValue("etherium_y",player.getY());state.doubleValue("etherium_z",player.getZ()); }
                }
            }
        }
        if(finished) {
            cleanup(player);for(String form:List.of("helmet","chestplate","leggings","boots")) VersionBridge.cooldown(player,Content.stack("etherium_"+form),400);return;
        }
        player.noPhysics=true;player.setDeltaMovement(player.getDeltaMovement().with(net.minecraft.core.Direction.Axis.Y,0));
        if(starting && player instanceof ServerPlayer server) ArmorNetwork.phase(server,true);
        var box=player.getBoundingBox().inflate(0.1,0,0.1);
        for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,player.getY(),box.minZ),BlockPos.containing(Math.ceil(box.maxX),Math.ceil(box.maxY),Math.ceil(box.maxZ)))) {
            if(!player.level().hasChunkAt(pos) || !Arrays.asList(it.hurts.metallurgy_reforged.config.EffectTweaksConfig.etheriumEffectArmorBlacklist).contains(VersionBridge.blockId(player.level().getBlockState(pos))) || !box.intersects(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+1,pos.getZ()+1)) continue;
            Vec3 push=player.position().subtract(pos.getCenter()).normalize().scale(0.1);player.setDeltaMovement(player.getDeltaMovement().add(push.x,0,push.z));VersionBridge.velocity(player);
        }
    }
    private static void disable(Player player,boolean safeExit) {
        boolean owned=false;Vec3 exit=null;
        try(State state=VersionBridge.entity(player)) {
            owned=state.flag("etherium_owned");
            if(owned) {
                player.noPhysics=state.flag("etherium_prior_physics")||player.isSpectator();
                if(safeExit && !player.noPhysics && player.isAlive() && !player.level().noCollision(player,player.getBoundingBox()) && state.flag("etherium_has_exit")) exit=new Vec3(state.doubleValue("etherium_x"),state.doubleValue("etherium_y"),state.doubleValue("etherium_z"));
                state.remove("etherium_owned");state.remove("etherium_prior_physics");state.remove("etherium_has_exit");
            }
        }
        if(exit!=null && player.level().hasChunkAt(BlockPos.containing(exit)) && player.level().noCollision(player,player.getBoundingBox().move(exit.subtract(player.position())))) player.teleportTo(exit.x,exit.y,exit.z);
        if(owned && player instanceof ServerPlayer server) ArmorNetwork.phase(server,false);
    }
    public static void cleanup(Player player) {
        if(VersionBridge.client(player)) { CLIENT.remove(player);return; }
        disable(player,true);
        try(State state=VersionBridge.entity(player)) { state.remove("etherium_step");state.remove("etherium_at"); }
    }
    private ArmorPhasing() {}
}
