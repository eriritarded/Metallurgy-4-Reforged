package it.hurts.metallurgy_reforged.gadget;
import it.hurts.metallurgy_reforged.effect.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.damagesource.DamageSource;
import java.util.*;

public final class ShieldLogic {
    private static final ThreadLocal<Boolean> RETALIATING=ThreadLocal.withInitial(()->false);
    private static final Map<LivingEntity,List<BlockPos>> DOMES=Collections.synchronizedMap(new WeakHashMap<>());
    public static boolean buckler(ItemStack stack) { return GadgetLogic.is(stack,"zinc_buckler") || GadgetLogic.is(stack,"vulcanite_buckler") || GadgetLogic.is(stack,"orichalcum_buckler"); }
    public static boolean invisible(LivingEntity entity) { return entity.isUsingItem() && GadgetLogic.is(entity.getUseItem(),"lemurite_shield"); }
    public static int duration(String id) { return switch(id) {case "zinc_buckler"->40;case "vulcanite_buckler"->50;case "orichalcum_buckler"->30;default->200;}; }
    public static void start(Player player,ItemStack stack) {
        if(VersionBridge.client(player)) return;
        if(GadgetLogic.is(stack,"lemurite_shield")) {
            try(State state=VersionBridge.entity(player)) {
                if(!state.flag("lemurite_owned")) {state.flag("lemurite_previous",player.isInvisible());state.flag("lemurite_owned",true);}
            }
            player.setInvisible(true);GadgetBridge.shieldParticle(player);
        } else if(GadgetLogic.is(stack,"ceruclase_shield") && !DOMES.containsKey(player) && !GadgetBridge.eatingOtherHand(player,stack)) {
            List<BlockPos> placed=new ArrayList<>();BlockPos center=player.blockPosition();var ice=it.hurts.metallurgy_reforged.content.Content.BLOCK_MAP.get("ice_shield").get();
            player.fallDistance/=2;
            for(int x=-6;x<=5;x++) for(int y=-6;y<=5;y++) for(int z=-6;z<=5;z++) {
                int distance=x*x+y*y+z*z;if(distance<=16 || distance>25) continue;
                BlockPos pos=center.offset(x,y,z);
                if(!GadgetBridge.editable(player,pos,Direction.UP,stack) || !player.level().getBlockState(pos).canBeReplaced()) continue;
                if(GadgetBridge.place(player,pos,Direction.UP,stack,ice.defaultBlockState())) {
                    placed.add(pos.immutable());player.level().scheduleTick(pos,ice,10+player.getRandom().nextInt(11));
                }
            }
            DOMES.put(player,placed);VersionBridge.sound(player,"block.glass.place",1,2);
        }
    }
    public static boolean holdsDome(Level level,BlockPos pos) {
        synchronized(DOMES) {
            for(var entry:DOMES.entrySet()) {
                var entity=entry.getKey();if(entity!=null && entity.level()==level && entity.isAlive() && entity.isUsingItem() && GadgetLogic.is(entity.getUseItem(),"ceruclase_shield") && entry.getValue().contains(pos)) return true;
            }
        }
        return false;
    }
    public static void end(LivingEntity entity,ItemStack stack,int remaining) {
        if(VersionBridge.client(entity)) return;
        if(buckler(stack) && entity instanceof Player player) VersionBridge.cooldown(player,stack,GadgetLogic.is(stack,"vulcanite_buckler")?40:30);
        if(GadgetLogic.is(stack,"lemurite_shield")) {
            boolean owned=false;
            try(State state=VersionBridge.entity(entity)) {
                owned=state.flag("lemurite_owned");
                if(owned) { entity.setInvisible(state.flag("lemurite_previous") || VersionBridge.amplifier(entity,"invisibility")>=0);state.remove("lemurite_owned");state.remove("lemurite_previous"); }
            }
            if(owned) {
                int cooldown=Math.max(20,Math.min(200,200-remaining));
                if(entity instanceof Player player) VersionBridge.cooldown(player,stack,cooldown);
                GadgetBridge.damage(entity,stack,Math.round(cooldown/8F),entity.getMainHandItem()==stack?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);GadgetBridge.shieldParticle(entity);
            }
        }
        if(GadgetLogic.is(stack,"ceruclase_shield")) {
            if(DOMES.remove(entity)!=null && entity instanceof Player player) VersionBridge.cooldown(player,stack,140);
        }
    }
    public static void blocked(LivingEntity entity,DamageSource source,float amount) {
        if(VersionBridge.client(entity) || amount<=0 || RETALIATING.get()) return;
        ItemStack stack=entity.getUseItem();
        if(!buckler(stack)) return;
        RETALIATING.set(true);
        try {
        if(GadgetLogic.is(stack,"vulcanite_buckler") && source.getDirectEntity() instanceof LivingEntity target) {
            VersionBridge.explode(entity,target,target.position(),0.45F);target.knockback(2.5,entity.getX()-target.getX(),entity.getZ()-target.getZ());VersionBridge.velocity(target);
        } else if(GadgetLogic.is(stack,"orichalcum_buckler") && source.getDirectEntity()!=source.getEntity() && source.getEntity() instanceof LivingEntity target) {
            // Preserve the source's projectile damage identity, then native
            // randomTeleport validates collision and fires teleport hooks.
            VersionBridge.hit(target,source,amount*1.45F);
            var position=entity.position().add(entity.getLookAngle().scale(2)).add(0,2,0);
            if(target.level()==entity.level() && target.level().hasChunkAt(BlockPos.containing(position))) target.randomTeleport(position.x,position.y,position.z,true);
        }
        } finally {RETALIATING.remove();}
        // Native damage processing still needs the active stack for durability.
        try(State state=VersionBridge.entity(entity)) { state.flag("buckler_release",true); }
    }
    public static void tick(Player player) {
        if(VersionBridge.client(player)) return;
        boolean release;
        try(State state=VersionBridge.entity(player)) {
            release=state.flag("buckler_release");state.remove("buckler_release");
            if(state.flag("lemurite_owned") && !invisible(player)) {
                player.setInvisible(state.flag("lemurite_previous") || VersionBridge.amplifier(player,"invisibility")>=0);
                state.remove("lemurite_owned");state.remove("lemurite_previous");
            }
        }
        if(release && buckler(player.getUseItem())) player.releaseUsingItem();
        if(DOMES.containsKey(player) && (!player.isAlive() || !player.isUsingItem() || !GadgetLogic.is(player.getUseItem(),"ceruclase_shield"))) {
            DOMES.remove(player);VersionBridge.cooldown(player,it.hurts.metallurgy_reforged.content.Content.stack("ceruclase_shield"),140);
        }
    }
    public static void cleanup(Player player) {
        if(VersionBridge.client(player)) return;
        end(player,player.getUseItem(),player.getUseItemRemainingTicks());DOMES.remove(player);
        try(State state=VersionBridge.entity(player)) {
            if(state.flag("lemurite_owned")) {player.setInvisible(state.flag("lemurite_previous") || VersionBridge.amplifier(player,"invisibility")>=0);state.remove("lemurite_owned");state.remove("lemurite_previous");}
            state.remove("buckler_release");
        }
    }
    private ShieldLogic() {}
}
