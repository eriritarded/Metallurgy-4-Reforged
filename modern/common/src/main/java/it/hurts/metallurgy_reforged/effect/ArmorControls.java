package it.hurts.metallurgy_reforged.effect;

import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actions carry intent only. Equipment, jump budgets and height bounds are
 * derived on the server, replacing trusted client-supplied1.12 limits. */
public final class ArmorControls {
    private record Recall(BlockPos pos,float health,int food,float saturation) {}
    private static final Map<Player,Deque<Recall>> HISTORY=new WeakHashMap<>();
    private static final Map<Player,Recall> PENDING=new WeakHashMap<>();
    private static int level(Player player,String metal) { return EquipmentEffects.armorLevel(player,metal); }
    public static void action(ServerPlayer player,int action) {
        if(!player.isAlive() || player.isSpectator() || action<0 || action>2) return;
        long now=player.level().getGameTime();
        try(State state=VersionBridge.entity(player)) {
            String key=action==0?"amordrine_last_action":"krik_last_action";
            if(state.has(key) && state.longValue(key)==now) return;
            state.longValue(key,now);
            if(action==0) {
                int maximum=level(player,"amordrine"),used=state.integer("amordrine_jumps");
                if(maximum==0 || player.onGround() || player.getAbilities().flying || used>=maximum) return;
                state.integer("amordrine_jumps",used+1);
            } else {
                if(level(player,"krik")<4) return;
                state.integer("krik_height",Math.max(0,Math.min(krikMaximum(player),state.integer("krik_height")+(action==1?1:-1))));return;
            }
        }
        player.jumpFromGround();player.setDeltaMovement(player.getDeltaMovement().add(0,0.15,0));player.fallDistance=0;VersionBridge.velocity(player);
        VersionBridge.sound(player,"entity.ender_dragon.flap",0.5F,2);
        try(State state=VersionBridge.entity(player)) {it.hurts.metallurgy_reforged.visual.EffectParticles.jump(player,state.integer("amordrine_jumps")-1);}
    }
    private static int krikMaximum(Player player) {
        int empty=0;for(int i=9;i<36;i++) if(player.getInventory().getItem(i).isEmpty()) empty++;return empty;
    }
    public static boolean fall(Player player) { return level(player,"krik")==4; }
    public static void tick(Player player) {
        if(!(player.level() instanceof net.minecraft.server.level.ServerLevel world)) return;
        int amordrine=level(player,"amordrine");
        try(State state=VersionBridge.entity(player)) {
            if(amordrine==0 || player.onGround()) state.integer("amordrine_jumps",0);
            else if(state.integer("amordrine_jumps")>0) player.fallDistance=0;
            if(level(player,"krik")==4) {
                int height=Math.min(state.integer("krik_height"),krikMaximum(player));state.integer("krik_height",Math.max(0,height));
                if(player instanceof ServerPlayer server) it.hurts.metallurgy_reforged.network.VisualNetwork.height(server,height);
                if(player.getY()<height*9) { player.setDeltaMovement(player.getDeltaMovement().with(net.minecraft.core.Direction.Axis.Y,0.4));VersionBridge.velocity(player); }
                else if(Math.round(player.getY())==(long)height*9) { player.setDeltaMovement(player.getDeltaMovement().with(net.minecraft.core.Direction.Axis.Y,0));VersionBridge.velocity(player); }
            } else if(player instanceof ServerPlayer server) it.hurts.metallurgy_reforged.network.VisualNetwork.height(server,0);
            if(state.has("inolashite_recall_at") && world.getGameTime()>=state.longValue("inolashite_recall_at")) {
                state.remove("inolashite_recall_at");Recall recall=PENDING.remove(player);
                if(recall!=null) {
                    player.teleportTo(recall.pos.getX(),recall.pos.getY()+0.1,recall.pos.getZ());player.setHealth(recall.health);
                    int delta=recall.food-player.getFoodData().getFoodLevel();float saturation=recall.saturation-player.getFoodData().getSaturationLevel();
                    // Retain addStats(foodDelta,saturationDelta) arithmetic,
                    // rather than silently replacing it with exact setters.
                    VersionBridge.recallFood(player,delta,saturation);it.hurts.metallurgy_reforged.visual.EffectParticles.entity(player,"inolashite",25,1,true,4);
                }
            }
        }
        int inolashite=level(player,"inolashite");
        if(inolashite>0 && player.tickCount%5==0) {
            Deque<Recall> queue=HISTORY.computeIfAbsent(player,k->new ArrayDeque<>());
            while(!queue.isEmpty() && queue.size()>=inolashite*8) queue.removeFirst();
            queue.addLast(new Recall(player.blockPosition(),player.getHealth(),player.getFoodData().getFoodLevel(),player.getFoodData().getSaturationLevel()));
        }
        if(inolashite>0 && player.isShiftKeyDown() && player.tickCount%2==0 && ready(player,"inolashite")) {
            for(int i=0;i<20;i++) it.hurts.metallurgy_reforged.visual.EffectParticles.point(world,player.position().add(Math.sin(i*Math.PI/10),2,Math.cos(i*Math.PI/10)),"inolashite",1,1,true,4,null);
        }
    }
    private static boolean ready(Player player,String metal) {
        for(var slot:EquipmentEffects.ARMOR_SLOTS) {
            var stack=player.getItemBySlot(slot);if(EquipmentEffects.material(stack).equals(metal)) return VersionBridge.cooldown(player,stack)==0;
        }
        return false;
    }
    public static void jumped(Player player) {
        int count=level(player,"inolashite");
        if(count==0 || !player.isShiftKeyDown() || !ready(player,"inolashite")) return;
        Deque<Recall> queue=HISTORY.get(player);
        if(queue!=null && !queue.isEmpty()) {
            PENDING.put(player,queue.getFirst());
            try(State state=VersionBridge.entity(player)) { state.longValue("inolashite_recall_at",player.level().getGameTime()+10); }
        }
        int ticks=(1<<7-count)*20;
        for(String form:List.of("helmet","chestplate","leggings","boots")) VersionBridge.cooldown(player,Content.stack("inolashite_"+form),ticks);
    }
    public static void cleanup(Player player) {
        HISTORY.remove(player);PENDING.remove(player);
        if(player instanceof ServerPlayer server) it.hurts.metallurgy_reforged.network.VisualNetwork.clear(server);
        try(State state=VersionBridge.entity(player)) { state.remove("inolashite_recall_at");state.remove("amordrine_jumps");state.remove("krik_height"); }
    }
    private ArmorControls() {}
}
