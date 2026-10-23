package it.hurts.metallurgy_reforged.gadget;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.Level;

public final class MetallurgyShieldItem extends ShieldItem {
    private final String id;
    public MetallurgyShieldItem(String id,Properties properties) {
        super(properties.delayedComponent(DataComponents.BLOCKS_ATTACKS,MetallurgyShieldItem::blockingComponent)
                .component(DataComponents.USE_EFFECTS,id.endsWith("_buckler")?new UseEffects(true,true,1):UseEffects.DEFAULT));
        this.id=id;
    }
    private static BlocksAttacks blockingComponent(HolderLookup.Provider registries) {
        // Match native 26.1.2 shield defaults without reading an unbound item's components.
        // The damage-type tag is resolved only when the component initializer receives registry context.
        return new BlocksAttacks(0.25F,1F,
                List.of(new BlocksAttacks.DamageReduction(90F,Optional.empty(),0F,1F)),
                new BlocksAttacks.ItemDamageFunction(3F,1F,1F),
                Optional.of(registries.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                Optional.of(SoundEvents.SHIELD_BLOCK),Optional.of(SoundEvents.SHIELD_BREAK));
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity user) { return ShieldLogic.duration(id); }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);player.startUsingItem(hand);ShieldLogic.start(player,stack);return InteractionResult.CONSUME;
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining) { if(remaining<=1) ShieldLogic.end(entity,stack,0); }
    @Override public boolean releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining) { ShieldLogic.end(entity,stack,remaining);return true; }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) { ShieldLogic.end(entity,stack,0);return stack; }
}
