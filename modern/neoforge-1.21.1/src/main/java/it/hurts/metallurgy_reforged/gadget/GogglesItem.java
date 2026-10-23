package it.hurts.metallurgy_reforged.gadget;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
public final class GogglesItem extends Item {
    public GogglesItem(Properties properties) {super(properties);}
    @Override public EquipmentSlot getEquipmentSlot(ItemStack stack) {return EquipmentSlot.HEAD;}
    @Override public boolean canEquip(ItemStack stack,EquipmentSlot slot,LivingEntity entity) {return entity instanceof Player && slot==EquipmentSlot.HEAD;}
}
