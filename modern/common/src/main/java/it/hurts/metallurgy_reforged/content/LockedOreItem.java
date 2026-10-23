package it.hurts.metallurgy_reforged.content;
import it.hurts.metallurgy_reforged.effect.State;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
/** Source transmuted/duplicated ores cannot be placed and multiplied again. */
public final class LockedOreItem extends BlockItem {
    public LockedOreItem(Block block,Item.Properties properties) {super(block,properties);}
    @Override public InteractionResult useOn(UseOnContext context) {
        try(State data=VersionBridge.stack(context.getItemInHand())) {if(data.flag("locked")) return InteractionResult.FAIL;}
        return super.useOn(context);
    }
    @Override public boolean isFoil(ItemStack stack) {
        try(State data=VersionBridge.stack(stack)) {if(data.flag("locked")) return true;}
        return super.isFoil(stack);
    }
}
