package it.hurts.metallurgy_reforged.verification;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
public final class TestAttributes {
    public static double amount(ItemStack stack,EquipmentSlot slot,Holder<Attribute> attribute) {
        double[] sum={0};stack.forEachModifier(slot,(type,modifier)->{if(type.equals(attribute)) sum[0]+=modifier.amount();});return sum[0];
    }
}
