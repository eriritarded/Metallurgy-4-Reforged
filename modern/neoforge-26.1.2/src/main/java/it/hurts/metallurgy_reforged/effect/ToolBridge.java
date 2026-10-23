package it.hurts.metallurgy_reforged.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Native harvesting boundary. Real destruction is separate from loot queries. */
public final class ToolBridge {
    public static boolean same(ItemStack first,ItemStack second) {return ItemStack.isSameItemSameComponents(first,second);}
    public static boolean oreStack(ItemStack stack) {return stack.typeHolder().tags().anyMatch(t->t.location().getPath().startsWith("ores/"));}
    public static boolean oreBlock(BlockState state) {return state.typeHolder().tags().anyMatch(t->t.location().getPath().startsWith("ores/"));}
    public static int fortune(Player player,ItemStack stack) { return EnchantmentHelper.getItemEnchantmentLevel(player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),stack); }
    public static int required(BlockState state) {
        String id=VersionBridge.blockId(state);
        if(id.startsWith("metallurgy:") && id.endsWith("_ore")) {
            var metal=it.hurts.metallurgy_reforged.material.Materials.BY_NAME.get(id.substring(11,id.length()-4));
            if(metal!=null) return metal.oreLevel();
        }
        return state.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL)?3:state.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL)?2:state.is(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL)?1:0;
    }
    public static boolean canHarvest(Player player,BlockPos pos,BlockState state) {
        return state.getDestroySpeed(player.level(),pos)>=0 && (boostedHarvest(player,state) || player.getMainHandItem().isCorrectToolForDrops(state)
            && (!state.requiresCorrectToolForDrops() || player.getMainHandItem().isCorrectToolForDrops(state)));
    }
    public static boolean plant(BlockState state) { return state.getBlock() instanceof VegetationBlock; }
    public static boolean grass(BlockState state) { return state.getBlock()==Blocks.SHORT_GRASS || state.getBlock()==Blocks.FERN; }
    public static int boost(ItemStack stack) {
        if(!EquipmentEffects.accepts(stack,EquipmentEffects.Category.TOOL)) return 0;
        String metal=EquipmentEffects.enabledMaterial(stack,EquipmentEffects.Category.TOOL);
        if(!metal.equals("electrum") && !metal.equals("sanguinite")) return 0;
        try(State state=VersionBridge.stack(stack)) {return Math.max(0,Math.min(1,state.integer("harvest_boost")));}
    }
    public static boolean boostedHarvest(Player player,BlockState state) {
        ItemStack held=player.getMainHandItem();int boost=boost(held);if(boost==0) return false;
        String id=VersionBridge.blockId(state);if(!id.startsWith("metallurgy:") && !id.startsWith("minecraft:")) return false;
        var material=it.hurts.metallurgy_reforged.material.Materials.BY_NAME.get(EquipmentEffects.baseMetal(held));
        boolean appropriate=switch(EquipmentEffects.form(held)) {
            case "pickaxe"->state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE);
            case "axe"->state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE);
            case "shovel"->state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL);
            default->false;
        };
        return appropriate && material!=null && material.tool("harvest_level",0)+boost>=required(state);
    }
    public static boolean harvest(Player player,BlockPos pos,boolean damage) {
        if(!(player.level() instanceof ServerLevel level) || !player.mayBuild() || !level.hasChunkAt(pos)
            || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player,pos)) return false;
        BlockState state=level.getBlockState(pos);
        if(state.isAir() || !canHarvest(player,pos,state)) return false;
        return ToolEffects.auxiliary(()-> {
            // Honor cancellation before touching blocks or charging durability.
            var event=new net.neoforged.neoforge.event.level.block.BreakBlockEvent(level,pos,state,player);
            if(NeoForge.EVENT_BUS.post(event).isCanceled()) return false;
            boolean changed=level.destroyBlock(pos,true,player);
            if(changed && damage) VersionBridge.damageStack(player,player.getMainHandItem(),1);
            return changed;
        });
    }
    public static boolean harvestCrop(Player player,BlockPos pos) {
        if(!(player instanceof ServerPlayer server) || !player.mayBuild() || !player.level().mayInteract(player,pos)
            || !player.level().hasChunkAt(pos)) return false;
        // Actual player harvesting retains the held hoe in the native loot path.
        return server.gameMode.destroyBlock(pos);
    }
    private ToolBridge() {}
}
