package it.hurts.metallurgy_reforged.content;

import it.hurts.metallurgy_reforged.inventory.*;
import it.hurts.metallurgy_reforged.entity.PierKnight;
import net.minecraft.world.entity.*;
import it.hurts.metallurgy_reforged.machine.*;
import it.hurts.metallurgy_reforged.recipe.ParagonRecipe;
import it.hurts.metallurgy_reforged.world.*;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.item.crafting.*;
import net.minecraft.sounds.SoundEvent;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.*;
import java.util.function.Supplier;

public final class Content {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"metallurgy");
    public static final Supplier<EntityType<PierKnight>> PIER_KNIGHT=ENTITIES.register("pierknight",()->EntityType.Builder.of(PierKnight::new,MobCategory.CREATURE).sized(0.6F,1.8F).clientTrackingRange(10).updateInterval(3).build(ResourceKey.create(Registries.ENTITY_TYPE,id("pierknight"))));
    public static final Supplier<EntityType<it.hurts.metallurgy_reforged.entity.VulcanitePrimedTnt>> VULCANITE_TNT_ENTITY=ENTITIES.register("metallurgy_primed_tnt",()->EntityType.Builder.<it.hurts.metallurgy_reforged.entity.VulcanitePrimedTnt>of(it.hurts.metallurgy_reforged.entity.VulcanitePrimedTnt::new,MobCategory.MISC).sized(0.98F,0.98F).clientTrackingRange(4).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE,id("metallurgy_primed_tnt"))));
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"metallurgy");
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"metallurgy");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"metallurgy");
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"metallurgy");
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,"metallurgy");
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,"metallurgy");
    public static final Supplier<Feature<OreBatchConfiguration>> ORE_BATCH=FEATURES.register("ore_batch",OreBatchFeature::new);
    public static final Supplier<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> TAR_LAKE=FEATURES.register("tar_lake",TarLakeFeature::new);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,"metallurgy");
    public static final Supplier<RecipeSerializer<ParagonRecipe>> PARAGON_RECIPE=RECIPE_SERIALIZERS.register("tartarite_paragon_shaped",()->statelessRecipe(new ParagonRecipe()));
    public static final Supplier<RecipeSerializer<it.hurts.metallurgy_reforged.recipe.DetectorRecipe>> DETECTOR_RECIPE=RECIPE_SERIALIZERS.register("ore_detector_shapeless",()->statelessRecipe(new it.hurts.metallurgy_reforged.recipe.DetectorRecipe()));
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,"metallurgy");
    public static final Map<String,Supplier<SoundEvent>> SOUND_MAP=new LinkedHashMap<>();
    public static final Supplier<BlockEntityType<MachineEntity>> MACHINE_ENTITY;
    public static final Supplier<BlockEntityType<ChamberEntity>> CHAMBER_ENTITY;
    public static final Supplier<MenuType<MachineMenu>> MACHINE_MENU;
    public static final Map<String,Supplier<Item>> ITEM_MAP=new LinkedHashMap<>();
    public static final Map<String,Supplier<Block>> BLOCK_MAP=new LinkedHashMap<>();
    public static final Map<String,Integer> CHEST_SIZES=new LinkedHashMap<>();
    public static final Supplier<BlockEntityType<MetalChestEntity>> CHEST_ENTITY;
    public static final Supplier<MenuType<MetalChestMenu>> CHEST_MENU;
    static {
        for(String sound:List.of("alloyer_ambience","alloyer_impact","alloyer_windup","crusher_ambience","crusher_impact","crusher_windup","sublimation_chamber_ambience","sublimation_chamber_windup","metal_detector_beep_1","metal_detector_beep_2","metal_detector_beep_3","metal_detector_ping")) SOUND_MAP.put(sound,SOUNDS.register(sound,()->SoundEvent.createVariableRangeEvent(id(sound))));
        for(String id:List.of("gold_dust","iron_dust","bitumen","tar","potash","sulfur_dust","thermite_dust","ignatius_fuel","phosphorus")) item(id,()->new Item(properties(id)));
        item("potash_fertilizer",()->new it.hurts.metallurgy_reforged.gadget.PotashFertilizerItem(properties("potash_fertilizer")));
        item("etherium_goggles",()->new it.hurts.metallurgy_reforged.gadget.GogglesItem(properties("etherium_goggles").equippable(EquipmentSlot.HEAD)));
        block("phosphorus_lamp",()->new it.hurts.metallurgy_reforged.gadget.PhosphorusLampBlock(blockProperties("phosphorus_lamp").strength(4).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL).lightLevel(state->it.hurts.metallurgy_reforged.config.GadgetsConfig.PhosphorusLamp.lanternLightLevel)));
        for(String id:List.of("road_block","striped_road_block")) block(id,()->new it.hurts.metallurgy_reforged.gadget.RoadBlock(blockProperties(id).strength(3).requiresCorrectToolForDrops().mapColor(MapColor.STONE)));
        block("vulcanite_tnt",()->new it.hurts.metallurgy_reforged.gadget.VulcaniteTntBlock(blockProperties("vulcanite_tnt").strength(0).sound(SoundType.GRASS).ignitedByLava()));
        item("flint_and_ignatius",()->new it.hurts.metallurgy_reforged.gadget.LighterItem(false,properties("flint_and_ignatius").durability(150)));
        item("flint_and_vulcanite",()->new it.hurts.metallurgy_reforged.gadget.LighterItem(true,properties("flint_and_vulcanite").durability(500)));
        for(String id:List.of("electrum_magnet","ore_detector","brass_knuckles","rubracium_gauntlet"))
            item(id,()->new it.hurts.metallurgy_reforged.gadget.GadgetItem(id,id.equals("brass_knuckles")?properties(id).durability(250).enchantable(1):id.equals("rubracium_gauntlet")?properties(id).durability(it.hurts.metallurgy_reforged.config.GadgetsConfig.Gauntlet.gauntletMaxDamage).enchantable(1):id.equals("ore_detector")?properties(id).stacksTo(1):properties(id)));
        for(String id:List.of("lemurite_shield","zinc_buckler","vulcanite_buckler","ceruclase_shield","orichalcum_buckler"))
            item(id,()->new it.hurts.metallurgy_reforged.gadget.MetallurgyShieldItem(id,properties(id).durability(switch(id) {case "lemurite_shield"->256;case "zinc_buckler"->396;case "vulcanite_buckler"->662;case "ceruclase_shield"->500;default->413;}).enchantable(switch(id) {case "lemurite_shield"->17;case "ceruclase_shield"->18;case "zinc_buckler"->8;case "vulcanite_buckler"->20;default->22;})));
        BLOCK_MAP.put("ice_shield",BLOCKS.register("ice_shield",()->new it.hurts.metallurgy_reforged.gadget.IceShieldBlock(blockProperties("ice_shield").strength(0.5F,1200).friction(0.98F).noOcclusion().noLootTable().sound(SoundType.GLASS))));
        for(String id:List.of("crusher","alloyer")) block(id,()->new MachineBlock(id.equals("alloyer"),blockProperties(id).strength(6,4.8F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
        MACHINE_ENTITY=BLOCK_ENTITIES.register("processing_machine",()->new BlockEntityType<>(MachineEntity::new,BLOCK_MAP.get("crusher").get(),BLOCK_MAP.get("alloyer").get()));
        block("sublimation_chamber",()->new ChamberBlock(blockProperties("sublimation_chamber").strength(6,4.8F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL).lightLevel(state->state.getValue(ChamberBlock.LIT)?8:0)));
        CHAMBER_ENTITY=BLOCK_ENTITIES.register("sublimation_chamber",()->new BlockEntityType<>(ChamberEntity::new,BLOCK_MAP.get("sublimation_chamber").get()));
        MACHINE_MENU=MENUS.register("processing_machine",()->IMenuTypeExtension.create(MachineMenu::fromNetwork));
        for(Materials.Definition material:Materials.ALL) {
            String name=material.name();
            for(String form:List.of("ingot","dust","nugget")) item(name+"_"+form,()->new Item(properties(name+"_"+form)));
            for(String form:List.of("block","engraved_block","large_bricks","bricks","crystals","hazard_block","reinforced_glass")) {
                String id=name+"_"+form;
                block(id,()->form.equals("reinforced_glass")
                    ?new TransparentBlock(blockProperties(id).strength(2,material.blastResistance()*0.6F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL))
                    :new Block(blockProperties(id).mapColor(MapColor.METAL).strength(form.equals("block")?5:2,material.blastResistance()*0.6F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
            }
            if(material.oreLevel()>=0) block(name+"_ore",()->new Block(blockProperties(name+"_ore").mapColor(MapColor.STONE).strength(material.hardness(),material.blastResistance()*0.6F).requiresCorrectToolForDrops().lightLevel(state->it.hurts.metallurgy_reforged.config.GeneralConfig.enableOreLight && material.oreLevel()>=5?5:0)) {
                @Override public void animateTick(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level world,net.minecraft.core.BlockPos pos,net.minecraft.util.RandomSource random) {it.hurts.metallurgy_reforged.client.OreParticles.block(world,pos,random,material);}
                @Override public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion) {return java.util.concurrent.ThreadLocalRandom.current().nextInt(4)>0;}
                @Override public void playerDestroy(net.minecraft.world.level.Level world,net.minecraft.world.entity.player.Player player,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.block.entity.BlockEntity entity,ItemStack tool) {super.playerDestroy(world,player,pos,state,entity,tool);if(player instanceof net.minecraft.server.level.ServerPlayer server) it.hurts.metallurgy_reforged.advancement.Advancements.fire(server,"harvest_level",material.oreLevel(),"");}
            });
            TagKey<Item> repair=TagKey.create(Registries.ITEM,id("ingots/"+name));
            if(material.hasTools()) {
                ToolMaterial tool=new ToolMaterial(TagKey.create(Registries.BLOCK,id("incorrect_for_level_"+(int)material.tool("harvest_level",2))),
                    (int)material.tool("durability",250),material.tool("efficiency",6),material.tool("damage",2),(int)material.tool("enchantability",10),repair);
                item(name+"_sword",()->new Item(properties(name+"_sword").sword(tool,3,-2.4F)));
                item(name+"_axe",()->new AxeItem(tool,material.tool("damage",0)*0.25F+3,-2.5F-material.tool("damage",0)/10,properties(name+"_axe")));
                item(name+"_pickaxe",()->new Item(properties(name+"_pickaxe").pickaxe(tool,1,-2.8F)));
                item(name+"_shovel",()->new ShovelItem(tool,1.5F,-3,properties(name+"_shovel")));
                item(name+"_hoe",()->new HoeItem(tool,-(int)material.tool("damage",0),material.tool("damage",0)-3,properties(name+"_hoe")));
            }
            if(material.hasArmor()) {
                ArmorMaterial armor=new ArmorMaterial((int)material.armor("durability",15),
                    Map.of(ArmorType.BOOTS,material.defense(0),ArmorType.LEGGINGS,material.defense(1),ArmorType.CHESTPLATE,material.defense(2),ArmorType.HELMET,material.defense(3)),
                    (int)material.armor("enchantability",10),SoundEvents.ARMOR_EQUIP_IRON,material.armor("toughness",0),0,
                    repair,ResourceKey.create(EquipmentAssets.ROOT_ID,id(name)));
                for(ArmorType type:List.of(ArmorType.BOOTS,ArmorType.LEGGINGS,ArmorType.CHESTPLATE,ArmorType.HELMET)) {
                    item(name+"_"+type.getName(),()->new Item(properties(name+"_"+type.getName()).humanoidArmor(armor,type)));
                }
            }
        }
        for(String id:List.of("sulfur_ore","phosphorite_ore","tar_ore","potash_ore","bitumen_block","charcoal_block","sulfur_block"))
            block(id,()->{var properties=blockProperties(id).mapColor(MapColor.STONE).strength(3,6).requiresCorrectToolForDrops();return id.endsWith("_ore")?new BasicOreBlock(properties):new Block(properties);});
        for(String metal:List.of("iron","gold")) for(String form:List.of("engraved_block","large_bricks","bricks","crystals","hazard_block","reinforced_glass")) {
            String id=metal+"_"+form;block(id,()->form.equals("reinforced_glass")?new TransparentBlock(blockProperties(id).strength(3,3).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.METAL)):new Block(blockProperties(id).strength(3,3).requiresCorrectToolForDrops().sound(SoundType.METAL)));
        }
        block("bimetal_structure_block",()->new Block(blockProperties("bimetal_structure_block").mapColor(MapColor.METAL).strength(3,3).noOcclusion().sound(SoundType.METAL)));
        CHEST_SIZES.put("brass",54);CHEST_SIZES.put("silver",72);CHEST_SIZES.put("electrum",81);CHEST_SIZES.put("rubracium",90);CHEST_SIZES.put("platinum",108);
        CHEST_SIZES.forEach((metal,slots)->block(metal+"_chest",()->new MetalChestBlock(slots,blockProperties(metal+"_chest").mapColor(MapColor.METAL).strength(6,10.8F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion())));
        CHEST_ENTITY=BLOCK_ENTITIES.register("metal_chest",()->new BlockEntityType<>(MetalChestEntity::new,CHEST_SIZES.keySet().stream().map(m->BLOCK_MAP.get(m+"_chest").get()).toArray(Block[]::new)));
        CHEST_MENU=MENUS.register("metal_chest",()->IMenuTypeExtension.create(MetalChestMenu::fromNetwork));
        TABS.register("metallurgy",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.metallurgy")).icon(()->stack("tartarite_ingot"))
            .displayItems((parameters,output)->ITEM_MAP.forEach((id,item)->{if(it.hurts.metallurgy_reforged.config.ContentAvailability.enabled("metallurgy:"+id)) output.accept(item.get());})).build());
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("metallurgy",path); }
    public static Item.Properties properties(String path) { return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id(path))); }
    public static BlockBehaviour.Properties blockProperties(String path) { return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id(path))); }
    public static Supplier<Item> item(String path,Supplier<Item> factory) { Supplier<Item> result=ITEMS.register(path,factory);ITEM_MAP.put(path,result);return result; }
    public static Supplier<Block> block(String path,Supplier<Block> factory) {
        Supplier<Block> result=BLOCKS.register(path,factory);BLOCK_MAP.put(path,result);
        item(path,()->path.endsWith("_ore")?new LockedOreItem(result.get(),properties(path).useBlockDescriptionPrefix()):new BlockItem(result.get(),properties(path).useBlockDescriptionPrefix()));return result;
    }
    public static ItemStack stack(String path) { return new ItemStack(ITEM_MAP.get(path).get()); }
    /** Unit stream codecs compare values: JSON and network must share this exact stateless instance. */
    private static <T extends Recipe<?>> RecipeSerializer<T> statelessRecipe(T recipe) {
        return new RecipeSerializer<>(com.mojang.serialization.MapCodec.unit(recipe),net.minecraft.network.codec.StreamCodec.unit(recipe));
    }
    public static void register(IEventBus bus) {
        it.hurts.metallurgy_reforged.fluid.FluidContent.register(bus); ENTITIES.register(bus);bus.addListener(Content::entityAttributes);FEATURES.register(bus);RECIPE_SERIALIZERS.register(bus);SOUNDS.register(bus);BLOCKS.register(bus);ITEMS.register(bus);BLOCK_ENTITIES.register(bus);MENUS.register(bus);TABS.register(bus); }
    private static void entityAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) { event.put(PIER_KNIGHT.get(),PierKnight.attributes().build()); }
    private Content() {}
}
