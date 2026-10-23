package it.hurts.metallurgy_reforged.gadget;
import it.hurts.metallurgy_reforged.effect.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class GadgetItem extends Item {
    private final String id;
    public GadgetItem(String id,Properties properties) { super(gauntletProperties(id,properties));this.id=id; }
    // GAUNTLET_TOOL_START
    private static Properties gauntletProperties(String id,Properties properties) {
        if(!id.equals("rubracium_gauntlet")) return properties;
        var blocks=net.minecraft.core.registries.BuiltInRegistries.acquireBootstrapRegistrationLookup(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        return properties.component(net.minecraft.core.component.DataComponents.TOOL,new net.minecraft.world.item.component.Tool(java.util.List.of(
            net.minecraft.world.item.component.Tool.Rule.deniesDrops(blocks.getOrThrow(ToolMaterial.IRON.incorrectBlocksForDrops())),
            net.minecraft.world.item.component.Tool.Rule.minesAndDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE),ToolMaterial.IRON.speed()),
            net.minecraft.world.item.component.Tool.Rule.minesAndDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL),ToolMaterial.IRON.speed())),1.0F,1,true));
    }
    private boolean gauntlet() { return id.equals("rubracium_gauntlet"); }
    @Override public boolean canPerformAction(ItemInstance stack,net.neoforged.neoforge.common.ItemAbility ability) {
        return gauntlet() && net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability) || super.canPerformAction(stack,ability);
    }
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return gauntlet()?Items.IRON_SHOVEL.useOn(context):super.useOn(context);
    }
    // GAUNTLET_TOOL_END
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        return GadgetLogic.use(player,hand)?InteractionResult.SUCCESS:InteractionResult.PASS;
    }
    @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) { GadgetLogic.hit(stack,target,attacker); }
    @Override public boolean onEntitySwing(ItemStack stack,LivingEntity entity,InteractionHand hand) {
        if(id.equals("rubracium_gauntlet") && stack==entity.getMainHandItem() && entity.swingingArm==InteractionHand.MAIN_HAND && GadgetLogic.dualGauntlets(entity)) {entity.swing(InteractionHand.OFF_HAND,true);return true;}
        return false;
    }
    @Override public boolean supportsEnchantment(ItemStack stack,net.minecraft.core.Holder<Enchantment> enchantment) {
        if(gauntlet() && enchantment.value().isSupportedItem(stack)) return true;
        return (id.equals("brass_knuckles") || gauntlet()) && java.util.List.of("bane_of_arthropods","smite","mending","vanishing_curse","unbreaking","sharpness").stream().anyMatch(name->enchantment.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,Identifier.fromNamespaceAndPath("minecraft",name))));
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
