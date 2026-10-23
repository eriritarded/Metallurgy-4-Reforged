package it.hurts.metallurgy_reforged.compat;

import com.google.gson.*;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reforged's source assignments. No optional Hephaestus classes are linked here. */
public final class HephaestusTraitData {
    private static final JsonObject MATERIALS;
    private static final String[] ROLES={"head_stats","handle_stats","binding_stats"};
    static {
        try(var stream=HephaestusTraitData.class.getResourceAsStream("/assets/metallurgy/hephaestus_traits.json")) {
            if(stream==null) throw new IllegalStateException("Missing Hephaestus trait assignments");
            MATERIALS=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("materials");
        } catch(java.io.IOException error) {throw new IllegalStateException(error);}
    }
    public static Set<String> effects(String metal,int role) {
        if(role<0 || role>=ROLES.length || !HephaestusData.enabled(metal) || !MATERIALS.has(metal)) return Set.of();
        var result=new LinkedHashSet<String>();
        for(var entry:MATERIALS.getAsJsonArray(metal).get(role).getAsJsonArray()) {
            var id=entry.getAsString();
            if(!id.equals("duplication") || GeneralConfig.enableDuplicationTrait) result.add(id);
        }
        return Collections.unmodifiableSet(result);
    }
    public static void decorate(JsonObject data,String metal) {
        if(!MATERIALS.has(metal)) return;
        for(int role=0;role<ROLES.length;role++) {
            if(data.has(ROLES[role]) && !MATERIALS.getAsJsonArray(metal).get(role).getAsJsonArray().isEmpty())
                data.getAsJsonObject(ROLES[role]).addProperty("trait","metallurgy:"+metal);
        }
    }
    private HephaestusTraitData() {}
}
