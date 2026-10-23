package it.hurts.metallurgy_reforged.gadget;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gameevent.GameEvent;

public final class LighterItem extends FlintAndSteelItem {
    private final boolean vulcanite;
    public LighterItem(boolean vulcanite,Properties properties) { super(properties);this.vulcanite=vulcanite; }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();if(player==null) return InteractionResult.PASS;
        var level=context.getLevel();var stack=context.getItemInHand();BlockPos clicked=context.getClickedPos(),pos=clicked.relative(context.getClickedFace());
        if(!GadgetBridge.editable(player,clicked,context.getClickedFace(),stack)) return InteractionResult.FAIL;
        if(level.getBlockState(clicked).getBlock() instanceof TntBlock) {
            if(!VersionBridge.client(player)) {
                // Honor the TNT's native ignition hook, including modded subclasses.
                var state=level.getBlockState(clicked);if(!state.onCaughtFire(level,clicked,context.getClickedFace(),player)) return InteractionResult.FAIL;
                level.removeBlock(clicked,false);GadgetBridge.damage(player,stack,1,context.getHand()==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
            }
            return InteractionResult.SUCCESS;
        }
        if(!vulcanite && !player.isShiftKeyDown()) return super.useOn(context);
        if(!GadgetBridge.editable(player,pos,context.getClickedFace(),stack)) return InteractionResult.FAIL;
        if(vulcanite && player.isShiftKeyDown()) {
            if(!level.getBlockState(pos).canBeReplaced()) return InteractionResult.FAIL;
            if(!VersionBridge.client(player) && GadgetBridge.place(player,pos,context.getClickedFace(),stack,Blocks.LAVA.defaultBlockState())) {
                VersionBridge.sound(player,"block.fire.extinguish",1,1);
                if(!player.isCreative()) VersionBridge.cooldown(player,stack,400);
                GadgetBridge.damage(player,stack,25,context.getHand()==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
            }
            return InteractionResult.SUCCESS;
        }
        if(!VersionBridge.client(player)) {
            VersionBridge.sound(player,vulcanite?"entity.ghast.shoot":"item.flintandsteel.use",1,vulcanite?2:level.getRandom().nextFloat()*0.4F+0.8F);
            if(vulcanite || level.getRandom().nextBoolean()) {
                BlockPos center=pos.relative(player.getDirection());
                for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
                    BlockPos fire=center.offset(x,0,z);
                    if(level.hasChunkAt(fire) && level.isEmptyBlock(fire) && Blocks.FIRE.defaultBlockState().canSurvive(level,fire))
                        if(GadgetBridge.place(player,fire,context.getClickedFace(),stack,Blocks.FIRE.defaultBlockState())) level.gameEvent(player,GameEvent.BLOCK_PLACE,fire);
                }
                if(!player.isCreative()) GadgetBridge.damage(player,stack,vulcanite?1:5,context.getHand()==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
