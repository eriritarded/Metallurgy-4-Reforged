package it.hurts.metallurgy_reforged.machine;
import it.hurts.metallurgy_reforged.effect.EquipmentEffects;
import net.minecraft.world.item.ItemStack;
import java.util.List;
public final class SublimationRecipes {
    public record Recipe(String metal,int blocks,String effect,int duration,int amplifier) {}
    public static final List<Recipe> ALL=List.of(
        new Recipe("adamantine",3,"health_boost",18000,3),new Recipe("amordrine",3,"jump_boost",18000,0),
        new Recipe("astral_silver",3,"night_vision",18000,0),new Recipe("black_steel",3,"resistance",18000,0),
        new Recipe("carmot",4,"haste",21600,0),new Recipe("deep_iron",4,"water_breathing",24000,0),
        new Recipe("ignatius",4,"fire_resistance",18000,0),new Recipe("kalendrite",4,"regeneration",24000,1),
        new Recipe("lemurite",3,"invisibility",19200,0),new Recipe("lutetium",4,"levitation",24000,0),
        new Recipe("midasium",3,"luck",21600,0),new Recipe("orichalcum",3,"strength",21600,1),
        new Recipe("quicksilver",4,"speed",24000,1),new Recipe("sanguinite",3,"saturation",19200,0));
    public static Recipe forBlock(ItemStack input) {
        if(!EquipmentEffects.form(input).equals("block")) return null;
        String metal=EquipmentEffects.baseMetal(input);
        return ALL.stream().filter(r->r.metal.equals(metal)).findFirst().orElse(null);
    }
    public static Recipe match(ItemStack input) { var recipe=forBlock(input);return recipe!=null&&input.getCount()==recipe.blocks?recipe:null; }
    private SublimationRecipes() {}
}
