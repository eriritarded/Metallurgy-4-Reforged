package it.hurts.metallurgy_reforged.material;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original1.4.1 numeric definitions shared by the three native implementations. */
public final class Materials {
    public record Definition(String name, float hardness, float blastResistance, int oreLevel,
                             int color, JsonObject armor, JsonObject tool, JsonObject original) {
        public boolean hasArmor() { return armor != null; }
        public boolean hasTools() { return tool != null; }
        public float tool(String key, float fallback) { return number(tool, key, fallback); }
        public float armor(String key, float fallback) { return number(armor, key, fallback); }
        public int defense(int index) { return armor.getAsJsonArray("damage_reduction").get(index).getAsInt(); }
    }

    public static final List<Definition> ALL;
    public static final Map<String, Definition> BY_NAME;
    static {
        List<Definition> entries = new ArrayList<>();
        try (InputStream stream = Materials.class.getResourceAsStream("/assets/metallurgy/materials.json")) {
            if (stream == null) throw new IOException("Missing original material definitions");
            JsonArray array = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            array=it.hurts.metallurgy_reforged.config.ConfigFiles.materialStats(array);
            for (JsonElement entry : array) {
                JsonObject value = entry.getAsJsonObject();
                entries.add(new Definition(value.get("name").getAsString(), value.get("hardness").getAsFloat(),
                    value.get("blast_resistance").getAsFloat(), value.get("ore_harvest_level").getAsInt(),
                    Integer.parseInt(value.get("color").getAsString(), 16), value.getAsJsonObject("armor_stats"),
                    value.getAsJsonObject("tool_stats"), value));
            }
        } catch (IOException | RuntimeException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        ALL = List.copyOf(entries);
        Map<String, Definition> map = new LinkedHashMap<>();
        for (Definition value : ALL) {
            if (map.put(value.name(), value) != null) throw new IllegalStateException("Duplicate material " + value.name());
        }
        BY_NAME = Collections.unmodifiableMap(map);
    }

    private static float number(JsonObject object, String key, float fallback) {
        return object != null && object.has(key) ? object.get(key).getAsFloat() : fallback;
    }

    private Materials() {}
}
