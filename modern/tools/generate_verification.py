"""Maintain equivalent native fixtures for all targets; never build or launch."""
from pathlib import Path
import re,json,shutil
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'verification/forge-1.20.1'
PACKAGE='it/hurts/metallurgy_reforged/verification'

def add_argument(text,call,argument):
    cursor=0
    while (start:=text.find(call,cursor))>=0:
        begin=start+len(call);position=begin;depth=1
        while depth:
            depth+=(text[position]=='(')-(text[position]==')');position+=1
        text=text[:position-1]+','+argument+text[position-1:];cursor=position+len(argument)+1
    return text

def main():
    original=(BASE/'java'/PACKAGE/'CoreGameTests.java').read_text(encoding='utf-8')
    tests=re.findall(r'@GameTest\(template="empty",timeoutTicks=(\d+)\)\s+public static void (\w+)',original)
    for target in ['neoforge-1.21.1','neoforge-26.1.2']:
        latest=target.endswith('26.1.2');out=ROOT/'verification'/target/'java'/PACKAGE;out.mkdir(parents=True,exist_ok=True)
        t=original.replace('net.minecraftforge.','net.neoforged.neoforge.')
        t=t.replace('var result=(net.minecraft.server.level.ServerPlayer)player(helper.getLevel());helper.getLevel().addNewPlayer(result);return result;', 'return helper.makeMockServerPlayerInLevel();')
        t=t.replace('import net.neoforged.neoforge.common.capabilities.ForgeCapabilities;','import net.neoforged.neoforge.capabilities.Capabilities;')
        t=t.replace('weapon.getOrCreateTag().putString("paragon","amordrine")','setParagon(weapon,"amordrine")')
        t=t.replace('platinum.getOrCreateTag().getBoolean("Unbreakable")','platinum.has(net.minecraft.core.component.DataComponents.UNBREAKABLE)')
        t=t.replace('input.setHoverName(net.minecraft.network.chat.Component.literal("Retained name"))','input.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Retained name"))')
        t=re.sub(r'\b([a-z]\w*)\.hurt\(',r'VersionBridge.hit(\1,',t)
        t=t.replace('new Arrow(level,attacker)','new Arrow(level,attacker,new ItemStack(Items.ARROW),null)')
        t=t.replace('chest.getItem() instanceof ArmorItem','VersionBridge.isArmor(chest)')
        t=t.replace('((ArmorItem)chest.getItem()).getDefense()', '(int)TestAttributes.amount(chest,EquipmentSlot.CHEST,Attributes.ARMOR)')
        t=re.sub(r'(\w+)\.getAttributeModifiers\((EquipmentSlot\.\w+)\)\.get\((Attributes\.\w+)\)\.stream\(\)\.mapToDouble\(m->m.getAmount\(\)\)\.sum\(\)',r'TestAttributes.amount(\1,\2,\3)',t)
        t=t.replace('mithril.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING,1)', 'mithril.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING),1)')
        t=t.replace('chest.saveWithFullMetadata()', 'chest.saveWithFullMetadata(level.registryAccess())').replace('crusher.saveWithFullMetadata()', 'crusher.saveWithFullMetadata(level.registryAccess())')
        t=add_argument(t,'BlockEntity.loadStatic(','level.registryAccess()')
        t=t.replace('RecipeType.CRAFTING,grid,helper.getLevel()', 'RecipeType.CRAFTING,grid.asCraftInput(),helper.getLevel()')
        t=t.replace('recipe.get().assemble(grid,helper.getLevel().registryAccess())','recipe.get().value().assemble(grid.asCraftInput(),helper.getLevel().registryAccess())')
        t=t.replace('recipe.get().matches(grid,helper.getLevel())','recipe.get().value().matches(grid.asCraftInput(),helper.getLevel())')
        t=t.replace('new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(corpse,corpse.damageSources().generic(),drops,0,false)', 'new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(corpse,corpse.damageSources().generic(),drops,false)')
        t=t.replace('net.neoforged.neoforge.common.MinecraftForge.EVENT_BUS', 'net.neoforged.neoforge.common.NeoForge.EVENT_BUS')
        t=t.replace('chest.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("Missing item handler"))', 'level.getCapability(Capabilities.ItemHandler.BLOCK,pos,null)')
        t=t.replace('crusher.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException("Top handler"))', 'level.getCapability(Capabilities.ItemHandler.BLOCK,crusherPos,Direction.UP)')
        t=t.replace('crusher.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(()->new IllegalStateException("Bottom handler"))', 'level.getCapability(Capabilities.ItemHandler.BLOCK,crusherPos,Direction.DOWN)')
        t=t.replace('    private static Player player(', '    private static void setParagon(ItemStack stack,String metal) { try(State state=VersionBridge.stack(stack)) { state.string("paragon",metal); } }\n    private static Player player(')
        t=re.sub(r'new net.minecraft.resources.ResourceLocation\(', 'net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(',t)
        if latest:
            t=t.replace('metallurgy$loot(cow.damageSources()', 'metallurgy$loot(helper.getLevel(),cow.damageSources()')
            t=t.replace('import net.neoforged.neoforge.gametest.*;','')
            t=t.replace('@GameTestHolder("metallurgy")','').replace('@PrefixGameTestTemplate(false)','')
            t=re.sub(r'^    @GameTest[^\n]*\n','',t,flags=re.M)
            t=t.replace('net.minecraft.world.entity.projectile.Arrow','net.minecraft.world.entity.projectile.arrow.Arrow')
            t=t.replace('net.minecraft.world.entity.monster.Zombie','net.minecraft.world.entity.monster.zombie.Zombie').replace('net.minecraft.world.entity.animal.Cow','net.minecraft.world.entity.animal.cow.Cow')
            t=t.replace('ResourceLocation','Identifier').replace('getRecipeManager()', 'recipeAccess()')
            t=t.replace('recipe.get().value().assemble(grid.asCraftInput(),helper.getLevel().registryAccess())','recipe.get().value().assemble(grid.asCraftInput())')
            t=re.sub(r'byKey\((net.minecraft.resources.Identifier.fromNamespaceAndPath\([^\n]+?\))\)',r'byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,\1))',t)
            t=t.replace('level.getCapability(Capabilities.ItemHandler.BLOCK,pos,null)', 'new TestTransferHandler(level.getCapability(Capabilities.Item.BLOCK,pos,null))')
            t=t.replace('level.getCapability(Capabilities.ItemHandler.BLOCK,crusherPos,Direction.UP)', 'new TestTransferHandler(level.getCapability(Capabilities.Item.BLOCK,crusherPos,Direction.UP))')
            t=t.replace('level.getCapability(Capabilities.ItemHandler.BLOCK,crusherPos,Direction.DOWN)', 'new TestTransferHandler(level.getCapability(Capabilities.Item.BLOCK,crusherPos,Direction.DOWN))')
            bootstrap='''package it.hurts.metallurgy_reforged.verification;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
@EventBusSubscriber(modid="metallurgy")
public final class VerificationBootstrap {
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION,helper->{
'''+''.join('            helper.register(Identifier.fromNamespaceAndPath("metallurgy","'+name.lower()+'"),CoreGameTests::'+name+');\n' for ticks,name in tests)+'''        });
    }
}
'''
            (out/'VerificationBootstrap.java').write_text(bootstrap)
            for ticks,name in tests:
                p=ROOT/'verification'/target/'resources/data/metallurgy/test_instance'/(name.lower()+'.json');p.parent.mkdir(parents=True,exist_ok=True)
                p.write_text(json.dumps({'type':'minecraft:function','function':'metallurgy:'+name.lower(),'environment':'minecraft:default','structure':'metallurgy:empty','max_ticks':int(ticks),'required':True},indent=2)+'\n')
            (out/'TestTransferHandler.java').write_text('''package it.hurts.metallurgy_reforged.verification;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
/** Test adapter exercises native committed and rolled-back transactions. */
public record TestTransferHandler(ResourceHandler<ItemResource> handler) {
    public TestTransferHandler { java.util.Objects.requireNonNull(handler,"Missing native item capability"); }
    public int getSlots() { return handler.size(); }
    public ItemStack extractItem(int slot,int amount,boolean simulate) {
        var resource=handler.getResource(slot);
        try(var transaction=Transaction.openRoot()) {
            int extracted=handler.extract(slot,resource,amount,transaction);
            if(!simulate) transaction.commit();return resource.toStack(extracted);
        }
    }
    public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
        try(var transaction=Transaction.openRoot()) {
            int inserted=handler.insert(slot,ItemResource.of(stack),stack.getCount(),transaction);
            if(!simulate) transaction.commit();return stack.copyWithCount(stack.getCount()-inserted);
        }
    }
}
''')
        (out/'CoreGameTests.java').write_text(t)
        (out/'TestAttributes.java').write_text('''package it.hurts.metallurgy_reforged.verification;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
public final class TestAttributes {
    public static double amount(ItemStack stack,EquipmentSlot slot,Holder<Attribute> attribute) {
        double[] sum={0};stack.forEachModifier(slot,(type,modifier)->{if(type.equals(attribute)) sum[0]+=modifier.amount();});return sum[0];
    }
}
''')
        fixture=ROOT/'verification'/target/'resources/data/metallurgy/structure/empty.nbt';fixture.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(BASE/'resources/data/metallurgy/structures/empty.nbt',fixture)
    print('Maintained',len(tests),'native fixtures for each NeoForge target.')
if __name__=='__main__':main()
