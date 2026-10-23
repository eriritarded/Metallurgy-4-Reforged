package it.hurts.metallurgy_reforged.recipe;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.Paragon;
import net.minecraft.resources.Identifier;

import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;
public final class ParagonRecipe extends CustomRecipe {
    public ParagonRecipe() {}
    private List<ItemStack> items(CraftingInput input) {
        List<ItemStack> items=new ArrayList<>();for(int i=0;i<input.size();i++) items.add(input.getItem(i));return items;
    }
    @Override public boolean matches(CraftingInput input,Level level) { return Paragon.matches(items(input),input.width(),input.height()); }
    @Override public ItemStack assemble(CraftingInput input) { return Paragon.assemble(items(input),input.width(),input.height()); }
    @Override public RecipeSerializer<ParagonRecipe> getSerializer() { return Content.PARAGON_RECIPE.get(); }
}
