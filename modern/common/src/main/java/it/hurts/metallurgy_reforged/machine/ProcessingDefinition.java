package it.hurts.metallurgy_reforged.machine;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.*;
/** Immutable native recipe data. Counts and orientation are authoritative. */
public record ProcessingDefinition(String kind,List<Ingredient> ingredients,List<Integer> amounts,ItemStack output,float xp,String effect,int duration,int amplifier) {
    public ProcessingDefinition {
        if(!Set.of("crusher","alloyer","chamber","generic_crusher").contains(kind)) throw new IllegalArgumentException("Unknown Metallurgy processing kind: "+kind);
        ingredients=List.copyOf(ingredients);amounts=List.copyOf(amounts);output=output.copy();
        int expected=kind.equals("generic_crusher")?0:kind.equals("alloyer")?2:1;
        if(ingredients.size()!=expected || amounts.size()!=expected || amounts.stream().anyMatch(n->n<1 || n>64)) throw new IllegalArgumentException("Invalid processing ingredients/counts");
        if(!Float.isFinite(xp) || xp<0 || xp>1000) throw new IllegalArgumentException("Invalid processing XP");
        if(kind.equals("chamber")) {
            if(!effect.matches("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+") || duration<1 || duration>1728000 || amplifier<0 || amplifier>255) throw new IllegalArgumentException("Invalid chamber effect");
            if(!it.hurts.metallurgy_reforged.effect.VersionBridge.registeredEffect(effect)) throw new IllegalArgumentException("Unknown chamber effect: "+effect);
        } else if(!kind.equals("generic_crusher") && (output.isEmpty() || output.getCount()>output.getMaxStackSize())) throw new IllegalArgumentException("Invalid processing output");
    }
    @Override public ItemStack output() {return output.copy();}
    public boolean input(ItemStack stack) {return ingredients.stream().anyMatch(i->i.test(stack));}
    private boolean slot(int index,ItemStack stack,boolean counts) {return ingredients.get(index).test(stack) && (!counts || (kind.equals("chamber")?stack.getCount()==amounts.get(index):stack.getCount()>=amounts.get(index)));}
    public int orientation(ItemStack first,ItemStack second,boolean counts) {
        if(kind.equals("generic_crusher")) return GenericCrushing.find(first)==null?0:1;
        if(ingredients.size()==1) return slot(0,first,counts)?1:0;
        if(slot(0,first,counts) && slot(1,second,counts)) return 1;
        return slot(0,second,counts) && slot(1,first,counts)?-1:0;
    }
    public MachineRecipes.Result result(ItemStack first,ItemStack second) {
        if(kind.equals("generic_crusher")) return GenericCrushing.find(first);
        int order=orientation(first,second,true);if(order==0 || kind.equals("chamber")) return null;
        return new MachineRecipes.Result(output(),amounts.get(order>0?0:1),amounts.size()==1?0:amounts.get(order>0?1:0),xp);
    }
}
