package it.hurts.metallurgy_reforged.recipe;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
public final class ProcessingInput extends SimpleContainer {
    private final String kind;
    public ProcessingInput(String kind,ItemStack first,ItemStack second) {super(first,second);this.kind=kind;}
    public String kind() {return kind;}
}
