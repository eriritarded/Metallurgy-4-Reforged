package it.hurts.metallurgy_reforged.machine;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.world.item.*;
import java.util.*;

/** Source1.4.1 default recipes. Native tag matching replaces OreDictionary.
 * Machine inputs and results are copied; shared recipe stacks are never mutated. */
public final class MachineRecipes {
    public record Result(ItemStack output,int first,int second,float xp) {}
    private record Alloy(String first,int a,String second,int b,String result,int count,float xp) {}
    private static final List<Alloy> ALLOYS=List.of(
        new Alloy("copper",3,"tin",1,"bronze",4,1.75F),
        new Alloy("shadow_iron",2,"lemurite",1,"shadow_steel",3,1.5F),
        new Alloy("alduorite",1,"ceruclase",1,"inolashite",2,1.25F),
        new Alloy("copper",3,"zinc",1,"brass",4,1.75F),
        new Alloy("deep_iron",3,"infuscolium",1,"black_steel",4,1.75F),
        new Alloy("iron",1,"manganese",2,"steel",2,1.5F),
        new Alloy("iron",1,"bronze",2,"damascus_steel",3,1.5F),
        new Alloy("silver",1,"gold",1,"electrum",2,1.25F),
        new Alloy("orichalcum",1,"platinum",1,"celenegil",2,1.25F),
        new Alloy("kalendrite",1,"platinum",1,"amordrine",2,1.25F),
        new Alloy("mithril",1,"rubracium",2,"haderoth",3,1.5F),
        new Alloy("adamantine",1,"atlarus",1,"tartarite",1,1.5F),
        new Alloy("eximite",1,"meutoite",1,"desichalkos",2,1.25F),
        // Source fallback recipes accept ingot/ingot or dust/dust, never mixed.
        new Alloy("@gold_ingot",1,"@iron_ingot",1,"angmallen",2,1.25F),
        new Alloy("@gold_dust",1,"@iron_dust",1,"angmallen",2,1.25F),
        new Alloy("infuscolium",1,"steel",1,"hepatizon",2,1.25F),
        new Alloy("silver",1,"astral_silver",1,"quicksilver",2,1.25F),
        new Alloy("lutetium",1,"osmium",1,"krik",2,1.25F),
        new Alloy("sanguinite",1,"alduorite",1,"etherium",2,1.25F));
    private static boolean matches(ItemStack stack,String metal) {
        if(metal.startsWith("@")) {
            String name=metal.substring(1);
            String namespace=name.endsWith("_dust")?"metallurgy:":"minecraft:";
            return VersionBridge.itemId(stack).equals(namespace+name);
        }
        if(metal.equals("iron") || metal.equals("gold")) {
            //1.12 fallback entries used concrete vanilla ingots and our dusts.
            return matches(stack,"@"+metal+"_ingot") || matches(stack,"@"+metal+"_dust");
        }
        return VersionBridge.itemId(stack).equals("metallurgy:"+metal+"_ingot") || VersionBridge.itemId(stack).equals("metallurgy:"+metal+"_dust")
            || VersionBridge.itemTag(stack,"ingots/"+metal) || VersionBridge.itemTag(stack,"dusts/"+metal);
    }
    public static Result alloy(ItemStack first,ItemStack second) {
        if(first.isEmpty() || second.isEmpty()) return null;
        for(Alloy recipe:ALLOYS) {
            boolean forward=matches(first,recipe.first)&&matches(second,recipe.second);
            boolean reverse=matches(first,recipe.second)&&matches(second,recipe.first);
            if(forward || reverse) {
                if(!it.hurts.metallurgy_reforged.config.ContentAvailability.enabled("metallurgy:"+recipe.result+"_ingot")) continue;
                ItemStack output=Content.stack(recipe.result+"_ingot");output.setCount(recipe.count);
                return new Result(output,forward?recipe.a:recipe.b,forward?recipe.b:recipe.a,recipe.xp);
            }
        }
        return null;
    }
    public static boolean alloyInput(ItemStack stack) { return ALLOYS.stream().anyMatch(r->matches(stack,r.first)||matches(stack,r.second)); }
    public static int ingotTier(String metal) {
        var definition=Materials.BY_NAME.get(metal);if(definition==null) return 1;if(definition.oreLevel()>=0) return definition.oreLevel();
        for(var alloy:ALLOYS) if(alloy.result.equals(metal)) {var first=Materials.BY_NAME.get(alloy.first);var second=Materials.BY_NAME.get(alloy.second);if(first!=null && second!=null) return Math.max(first.oreLevel(),second.oreLevel());return 1;}
        return 1;
    }
    public static Result crush(ItemStack input) {
        if(input.isEmpty()) return null;
        // Source always matched known recipes by OreDictionary. The switch only
        // controls automatic additional recipes, not known-metal equivalence.
        boolean tags=true;
        for(var metal:Materials.ALL) {
            String name=metal.name(),id=VersionBridge.itemId(input);
            if(metal.oreLevel()>=0 && (id.equals("metallurgy:"+name+"_ore") || tags && VersionBridge.itemTag(input,"ores/"+name))) return result(name+"_dust",2,0.75F);
            if(id.equals("minecraft:raw_"+name) || VersionBridge.itemTag(input,"raw_materials/"+name)) return result(name+"_dust",2,0.75F);
            if(id.equals("metallurgy:"+name+"_ingot") || tags && VersionBridge.itemTag(input,"ingots/"+name)) return result(name+"_dust",1,0);
        }
        for(String name:List.of("iron","gold")) {
            if(VersionBridge.itemId(input).equals("minecraft:raw_"+name) || VersionBridge.itemTag(input,"raw_materials/"+name)) return result(name+"_dust",2,0.75F);
            if(tags && VersionBridge.itemTag(input,"ores/"+name) || VersionBridge.itemId(input).equals("minecraft:"+name+"_ore") || VersionBridge.itemId(input).equals("minecraft:deepslate_"+name+"_ore")) return result(name+"_dust",2,0.75F);
            if(VersionBridge.itemId(input).equals("minecraft:"+name+"_ingot") || VersionBridge.itemTag(input,"ingots/"+name)) return result(name+"_dust",1,0);
        }
        String id=VersionBridge.itemId(input);
        return switch(id) {
            case "metallurgy:tar_ore"->result("tar",4,0.75F);
            case "metallurgy:phosphorite_ore"->result("phosphorus",4,0.75F);
            case "metallurgy:potash_ore"->result("potash",4,0.75F);
            case "metallurgy:sulfur_ore"->result("sulfur_dust",4,0.75F);
            case "metallurgy:potash"->result("potash_fertilizer",1,1);
            default->GenericCrushing.find(input);
        };
    }
    private static Result result(String id,int count,float xp) {if(!it.hurts.metallurgy_reforged.config.ContentAvailability.enabled("metallurgy:"+id)) return null; ItemStack stack=Content.stack(id);stack.setCount(count);return new Result(stack,1,0,xp); }
    public static float experience(ItemStack output,boolean alloy) {
        String id=VersionBridge.itemId(output);
        if(alloy) {
            for(Alloy recipe:ALLOYS) if(id.equals("metallurgy:"+recipe.result+"_ingot")) return recipe.xp;
        } else {
            if(id.equals("metallurgy:potash_fertilizer")) return 1;
            if(id.endsWith("_dust") || Set.of("metallurgy:tar","metallurgy:phosphorus","metallurgy:potash").contains(id)) return 0.75F;
        }
        return alloy?0:GenericCrushing.experience(output);
    }
    private MachineRecipes() {}
}
