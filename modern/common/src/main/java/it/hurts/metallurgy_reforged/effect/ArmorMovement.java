package it.hurts.metallurgy_reforged.effect;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;
/** Quicksilver acceleration and its original full-block liquid collision. */
public final class ArmorMovement {
    private static int maximum(int count) { return Math.round(Math.max(8*(1-count/4F),0.5F)*20); }
    public static void tick(Player player) {
        int count=EquipmentEffects.armorLevel(player,"quicksilver");
        try(State state=VersionBridge.entity(player)) {
            int steps=state.integer("quicksilver_armor_steps");
            if(count==0 || !player.isSprinting()) {
                if(steps>0 || state.flag("quicksilver_step_owned")) { state.remove("quicksilver_armor_steps");VersionBridge.removeAttribute(player,"movement_speed","quicksilver_armor");VersionBridge.step(player,false); }
                return;
            }
            int maximum=maximum(count);
            if(steps<maximum) {
                steps++;state.integer("quicksilver_armor_steps",steps);
                VersionBridge.attribute(player,"movement_speed","quicksilver_armor",(2.5*count*count+2.5*count)*0.01*Math.min(steps/(double)maximum,1),2);
            } else VersionBridge.step(player,true);
        }
    }
    public static boolean liquid(Player player,FluidState fluid) {
        int count=EquipmentEffects.armorLevel(player,"quicksilver");if(count==0) return false;
        try(State state=VersionBridge.entity(player)) {
            return state.integer("quicksilver_armor_steps")>=maximum(count) && (fluid.is(FluidTags.WATER) || count>2 && fluid.is(FluidTags.LAVA));
        }
    }
    public static boolean attack(LivingEntity entity,DamageSource source) {
        int count=EquipmentEffects.armorLevel(entity,"quicksilver");if(count<3 || !(entity instanceof Player) || !source.is(DamageTypes.IN_FIRE)) return false;
        try(State state=VersionBridge.entity(entity)) { return state.integer("quicksilver_armor_steps")>=maximum(count); }
    }
    public static void cleanup(Player player) {
        VersionBridge.removeAttribute(player,"movement_speed","quicksilver_armor");VersionBridge.step(player,false);
        try(State state=VersionBridge.entity(player)) { state.remove("quicksilver_armor_steps"); }
    }
    private ArmorMovement() {}
}
