package it.hurts.metallurgy_reforged.compat;

import com.google.gson.*;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import it.hurts.metallurgy_reforged.material.Materials;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Hephaestus/Hephaestus Tools data under the Tinkers switches. Plain JSON only; no Hephaestus API linkage. */
public final class HephaestusData {
    private static final String MATERIALS="hephaestus_tools/materials/",METALS="recipe/hephaestus/metals/";
    /** Hephaestus HarvestTier ordinals for Metallurgy harvest levels0-6 (ordinal4 is gold). */
    private static final int[] TIERS={0,1,2,3,5,6,7};
    private static final Map<String,String> TRAITS=Map.of("damascus_steel","dense");
    public static boolean enabled(String name) {
        return GeneralConfig.tiCIntegration && Materials.BY_NAME.containsKey(name) && !Arrays.asList(GeneralConfig.tinkerMaterialsBlacklist).contains(name);
    }
    public static boolean accept(String path) {
        String name=null;
        if(path.startsWith(MATERIALS)) name=path.substring(MATERIALS.length()).replace(".json","");
        else if(path.startsWith(METALS)) name=path.substring(METALS.length()).split("/")[0];
        return name==null || enabled(name);
    }
    private static double clamp(double value,double low,double high) {return Math.rint(Math.max(low,Math.min(high,value))*1000)/1000;}
    /** Mirror of generate_hephaestus_resources.material, from the configured Metallurgy stats. */
    public static void stats(JsonObject data,Materials.Definition m) {
        for(String key:List.of("head_stats","handle_stats","binding_stats")) data.remove(key);
        JsonObject head=TinkerData.part(m,"head"),handle=TinkerData.part(m,"handle"),extra=TinkerData.part(m,"extra");
        if(head!=null) {
            var value=new JsonObject();
            value.addProperty("durability",Math.max(1,2*head.get("durability").getAsInt()));
            value.addProperty("mining_speed",head.get("mining_speed").getAsDouble());
            value.addProperty("attack_damage",head.get("melee_attack").getAsDouble());
            value.addProperty("attack_speed",1.0);
            value.addProperty("tier",TIERS[Math.max(0,Math.min(6,head.get("harvest_level").getAsInt()))]);
            value.addProperty("enchantability",(int)m.tool("enchantability",14));
            if(TRAITS.containsKey(m.name())) value.addProperty("trait",TRAITS.get(m.name()));
            data.add("head_stats",value);
        }
        if(handle!=null) {
            double speed=head==null?6:head.get("mining_speed").getAsDouble(),attack=head==null?2:head.get("melee_attack").getAsDouble();
            var value=new JsonObject();
            value.addProperty("durability_mult",clamp(handle.get("multiplier").getAsDouble()-1,-.5,.4));
            value.addProperty("speed_mult",clamp((speed-6)*.02,-.2,.45));
            value.addProperty("damage_mult",clamp((attack-2)*.03,-.1,.25));
            value.addProperty("attack_speed_mult",clamp(m.hasTools()?m.tool("attack_speed",0)*.1:0,-.1,.15));
            data.add("handle_stats",value);
        }
        if(extra!=null) {var value=new JsonObject();value.addProperty("durability",Math.max(1,extra.get("durability").getAsInt()));data.add("binding_stats",value);}
    }
    public static byte[] transform(String path,byte[] bytes) {
        if(path.startsWith(MATERIALS)) {
            var material=Materials.BY_NAME.get(path.substring(MATERIALS.length(),path.length()-5));
            if(material==null) return bytes;
            var data=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            stats(data,material);
            data.addProperty("color",material.color());
            return data.toString().getBytes(StandardCharsets.UTF_8);
        }
        if(path.endsWith("/thermite_melting.json")) {
            var data=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            data.addProperty("temperature",Math.max(1,it.hurts.metallurgy_reforged.config.GadgetsConfig.ThermiteDust.temperature-300));
            return data.toString().getBytes(StandardCharsets.UTF_8);
        }
        return bytes;
    }
    private HephaestusData() {}
}
