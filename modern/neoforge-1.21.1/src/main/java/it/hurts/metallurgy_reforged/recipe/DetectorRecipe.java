package it.hurts.metallurgy_reforged.recipe;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.gadget.DetectorCrafting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;
public final class DetectorRecipe extends CustomRecipe {
    public DetectorRecipe(CraftingBookCategory category) { super(category); }
    private List<ItemStack> items(CraftingInput input) {
        List<ItemStack> items=new ArrayList<>();for(int i=0;i<input.size();i++) items.add(input.getItem(i));return items;
    }
    @Override public boolean matches(CraftingInput input,Level level) { return !DetectorCrafting.assemble(items(input)).isEmpty(); }
    @Override public ItemStack assemble(CraftingInput input,net.minecraft.core.HolderLookup.Provider registries) { return DetectorCrafting.assemble(items(input)); }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=1; }
    @Override public RecipeSerializer<DetectorRecipe> getSerializer() { return Content.DETECTOR_RECIPE.get(); }
}
