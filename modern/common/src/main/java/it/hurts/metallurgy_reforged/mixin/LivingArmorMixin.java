package it.hurts.metallurgy_reforged.mixin;
import it.hurts.metallurgy_reforged.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class LivingArmorMixin {
    @Inject(method="tickDeath",at=@At("HEAD"),cancellable=true)
    private void metallurgy$corpse(CallbackInfo callback) {
        if(CorpseLifecycle.deathTick((LivingEntity)(Object)this)) callback.cancel();
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void metallurgy$movement(CallbackInfo callback) {
        if((Object)this instanceof Player player) { ArmorMovement.tick(player);ArmorPhasing.tick(player); }
    }
}
