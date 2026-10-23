package it.hurts.metallurgy_reforged.mixin;
import it.hurts.metallurgy_reforged.entity.VulcanitePrimedTnt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
/** Forge1.20's calculator lacks the later native entity-damage callback. */
@Mixin(Explosion.class)
public abstract class VulcaniteExplosionMixin {
    @Shadow @Final private Entity source;
    @ModifyArg(method="explode",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),index=1)
    private float metallurgy$vulcaniteDamage(float damage) {return source instanceof VulcanitePrimedTnt?(float)Math.floor((damage+1)/2):damage;}
}
