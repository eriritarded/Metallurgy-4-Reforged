package it.hurts.metallurgy_reforged.recipe;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.gadget.DetectorCrafting;
import net.minecraft.resources.Identifier;

import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;
public final class DetectorRecipe extends CustomRecipe {
    public DetectorRecipe() {}
    private List<ItemStack> items(CraftingInput input) {
        List<ItemStack> items=new ArrayList<>();for(int i=0;i<input.size();i++) items.add(input.getItem(i));return items;
    }
    @Override public boolean matches(CraftingInput input,Level level) { return !DetectorCrafting.assemble(items(input)).isEmpty(); }
    @Override public ItemStack assemble(CraftingInput input) { return DetectorCrafting.assemble(items(input)); }
    @Override public RecipeSerializer<DetectorRecipe> getSerializer() { return Content.DETECTOR_RECIPE.get(); }
}
