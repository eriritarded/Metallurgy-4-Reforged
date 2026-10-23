package it.hurts.metallurgy_reforged.recipe;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.Paragon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;
public final class ParagonRecipe extends CustomRecipe {
    public ParagonRecipe(ResourceLocation id,CraftingBookCategory category) { super(id,category); }
    private List<ItemStack> items(CraftingContainer input) {
        List<ItemStack> items=new ArrayList<>();for(int i=0;i<input.getContainerSize();i++) items.add(input.getItem(i));return items;
    }
    @Override public boolean matches(CraftingContainer input,Level level) { return Paragon.matches(items(input),input.getWidth(),input.getHeight()); }
    @Override public ItemStack assemble(CraftingContainer input,RegistryAccess registries) { return Paragon.assemble(items(input),input.getWidth(),input.getHeight()); }
    @Override public boolean canCraftInDimensions(int width,int height) { return width>=3 && height>=3; }
    @Override public RecipeSerializer<ParagonRecipe> getSerializer() { return Content.PARAGON_RECIPE.get(); }
}
