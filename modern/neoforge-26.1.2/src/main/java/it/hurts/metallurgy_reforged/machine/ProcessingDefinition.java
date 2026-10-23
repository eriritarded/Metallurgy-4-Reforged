package it.hurts.metallurgy_reforged.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.*;

/** Native 26 recipe data: outputs remain templates until runtime component binding is complete. */
public record ProcessingDefinition(String kind,List<Ingredient> ingredients,List<Integer> amounts,Optional<ItemStackTemplate> outputTemplate,float xp,String effect,int duration,int amplifier) {
    public ProcessingDefinition {
        if(!Set.of("crusher","alloyer","chamber","generic_crusher").contains(kind)) throw new IllegalArgumentException("Unknown Metallurgy processing kind: "+kind);
        ingredients=List.copyOf(ingredients);amounts=List.copyOf(amounts);outputTemplate=Objects.requireNonNull(outputTemplate);
        int expected=kind.equals("generic_crusher")?0:kind.equals("alloyer")?2:1;
        if(ingredients.size()!=expected || amounts.size()!=expected || amounts.stream().anyMatch(n->n<1 || n>64)) throw new IllegalArgumentException("Invalid processing ingredients/counts");
        if(!Float.isFinite(xp) || xp<0 || xp>1000) throw new IllegalArgumentException("Invalid processing XP");
        if(outputTemplate.isPresent()) {
            var template=outputTemplate.get();
            if(template.item().value()==Items.AIR || template.count()<1 || template.count()>99) throw new IllegalArgumentException("Invalid processing output template");
        }
        if(kind.equals("chamber")) {
            if(!effect.matches("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+") || duration<1 || duration>1728000 || amplifier<0 || amplifier>255) throw new IllegalArgumentException("Invalid chamber effect");
            if(!it.hurts.metallurgy_reforged.effect.VersionBridge.registeredEffect(effect)) throw new IllegalArgumentException("Unknown chamber effect: "+effect);
        } else if(!kind.equals("generic_crusher") && outputTemplate.isEmpty()) throw new IllegalArgumentException("Missing processing output");
    }
    /** Vanilla validates stack components and capacity when the template creates a runtime stack. */
    public ItemStack output() {return outputTemplate.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY);}
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
        ItemStack output=output();
        // An invalid custom template must never consume inputs for an empty result.
        if(output.isEmpty()) return null;
        return new MachineRecipes.Result(output,amounts.get(order>0?0:1),amounts.size()==1?0:amounts.get(order>0?1:0),xp);
    }
}
