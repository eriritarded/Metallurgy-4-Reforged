package it.hurts.metallurgy_reforged.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
@Mixin(LivingEntity.class)
public interface LivingAccess {
    @Accessor("dead") void metallurgy$dead(boolean dead);
    @Invoker("dropFromLootTable") void metallurgy$loot(net.minecraft.server.level.ServerLevel level,DamageSource source,boolean playerKilled);
}
