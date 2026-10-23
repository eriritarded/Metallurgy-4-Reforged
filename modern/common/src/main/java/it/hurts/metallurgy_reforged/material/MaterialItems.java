package it.hurts.metallurgy_reforged.material;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.world.item.ItemStack;
import java.util.Set;
public final class MaterialItems {
    private static final Set<String> FORMS=Set.of("ingot","dust","nugget","ore","block","engraved_block","large_bricks","bricks","crystals","hazard_block","reinforced_glass","sword","axe","pickaxe","shovel","hoe","helmet","chestplate","leggings","boots");
    public static Materials.Definition material(ItemStack stack) {
        String id=VersionBridge.itemId(stack);if(!id.startsWith("metallurgy:")) return null;String path=id.substring(11);
        for(var metal:Materials.ALL) if(path.startsWith(metal.name()+"_") && FORMS.contains(path.substring(metal.name().length()+1))) return metal;
        return null;
    }
    private MaterialItems() {}
}
