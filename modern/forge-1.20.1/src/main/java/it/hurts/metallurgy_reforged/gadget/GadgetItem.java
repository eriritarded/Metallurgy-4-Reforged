package it.hurts.metallurgy_reforged.gadget;
import it.hurts.metallurgy_reforged.effect.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;

public final class GadgetItem extends Item {
    private final String id;
    public GadgetItem(String id,Properties properties) { super(gauntletProperties(id,properties));this.id=id; }
    // GAUNTLET_TOOL_START
    private static Properties gauntletProperties(String id,Properties properties) { return properties; }
    private boolean gauntlet() { return id.equals("rubracium_gauntlet"); }
    private static boolean gauntletMineable(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) || state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL);
    }
    @Override public float getDestroySpeed(ItemStack stack,net.minecraft.world.level.block.state.BlockState state) {
        return gauntlet() && gauntletMineable(state)?Tiers.IRON.getSpeed():super.getDestroySpeed(stack,state);
    }
    @Override public boolean isCorrectToolForDrops(net.minecraft.world.level.block.state.BlockState state) {
        return gauntlet() && gauntletMineable(state) && net.minecraftforge.common.TierSortingRegistry.isCorrectTierForDrops(Tiers.IRON,state);
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack,net.minecraft.world.level.block.state.BlockState state) {
        return gauntlet()?isCorrectToolForDrops(state):super.isCorrectToolForDrops(stack,state);
    }
    @Override public boolean mineBlock(ItemStack stack,Level level,net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.BlockPos pos,LivingEntity owner) {
        if(!gauntlet()) return super.mineBlock(stack,level,state,pos,owner);
        if(!level.isClientSide && state.getDestroySpeed(level,pos)!=0.0F) GadgetBridge.damage(owner,stack,1,EquipmentSlot.MAINHAND);
        return true;
    }
    @Override public boolean canPerformAction(ItemStack stack,net.minecraftforge.common.ToolAction action) {
        return gauntlet() && (net.minecraftforge.common.ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(action) || net.minecraftforge.common.ToolActions.DEFAULT_SHOVEL_ACTIONS.contains(action)) || super.canPerformAction(stack,action);
    }
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return gauntlet()?Items.IRON_SHOVEL.useOn(context):super.useOn(context);
    }
    // GAUNTLET_TOOL_END
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        // Item invokes this from its superclass constructor, before id is assigned.
        // Only the knuckles' baked model asks for this custom renderer.
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {return it.hurts.metallurgy_reforged.client.KnucklesRenderer.instance();}
        });
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        return GadgetLogic.use(player,hand)?InteractionResultHolder.success(player.getItemInHand(hand)):InteractionResultHolder.pass(player.getItemInHand(hand));
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) { GadgetLogic.hit(stack,target,attacker);return true; }
    @Override public boolean onEntitySwing(ItemStack stack,LivingEntity entity) {
        if(id.equals("rubracium_gauntlet") && stack==entity.getMainHandItem() && entity.swingingArm==InteractionHand.MAIN_HAND && GadgetLogic.dualGauntlets(entity)) {entity.swing(InteractionHand.OFF_HAND,true);return true;}
        return false;
    }
    @Override public int getEnchantmentValue() { return id.equals("brass_knuckles") || id.equals("rubracium_gauntlet")?1:0; }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack,Enchantment enchantment) {
        if(gauntlet() && (enchantment.canApplyAtEnchantingTable(new ItemStack(Items.IRON_PICKAXE)) || enchantment.canApplyAtEnchantingTable(new ItemStack(Items.IRON_SHOVEL)))) return true;
        return (id.equals("brass_knuckles") || gauntlet()) && java.util.List.of(Enchantments.BANE_OF_ARTHROPODS,Enchantments.SMITE,Enchantments.MENDING,Enchantments.VANISHING_CURSE,Enchantments.UNBREAKING,Enchantments.SHARPNESS).contains(enchantment);
    }
    @Override public boolean isFoil(ItemStack stack) {
        if(id.equals("electrum_magnet")) return GadgetLogic.magnetMode(stack)>1;
        try(State state=VersionBridge.stack(stack)) { return super.isFoil(stack) || state.flag("hasEffect"); }
    }
    @Override public boolean isBarVisible(ItemStack stack) { return id.equals("ore_detector")?GadgetLogic.detectorUses(stack)>0:super.isBarVisible(stack); }
    @Override public int getBarWidth(ItemStack stack) { return id.equals("ore_detector")?Math.max(0,Math.round(13F-13F*GadgetLogic.detectorUses(stack)/Math.max(1,25*GadgetLogic.detectorMetals(stack).size()))):super.getBarWidth(stack); }
    @Override public int getBarColor(ItemStack stack) {
        var metals=GadgetLogic.detectorMetals(stack);
        return id.equals("ore_detector") && !metals.isEmpty()?GadgetLogic.detectorColor(stack):super.getBarColor(stack);
    }
}
