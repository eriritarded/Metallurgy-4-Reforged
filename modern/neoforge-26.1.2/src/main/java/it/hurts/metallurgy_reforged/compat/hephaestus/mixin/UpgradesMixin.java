package it.hurts.metallurgy_reforged.compat.hephaestus.mixin;

import it.hurts.metallurgy_reforged.compat.hephaestus.HephaestusTraits;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.titammods.hephaestus_tools.table.ToolUpgrades",remap=false)
public abstract class UpgradesMixin {
    @Inject(method="unlockedSlots",at=@At("RETURN"),cancellable=true,remap=false)
    private static void metallurgy$writable(ItemStack tool,CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(HephaestusTraits.writableSlots(tool,callback.getReturnValue()));
    }
}
