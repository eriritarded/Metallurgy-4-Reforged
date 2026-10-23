package it.hurts.metallurgy_reforged.mixin;

import it.hurts.metallurgy_reforged.effect.ToolEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Redirect only real drop emission, never arbitrary getDrops simulations. */
@Mixin(Block.class)
public abstract class BlockLootMixin {
    // Older Forge emits drops here; newer Forge delegates to its added overload.
    // Exactly one real getDrops call must be intercepted across the two layouts.
    @Group(name="metallurgy$emittedDrops",min=1,max=1)
    @Redirect(method="dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
        require=0,expect=0,
        at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"))
    private static List<ItemStack> metallurgy$actualDrops(BlockState state,ServerLevel level,BlockPos pos,BlockEntity blockEntity,Entity breaker,ItemStack tool) {
        return ToolEffects.drops(level,pos,state,blockEntity,breaker,tool,Block.getDrops(state,level,pos,blockEntity,breaker,tool));
    }

    // Forge-added method has no Mojang/SRG mapping. Its getDrops invocation does.
    @Group(name="metallurgy$emittedDrops",min=1,max=1)
    @Redirect(method="dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Z)V",
        remap=false,require=0,expect=0,
        at=@At(value="INVOKE",remap=true,target="Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"))
    private static List<ItemStack> metallurgy$forgeActualDrops(BlockState state,ServerLevel level,BlockPos pos,BlockEntity blockEntity,Entity breaker,ItemStack tool) {
        return ToolEffects.drops(level,pos,state,blockEntity,breaker,tool,Block.getDrops(state,level,pos,blockEntity,breaker,tool));
    }
}
