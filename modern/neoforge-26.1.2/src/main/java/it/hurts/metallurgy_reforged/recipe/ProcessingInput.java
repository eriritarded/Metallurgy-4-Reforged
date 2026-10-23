package it.hurts.metallurgy_reforged.recipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
public record ProcessingInput(String kind,ItemStack first,ItemStack second) implements RecipeInput {
    @Override public ItemStack getItem(int slot) {return switch(slot) {case 0->first;case 1->second;default->throw new IndexOutOfBoundsException(slot);};}
    @Override public int size() {return 2;}
}
