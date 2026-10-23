package it.hurts.metallurgy_reforged.compat;
import com.google.gson.*;
import it.hurts.metallurgy_reforged.config.ContentAvailability;
import it.hurts.metallurgy_reforged.material.Materials;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Read-only, in-memory adaptation of our bundled optional data to restart settings. */
public final class DynamicPackData {
    public static byte[] transform(String module,String path,byte[] bytes) {
        if(!path.endsWith(".json")) return bytes;
        if(module.equals("tconstruct")) return TinkerData.transform(path,bytes);
        if(module.equals("hephaestus")) return HephaestusData.transform(path,bytes);
        if(module.equals("core_tiers")) return mining(path,bytes);
        if(module.equals("projecte")) return projecte(bytes);
        boolean gear=module.equals("silentgear") && path.startsWith("silentgear_materials/"),tetra=module.equals("tetra") && path.startsWith("materials/metallurgy/");
        if(!gear && !tetra) return bytes;
        String name=path.substring(path.lastIndexOf('/')+1,path.length()-5);var material=Materials.BY_NAME.get(name);if(material==null) return bytes;
        JsonObject data=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
        if(tetra) tetra(data,material);else gear(data,material);
        return data.toString().getBytes(StandardCharsets.UTF_8);
    }
    public static boolean accept(String module,String path) {
        if(module.equals("tconstruct")) return TinkerData.accept(path);
        return !module.equals("hephaestus") || HephaestusData.accept(path);
    }
    private static void tetra(JsonObject data,Materials.Definition m) {
        int level=(int)m.tool("harvest_level",0);String color=String.format(Locale.ROOT,"%06X",m.color());
        data.addProperty("primary",rounded(3+m.tool("damage",0)));data.addProperty("secondary",rounded(2.8+m.hardness()/3));data.addProperty("tertiary",rounded(Math.max(1,Math.min(6,m.tool("efficiency",0)/2))));
        data.addProperty("durability",(int)m.tool("durability",1));data.addProperty("integrityCost",Math.max(2,(level+2)/2));data.addProperty("integrityGain",Math.min(8,3+level));data.addProperty("magicCapacity",6*(int)m.tool("enchantability",0));
        data.addProperty("toolLevel",tier(level));data.addProperty("toolEfficiency",m.tool("efficiency",0));data.getAsJsonObject("tints").addProperty("glyph",color);data.getAsJsonObject("tints").addProperty("texture",color);
        data.getAsJsonObject("requiredTools").addProperty("hammer_dig","minecraft:"+(level<=2?"gold":level<=4?"iron":level<=6?"diamond":"netherite"));
    }
    private static void gear(JsonObject data,Materials.Definition m) {
        boolean modern=data.has("properties");String prefix=modern?"silentgear:":"";
        JsonObject parts=data.getAsJsonObject(modern?"properties":"stats"),main=parts.getAsJsonObject(prefix+"main");
        if(m.hasTools()) {
            main.addProperty("durability",(int)m.tool("durability",1));main.addProperty("harvest_speed",m.tool("efficiency",0));main.addProperty("enchantment_value",(int)m.tool("enchantability",0));main.addProperty(modern?"attack_damage":"melee_damage",m.tool("damage",0));
            if(modern) main.add("harvest_tier",harvest(m));else {data.addProperty("harvest_tier",tier((int)m.tool("harvest_level",0)));data.getAsJsonObject("availability").addProperty("tier",(int)m.tool("harvest_level",0));}
            JsonObject rod=parts.getAsJsonObject(prefix+"rod"),tip=parts.getAsJsonObject(prefix+"tip");
            rod.add("durability",modifier(m.tool("durability",1)*0.15,modern));tip.add("durability",modifier(m.tool("durability",1)*0.15,modern));tip.add("harvest_speed",modifier(m.tool("efficiency",0)*0.10,modern));tip.add(modern?"attack_damage":"melee_damage",modifier(m.tool("damage",0)*0.15,modern));
            if(modern) tip.add("harvest_tier",harvest(m));
        }
        if(m.hasArmor()) {
            main.addProperty("armor_durability",(int)m.armor("durability",1));int total=0;String[] forms={"boots","leggings","chestplate","helmet"};
            for(int i=0;i<4;i++) {int defense=m.defense(i);total+=defense;main.addProperty("armor/"+forms[i],defense);}
            main.addProperty("armor",total);main.addProperty("armor_toughness",m.armor("toughness",0));if(!m.hasTools()) main.addProperty("enchantment_value",(int)m.armor("enchantability",0));
        }
        color(data,String.format(Locale.ROOT,"#FF%06X",m.color()));
    }
    private static JsonObject harvest(Materials.Definition m) {var result=new JsonObject();result.addProperty("name",m.name().replace('_',' '));result.addProperty("level_hint",Integer.toString((int)m.tool("harvest_level",0)));result.addProperty("incorrect_blocks_for_tool","metallurgy:incorrect_for_level_"+(int)m.tool("harvest_level",0));return result;}
    private static JsonObject modifier(double value,boolean modern) {var result=new JsonObject();if(modern) result.addProperty("operation","ADD");result.addProperty(modern?"value":"add",rounded(value));return result;}
    private static String tier(int level) {return level<=4?"minecraft:"+List.of("wood","stone","iron","diamond","netherite").get(level):"metallurgy:level_"+level;}
    private static double rounded(double value) {return Math.rint(value*1000)/1000;}
    private static void color(JsonElement value,String color) {
        if(value.isJsonArray()) {for(var entry:value.getAsJsonArray()) color(entry,color);}
        else if(value.isJsonObject()) {var object=value.getAsJsonObject();for(String key:new ArrayList<>(object.keySet())) {if(key.equals("color") && object.get(key).isJsonPrimitive()) object.addProperty(key,color);else color(object.get(key),color);}}
    }
    private static byte[] projecte(byte[] bytes) {
        JsonObject data=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject(),values=data.getAsJsonObject("values");if(values==null) return bytes;
        for(String key:List.of("before","after")) if(values.has(key)) {
            var value=values.get(key);
            if(value.isJsonObject()) {var object=value.getAsJsonObject();for(String id:new ArrayList<>(object.keySet())) if(!ContentAvailability.enabled(id)) object.remove(id);}
            else {var filtered=new JsonArray();for(var entry:value.getAsJsonArray()) {var object=entry.getAsJsonObject();if(!object.has("id") || ContentAvailability.enabled(object.get("id").getAsString())) filtered.add(object);}values.add(key,filtered);}
        }
        if(values.has("conversion")) {var filtered=new JsonArray();for(var entry:values.getAsJsonArray("conversion")) {var output=entry.getAsJsonObject().get("output");String id=output.isJsonPrimitive()?output.getAsString():output.getAsJsonObject().get("id").getAsString();if(ContentAvailability.enabled(id)) filtered.add(entry);}values.add("conversion",filtered);}
        return data.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static byte[] mining(String path,byte[] bytes) {
        if(path.endsWith("/beacon_base_blocks.json")) {
            var data=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();var values=new JsonArray();
            for(var m:Materials.ALL) if((m.oreLevel()<0?m.tool("harvest_level",0):m.oreLevel())>1) values.add("metallurgy:"+m.name()+"_block");
            data.add("values",values);return data.toString().getBytes(StandardCharsets.UTF_8);
        }
        String name=path.substring(path.lastIndexOf('/')+1,path.length()-5);int level;boolean exact=false;
        if(name.startsWith("needs_level_")) {level=Integer.parseInt(name.substring(12));exact=true;}
        else if(name.startsWith("incorrect_for_level_")) level=Integer.parseInt(name.substring(20));
        else {
            var levels=Map.of("wooden",0,"gold",0,"stone",1,"iron",2,"diamond",3,"netherite",4);String tool=name.replace("incorrect_for_","").replace("_tool","");
            if(name.startsWith("incorrect_for_") && levels.containsKey(tool)) level=levels.get(tool);
            else if(name.equals("needs_stone_tool")) {level=1;exact=true;}
            else if(name.equals("needs_iron_tool")) {level=2;exact=true;}
            else if(name.equals("needs_diamond_tool")) level=2;
            else return bytes;
        }
        var ores=Materials.ALL.stream().filter(m->m.oreLevel()>=0).toList();Set<String> own=new HashSet<>();for(var m:ores) own.add("metallurgy:"+m.name()+"_ore");
        var data=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();var values=new JsonArray();
        for(var value:data.getAsJsonArray("values")) if(!value.isJsonPrimitive() || !own.contains(value.getAsString())) values.add(value);
        for(var m:ores) if(exact?m.oreLevel()==level:m.oreLevel()>level) values.add("metallurgy:"+m.name()+"_ore");
        data.add("values",values);return data.toString().getBytes(StandardCharsets.UTF_8);
    }
    private DynamicPackData() {}
}
