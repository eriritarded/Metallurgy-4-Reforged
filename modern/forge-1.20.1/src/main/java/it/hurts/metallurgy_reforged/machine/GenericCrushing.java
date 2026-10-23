package it.hurts.metallurgy_reforged.machine;
import it.hurts.metallurgy_reforged.config.*;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import java.util.*;
/** Modern equivalent of source CrusherRecipes.registerDefaultOreRecipes. */
public final class GenericCrushing {
    public static MachineRecipes.Result find(ItemStack input) {
        if(input.isEmpty() || !GeneralConfig.enableOreDictCrusherRecipes) return null;
        // These materials have explicit data recipes. Removing one must not
        // silently recreate it through the automatic OreDictionary equivalent.
        String item=BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        if(item.startsWith("metallurgy:") || Set.of("minecraft:iron_ingot","minecraft:gold_ingot","minecraft:raw_iron","minecraft:raw_gold","minecraft:raw_copper","minecraft:iron_ore","minecraft:gold_ore","minecraft:copper_ore","minecraft:deepslate_iron_ore","minecraft:deepslate_gold_ore","minecraft:deepslate_copper_ore").contains(item)) return null;
        var tags=input.getTags().map(TagKey::location).filter(id->id.getNamespace().equals("forge") || id.getNamespace().equals("c")).sorted(Comparator.comparing(ResourceLocation::toString)).toList();
        for(var id:tags) {
            String path=id.getPath(),metal;boolean ore=path.startsWith("ores/"),raw=path.startsWith("raw_materials/");
            if(ore) metal=path.substring(5);else if(raw) metal=path.substring(14);else if(path.startsWith("ingots/")) metal=path.substring(7);else continue;
            if(Materials.BY_NAME.containsKey(metal) || metal.equals("iron") || metal.equals("gold")) continue;
            for(String output:ore?List.of("dusts/","gems/"):List.of("dusts/")) {
                // Source chooses the first registered output. Native registry order
                // is stable; prefer its own namespace, then the other common convention.
                for(String namespace:List.of(id.getNamespace(),id.getNamespace().equals("forge")?"c":"forge")) {
                    var key=TagKey.create(Registries.ITEM,new ResourceLocation(namespace,output+metal));
                    for(var holder:BuiltInRegistries.ITEM.getTagOrEmpty(key)) {
                        if(holder.value()==Items.AIR) continue;
                        String result=BuiltInRegistries.ITEM.getKey(holder.value()).toString();if(!ContentAvailability.enabled(result)) continue;
                        return new MachineRecipes.Result(new ItemStack(holder.value(),ore || raw?2:1),1,0,output.equals("gems/")?0.5F:ore || raw?0.75F:0.1F);
                    }
                }
            }
        }
        return null;
    }
    public static float experience(ItemStack output) {
        var paths=output.getTags().map(tag->tag.location().getPath()).toList();
        if(paths.stream().anyMatch(path->path.startsWith("gems/"))) return 0.5F;
        if(paths.stream().anyMatch(path->path.startsWith("dusts/"))) return 0.75F;
        return 0;
    }
    private GenericCrushing() {}
}
