package it.hurts.metallurgy_reforged.compat.thaumaturge;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.spell.event.SpellCastEvent;
import com.leclowndu93150.thaumaturge.api.wands.WandCap;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.content.wands.ItemWandCap;
import com.leclowndu93150.thaumaturge.content.wands.ItemWandRod;
import com.leclowndu93150.thaumaturge.content.wands.WandRodPrimalOnUpdate;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTAttributes;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import it.hurts.metallurgy_reforged.config.GadgetsConfig;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.gadget.GadgetItem;
import it.hurts.metallurgy_reforged.gadget.MetallurgyShieldItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Optional Thaumaturge content, registered only when Thaumaturge is installed: wand caps and rods, native clusters,
 * the thaumium and void gadgets, and the cap, goggle and armour effects. The items always register with Thaumaturge
 * so saves stay stable; their recipes and research are data, gated by the "Thaumaturge Integration" config.
 * Numbers live in {@link ThaumaturgeTuning}.
 */
public final class ThaumaturgeCompat {
    // Thaumaturge's cap and rod registries are static; it generates the wand assembly for every pairing from them.
    private static final DeferredRegister<WandCap> CAPS = DeferredRegister.create(WandCap.REGISTRY_KEY, "metallurgy");
    private static final DeferredRegister<WandRod> RODS = DeferredRegister.create(WandRod.REGISTRY_KEY, "metallurgy");
    /** Metal name to its cap, so a cast can tell which Metallurgy cap the wand carries. */
    private static final Map<String, Supplier<WandCap>> CAP_BY_METAL = new LinkedHashMap<>();
    public static final List<String> CLUSTERS = List.of("adamantine", "alduorite", "astral_silver", "atlarus", "carmot", "ceruclase",
            "deep_iron", "eximite", "ignatius", "infuscolium", "kalendrite", "lemurite", "lutetium", "manganese", "meutoite", "midasium",
            "mithril", "orichalcum", "osmium", "oureclase", "platinum", "prometheum", "rubracium", "sanguinite", "shadow_iron", "vulcanite",
            "vyroxeres", "zinc");
    private static final List<String> ARMOUR = List.of("helmet", "chestplate", "leggings", "boots");

    public static void register(IEventBus bus) {
        // Tier caps: cost multiplier by tier, between Thaumaturge's gold (1.00) and void (0.80). The last three are
        // crafted inert and charged by infusion, like silver and thaumium.
        cap("bronze", 1.05F, null, 2, false, Rarity.COMMON);
        cap("steel", 1.00F, null, 3, false, Rarity.COMMON);
        cap("platinum", 0.95F, null, 4, false, Rarity.COMMON);
        cap("mithril", 0.92F, null, 5, true, Rarity.UNCOMMON);
        cap("orichalcum", 0.90F, null, 6, true, Rarity.UNCOMMON);
        cap("adamantine", 0.85F, null, 8, true, Rarity.RARE);
        // Themed caps: full price, but 20% off one primal. Caps can only discount vis, which is primal, so each
        // metal's theme maps onto the nearest primal.
        cap("ignatius", 1.00F, TTAspects.IGNIS, 4, false, Rarity.UNCOMMON);
        cap("ceruclase", 1.00F, TTAspects.AQUA, 4, false, Rarity.UNCOMMON);
        cap("shadow_steel", 1.00F, TTAspects.PERDITIO, 4, false, Rarity.UNCOMMON);
        cap("sanguinite", 1.00F, TTAspects.TERRA, 4, false, Rarity.UNCOMMON);
        cap("midasium", 1.00F, TTAspects.ORDO, 4, false, Rarity.UNCOMMON);
        cap("etherium", 1.00F, TTAspects.AER, 4, false, Rarity.UNCOMMON);
        // Wacky caps: full price on paper, changed per cast by spellCast below.
        for (String metal : List.of("quicksilver", "kalendrite", "astral_silver", "tartarite", "krik", "amordrine", "desichalkos", "prometheum"))
            cap(metal, 1.00F, null, 4, false, Rarity.UNCOMMON);

        // Rods: Vulcanite burns like blaze (glows, refills Ignis); Eximite holds silverwood's wand capacity.
        rod("vulcanite", 75, 6, 175, 14, true, new WandRodPrimalOnUpdate(TTAspects.IGNIS), Rarity.UNCOMMON);
        rod("eximite", 100, 9, 200, 18, false, new EximiteCore(), Rarity.RARE);

        for (String metal : CLUSTERS) Content.item("cluster_" + metal, () -> new Item(Content.properties("cluster_" + metal)));

        // Thaumaturge versions of the Metallurgy gadgets; behaviour comes from GadgetLogic/ShieldLogic by id.
        Content.item("thaumium_knuckles", () -> new GadgetItem("thaumium_knuckles", Content.properties("thaumium_knuckles").durability(500).enchantable(15)));
        Content.item("void_gauntlet", () -> new GadgetItem("void_gauntlet", Content.properties("void_gauntlet")
                .durability(GadgetsConfig.Gauntlet.gauntletMaxDamage * 3 / 2).enchantable(10).rarity(Rarity.UNCOMMON)));
        Content.item("thaumium_buckler", () -> new MetallurgyShieldItem("thaumium_buckler", Content.properties("thaumium_buckler").durability(500).enchantable(20)));
        Content.item("void_buckler", () -> new MetallurgyShieldItem("void_buckler", Content.properties("void_buckler").durability(800).enchantable(22).rarity(Rarity.UNCOMMON)));

        CAPS.register(bus);
        RODS.register(bus);
        bus.addListener(ThaumaturgeCompat::components);
        NeoForge.EVENT_BUS.addListener(ThaumaturgeCompat::spellCast);
        NeoForge.EVENT_BUS.addListener(ThaumaturgeCompat::attributes);
        NeoForge.EVENT_BUS.addListener(ThaumaturgeCompat::voidRepair);
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) ->
                event.addListener(Content.id("thaumaturge_tuning"), new ThaumaturgeTuning()));
    }

    private static void cap(String metal, float cost, ResourceKey<IAspect> special, int craftCost, boolean charged, Rarity rarity) {
        Identifier texture = Content.id("textures/models/wand_cap_" + metal + ".png");
        Supplier<WandCap> cap = CAPS.register(metal, () -> special == null
                ? new WandCap(cost, List.of(), 1.0F, craftCost, texture)
                : new WandCap(cost, List.of(special), 0.8F, craftCost, texture));
        CAP_BY_METAL.put(metal, cap);
        String id = "wand_cap_" + metal;
        Content.item(id, () -> new ItemWandCap(Content.properties(id).rarity(rarity), cap));
        if (charged) Content.item(id + "_inert", () -> new Item(Content.properties(id + "_inert")));
    }

    private static void rod(String metal, int capacity, int craftCost, int staffCapacity, int staffCraftCost, boolean glow,
                            com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate core, Rarity rarity) {
        Identifier texture = Content.id("textures/models/wand_rod_" + metal + ".png");
        Identifier research = Content.id("rod_" + metal);
        Supplier<WandRod> wand = RODS.register(metal, () -> new WandRod(capacity, craftCost, texture, core, glow, false, false, research));
        Supplier<WandRod> staff = RODS.register(metal + "_staff", () -> new WandRod(staffCapacity, staffCraftCost, texture, core, glow, true, true, research));
        Content.item("wand_rod_" + metal, () -> new ItemWandRod(Content.properties("wand_rod_" + metal).rarity(rarity), wand));
        Content.item("staff_rod_" + metal, () -> new ItemWandRod(Content.properties("staff_rod_" + metal).rarity(rarity), staff));
    }

    /** Etherium Goggles count as Goggles of Revealing; their 5% discount comes from attributes below. */
    private static void components(ModifyDefaultComponentsEvent event) {
        event.modify(Content.ITEM_MAP.get("etherium_goggles").get(), builder -> builder.set(TTDataComponents.GOGGLES_UPGRADE.get(), Unit.INSTANCE));
    }

    private static void attributes(ItemAttributeModifierEvent event) {
        Identifier id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!id.getNamespace().equals("metallurgy")) return;
        String path = id.getPath();
        if (path.equals("etherium_goggles")) {
            discount(event, ThaumaturgeTuning.get("etherium_goggles.discount"), "helmet", EquipmentSlotGroup.HEAD);
            return;
        }
        for (Map.Entry<String, Double> metal : ThaumaturgeTuning.armourDiscounts().entrySet()) {
            for (int i = 0; i < ARMOUR.size(); i++) {
                if (path.equals(metal.getKey() + "_" + ARMOUR.get(i)))
                    discount(event, metal.getValue(), ARMOUR.get(i), List.of(EquipmentSlotGroup.HEAD, EquipmentSlotGroup.CHEST, EquipmentSlotGroup.LEGS, EquipmentSlotGroup.FEET).get(i));
            }
        }
    }

    private static void discount(ItemAttributeModifierEvent event, double percent, String piece, EquipmentSlotGroup slot) {
        if (percent > 0) event.addModifier(TTAttributes.VIS_DISCOUNT,
                new AttributeModifier(Content.id("thaumaturge_vis_discount_" + piece), percent / 100.0, AttributeModifier.Operation.ADD_VALUE), slot);
    }

    /** Void metal mends itself; so do the void gadgets. */
    private static void voidRepair(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % Math.max(1, (int) ThaumaturgeTuning.get("void_repair.interval_ticks")) != 0) return;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isDamaged() && (stack.is(Content.ITEM_MAP.get("void_gauntlet").get()) || stack.is(Content.ITEM_MAP.get("void_buckler").get())))
                stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    private static void spellCast(SpellCastEvent.Pre event) {
        LivingEntity caster = event.caster();
        String metal = capMetal(caster);
        if (metal == null) return;
        Level level = caster.level();
        BlockPos pos = caster.blockPosition();
        double cost = 1, power = 1;
        switch (metal) {
            case "quicksilver" -> {
                double low = t("quicksilver.min_cost"), high = t("quicksilver.max_cost");
                cost = low + caster.getRandom().nextDouble() * (high - low);
            }
            case "kalendrite" -> cost = level.isDarkOutside() ? t("kalendrite.night_cost") : t("kalendrite.day_cost");
            case "astral_silver" -> {
                if (level.isDarkOutside() && level.canSeeSkyFromBelowWater(pos.above()) && level instanceof ServerLevel server)
                    power = server.getMoonBrightness(pos) >= 1.0F ? t("astral_silver.full_moon_power") : t("astral_silver.night_power");
            }
            case "tartarite" -> {
                cost = t("tartarite.cost");
                if (level instanceof ServerLevel server) caster.hurtServer(server, caster.damageSources().magic(), (float) t("tartarite.damage"));
            }
            case "krik" -> {
                if (!caster.onGround() && !caster.isInWater()) cost = t("krik.airborne_cost");
            }
            case "amordrine" -> {
                if (caster instanceof Player player)
                    power = 1 + (t("amordrine.max_power") - 1) * Math.min(1, player.experienceLevel / Math.max(1, t("amordrine.max_level")));
            }
            case "desichalkos" -> cost = level.dimension() == Level.END ? t("desichalkos.end_cost") : t("desichalkos.other_cost");
            case "prometheum" -> {
                if (!level.isDarkOutside() && level.canSeeSkyFromBelowWater(pos.above())) {
                    BlockPos plant = nearbyPlant(level, pos, (int) t("prometheum.plant_radius"));
                    if (plant != null) {
                        cost = t("prometheum.cost");
                        if (!level.isClientSide() && caster.getRandom().nextDouble() < t("prometheum.bonemeal_chance"))
                            BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, plant);
                    }
                }
            }
            default -> { }
        }
        event.setVisCost((float) (event.visCost() * cost));
        event.setPower((float) (event.power() * power));
    }

    private static double t(String key) {
        return ThaumaturgeTuning.get(key);
    }

    private static String capMetal(LivingEntity caster) {
        for (ItemStack stack : List.of(caster.getMainHandItem(), caster.getOffhandItem())) {
            if (!(stack.getItem() instanceof ItemWand)) continue;
            WandCap cap = WandVisHelper.partsOf(stack).cap();
            for (Map.Entry<String, Supplier<WandCap>> entry : CAP_BY_METAL.entrySet())
                if (entry.getValue().get() == cap) return entry.getKey();
            return null;
        }
        return null;
    }

    private static BlockPos nearbyPlant(Level level, BlockPos centre, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -2, -radius), centre.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock plant && plant.isValidBonemealTarget(level, pos, state)) return pos.immutable();
        }
        return null;
    }

    private ThaumaturgeCompat() {
    }
}
