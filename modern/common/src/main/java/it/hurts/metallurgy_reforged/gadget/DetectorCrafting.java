package it.hurts.metallurgy_reforged.gadget;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.*;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class DetectorCrafting {
    public static ItemStack assemble(List<ItemStack> input) {
        ItemStack detector=ItemStack.EMPTY;List<String> metals=new ArrayList<>();int occupied=0;
        for(ItemStack stack:input) {
            if(stack.isEmpty()) continue;occupied++;
            if(GadgetLogic.is(stack,"ore_detector")) {
                if(!detector.isEmpty()) return ItemStack.EMPTY;detector=stack;
            } else {
                String metal=GadgetBridge.ingotMetal(stack);var definition=Materials.BY_NAME.get(metal);
                if(definition==null || definition.oreLevel()<0 || metals.size()>=3 || !metals.isEmpty() && metals.get(metals.size()-1).equals(metal)) return ItemStack.EMPTY;
                metals.add(metal);
            }
        }
        if(detector.isEmpty()) return ItemStack.EMPTY;
        if(occupied==1) return Content.stack("ore_detector");
        if(!GadgetLogic.detectorMetals(detector).isEmpty() || metals.isEmpty()) return ItemStack.EMPTY;
        ItemStack result=detector.copy();result.setCount(1);
        try(State state=VersionBridge.stack(result)) {
            state.remove("detector_uses");for(int i=1;i<=3;i++) state.remove("metal_"+i);
            for(int i=0;i<metals.size();i++) state.string("metal_"+(i+1),metals.get(i));
        }
        return result;
    }
    private DetectorCrafting() {}
}
