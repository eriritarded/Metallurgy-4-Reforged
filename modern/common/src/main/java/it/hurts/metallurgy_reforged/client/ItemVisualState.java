package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.effect.*;
import net.minecraft.world.item.ItemStack;
/** Source Celenegil/Electrum activation predicates, including absorbed Tartarite. */
public final class ItemVisualState {
    public static boolean active(ItemStack stack) {
        String metal=EquipmentEffects.material(stack);
        try(State state=VersionBridge.stack(stack)) {return metal.equals("celenegil") && state.flag("glory_seeker") || metal.equals("electrum") && state.flag("voltage_control");}
    }
    private ItemVisualState() {}
}
