package it.hurts.metallurgy_reforged.verification;

import com.mojang.authlib.GameProfile;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.inventory.*;
import it.hurts.metallurgy_reforged.material.Materials;
import it.hurts.metallurgy_reforged.effect.*;
import it.hurts.metallurgy_reforged.machine.*;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

/** Native server fixtures. These sources/resources never enter ordinary jars. */
@GameTestHolder("metallurgy")
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void materialStatsAndRegistrations(GameTestHelper helper) {
        helper.assertTrue(Materials.ALL.size()==49,"All49 original material definitions");
        int tools=0,armor=0;
        for(Materials.Definition material:Materials.ALL) {
            helper.assertTrue(!Content.stack(material.name()+"_ingot").isEmpty(),material.name()+" ingot exists");
            if(material.hasTools()) {
                tools++;
                helper.assertTrue(Content.stack(material.name()+"_sword").getMaxDamage()==(int)material.tool("durability",0),material.name()+" tool durability");
            }
            if(material.hasArmor()) {
                armor++;
                ItemStack chest=Content.stack(material.name()+"_chestplate");
                helper.assertTrue(chest.getItem() instanceof ArmorItem,material.name()+" native armor item");
                helper.assertTrue(((ArmorItem)chest.getItem()).getDefense()==material.defense(2),material.name()+" chestplate defense");
            }
        }
        helper.assertTrue(tools==39 && armor==41,"Original tool/armor set counts");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void chestPersistenceAutomationAndShiftClick(GameTestHelper helper) {
        ServerLevel level=helper.getLevel();
        Player player=player(level);
        int n=0;
        for(var tier:Content.CHEST_SIZES.entrySet()) {
            BlockPos pos=helper.absolutePos(new BlockPos(2+n*2,2,2));n++;
            var state=Content.BLOCK_MAP.get(tier.getKey()+"_chest").get().defaultBlockState();
            level.setBlockAndUpdate(pos,state);
            MetalChestEntity chest=(MetalChestEntity)level.getBlockEntity(pos);
            helper.assertTrue(chest!=null && chest.getContainerSize()==tier.getValue(),tier.getKey()+" capacity");
            int last=chest.getContainerSize()-1;
            chest.setItem(last,new ItemStack(Items.DIAMOND,17));
            CompoundTag saved=chest.saveWithFullMetadata();
            BlockEntity restored=BlockEntity.loadStatic(pos,state,saved);
            helper.assertTrue(restored instanceof MetalChestEntity,tier.getKey()+" native block entity restore");
            helper.assertTrue(((MetalChestEntity)restored).getItem(last).getCount()==17,tier.getKey()+" final slot survived save/load");
            var automation=chest.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("Missing item handler"));
            helper.assertTrue(automation.getSlots()==tier.getValue(),tier.getKey()+" exposes all automation slots");
            helper.assertTrue(automation.extractItem(last,5,true).getCount()==5 && chest.getItem(last).getCount()==17,"Simulation does not consume");
            helper.assertTrue(automation.extractItem(last,5,false).getCount()==5 && chest.getItem(last).getCount()==12,"Actual extraction consumes once");
            player.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);
            MetalChestMenu menu=new MetalChestMenu(1,player.getInventory(),chest,pos);
            player.containerMenu=menu;
            helper.assertTrue(menu.slots.size()==tier.getValue()+36,tier.getKey()+" complete menu slots");
            ItemStack moved=menu.quickMoveStack(player,last);
            helper.assertTrue(moved.getCount()==12 && chest.getItem(last).isEmpty(),tier.getKey()+" shift-click uses final chest slot");
            menu.removed(player);
            player.getInventory().clearContent();
        }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void brokenChestDropsInventoryOnce(GameTestHelper helper) {
        BlockPos pos=helper.absolutePos(new BlockPos(4,2,4));
        ServerLevel level=helper.getLevel();
        level.setBlockAndUpdate(pos,Content.BLOCK_MAP.get("platinum_chest").get().defaultBlockState());
        MetalChestEntity chest=(MetalChestEntity)level.getBlockEntity(pos);
        chest.setItem(107,new ItemStack(Items.DIAMOND,17));
        level.destroyBlock(pos,true);
        helper.runAfterDelay(3,()->{
            int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream()
                .filter(entity->entity.getItem().is(Items.DIAMOND)).mapToInt(entity->entity.getItem().getCount()).sum();
            helper.assertTrue(count==17,"Breaking a chest drops exactly17 stored diamonds; got"+count);
            helper.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void divineGraceOwnershipRegression(GameTestHelper helper) {
        ServerLevel level=helper.getLevel();
        Player attacker=player(level);
        attacker.setPos(helper.absolutePos(new BlockPos(2,2,2)).getCenter());
        attacker.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.BOW));
        for(String id:new String[]{"amordrine_sword","amordrine_axe","tartarite_sword","tartarite_axe"}) {
            ItemStack weapon=Content.stack(id);
            if(id.startsWith("tartarite")) weapon.getOrCreateTag().putString("paragon","amordrine");
            attacker.setItemInHand(InteractionHand.MAIN_HAND,weapon);
            Zombie melee=victim(level,helper);
            melee.hurt(level.damageSources().playerAttack(attacker),10);
            close(helper,50-melee.getHealth(),20,id+" direct melee follows missing-health curve");
            Zombie ranged=victim(level,helper);
            Arrow arrow=new Arrow(level,attacker);
            ranged.hurt(level.damageSources().arrow(arrow,attacker),10);
            close(helper,50-ranged.getHealth(),10,id+" must not boost off-hand arrow damage");
            Zombie magic=victim(level,helper);
            magic.hurt(level.damageSources().indirectMagic(arrow,attacker),10);
            close(helper,50-magic.getHealth(),10,id+" must not boost indirect magic");
        }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void immediateWeaponAndTrueDamage(GameTestHelper helper) {
        ServerLevel level=helper.getLevel();Player attacker=player(level);
        attacker.setItemInHand(InteractionHand.MAIN_HAND,Content.stack("oureclase_sword"));
        Zombie full=victim(level,helper);full.setHealth(100);
        full.hurt(level.damageSources().playerAttack(attacker),10);
        close(helper,100-full.getHealth(),20,"Oureclase doubles first strike");
        attacker.setItemInHand(InteractionHand.MAIN_HAND,Content.stack("desichalkos_sword"));
        Zombie armored=victim(level,helper);armored.getAttribute(Attributes.ARMOR).setBaseValue(20);
        armored.setAbsorptionAmount(4);
        armored.hurt(level.damageSources().playerAttack(attacker),10);
        close(helper,armored.getHealth(),40,"Desichalkos bypasses armor and absorption");
        close(helper,armored.getAbsorptionAmount(),0,"Desichalkos also removes absorption");
        Zombie lethal=victim(level,helper);lethal.setHealth(5);
        lethal.hurt(level.damageSources().playerAttack(attacker),10);
        helper.runAfterDelay(25,()->{
            helper.assertTrue(lethal.isRemoved(),"Native death processing removes lethal true-damage victim");
            helper.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void delayedMithrilAndOwnedDashCleanup(GameTestHelper helper) {
        ServerLevel level=helper.getLevel();Player attacker=player(level);
        attacker.setItemInHand(InteractionHand.MAIN_HAND,Content.stack("mithril_sword"));
        Zombie target=victim(level,helper);attacker.setLastHurtMob(target);
        target.hurt(level.damageSources().playerAttack(attacker),10);
        close(helper,target.getHealth(),40,"Mithril follow-up is delayed");
        helper.onEachTick(()->WeaponLifecycle.tick(attacker));
        helper.runAfterDelay(24,()->{
            float extra=Math.max(1,Materials.BY_NAME.get("mithril").tool("damage",0)*0.3F);
            close(helper,target.getHealth(),40-extra,"Mithril deals source-derived delayed magic damage once");
            attacker.setItemInHand(InteractionHand.MAIN_HAND,Content.stack("quicksilver_sword"));
            attacker.setInvulnerable(true);
            WeaponLifecycle.damaged(attacker);
        });
        helper.runAfterDelay(28,()->{
            WeaponLifecycle.cleanup(attacker);
            helper.assertTrue(attacker.isInvulnerable(),"Cleanup preserves pre-existing invulnerability");
            helper.assertTrue(!WeaponLifecycle.active(attacker,"quicksilver"),"Cleanup clears owned dash timer");
            helper.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void equipmentExtraAttributes(GameTestHelper helper) {
        ItemStack helmet=Content.stack("adamantine_helmet");
        double health=helmet.getAttributeModifiers(EquipmentSlot.HEAD).get(Attributes.MAX_HEALTH).stream().mapToDouble(m->m.getAmount()).sum();
        close(helper,(float)health,5,"Adamantine helmet receives one quarter of20 set health");
        ItemStack axe=Content.stack("tartarite_axe");
        double damage=axe.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(m->m.getAmount()).sum();
        close(helper,(float)damage,9*1.25F+3,"Axe custom1.12 damage formula retained");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=220)
    public static void machineProcessingFuelAndSidedAutomation(GameTestHelper helper) {
        ServerLevel level=helper.getLevel();
        BlockPos crusherPos=helper.absolutePos(new BlockPos(3,2,3));
        BlockPos alloyPos=helper.absolutePos(new BlockPos(7,2,3));
        level.setBlockAndUpdate(crusherPos,Content.BLOCK_MAP.get("crusher").get().defaultBlockState());
        level.setBlockAndUpdate(alloyPos,Content.BLOCK_MAP.get("alloyer").get().defaultBlockState());
        MachineEntity crusher=(MachineEntity)level.getBlockEntity(crusherPos),alloyer=(MachineEntity)level.getBlockEntity(alloyPos);
        crusher.setItem(0,Content.stack("copper_ore"));crusher.setItem(1,new ItemStack(Items.COAL));
        ItemStack copper=Content.stack("copper_ingot");copper.setCount(3);
        alloyer.setItem(0,Content.stack("tin_ingot"));alloyer.setItem(1,copper);alloyer.setItem(2,new ItemStack(Items.COAL));
        var top=crusher.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException("Top handler"));
        var bottom=crusher.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(()->new IllegalStateException("Bottom handler"));
        helper.assertTrue(top.getSlots()==1 && bottom.getSlots()==3,"Crusher top/bottom automation slot sets");
        helper.assertTrue(!top.insertItem(0,new ItemStack(Items.DIAMOND),true).isEmpty(),"Top refuses non-crushable input");
        helper.assertTrue(bottom.insertItem(0,Content.stack("copper_dust"),true).getCount()==1,"Output refuses automation insertion");
        helper.runAfterDelay(145,()->{
            helper.assertTrue(crusher.getItem(0).isEmpty() && crusher.getItem(2).getCount()==2,"One ore becomes2 dust after140 working ticks");
            helper.assertTrue(alloyer.getItem(0).isEmpty() && alloyer.getItem(1).isEmpty() && alloyer.getItem(3).getCount()==4,"Reversed3 copper +1 tin consumes exact ratio and yields4 bronze");
            helper.assertTrue(alloyer.getItem(3).is(Content.ITEM_MAP.get("bronze_ingot").get()),"Alloy output material");
            helper.assertTrue(crusher.totalBurnTime==1120 && alloyer.totalBurnTime==1120,"Coal burn scales1600 *140 /200");
            BlockEntity restored=BlockEntity.loadStatic(crusherPos,crusher.getBlockState(),crusher.saveWithFullMetadata());
            helper.assertTrue(restored instanceof MachineEntity saved && saved.burnTime==crusher.burnTime && saved.getItem(2).getCount()==2,"Machine native inventory/burn persistence");
            helper.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=130)
    public static void enhancedFuelAndLoadedCraftingRecipes(GameTestHelper helper) {
        ServerLevel level=helper.getLevel();
        BlockPos pos=helper.absolutePos(new BlockPos(3,2,3));
        level.setBlockAndUpdate(pos,Content.BLOCK_MAP.get("crusher").get().defaultBlockState());
        MachineEntity crusher=(MachineEntity)level.getBlockEntity(pos);
        crusher.setItem(0,Content.stack("copper_ore"));crusher.setItem(1,Content.stack("ignatius_fuel"));
        helper.assertTrue(level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("metallurgy","legacy/block/crusher")).isPresent(),"Converted original crusher crafting recipe loaded");
        helper.assertTrue(level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("metallurgy","smelting/tartarite_dust")).isPresent(),"Original metal dust smelting loaded");
        helper.runAfterDelay(49,()->{
            helper.assertTrue(crusher.getItem(0).isEmpty() && crusher.getItem(2).getCount()==2,"Ignatius fuel3x speed completes within48 ticks");
            helper.assertTrue(crusher.totalBurnTime==3360 && crusher.fuelBoost==3,"Ignatius source fuel value and speed");
            helper.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeParagonRecipePreservesEquipment(GameTestHelper helper) {
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(Player player) { return true; }
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        for(String form:new String[]{"sword","axe","pickaxe","shovel","hoe","helmet","chestplate","leggings","boots"}) {
            grid.clearContent();ItemStack input=Content.stack("tartarite_"+form);input.setDamageValue(20);
            input.setHoverName(net.minecraft.network.chat.Component.literal("Retained name"));
            grid.setItem(4,input);grid.setItem(1,Content.stack("amordrine_block"));
            grid.setItem(3,Content.stack("copper_block"));grid.setItem(5,Content.stack("steel_block"));grid.setItem(7,Content.stack("bronze_block"));
            var recipe=helper.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,helper.getLevel());
            helper.assertTrue(recipe.isPresent(),"Native Paragon recipe matches "+form);
            ItemStack output=recipe.get().assemble(grid,helper.getLevel().registryAccess());
            helper.assertTrue(output.is(input.getItem()) && output.getDamageValue()==20 && output.getHoverName().getString().equals("Retained name"),"Paragon copies item data for "+form);
            try(State state=VersionBridge.stack(output)) { helper.assertTrue(state.string("paragon").equals("amordrine") && state.flag("paragon_absorbed"),"Top block controls absorbed metal"); }
            helper.assertTrue(output.getMaxDamage()>input.getMaxDamage(),"Paragon increases valid durability for "+form);
            grid.setItem(4,output);
            helper.assertTrue(!recipe.get().matches(grid,helper.getLevel()),"Cannot absorb twice");
        }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void haderothRebirthAndArcaneEquipment(GameTestHelper helper) {
        Player attacker=player(helper.getLevel());ItemStack sword=Content.stack("haderoth_sword");int base=sword.getMaxDamage();
        sword.setDamageValue(base-1);attacker.setItemInHand(InteractionHand.MAIN_HAND,sword);AllEquipmentEffects.use(attacker);
        helper.assertTrue(sword.getMaxDamage()==base*25 && sword.getDamageValue()==0,"Haderoth rebirth restores25x durability");
        Zombie target=victim(helper.getLevel(),helper);target.hurt(target.damageSources().playerAttack(attacker),10);
        close(helper,target.getHealth(),38,"Reborn weapon adds2 native damage");
        ItemStack helmet=Content.stack("haderoth_helmet");int armorBase=helmet.getMaxDamage();helmet.setDamageValue(armorBase-1);
        target.setItemSlot(EquipmentSlot.HEAD,helmet);target.invulnerableTime=0;target.hurt(target.damageSources().generic(),8);
        ItemStack reborn=target.getItemBySlot(EquipmentSlot.HEAD);
        helper.assertTrue(reborn.getMaxDamage()==armorBase*25,"Armor rebirth before break");
        double protection=reborn.getAttributeModifiers(EquipmentSlot.HEAD).get(Attributes.ARMOR).stream().mapToDouble(m->m.getAmount()).sum();
        close(helper,(float)protection,Materials.BY_NAME.get("haderoth").defense(3)+2,"Reborn armor replaces defaults with source protection");
        ItemStack mithril=Content.stack("mithril_sword");mithril.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING,1);
        AllEquipmentEffects.equipped(attacker,EquipmentSlot.MAINHAND,mithril);
        double attack=mithril.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(m->m.getAmount()).sum();
        close(helper,(float)attack,3+Materials.BY_NAME.get("mithril").tool("damage",0)+4,"Mithril enchantment count adds source arcane combat bonus");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void soulboundDropsCloneAndPlatinumUnbreakable(GameTestHelper helper) {
        Player corpse=player(helper.getLevel());corpse.getInventory().clearContent();
        var drops=new java.util.ArrayList<ItemEntity>();
        drops.add(new ItemEntity(helper.getLevel(),0,0,0,Content.stack("amordrine_sword")));
        drops.add(new ItemEntity(helper.getLevel(),0,0,0,Content.stack("sanguinite_sword")));
        drops.add(new ItemEntity(helper.getLevel(),0,0,0,new ItemStack(Items.DIAMOND)));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingDropsEvent(corpse,corpse.damageSources().generic(),drops,0,false));
        helper.assertTrue(drops.size()==1 && drops.get(0).getItem().is(Items.DIAMOND),"Soulbound retains Amordrine and Sanguinite is destroyed");
        Player replacement=player(helper.getLevel());replacement.getInventory().clearContent();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(replacement,corpse,true));
        helper.assertTrue(replacement.getInventory().countItem(Content.ITEM_MAP.get("amordrine_sword").get())==1,"Clone event restores exactly one soulbound sword");
        ItemStack platinum=Content.stack("platinum_sword");replacement.setItemInHand(InteractionHand.MAIN_HAND,platinum);
        AllEquipmentEffects.equipped(replacement,EquipmentSlot.MAINHAND,platinum);VersionBridge.damageHeld(replacement,50);
        helper.assertTrue(platinum.getDamageValue()==0 && platinum.getOrCreateTag().getBoolean("Unbreakable"),"Platinum held tool ignores durability damage");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void adamantineNativeDeathProtection(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow target=livingVictim(helper.getLevel(),helper);target.setHealth(5);
        target.setItemSlot(EquipmentSlot.HEAD,Content.stack("adamantine_helmet"));target.setItemSlot(EquipmentSlot.CHEST,Content.stack("adamantine_chestplate"));
        target.setItemSlot(EquipmentSlot.LEGS,Content.stack("adamantine_leggings"));target.setItemSlot(EquipmentSlot.FEET,Content.stack("adamantine_boots"));
        target.hurt(target.damageSources().generic(),1000);
        helper.assertTrue(target.isAlive() && target.getHealth()==1,"Full Adamantine set sacrifices a piece to cancel native lethal damage");
        int remaining=0;for(var slot:EquipmentEffects.ARMOR_SLOTS) if(!target.getItemBySlot(slot).isEmpty()) remaining++;
        helper.assertTrue(remaining==3,"Exactly one armor piece is sacrificed");
        helper.assertTrue(VersionBridge.amplifier(target,"regeneration")==2 && VersionBridge.amplifier(target,"absorption")==1,"Source revival buffs");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void platinumPurificationAndVyroxeresStacks(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow purified=livingVictim(helper.getLevel(),helper);purified.setItemSlot(EquipmentSlot.HEAD,Content.stack("platinum_helmet"));
        VersionBridge.buff(purified,"poison",100,1);ArmorEffects.tick(purified);
        helper.assertTrue(VersionBridge.amplifier(purified,"poison")==-1 && VersionBridge.amplifier(purified,"regeneration")==1,"Platinum replaces poison with regeneration");
        helper.assertTrue(VersionBridge.duration(purified,"regeneration")==50,"One Platinum piece gives50% source duration");
        net.minecraft.world.entity.animal.Cow venom=livingVictim(helper.getLevel(),helper);venom.setItemSlot(EquipmentSlot.HEAD,Content.stack("vyroxeres_helmet"));
        VersionBridge.buff(venom,"speed",200,1);VersionBridge.buff(venom,"resistance",200,0);
        helper.runAfterDelay(3,()->{
            float base=Materials.BY_NAME.get("vyroxeres").defense(3);
            close(helper,(float)venom.getAttributeValue(Attributes.ARMOR),base+3,"Vyroxeres counts effect amplifiers,2+1 stacks");
            VersionBridge.removeBuff(venom,"speed");
            close(helper,(float)venom.getAttributeValue(Attributes.ARMOR),base+1,"Removing a potion updates protection immediately");
            venom.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
        });
        helper.runAfterDelay(6,()->{
            close(helper,(float)venom.getAttributeValue(Attributes.ARMOR),0,"Unequipping final Vyroxeres piece cleans its modifier");
            helper.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=110)
    public static void kalendriteRegenDelay(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow target=livingVictim(helper.getLevel(),helper);
        target.setItemSlot(EquipmentSlot.HEAD,Content.stack("kalendrite_helmet"));target.setItemSlot(EquipmentSlot.CHEST,Content.stack("kalendrite_chestplate"));
        target.setItemSlot(EquipmentSlot.LEGS,Content.stack("kalendrite_leggings"));target.setItemSlot(EquipmentSlot.FEET,Content.stack("kalendrite_boots"));
        target.hurt(target.damageSources().generic(),2);
        helper.runAfterDelay(30,()->helper.assertTrue(VersionBridge.amplifier(target,"regeneration")==-1,"No premature Kalendrite regen"));
        helper.runAfterDelay(48,()->{
            helper.assertTrue(VersionBridge.amplifier(target,"regeneration")==3,"Four pieces regenerate after source40-tick threshold");
            helper.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=130)
    public static void sublimationAuraPauseAndStatefulBlockItem(GameTestHelper helper) {
        ServerLevel level=helper.getLevel();BlockPos pos=helper.absolutePos(new BlockPos(3,2,3)),next=helper.absolutePos(new BlockPos(9,2,3));
        var block=Content.BLOCK_MAP.get("sublimation_chamber").get();level.setBlockAndUpdate(pos,block.defaultBlockState());
        ChamberEntity chamber=(ChamberEntity)level.getBlockEntity(pos);
        Player user=player(level);ItemStack blocks=Content.stack("adamantine_block");blocks.setCount(5);user.setItemInHand(InteractionHand.MAIN_HAND,blocks);
        helper.assertTrue(chamber.insert(user,InteractionHand.MAIN_HAND,Direction.UP),"Top accepts recipe block batch");
        helper.assertTrue(chamber.getItem(0).getCount()==3 && blocks.getCount()==2,"Manual insertion takes exactly3 blocks");
        user.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COAL));chamber.insert(user,InteractionHand.MAIN_HAND,Direction.NORTH);
        var nearby=chamberPlayer(helper);nearby.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
        chamber.tick();chamber.tick();chamber.tick();
        helper.assertTrue(chamber.fullDuration==18000 && chamber.recipeAmplifier==3 && VersionBridge.amplifier(nearby,"health_boost")==0,"Original recipe duration and actual amplifier0 aura behavior");
        int elapsed=chamber.activeTime,fuel=chamber.fuelTime;
        level.destroyBlock(pos,true);
        helper.runAfterDelay(3,()->{
            var dropped=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2),e->e.getItem().is(Content.ITEM_MAP.get("sublimation_chamber").get()));
            helper.assertTrue(dropped.size()==1 && dropped.get(0).getItem().getCount()==1,"Breaking chamber drops exactly one stateful item");
            helper.assertTrue(VersionBridge.amplifier(nearby,"health_boost")==-1,"Breaking chamber clears its tracked aura");
            ItemStack packed=dropped.get(0).getItem().copy();dropped.get(0).discard();
            level.setBlockAndUpdate(next,block.defaultBlockState());
            helper.assertTrue(BlockItem.updateCustomBlockEntityTag(level,user,next,packed),"Vanilla placement data path restores chamber");
            ChamberEntity restored=(ChamberEntity)level.getBlockEntity(next);
            helper.assertTrue(restored.activeTime==elapsed && restored.fuelTime==fuel && restored.getItem(0).getCount()==3 && restored.effect.equals("health_boost"),"Item retains original clock, fuel, batch and cached effect");
            restored.fuelTime=1;restored.setItem(1,ItemStack.EMPTY);restored.tick();
            helper.assertTrue(restored.activeTime==elapsed && !restored.getBlockState().getValue(ChamberBlock.LIT),"Fuel exhaustion pauses clock");
            restored.setItem(1,new ItemStack(Items.COAL));restored.tick();restored.tick();
            helper.assertTrue(restored.activeTime==elapsed+1 && restored.getBlockState().getValue(ChamberBlock.LIT),"Refueling resumes existing clock");
            restored.activeTime=restored.fullDuration;restored.tick();
            helper.assertTrue(restored.getItem(0).isEmpty() && restored.effect.isEmpty() && restored.activeTime==0,"Completed sublimation consumes batch once and clears cached effect");
            level.removePlayerImmediately(nearby,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);helper.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void sublimationRecipeBatchBoundaries(GameTestHelper helper) {
        helper.assertTrue(SublimationRecipes.ALL.size()==14,"All14 original sublimation recipes");
        for(var recipe:SublimationRecipes.ALL) {
            ItemStack blocks=Content.stack(recipe.metal()+"_block");blocks.setCount(recipe.blocks());
            helper.assertTrue(SublimationRecipes.match(blocks)==recipe,recipe.metal()+" exact batch matches");
            blocks.grow(1);helper.assertTrue(SublimationRecipes.match(blocks)==null,"Oversized batches do not start");
            blocks.setCount(recipe.blocks()-1);helper.assertTrue(SublimationRecipes.match(blocks)==null,"Undersized batches do not start");
        }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void atlarusIncomingProjectileWind(GameTestHelper helper) {
        ServerLevel level=helper.getLevel();Player attacker=player(level);attacker.setPos(helper.absolutePos(new BlockPos(5,2,5)).getCenter());
        attacker.setItemSlot(EquipmentSlot.HEAD,Content.stack("atlarus_helmet"));
        Arrow arrow=new Arrow(level,attacker);arrow.setPos(attacker.position().add(2.7,1,0));arrow.setDeltaMovement(-3,0,0);level.addFreshEntity(arrow);
        ArmorEffects.tick(attacker);
        helper.assertTrue(!arrow.getDeltaMovement().equals(new net.minecraft.world.phys.Vec3(-3,0,0)) && Double.isFinite(arrow.getDeltaMovement().length()),"Wind redirects incoming arrow with finite motion");
        arrow.setPos(attacker.position().add(0,1,0));arrow.setDeltaMovement(-3,0,0);ArmorEffects.tick(attacker);
        helper.assertTrue(arrow.getDeltaMovement().x==-3,"Wind does not redirect already overlapping projectile");
        arrow.setPos(attacker.position().add(2.7,1,0));arrow.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);ArmorEffects.tick(attacker);
        helper.assertTrue(arrow.getDeltaMovement().equals(net.minecraft.world.phys.Vec3.ZERO),"Zero motion avoids invalid normalization");
        arrow.discard();helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void ceruclaseAuraOpposingEntityCategory(GameTestHelper helper) {
        Player wearer=player(helper.getLevel());var target=livingVictim(helper.getLevel(),helper);wearer.setPos(target.position());
        wearer.setItemSlot(EquipmentSlot.HEAD,Content.stack("ceruclase_helmet"));ArmorEffects.tick(wearer);
        helper.assertTrue(VersionBridge.amplifier(target,"weakness")==0 && VersionBridge.duration(target,"mining_fatigue")==10,"One-piece aura debuffs nearby mob each tick");
        helper.assertTrue(VersionBridge.amplifier(wearer,"weakness")==-1,"Player aura excludes wearer");
        wearer.setItemSlot(EquipmentSlot.CHEST,Content.stack("ceruclase_chestplate"));wearer.setItemSlot(EquipmentSlot.LEGS,Content.stack("ceruclase_leggings"));ArmorEffects.tick(wearer);
        helper.assertTrue(VersionBridge.amplifier(target,"weakness")==1,"Three pieces use level minus2");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void celenegilSixthHitPurge(GameTestHelper helper) {
        Player wearer=player(helper.getLevel());var target=livingVictim(helper.getLevel(),helper);wearer.setPos(target.position().add(-2,0,0));
        wearer.setItemSlot(EquipmentSlot.HEAD,Content.stack("celenegil_helmet"));VersionBridge.buff(wearer,"poison",200,0);
        for(int i=0;i<5;i++) ArmorEffects.incoming(wearer,wearer.damageSources().generic(),1);
        helper.assertTrue(VersionBridge.amplifier(wearer,"poison")==0 && VersionBridge.amplifier(wearer,"regeneration")==-1,"First5 hits retain effects");
        ArmorEffects.incoming(wearer,wearer.damageSources().generic(),1);
        helper.assertTrue(VersionBridge.amplifier(wearer,"poison")==-1 && VersionBridge.amplifier(wearer,"regeneration")==1 && VersionBridge.duration(wearer,"regeneration")==40,"Sixth hit purges and gives source regeneration");
        helper.assertTrue(target.getDeltaMovement().x>0 && target.getDeltaMovement().y>0,"Purge repels nearby entities");
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("celenegil_hits")==0,"Hit cycle resets"); }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void desichalkosChargeAndConsumption(GameTestHelper helper) {
        Player wearer=player(helper.getLevel());wearer.setItemSlot(EquipmentSlot.HEAD,Content.stack("desichalkos_helmet"));
        for(int i=0;i<200;i++) ArmorLifecycle.tick(wearer);
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("desichalkos_quiet")==200 && state.integer("desichalkos_shields")==0,"Requires200 quiet ticks"); }
        wearer.tickCount=280;ArmorLifecycle.tick(wearer);
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("desichalkos_shields")==1,"140-tick boundary generates one shield"); }
        helper.assertTrue(ArmorLifecycle.hurt(wearer,wearer.damageSources().generic(),4),"Shield absorbs incoming hit");
        helper.assertTrue(!ArmorLifecycle.hurt(wearer,wearer.damageSources().generic(),4),"Consumed shield cannot absorb twice");
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("desichalkos_quiet")==0,"Any hit resets quiet timer"); }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void ignatiusFireAndCustomWaterDamage(GameTestHelper helper) {
        var wearer=livingVictim(helper.getLevel(),helper);wearer.setItemSlot(EquipmentSlot.HEAD,Content.stack("ignatius_helmet"));
        float health=wearer.getHealth();wearer.hurt(wearer.damageSources().inFire(),5);
        close(helper,wearer.getHealth(),health,"Native fire hook is canceled");
        helper.assertTrue(VersionBridge.waterDamage(wearer).is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),"Original water penalty bypasses armor");
        ArmorLifecycle.drank(wearer,new ItemStack(Items.POTION));close(helper,wearer.getHealth(),health-2,"Drinking applies2 water damage");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void prometheumCrouchGrowthCycle(GameTestHelper helper) {
        ServerLevel world=helper.getLevel();Player wearer=player(world);BlockPos center=helper.absolutePos(new BlockPos(10,3,10));wearer.setPos(center.getCenter());
        wearer.setItemSlot(EquipmentSlot.HEAD,Content.stack("prometheum_helmet"));wearer.setItemSlot(EquipmentSlot.CHEST,Content.stack("prometheum_chestplate"));
        wearer.setItemSlot(EquipmentSlot.LEGS,Content.stack("prometheum_leggings"));wearer.setItemSlot(EquipmentSlot.FEET,Content.stack("prometheum_boots"));
        for(BlockPos p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1))) { world.setBlockAndUpdate(p.below(),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());world.setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.OAK_SAPLING.defaultBlockState()); }
        wearer.getRandom().setSeed(83);wearer.setShiftKeyDown(true);ArmorLifecycle.tick(wearer);
        for(int step=1;step<=4;step++) {
            wearer.setShiftKeyDown(step==1 || step==4);
            try(State state=VersionBridge.entity(wearer)) { state.longValue("prometheum_armor_at",world.getGameTime()-10); }
            ArmorLifecycle.tick(wearer);
        }
        helper.assertTrue(VersionBridge.cooldown(wearer,wearer.getItemBySlot(EquipmentSlot.HEAD))>0,"Four10-tick steps end with9-second cooldown");
        boolean grew=false;for(BlockPos p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1))) if(world.getBlockState(p).is(net.minecraft.world.level.block.Blocks.OAK_LOG) || world.getBlockState(p).is(net.minecraft.world.level.block.Blocks.OAK_SAPLING) && world.getBlockState(p).getValue(net.minecraft.world.level.block.SaplingBlock.STAGE)>0) grew=true;
        helper.assertTrue(grew && wearer.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()>0,"Crouch transitions grow native plants at armor durability cost");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void amordrineServerBudgetAndIntentValidation(GameTestHelper helper) {
        var wearer=(net.minecraft.server.level.ServerPlayer)player(helper.getLevel());wearer.setOnGround(false);
        wearer.setItemSlot(EquipmentSlot.HEAD,Content.stack("amordrine_helmet"));wearer.setItemSlot(EquipmentSlot.CHEST,Content.stack("amordrine_chestplate"));
        ArmorControls.action(wearer,0);ArmorControls.action(wearer,0);
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("amordrine_jumps")==1,"Repeated packet in same tick cannot add jumps");state.remove("amordrine_last_action"); }
        ArmorControls.action(wearer,0);
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("amordrine_jumps")==2,"Two equipped pieces grant2 extra jumps");state.remove("amordrine_last_action"); }
        ArmorControls.action(wearer,0);ArmorControls.action(wearer,9999);
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("amordrine_jumps")==2,"Budget and unknown action rejected"); }
        wearer.setOnGround(true);ArmorControls.tick(wearer);
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("amordrine_jumps")==0,"Landing resets budget"); }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void krikInventoryHeightBoundAndFallProtection(GameTestHelper helper) {
        var wearer=(net.minecraft.server.level.ServerPlayer)player(helper.getLevel());wearer.getInventory().clearContent();
        wearer.setItemSlot(EquipmentSlot.HEAD,Content.stack("krik_helmet"));wearer.setItemSlot(EquipmentSlot.CHEST,Content.stack("krik_chestplate"));
        wearer.setItemSlot(EquipmentSlot.LEGS,Content.stack("krik_leggings"));wearer.setItemSlot(EquipmentSlot.FEET,Content.stack("krik_boots"));
        for(int i=0;i<40;i++) { try(State state=VersionBridge.entity(wearer)) { state.remove("krik_last_action"); } ArmorControls.action(wearer,1); }
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("krik_height")==27,"Only27 non-hotbar empty slots contribute"); }
        wearer.getInventory().setItem(9,new ItemStack(Items.STONE));ArmorControls.tick(wearer);
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("krik_height")==26,"Filling inventory clamps levitation height"); }
        helper.assertTrue(ArmorControls.fall(wearer),"Full Krik set cancels fall damage");wearer.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
        helper.assertTrue(!ArmorControls.fall(wearer),"Partial set does not cancel fall damage");ArmorControls.cleanup(wearer);helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void inolashiteDelayedRecallAndCleanup(GameTestHelper helper) {
        ServerLevel world=helper.getLevel();Player wearer=new net.minecraftforge.common.util.FakePlayer(world,new GameProfile(UUID.randomUUID(),"RecallTest")) {
            @Override public void teleportTo(double x,double y,double z) { setPos(x,y,z); }
        };
        BlockPos start=helper.absolutePos(new BlockPos(8,3,8));wearer.setPos(start.getCenter());wearer.setHealth(18);wearer.getFoodData().setFoodLevel(19);
        wearer.setItemSlot(EquipmentSlot.HEAD,Content.stack("inolashite_helmet"));wearer.tickCount=5;ArmorControls.tick(wearer);
        wearer.setPos(start.getCenter().add(4,0,0));wearer.setHealth(5);wearer.getFoodData().setFoodLevel(6);wearer.setShiftKeyDown(true);ArmorControls.jumped(wearer);
        close(helper,wearer.getHealth(),5,"Recall has10-tick delay");
        try(State state=VersionBridge.entity(wearer)) { state.longValue("inolashite_recall_at",world.getGameTime()); }
        ArmorControls.tick(wearer);
        close(helper,wearer.getHealth(),18,"Recall restores captured health");helper.assertTrue(wearer.blockPosition().equals(start) && wearer.getFoodData().getFoodLevel()==19,"Captured block position and food restored");
        helper.assertTrue(VersionBridge.cooldown(wearer,wearer.getItemBySlot(EquipmentSlot.HEAD))>0,"One piece gives1280-tick cooldown");
        ArmorControls.cleanup(wearer);
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(!state.has("inolashite_recall_at"),"Dimension/death cleanup clears pending recall"); }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void quicksilverNativeLiquidCollisionAndCleanup(GameTestHelper helper) {
        Player wearer=player(helper.getLevel());
        float previousStep=wearer.maxUpStep();
        wearer.setItemSlot(EquipmentSlot.HEAD,Content.stack("quicksilver_helmet"));wearer.setItemSlot(EquipmentSlot.CHEST,Content.stack("quicksilver_chestplate"));
        wearer.setItemSlot(EquipmentSlot.LEGS,Content.stack("quicksilver_leggings"));wearer.setItemSlot(EquipmentSlot.FEET,Content.stack("quicksilver_boots"));
        var water=net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();var lava=net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState();
        var context=net.minecraft.world.phys.shapes.CollisionContext.of(wearer);BlockPos pos=helper.absolutePos(new BlockPos(4,2,4));
        helper.assertTrue(water.getCollisionShape(helper.getLevel(),pos,context).isEmpty(),"Water is initially noncolliding");wearer.setSprinting(true);
        for(int i=0;i<11;i++) ArmorMovement.tick(wearer);
        helper.assertTrue(water.getCollisionShape(helper.getLevel(),pos,context).bounds().getYsize()==1 && lava.getCollisionShape(helper.getLevel(),pos,context).bounds().getYsize()==1,"Native liquid block mixin supplies full-block water and lava surfaces");
        helper.assertTrue(wearer.maxUpStep()>=1,"Maximum acceleration raises step height");
        helper.assertTrue(ArmorLifecycle.attack(wearer,wearer.damageSources().inFire(),2) && !ArmorLifecycle.attack(wearer,wearer.damageSources().onFire(),2),"Original Quicksilver cancels IN_FIRE only");
        wearer.setSprinting(false);ArmorMovement.tick(wearer);
        helper.assertTrue(water.getCollisionShape(helper.getLevel(),pos,context).isEmpty(),"Stopping clears owned liquid collision");
        close(helper,wearer.maxUpStep(),previousStep,"Stopping restores previous step height");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void etheriumPausedClockAndOwnedPhysicsCleanup(GameTestHelper helper) {
        Player wearer=player(helper.getLevel());BlockPos pos=helper.absolutePos(new BlockPos(12,3,12));wearer.setPos(pos.getCenter().add(0.2,0,0));
        helper.getLevel().setBlockAndUpdate(pos.east(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        wearer.setItemSlot(EquipmentSlot.HEAD,Content.stack("etherium_helmet"));wearer.setShiftKeyDown(true);ArmorPhasing.tick(wearer);
        helper.assertTrue(wearer.noPhysics,"Sneaking beside a wall enables phasing");wearer.setShiftKeyDown(false);ArmorPhasing.tick(wearer);
        helper.assertTrue(!wearer.noPhysics,"Releasing sneak restores collision");
        try(State state=VersionBridge.entity(wearer)) { helper.assertTrue(state.integer("etherium_step")==1,"Paused clock retains consumed step"); }
        wearer.setShiftKeyDown(true);ArmorPhasing.tick(wearer);
        for(int i=0;i<30;i++) { try(State state=VersionBridge.entity(wearer)) { state.longValue("etherium_at",helper.getLevel().getGameTime()-10); } ArmorPhasing.tick(wearer); }
        helper.assertTrue(!wearer.noPhysics && VersionBridge.cooldown(wearer,wearer.getItemBySlot(EquipmentSlot.HEAD))>0,"Thirty steps restore physics and give400-tick cooldown");
        Player other=player(helper.getLevel());other.setPos(wearer.position());other.setItemSlot(EquipmentSlot.HEAD,Content.stack("etherium_helmet"));other.noPhysics=true;other.setShiftKeyDown(true);ArmorPhasing.tick(other);ArmorPhasing.cleanup(other);
        helper.assertTrue(other.noPhysics,"Cleanup preserves pre-existing noPhysics ownership");helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=140)
    public static void sanguiniteOriginalMobRevivesOnlyOnce(GameTestHelper helper) {
        var killer=player(helper.getLevel());killer.setItemSlot(EquipmentSlot.HEAD,Content.stack("sanguinite_helmet"));
        var cow=livingVictim(helper.getLevel(),helper);cow.setCustomName(net.minecraft.network.chat.Component.literal("Original identity"));var identity=cow.getUUID();
        cow.hurt(cow.damageSources().playerAttack(killer),1000);
        helper.runAfterDelay(58,()->{
            helper.assertTrue(cow.isAlive() && helper.getLevel().getEntity(identity)==cow,"Original mob revives without a replacement entity");
            close(helper,cow.getHealth(),40,"Revival gives40% original maximum health");
            helper.assertTrue(cow.getCustomName().getString().equals("Original identity"),"Original entity data preserved");
            try(State state=VersionBridge.entity(cow)) { helper.assertTrue(state.integer("corpse_state")==2,"Corpse is marked spent"); }
            ((it.hurts.metallurgy_reforged.mixin.LivingAccess)cow).metallurgy$loot(cow.damageSources().playerAttack(killer),true);
            cow.hurt(cow.damageSources().playerAttack(killer),1000);
        });
        helper.runAfterDelay(95,()->{helper.assertTrue(cow.isRemoved(),"Second death removes spent corpse instead of reviving again");helper.succeed();});
    }

    @GameTest(template="empty",timeoutTicks=150)
    public static void damascusKnightIdentityDamageAndOwnerCleanup(GameTestHelper helper) {
        var owner=chamberPlayer(helper);var attacker=livingVictim(helper.getLevel(),helper);owner.setPos(attacker.position());owner.setNoGravity(true);
        owner.setItemSlot(EquipmentSlot.HEAD,Content.stack("damascus_steel_helmet"));owner.setItemSlot(EquipmentSlot.CHEST,Content.stack("damascus_steel_chestplate"));
        owner.setItemSlot(EquipmentSlot.LEGS,Content.stack("damascus_steel_leggings"));owner.setItemSlot(EquipmentSlot.FEET,Content.stack("damascus_steel_boots"));
        CompanionArmor.damaged(owner,owner.damageSources().mobAttack(attacker));
        String id;try(State state=VersionBridge.entity(owner)) { id=state.string("pier_uuid"); }
        var knight=(it.hurts.metallurgy_reforged.entity.PierKnight)helper.getLevel().getEntity(UUID.fromString(id));
        helper.assertTrue(knight!=null && knight.owner()==owner && knight.thickness()==4 && knight.getMaxHealth()==40,"Native summoned knight owns level-derived stats and identity");
        CompanionArmor.damaged(owner,owner.damageSources().mobAttack(attacker));
        try(State state=VersionBridge.entity(owner)) { helper.assertTrue(state.string("pier_uuid").equals(id),"Repeated damage retains single active knight"); }
        float before=attacker.getHealth();VersionBridge.knightStrike(knight,attacker);close(helper,before-attacker.getHealth(),8,"Knight deals base8, without its held sword attribute bonus");
        helper.runAfterDelay(45,()->{
            helper.assertTrue(knight.vanish()==0 && knight.isAlive(),"Forty-tick arrival phase completes");
            for(var slot:EquipmentEffects.ARMOR_SLOTS) owner.setItemSlot(slot,ItemStack.EMPTY);knight.aiStep();
            helper.assertTrue(!knight.isAlive() && VersionBridge.cooldown(owner,Content.stack("damascus_steel_helmet"))>0,"Removing armor ends companion and gives200-tick cooldown");
            try(State state=VersionBridge.entity(owner)) { helper.assertTrue(state.string("pier_uuid").isEmpty(),"Owner identity flag clears"); }
        });
        helper.runAfterDelay(95,()->{
            helper.assertTrue(knight.isRemoved(),"Forty-tick departure removes knight");helper.getLevel().removePlayerImmediately(owner,Entity.RemovalReason.DISCARDED);helper.succeed();
        });
    }

    private static net.minecraft.world.entity.animal.Cow livingVictim(ServerLevel level,GameTestHelper helper) {
        var cow=new net.minecraft.world.entity.animal.Cow(EntityType.COW,level);
        cow.setPos(helper.absolutePos(new BlockPos(6,2,6)).getCenter());
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.getAttribute(Attributes.ARMOR).setBaseValue(0);
        cow.setHealth(50);cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);return cow;
    }
    private static Zombie victim(ServerLevel level,GameTestHelper helper) {
        Zombie zombie=new Zombie(level);
        zombie.setPos(helper.absolutePos(new BlockPos(6,2,6)).getCenter());
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);
        zombie.setHealth(50);zombie.setNoAi(true);zombie.setNoGravity(true);
        zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN)); // Zero armor; prevents random daylight damage in delayed assertions.
        level.addFreshEntity(zombie);
        return zombie;
    }
    private static Player player(ServerLevel level) { return FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"MetalTest")); }
    private static net.minecraft.server.level.ServerPlayer chamberPlayer(GameTestHelper helper) {
        var result=(net.minecraft.server.level.ServerPlayer)player(helper.getLevel());helper.getLevel().addNewPlayer(result);return result;
    }
    private static void close(GameTestHelper helper,float actual,float expected,String label) { helper.assertTrue(Math.abs(actual-expected)<0.001F,label+" expected"+expected+" actual"+actual); }
}
