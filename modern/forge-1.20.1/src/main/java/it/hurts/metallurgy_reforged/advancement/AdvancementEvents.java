package it.hurts.metallurgy_reforged.advancement;
import it.hurts.metallurgy_reforged.material.MaterialItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AdvancementEvent;
public final class AdvancementEvents {
    @SubscribeEvent public static void equip(LivingEquipmentChangeEvent event) {if(event.getEntity() instanceof ServerPlayer player) AdvancementRules.equipment(player,event.getSlot(),event.getTo());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if(event.getSource().getEntity() instanceof ServerPlayer player && MaterialItems.material(event.getEntity().getItemBySlot(EquipmentSlot.CHEST))!=null) Advancements.fire(player,"strong_opponent",0,"");
    }
    @SubscribeEvent public static void earned(AdvancementEvent.AdvancementEarnEvent event) {if(event.getEntity() instanceof ServerPlayer player) Advancements.fire(player,"advancement_granted",0,event.getAdvancement().getId().toString());}
    private AdvancementEvents() {}
}
