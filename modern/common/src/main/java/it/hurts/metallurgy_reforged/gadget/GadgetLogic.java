package it.hurts.metallurgy_reforged.gadget;

import it.hurts.metallurgy_reforged.effect.*;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Original gadget defaults; world mutation and item components live in native adapters. */
public final class GadgetLogic {
    private static volatile int detectorColorIndex;
    private static volatile double detectorColorFactor;
    public static void detectorColorClock(int index,double factor) {detectorColorIndex=index;detectorColorFactor=factor;}
    public static int detectorColor(ItemStack stack) {
        var metals=detectorMetals(stack);if(metals.isEmpty()) return 0xffffff;
        int from=Math.floorMod(detectorColorIndex,metals.size()),to=(from+1)%metals.size();
        int a=Materials.BY_NAME.get(metals.get(from)).color(),b=Materials.BY_NAME.get(metals.get(to)).color(),rgb=0;
        for(int shift:new int[]{16,8,0}) rgb|=(int)(((a>>shift)&255)*(1-detectorColorFactor)+((b>>shift)&255)*detectorColorFactor)<<shift;
        return rgb;
    }
    public static boolean is(ItemStack stack,String id) { return VersionBridge.itemId(stack).equals("metallurgy:"+id); }
    public static int magnetMode(ItemStack stack) {
        try(State state=VersionBridge.stack(stack)) { return Math.max(0,Math.min(2,state.integer("status"))); }
    }
    public static List<String> detectorMetals(ItemStack stack) {
        List<String> result=new ArrayList<>();
        try(State state=VersionBridge.stack(stack)) {
            for(int i=1;i<=3;i++) { String metal=state.string("metal_"+i);if(Materials.BY_NAME.containsKey(metal)) result.add(metal); }
        }
        return result;
    }
    public static int detectorUses(ItemStack stack) { try(State state=VersionBridge.stack(stack)) { return Math.max(0,state.integer("detector_uses")); } }
    public static boolean use(Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(is(stack,"electrum_magnet") && player.isShiftKeyDown()) {
            if(!VersionBridge.client(player)) {
                int mode=(magnetMode(stack)+1)%3;
                try(State state=VersionBridge.stack(stack)) { state.integer("status",mode); }
                GadgetBridge.magnetMessage(player,mode);
                VersionBridge.sound(player,"entity.experience_orb.pickup",0.2F,0.3F+mode/2F*0.6F);
            }
            return true;
        }
        if(is(stack,"ore_detector")) {
            List<String> metals=detectorMetals(stack);
            if(metals.isEmpty()) {
                if(!VersionBridge.client(player)) GadgetBridge.detectorMessage(player,"message.metallurgy.detector_unprogrammed");
                return true;
            }
            if(!VersionBridge.client(player) && VersionBridge.cooldown(player,stack)==0) {
                VersionBridge.modSound(player,"metal_detector_ping",1,1);
                VersionBridge.cooldown(player,stack,100);
                int uses=detectorUses(stack),maximum=25*metals.size();
                GadgetBridge.detectorMessage(player,uses>=maximum?"message.metallurgy.detector_empty":"message.metallurgy.detector_scanning");
                if(uses>=maximum) player.setItemInHand(hand,it.hurts.metallurgy_reforged.content.Content.stack("ore_detector"));
                else if(!player.isCreative()) try(State state=VersionBridge.stack(stack)) { state.integer("detector_uses",uses+1); }
            }
            return true;
        }
        return false;
    }
    public static void tick(Player player) {
        if(VersionBridge.client(player)) return;
        // All carried slots qualify. Consolidating identical magnet queries does
        // not change the resulting velocity: every source stack wrote the same vector.
        int mode=0;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            ItemStack stack=player.getInventory().getItem(i);
            if(is(stack,"electrum_magnet")) mode=Math.max(mode,magnetMode(stack));
        }
        if(mode>0) {
            final boolean all=mode==2;
            Vec3 position=player.position();AABB box=new AABB(position.x-6,position.y-6,position.z-6,position.x+6,position.y+6,position.z+6);
            for(ItemEntity item:player.level().getEntitiesOfClass(ItemEntity.class,box,e->e.isAlive() && (all || GadgetBridge.metal(e.getItem())))) {
                item.setNoPickUpDelay();item.setDeltaMovement(position.subtract(item.position()).normalize().scale(0.4));VersionBridge.velocity(item);
            }
        }
    }
    public static boolean dualGauntlets(LivingEntity entity) { return is(entity.getMainHandItem(),"rubracium_gauntlet") && is(entity.getOffhandItem(),"rubracium_gauntlet"); }
    public static void equipped(LivingEntity entity,EquipmentSlot slot,ItemStack stack) {
        if(!(entity instanceof Player player) || !is(stack,"rubracium_gauntlet") || slot!=EquipmentSlot.MAINHAND && slot!=EquipmentSlot.OFFHAND) return;
        EquipmentSlot other=slot==EquipmentSlot.MAINHAND?EquipmentSlot.OFFHAND:EquipmentSlot.MAINHAND;
        if(!player.getItemBySlot(other).isEmpty()) return;
        for(int i=0;i<36;i++) {
            ItemStack candidate=player.getInventory().getItem(i);
            if(candidate!=stack && is(candidate,"rubracium_gauntlet")) { player.setItemSlot(other,candidate.split(1));player.getInventory().setChanged();return; }
        }
    }
    public static void hit(ItemStack stack,LivingEntity target,LivingEntity attacker) {
        if(VersionBridge.client(attacker)) return;
        if(is(stack,"brass_knuckles")) {
            target.setDeltaMovement(target.getDeltaMovement().multiply(0.25,1,0.25));target.hurtTime=10;target.invulnerableTime=13;
            GadgetBridge.damage(attacker,stack,1,EquipmentSlot.MAINHAND);VersionBridge.velocity(target);
        } else if(is(stack,"rubracium_gauntlet") && attacker instanceof Player) {
            boolean main=attacker.getRandom().nextBoolean();
            GadgetBridge.damage(attacker,main?stack:attacker.getOffhandItem(),1,main?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        }
    }
    public static void attack(Player player,Entity target) {
        if(VersionBridge.client(player) || !dualGauntlets(player) || !(target instanceof LivingEntity living) || !living.isAlive()
            || living instanceof Player other && !player.canHarmPlayer(other)) return;
        var food=player.getFoodData();
        int hunger=it.hurts.metallurgy_reforged.config.GadgetsConfig.Gauntlet.gauntletHungerModifier*2;
        if(food.getFoodLevel()<hunger && !player.isCreative()) { GadgetBridge.tired(player);return; }
        try(State state=VersionBridge.entity(living)) {
            if(state.integer("punch_hits")<=0 || state.integer("punch_delay")>0) {
                state.integer("punch_delay",5);state.string("punch_owner",player.getUUID().toString());
                state.decimal("punch_yaw",player.getYHeadRot());state.decimal("punch_pitch",player.getXRot());
                if(living instanceof Mob mob && !state.flag("punch_ai_owned")) {
                    state.flag("punch_ai_previous",mob.isNoAi());state.flag("punch_ai_owned",true);mob.setNoAi(true);
                }
            }
        }
        if(!player.isCreative()) {
            int difference=hunger-Math.round(food.getSaturationLevel());
            if(food.getSaturationLevel()<=0) food.setFoodLevel(Math.max(0,food.getFoodLevel()-hunger));
            else if(difference>0) { food.setFoodLevel(Math.max(0,Math.min(20,food.getFoodLevel()+1)-difference));food.setSaturation(0); }
            else food.setSaturation(Math.max(0,food.getSaturationLevel()-hunger));
        }
        GadgetBridge.particles(living,false);
    }
    public static void livingTick(LivingEntity entity) {
        if(VersionBridge.client(entity) || !GadgetBridge.marked(entity,"punch_hits") && !GadgetBridge.marked(entity,"punch_delay")) return;
        try(State state=VersionBridge.entity(entity)) {
            if(state.integer("punch_delay")<=0 && state.integer("punch_hits")<=0) return;
            Player owner=null;
            try { owner=entity.level().getPlayerByUUID(UUID.fromString(state.string("punch_owner"))); }catch(IllegalArgumentException ignored) {}
            if(!entity.isAlive() || owner==null || !owner.isAlive()) { endPunch(entity,state);return; }
            int delay=state.integer("punch_delay"),hits=state.integer("punch_hits"),knock=state.integer("punch_knock");
            if(delay>0) {
                state.integer("punch_hits",Math.min(10000,hits+1));state.integer("punch_delay",delay-1);
                entity.setDeltaMovement(Vec3.ZERO);entity.invulnerableTime=11;VersionBridge.velocity(entity);return;
            }
            if(state.flag("punch_ai_owned")) restoreAi(entity,state);
            if(knock==0) { knock=1;state.integer("punch_knock",knock); }
            double yaw=Math.toRadians(state.decimal("punch_yaw")),pitch=Math.toRadians(state.decimal("punch_pitch"));
            Vec3 velocity=new Vec3(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch)).normalize().scale(hits>10?2:1);
            entity.setDeltaMovement(velocity);VersionBridge.velocity(entity);
            if(hits>10) {
                GadgetBridge.particles(entity,true);
                AABB area=entity.getBoundingBox().inflate(0.4);
                // One visit per block replaces the old0.1-block triple loop.
                if(!it.hurts.metallurgy_reforged.config.GadgetsConfig.Gauntlet.disableBlockGriefing && area.getXsize()*area.getYsize()*area.getZsize()<=512) for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(area.minX,area.minY,area.minZ),BlockPos.containing(area.maxX,area.maxY,area.maxZ))) {
                    if(GadgetBridge.punchBlock(owner,pos)) { entity.invulnerableTime=4;if(it.hurts.metallurgy_reforged.config.GadgetsConfig.Gauntlet.gauntletBlockDamageModifier!=0) VersionBridge.hit(entity,entity.damageSources().playerAttack(owner),(float)it.hurts.metallurgy_reforged.config.GadgetsConfig.Gauntlet.gauntletBlockDamageModifier); }
                }
            }
            state.integer("punch_knock",++knock);
            if(knock>hits/4) endPunch(entity,state);
        }
    }
    private static void restoreAi(LivingEntity entity,State state) {
        if(state.flag("punch_ai_owned") && entity instanceof Mob mob && mob.isNoAi()) mob.setNoAi(state.flag("punch_ai_previous"));
        state.remove("punch_ai_owned");state.remove("punch_ai_previous");
    }
    private static void endPunch(LivingEntity entity,State state) {
        restoreAi(entity,state);
        for(String key:List.of("punch_delay","punch_hits","punch_knock","punch_owner","punch_yaw","punch_pitch")) state.remove(key);
    }
    public static void cleanup(Player player) {
        if(player.level() instanceof ServerLevel level) for(Entity entity:level.getAllEntities()) if(entity instanceof LivingEntity living && GadgetBridge.marked(living,"punch_owner")) {
            try(State state=VersionBridge.entity(living)) { if(state.string("punch_owner").equals(player.getUUID().toString())) endPunch(living,state); }
        }
    }
    private GadgetLogic() {}
}
