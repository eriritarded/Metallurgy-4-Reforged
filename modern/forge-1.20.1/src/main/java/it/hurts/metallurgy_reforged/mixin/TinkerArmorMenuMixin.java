package it.hurts.metallurgy_reforged.mixin;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Native menu intent validation replaces mutating InventoryMenu's slot list. */
@Mixin(AbstractContainerMenu.class)
public abstract class TinkerArmorMenuMixin {
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void metallurgy$deeply(int slotId,int button,ClickType type,Player player,CallbackInfo callback) {
        var menu=(AbstractContainerMenu)(Object)this;
        if(slotId<0 || slotId>=menu.slots.size() || !it.hurts.metallurgy_reforged.compat.OptionalArmorGuard.locked(player)) return;
        var slot=menu.slots.get(slotId);
        if(slot.container==player.getInventory() && slot.getContainerSlot()>=36 && slot.getContainerSlot()<40) callback.cancel();
    }
}
