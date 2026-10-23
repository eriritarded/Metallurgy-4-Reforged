package it.hurts.metallurgy_reforged.gadget;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class MetallurgyShieldItem extends ShieldItem {
    private final String id;
    public MetallurgyShieldItem(String id,Properties properties) { super(properties);this.id=id; }
    @Override public int getUseDuration(ItemStack stack,LivingEntity user) { return ShieldLogic.duration(id); }
    @Override public int getEnchantmentValue() { return switch(id) {case "lemurite_shield"->17;case "ceruclase_shield"->18;case "zinc_buckler"->8;case "vulcanite_buckler"->20;default->22;}; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);player.startUsingItem(hand);ShieldLogic.start(player,stack);return InteractionResultHolder.consume(stack);
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining) { if(remaining<=1) ShieldLogic.end(entity,stack,0); }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining) { ShieldLogic.end(entity,stack,remaining); }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) { ShieldLogic.end(entity,stack,0);return stack; }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient) { return false; }
}
