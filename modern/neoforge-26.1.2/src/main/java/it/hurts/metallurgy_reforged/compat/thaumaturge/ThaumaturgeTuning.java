package it.hurts.metallurgy_reforged.compat.thaumaturge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Numbers for the cap, armour and goggle effects, read from {@code data/metallurgy/thaumaturge/tuning.json} so a
 * datapack can retune them. The defaults below match the shipped file and are used until it loads (and on clients,
 * which never see server data).
 */
public final class ThaumaturgeTuning implements ResourceManagerReloadListener {
    static final Identifier FILE = Identifier.fromNamespaceAndPath("metallurgy", "thaumaturge/tuning.json");
    private static final Map<String, Double> DEFAULTS = Map.ofEntries(
            Map.entry("quicksilver.min_cost", 0.6), Map.entry("quicksilver.max_cost", 1.3),
            Map.entry("kalendrite.night_cost", 0.8), Map.entry("kalendrite.day_cost", 1.1),
            Map.entry("astral_silver.night_power", 1.2), Map.entry("astral_silver.full_moon_power", 1.35),
            Map.entry("tartarite.cost", 0.7), Map.entry("tartarite.damage", 1.0),
            Map.entry("krik.airborne_cost", 0.75),
            Map.entry("amordrine.max_power", 1.25), Map.entry("amordrine.max_level", 30.0),
            Map.entry("desichalkos.end_cost", 0.7), Map.entry("desichalkos.other_cost", 1.1),
            Map.entry("prometheum.cost", 0.8), Map.entry("prometheum.plant_radius", 4.0), Map.entry("prometheum.bonemeal_chance", 0.05),
            Map.entry("armour_discount.astral_silver", 2.0), Map.entry("armour_discount.etherium", 3.0),
            Map.entry("armour_discount.mithril", 1.0), Map.entry("armour_discount.amordrine", 2.0),
            Map.entry("etherium_goggles.discount", 5.0),
            Map.entry("void_repair.interval_ticks", 40.0),
            Map.entry("eximite_rod.interval_ticks", 60.0), Map.entry("eximite_rod.end_interval_ticks", 20.0));
    private static volatile Map<String, Double> values = DEFAULTS;

    static double get(String key) {
        return values.getOrDefault(key, DEFAULTS.getOrDefault(key, 0.0));
    }

    static Map<String, Double> armourDiscounts() {
        Map<String, Double> out = new HashMap<>();
        values.forEach((key, value) -> {
            if (key.startsWith("armour_discount.")) out.put(key.substring(16), value);
        });
        return out;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, Double> next = new HashMap<>(DEFAULTS);
        manager.getResource(FILE).ifPresent(resource -> {
            try (Reader reader = resource.openAsReader()) {
                flatten("", JsonParser.parseReader(reader).getAsJsonObject(), next);
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().warn("Metallurgy: could not read {}, using defaults", FILE, e);
            }
        });
        values = Map.copyOf(next);
    }

    private static void flatten(String prefix, JsonObject object, Map<String, Double> out) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = prefix + entry.getKey();
            if (entry.getValue().isJsonObject()) flatten(key + ".", entry.getValue().getAsJsonObject(), out);
            else if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) out.put(key, entry.getValue().getAsDouble());
        }
    }
}
