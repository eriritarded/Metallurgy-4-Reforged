package it.hurts.metallurgy_reforged.compat;

import com.google.gson.*;
import it.hurts.metallurgy_reforged.config.GeneralConfig;
import it.hurts.metallurgy_reforged.material.Materials;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Source TCon2 exceptions and the explicit TCon3 stat adapter. No optional API linkage. */
public final class TinkerData {
    private static final JsonObject SOURCE;
    static {
        try(var stream=TinkerData.class.getResourceAsStream("/assets/metallurgy/tconstruct_legacy.json")) {
            if(stream==null) throw new IOException("Missing Tinkers source definitions");
            SOURCE=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(IOException error) {throw new ExceptionInInitializerError(error);}
    }
    public static boolean enabled(String name) {
        return GeneralConfig.tiCIntegration && Materials.BY_NAME.containsKey(name)
            && !contains(SOURCE.getAsJsonArray("excludedNativeMaterials"),name)
            && !contains(SOURCE.getAsJsonArray("native3ExistingMaterials"),name)
            && !Arrays.asList(GeneralConfig.tinkerMaterialsBlacklist).contains(name);
    }
    private static boolean contains(JsonArray array,String value) {for(var entry:array) if(entry.getAsString().equals(value)) return true;return false;}
    public static JsonObject part(Materials.Definition m,String part) {
        var overrides=SOURCE.getAsJsonObject("overrides").getAsJsonObject(m.name());
        if(overrides!=null) {
            var special=overrides.getAsJsonObject(part);
            if(special==null) return null;
            if(!special.has("auto")) return special.deepCopy();
        } else if(!m.hasTools()) return null;
        int durability=(int)m.tool("durability",1);double speed=m.tool("efficiency",0),attack=m.tool("damage",0);
        var value=new JsonObject();
        switch(part) {
            case "head"->{value.addProperty("durability",Math.max(1,durability/2));value.addProperty("mining_speed",Math.max(0,speed));value.addProperty("melee_attack",Math.max(0,attack));value.addProperty("harvest_level",(int)m.tool("harvest_level",0));}
            case "handle"->{double multiplier=Math.sqrt(durability)*.07;value.addProperty("multiplier",multiplier>2?multiplier*.5:multiplier);value.addProperty("durability",durability/4);}
            case "extra"->value.addProperty("durability",durability/4);
            case "bow"->{value.addProperty("draw_speed",(27-speed)/12.3);value.addProperty("range",speed/12);value.addProperty("bonus_damage",attack/12);}
            default->throw new IllegalArgumentException("Unknown legacy part "+part);
        }
        return value;
    }
    public static JsonObject stats(Materials.Definition material) {
        var stats=new JsonObject();JsonObject head=part(material,"head"),handle=part(material,"handle"),extra=part(material,"extra"),bow=part(material,"bow");
        if(head!=null) {
            var nativeHead=head.deepCopy();int level=nativeHead.remove("harvest_level").getAsInt();
            nativeHead.addProperty("mining_tier",level<=4?"minecraft:"+List.of("wood","stone","iron","diamond","netherite").get(level):"metallurgy:level_"+level);
            stats.add("tconstruct:head",nativeHead);
        }
        if(handle!=null) {var value=new JsonObject();value.addProperty("durability",handle.get("multiplier").getAsDouble()-1);stats.add("tconstruct:handle",value);}
        if(extra!=null) stats.add("tconstruct:binding",new JsonObject());
        if(bow!=null && head!=null) {
            var limb=new JsonObject();limb.addProperty("durability",Math.max(1,head.get("durability").getAsInt()));
            limb.addProperty("draw_speed",1/Math.max(.05,bow.get("draw_speed").getAsDouble())-1);
            limb.addProperty("velocity",bow.get("range").getAsDouble()-1);stats.add("tconstruct:limb",limb);
        }
        if(handle!=null && extra!=null) {var grip=new JsonObject();grip.addProperty("durability",handle.get("multiplier").getAsDouble()-1);stats.add("tconstruct:grip",grip);}
        if(GeneralConfig.conarmIntegration && material.hasArmor()) {
            int[] factors={13,15,16,11};String[] slots={"boots","leggings","chestplate","helmet"};
            for(int i=0;i<4;i++) {var plating=new JsonObject();plating.addProperty("durability",Math.max(1,Math.round(material.armor("durability",1)/3*factors[i])));plating.addProperty("armor",material.defense(i));stats.add("tconstruct:plating_"+slots[i],plating);}
            stats.add("tconstruct:maille",new JsonObject());
        }
        var result=new JsonObject();result.add("stats",stats);return result;
    }
    public static boolean accept(String path) {
        String name=null;
        if(path.startsWith("tinkering/materials/")) name=path.substring(path.lastIndexOf('/')+1).replace(".json","");
        else if(path.startsWith("recipes/tconstruct/metals/")) name=path.substring(26).split("/")[0];
        return name==null || enabled(name);
    }
    public static byte[] transform(String path,byte[] bytes) {
        if(path.startsWith("tinkering/materials/stats/")) {
            String name=path.substring(path.lastIndexOf('/')+1,path.length()-5);
            return stats(Materials.BY_NAME.get(name)).toString().getBytes(StandardCharsets.UTF_8);
        }
        if(path.startsWith("tinkering/materials/traits/") && !GeneralConfig.enableDuplicationTrait) {
            var data=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8));removeDuplication(data);return data.toString().getBytes(StandardCharsets.UTF_8);
        }
        if(path.endsWith("/thermite_melting.json")) {
            var data=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();data.addProperty("temperature",Math.max(1,it.hurts.metallurgy_reforged.config.GadgetsConfig.ThermiteDust.temperature-300));return data.toString().getBytes(StandardCharsets.UTF_8);
        }
        if(path.startsWith("recipes/tconstruct/metals/")) {
            var data=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            if(data.has("temperature")) {String name=path.substring(26).split("/")[0];data.addProperty("temperature",temperature(name));return data.toString().getBytes(StandardCharsets.UTF_8);}
        }
        return bytes;
    }
    private static int temperature(String name) {return it.hurts.metallurgy_reforged.fluid.FluidCatalog.ALL.stream().filter(f->f.id().equals(name)).findFirst().map(f->Math.max(1,f.temperature()-300)).orElse(1000);}
    private static void removeDuplication(JsonElement value) {
        if(value.isJsonArray()) {var array=value.getAsJsonArray();for(int i=array.size()-1;i>=0;i--) {var entry=array.get(i);if(entry.isJsonObject() && entry.getAsJsonObject().has("name") && entry.getAsJsonObject().get("name").getAsString().equals("metallurgy:duplication")) array.remove(i);else removeDuplication(entry);}}
        else if(value.isJsonObject()) for(var entry:value.getAsJsonObject().entrySet()) removeDuplication(entry.getValue());
    }
    private TinkerData() {}
}
