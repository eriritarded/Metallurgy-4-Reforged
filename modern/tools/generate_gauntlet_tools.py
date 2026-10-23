"""Maintain native iron pickaxe/shovel behavior on the existing gauntlet item."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
BEGIN='    // GAUNTLET_TOOL_START\n'
END='    // GAUNTLET_TOOL_END\n'

def tool_block(target):
    if target=='forge-1.20.1':
        return '''    private static Properties gauntletProperties(String id,Properties properties) { return properties; }
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
'''
    if target=='neoforge-1.21.1':
        return '''    private static Properties gauntletProperties(String id,Properties properties) {
        if(!id.equals("rubracium_gauntlet")) return properties;
        return properties.component(net.minecraft.core.component.DataComponents.TOOL,new net.minecraft.world.item.component.Tool(java.util.List.of(
            net.minecraft.world.item.component.Tool.Rule.deniesDrops(Tiers.IRON.getIncorrectBlocksForDrops()),
            net.minecraft.world.item.component.Tool.Rule.minesAndDrops(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE,Tiers.IRON.getSpeed()),
            net.minecraft.world.item.component.Tool.Rule.minesAndDrops(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL,Tiers.IRON.getSpeed())),1.0F,1));
    }
    private boolean gauntlet() { return id.equals("rubracium_gauntlet"); }
    @Override public boolean canPerformAction(ItemStack stack,net.neoforged.neoforge.common.ItemAbility ability) {
        return gauntlet() && (net.neoforged.neoforge.common.ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(ability) || net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability)) || super.canPerformAction(stack,ability);
    }
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return gauntlet()?Items.IRON_SHOVEL.useOn(context):super.useOn(context);
    }
'''
    if target=='neoforge-26.1.2':
        return '''    private static Properties gauntletProperties(String id,Properties properties) {
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
'''
    raise ValueError(target)

def apply(target):
    file=ROOT/target/'src/main/java/it/hurts/metallurgy_reforged/gadget/GadgetItem.java'
    text=file.read_text(encoding='utf-8-sig')
    text=text.replace('super(properties);this.id=id;','super(gauntletProperties(id,properties));this.id=id;')
    block=BEGIN+tool_block(target)+END
    if BEGIN in text:
        text=text[:text.index(BEGIN)]+block+text[text.index(END)+len(END):]
    else:
        constructor='    public GadgetItem(String id,Properties properties) { super(gauntletProperties(id,properties));this.id=id; }\n'
        if constructor not in text:raise RuntimeError('Unexpected gadget constructor')
        text=text.replace(constructor,constructor+block)
    method='canApplyAtEnchantingTable' if target.startswith('forge') else 'supportsEnchantment'
    start=text.index('    @Override public boolean '+method+'(')
    end=text.index('    @Override public boolean isFoil(',start)
    if target.startswith('forge'):
        enchant='''    @Override public boolean canApplyAtEnchantingTable(ItemStack stack,Enchantment enchantment) {
        if(gauntlet() && (enchantment.canApplyAtEnchantingTable(new ItemStack(Items.IRON_PICKAXE)) || enchantment.canApplyAtEnchantingTable(new ItemStack(Items.IRON_SHOVEL)))) return true;
        return (id.equals("brass_knuckles") || gauntlet()) && java.util.List.of(Enchantments.BANE_OF_ARTHROPODS,Enchantments.SMITE,Enchantments.MENDING,Enchantments.VANISHING_CURSE,Enchantments.UNBREAKING,Enchantments.SHARPNESS).contains(enchantment);
    }
'''
    else:
        identifier='Identifier' if target.endswith('26.1.2') else 'ResourceLocation'
        enchant='''    @Override public boolean supportsEnchantment(ItemStack stack,net.minecraft.core.Holder<Enchantment> enchantment) {
        if(gauntlet() && enchantment.value().isSupportedItem(stack)) return true;
        return (id.equals("brass_knuckles") || gauntlet()) && java.util.List.of("bane_of_arthropods","smite","mending","vanishing_curse","unbreaking","sharpness").stream().anyMatch(name->enchantment.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,IDENTIFIER.fromNamespaceAndPath("minecraft",name))));
    }
'''.replace('IDENTIFIER',identifier)
    text=text[:start]+enchant+text[end:]
    file.write_bytes(text.encode('utf-8'))
    folder='items' if target.startswith('forge') else 'item'
    for tag in ['pickaxes','shovels']:
        path=ROOT/target/'src/main/resources/data/minecraft/tags'/folder/(tag+'.json')
        value=json.loads(path.read_text(encoding='utf-8-sig')) if path.exists() else {'replace':False,'values':[]}
        if 'metallurgy:rubracium_gauntlet' not in value['values']:value['values'].append('metallurgy:rubracium_gauntlet')
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes((json.dumps(value,indent=2)+'\n').encode('utf-8'))

if __name__=='__main__':
    for target in ['forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2']:apply(target)
