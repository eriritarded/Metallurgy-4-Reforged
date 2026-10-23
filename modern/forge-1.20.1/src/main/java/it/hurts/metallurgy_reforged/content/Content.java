package it.hurts.metallurgy_reforged.content;

import it.hurts.metallurgy_reforged.material.Materials;
import it.hurts.metallurgy_reforged.inventory.*;
import it.hurts.metallurgy_reforged.machine.*;
import it.hurts.metallurgy_reforged.recipe.ParagonRecipe;
import it.hurts.metallurgy_reforged.world.*;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.item.crafting.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.*;
import it.hurts.metallurgy_reforged.entity.PierKnight;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import java.util.*;
import java.util.function.Supplier;

public final class Content {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"metallurgy");
    public static final Supplier<EntityType<PierKnight>> PIER_KNIGHT=ENTITIES.register("pierknight",()->EntityType.Builder.of(PierKnight::new,MobCategory.CREATURE).sized(0.6F,1.8F).clientTrackingRange(10).updateInterval(3).build("metallurgy:pierknight"));
    public static final Supplier<EntityType<it.hurts.metallurgy_reforged.entity.VulcanitePrimedTnt>> VULCANITE_TNT_ENTITY=ENTITIES.register("metallurgy_primed_tnt",()->EntityType.Builder.<it.hurts.metallurgy_reforged.entity.VulcanitePrimedTnt>of(it.hurts.metallurgy_reforged.entity.VulcanitePrimedTnt::new,MobCategory.MISC).sized(0.98F,0.98F).clientTrackingRange(4).updateInterval(1).build("metallurgy:metallurgy_primed_tnt"));
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "metallurgy");
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "metallurgy");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "metallurgy");
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "metallurgy");
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "metallurgy");
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,"metallurgy");
    public static final Supplier<Feature<OreBatchConfiguration>> ORE_BATCH=FEATURES.register("ore_batch",OreBatchFeature::new);
    public static final Supplier<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> TAR_LAKE=FEATURES.register("tar_lake",TarLakeFeature::new);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,"metallurgy");
    public static final Supplier<RecipeSerializer<ParagonRecipe>> PARAGON_RECIPE=RECIPE_SERIALIZERS.register("tartarite_paragon_shaped",()->new SimpleCraftingRecipeSerializer<>(ParagonRecipe::new));
    public static final Supplier<RecipeSerializer<it.hurts.metallurgy_reforged.recipe.DetectorRecipe>> DETECTOR_RECIPE=RECIPE_SERIALIZERS.register("ore_detector_shapeless",()->new SimpleCraftingRecipeSerializer<>(it.hurts.metallurgy_reforged.recipe.DetectorRecipe::new));
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,"metallurgy");
    public static final Map<String,Supplier<SoundEvent>> SOUND_MAP=new LinkedHashMap<>();
    public static final Supplier<BlockEntityType<MachineEntity>> MACHINE_ENTITY;
    public static final Supplier<BlockEntityType<ChamberEntity>> CHAMBER_ENTITY;
    public static final Supplier<MenuType<MachineMenu>> MACHINE_MENU;
    public static final Map<String, Supplier<Item>> ITEM_MAP = new LinkedHashMap<>();
    public static final Map<String, Supplier<Block>> BLOCK_MAP = new LinkedHashMap<>();
    public static final Map<String, Integer> CHEST_SIZES = new LinkedHashMap<>();
    public static final Supplier<BlockEntityType<MetalChestEntity>> CHEST_ENTITY;
    public static final Supplier<MenuType<MetalChestMenu>> CHEST_MENU;

    static {
        for(String sound:List.of("alloyer_ambience","alloyer_impact","alloyer_windup","crusher_ambience","crusher_impact","crusher_windup","sublimation_chamber_ambience","sublimation_chamber_windup","metal_detector_beep_1","metal_detector_beep_2","metal_detector_beep_3","metal_detector_ping")) SOUND_MAP.put(sound,SOUNDS.register(sound,()->SoundEvent.createVariableRangeEvent(new ResourceLocation("metallurgy",sound))));
        for(String id:List.of("gold_dust","iron_dust","bitumen","tar","potash","sulfur_dust","thermite_dust","ignatius_fuel","phosphorus")) item(id,()->new Item(new Item.Properties()));
        item("potash_fertilizer",()->new it.hurts.metallurgy_reforged.gadget.PotashFertilizerItem(new Item.Properties()));
        item("etherium_goggles",()->new it.hurts.metallurgy_reforged.gadget.GogglesItem(new Item.Properties()));
        block("phosphorus_lamp",()->new it.hurts.metallurgy_reforged.gadget.PhosphorusLampBlock(BlockBehaviour.Properties.of().strength(4).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL).lightLevel(state->it.hurts.metallurgy_reforged.config.GadgetsConfig.PhosphorusLamp.lanternLightLevel)));
        for(String id:List.of("road_block","striped_road_block")) block(id,()->new it.hurts.metallurgy_reforged.gadget.RoadBlock(BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops().mapColor(MapColor.STONE)));
        block("vulcanite_tnt",()->new it.hurts.metallurgy_reforged.gadget.VulcaniteTntBlock(BlockBehaviour.Properties.of().strength(0).sound(SoundType.GRASS).ignitedByLava()));
        item("flint_and_ignatius",()->new it.hurts.metallurgy_reforged.gadget.LighterItem(false,new Item.Properties().durability(150)));
        item("flint_and_vulcanite",()->new it.hurts.metallurgy_reforged.gadget.LighterItem(true,new Item.Properties().durability(500)));
        for(String id:List.of("electrum_magnet","ore_detector","brass_knuckles","rubracium_gauntlet"))
            item(id,()->new it.hurts.metallurgy_reforged.gadget.GadgetItem(id,id.equals("brass_knuckles")?new Item.Properties().durability(250):id.equals("rubracium_gauntlet")?new Item.Properties().durability(it.hurts.metallurgy_reforged.config.GadgetsConfig.Gauntlet.gauntletMaxDamage):id.equals("ore_detector")?new Item.Properties().stacksTo(1):new Item.Properties()));
        for(String id:List.of("lemurite_shield","zinc_buckler","vulcanite_buckler","ceruclase_shield","orichalcum_buckler"))
            item(id,()->new it.hurts.metallurgy_reforged.gadget.MetallurgyShieldItem(id,new Item.Properties().durability(switch(id) {case "lemurite_shield"->256;case "zinc_buckler"->396;case "vulcanite_buckler"->662;case "ceruclase_shield"->500;default->413;})));
        BLOCK_MAP.put("ice_shield",BLOCKS.register("ice_shield",()->new it.hurts.metallurgy_reforged.gadget.IceShieldBlock(BlockBehaviour.Properties.of().strength(0.5F,1200).friction(0.98F).noOcclusion().noLootTable().sound(SoundType.GLASS))));
        for(String id:List.of("crusher","alloyer")) block(id,()->new MachineBlock(id.equals("alloyer"),BlockBehaviour.Properties.of().strength(6,4.8F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
        MACHINE_ENTITY=BLOCK_ENTITIES.register("processing_machine",()->BlockEntityType.Builder.of(MachineEntity::new,BLOCK_MAP.get("crusher").get(),BLOCK_MAP.get("alloyer").get()).build(null));
        block("sublimation_chamber",()->new ChamberBlock(BlockBehaviour.Properties.of().strength(6,4.8F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL).lightLevel(state->state.getValue(ChamberBlock.LIT)?8:0)));
        CHAMBER_ENTITY=BLOCK_ENTITIES.register("sublimation_chamber",()->BlockEntityType.Builder.of(ChamberEntity::new,BLOCK_MAP.get("sublimation_chamber").get()).build(null));
        MACHINE_MENU=MENUS.register("processing_machine",()->IForgeMenuType.create(MachineMenu::fromNetwork));
        for (Materials.Definition material : Materials.ALL) {
            String name = material.name();
            for (String form : List.of("ingot", "dust", "nugget")) item(name + "_" + form, () -> new Item(new Item.Properties()));
            for (String form : List.of("block", "engraved_block", "large_bricks", "bricks", "crystals", "hazard_block", "reinforced_glass")) {
                block(name + "_" + form, () -> form.equals("reinforced_glass")
                    ? new GlassBlock(BlockBehaviour.Properties.of().strength(2, material.blastResistance()*0.6F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL))
                    : new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(form.equals("block") ? 5 : 2, material.blastResistance()*0.6F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
            }
            if (material.oreLevel() >= 0) block(name + "_ore", () -> new MetalOre(material));
            if (material.hasTools()) {
                Tier tier = new MetalTier(material);
                item(name + "_sword", () -> new SwordItem(tier, 3, -2.4F, new Item.Properties()) { @Override public int getMaxDamage(ItemStack stack) { return it.hurts.metallurgy_reforged.effect.Paragon.maximum(stack,super.getMaxDamage(stack)); } });
                item(name + "_axe", () -> new AxeItem(tier, material.tool("damage",0)*0.25F+3, -2.5F-material.tool("damage",0)/10, new Item.Properties()) { @Override public int getMaxDamage(ItemStack stack) { return it.hurts.metallurgy_reforged.effect.Paragon.maximum(stack,super.getMaxDamage(stack)); } });
                item(name + "_pickaxe", () -> new PickaxeItem(tier, 1, -2.8F, new Item.Properties()) { @Override public int getMaxDamage(ItemStack stack) { return it.hurts.metallurgy_reforged.effect.Paragon.maximum(stack,super.getMaxDamage(stack)); } });
                item(name + "_shovel", () -> new ShovelItem(tier, 1.5F, -3, new Item.Properties()) { @Override public int getMaxDamage(ItemStack stack) { return it.hurts.metallurgy_reforged.effect.Paragon.maximum(stack,super.getMaxDamage(stack)); } });
                item(name + "_hoe", () -> new HoeItem(tier, -(int)material.tool("damage", 0), material.tool("damage",0)-3, new Item.Properties()) { @Override public int getMaxDamage(ItemStack stack) { return it.hurts.metallurgy_reforged.effect.Paragon.maximum(stack,super.getMaxDamage(stack)); } });
            }
            if (material.hasArmor()) {
                ArmorMaterial armor = new MetalArmor(material);
                for (ArmorItem.Type type : ArmorItem.Type.values()) {
                    item(name + "_" + type.getName(), () -> new ArmorItem(armor, type, new Item.Properties()) { @Override public int getMaxDamage(ItemStack stack) { return it.hurts.metallurgy_reforged.effect.Paragon.maximum(stack,super.getMaxDamage(stack)); } });
                }
            }
        }
        for(String id:List.of("sulfur_ore","phosphorite_ore","tar_ore","potash_ore","bitumen_block","charcoal_block","sulfur_block"))
            block(id,()->{var properties=BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3,6).requiresCorrectToolForDrops();return id.endsWith("_ore")?new BasicOreBlock(properties):new Block(properties);});
        for(String metal:List.of("iron","gold")) for(String form:List.of("engraved_block","large_bricks","bricks","crystals","hazard_block","reinforced_glass")) {
            String id=metal+"_"+form;block(id,()->form.equals("reinforced_glass")?new GlassBlock(BlockBehaviour.Properties.of().strength(3,3).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.METAL)):new Block(BlockBehaviour.Properties.of().strength(3,3).requiresCorrectToolForDrops().sound(SoundType.METAL)));
        }
        block("bimetal_structure_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3,3).noOcclusion().sound(SoundType.METAL)));
        CHEST_SIZES.put("brass", 54); CHEST_SIZES.put("silver", 72); CHEST_SIZES.put("electrum", 81);
        CHEST_SIZES.put("rubracium", 90); CHEST_SIZES.put("platinum", 108);
        CHEST_SIZES.forEach((metal, slots) -> block(metal + "_chest", () -> new MetalChestBlock(slots,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6,10.8F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion())));
        CHEST_ENTITY = BLOCK_ENTITIES.register("metal_chest", () -> BlockEntityType.Builder.of(MetalChestEntity::new,
            CHEST_SIZES.keySet().stream().map(m -> BLOCK_MAP.get(m + "_chest").get()).toArray(Block[]::new)).build(null));
        CHEST_MENU = MENUS.register("metal_chest", () -> IForgeMenuType.create(MetalChestMenu::fromNetwork));
        TABS.register("metallurgy", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.metallurgy"))
            .icon(() -> stack("tartarite_ingot")).displayItems((parameters, output) -> ITEM_MAP.forEach((id,item) -> {if(it.hurts.metallurgy_reforged.config.ContentAvailability.enabled("metallurgy:"+id)) output.accept(item.get());})).build());
    }

    public static void register(IEventBus bus) {
        it.hurts.metallurgy_reforged.fluid.FluidContent.register(bus);
        ENTITIES.register(bus);bus.addListener(Content::entityAttributes);
        FEATURES.register(bus);RECIPE_SERIALIZERS.register(bus);SOUNDS.register(bus); ITEMS.register(bus); BLOCKS.register(bus); BLOCK_ENTITIES.register(bus); MENUS.register(bus); TABS.register(bus);
    }
    private static void entityAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) { event.put(PIER_KNIGHT.get(),PierKnight.attributes().build()); }

    public static Supplier<Item> item(String id, Supplier<Item> factory) {
        Supplier<Item> result = ITEMS.register(id, factory); ITEM_MAP.put(id, result); return result;
    }

    public static Supplier<Block> block(String id, Supplier<Block> factory) {
        Supplier<Block> result = BLOCKS.register(id, factory); BLOCK_MAP.put(id, result);
        item(id, () -> id.endsWith("_chest") ? new MetalChestItem(result.get(), new Item.Properties()) : id.endsWith("_ore") ? new LockedOreItem(result.get(), new Item.Properties()) : new BlockItem(result.get(), new Item.Properties())); return result;
    }

    public static ItemStack stack(String id) { return new ItemStack(ITEM_MAP.get(id).get()); }

    private record MetalTier(Materials.Definition data) implements Tier {
        public int getUses() { return (int)data.tool("durability", 250); }
        public float getSpeed() { return data.tool("efficiency", 6); }
        public float getAttackDamageBonus() { return data.tool("damage", 2); }
        public int getLevel() { return (int)data.tool("harvest_level", 2); }
        public int getEnchantmentValue() { return (int)data.tool("enchantability", 10); }
        public Ingredient getRepairIngredient() { return Ingredient.of(ITEM_MAP.get(data.name() + "_ingot").get()); }
    }

    private record MetalArmor(Materials.Definition data) implements ArmorMaterial {
        private int index(ArmorItem.Type type) { return switch(type) { case BOOTS -> 0; case LEGGINGS -> 1; case CHESTPLATE -> 2; case HELMET -> 3; }; }
        public int getDurabilityForType(ArmorItem.Type type) { return new int[]{13,15,16,11}[index(type)] * (int)data.armor("durability", 15); }
        public int getDefenseForType(ArmorItem.Type type) { return data.defense(index(type)); }
        public int getEnchantmentValue() { return (int)data.armor("enchantability", 10); }
        public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
        public Ingredient getRepairIngredient() { return Ingredient.of(ITEM_MAP.get(data.name() + "_ingot").get()); }
        public String getName() { return "metallurgy:" + data.name(); }
        public float getToughness() { return data.armor("toughness", 0); }
        public float getKnockbackResistance() { return 0; } // Signed source value is applied by ItemAttributes.
    }

    private static final class MetalOre extends Block {
        private final int requiredLevel;
        private final Materials.Definition material;
        MetalOre(Materials.Definition data) {
            super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(data.hardness(), data.blastResistance()*0.6F).requiresCorrectToolForDrops().lightLevel(state->it.hurts.metallurgy_reforged.config.GeneralConfig.enableOreLight && data.oreLevel()>=5?5:0));
            requiredLevel = data.oreLevel();material=data;
        }
        @Override public void animateTick(BlockState state,net.minecraft.world.level.Level world,BlockPos pos,net.minecraft.util.RandomSource random) {it.hurts.metallurgy_reforged.client.OreParticles.block(world,pos,random,material);}
        @Override public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion) {return java.util.concurrent.ThreadLocalRandom.current().nextInt(4)>0;}
        @Override public void playerDestroy(net.minecraft.world.level.Level world,Player player,BlockPos pos,BlockState state,net.minecraft.world.level.block.entity.BlockEntity entity,ItemStack tool) {super.playerDestroy(world,player,pos,state,entity,tool);if(player instanceof net.minecraft.server.level.ServerPlayer server) it.hurts.metallurgy_reforged.advancement.Advancements.fire(server,"harvest_level",requiredLevel,"");}
        @Override public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
            ItemStack held = player.getMainHandItem();
            String namespace = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.getItem()).getNamespace();
            if (namespace.equals("tetra") || namespace.equals("silentgear"))
                return it.hurts.metallurgy_reforged.compat.ModularTiers.isModularPickaxe(held, state);
            return held.getItem() instanceof PickaxeItem pickaxe && pickaxe.getTier().getLevel()+it.hurts.metallurgy_reforged.effect.ToolBridge.boost(held) >= requiredLevel;
        }
    }

    private Content() {}
}
