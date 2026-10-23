package it.hurts.metallurgy_reforged.effect;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** One dynamic crafting rule covers all9 original equipment recipes. The top
 * block selects the absorbed metal; the other three may be different metals. */
public final class Paragon {
    private record Match(ItemStack equipment,Materials.Definition infused) {}
    private static Match match(List<ItemStack> grid,int width,int height) {
        if(width<3 || height<3) return null;
        for(int y=0;y<=height-3;y++) for(int x=0;x<=width-3;x++) {
            boolean matches=true;
            for(int row=0;row<height && matches;row++) for(int col=0;col<width;col++) {
                int dx=col-x,dy=row-y;
                ItemStack item=grid.get(row*width+col);
                boolean arm=(dx==1 && (dy==0 || dy==2)) || (dy==1 && (dx==0 || dx==2));
                boolean center=dx==1 && dy==1;
                if(arm) matches=EquipmentEffects.form(item).equals("block")&&!EquipmentEffects.baseMetal(item).isEmpty();
                else if(center) matches=EquipmentEffects.baseMetal(item).equals("tartarite") && (EquipmentEffects.accepts(item,EquipmentEffects.Category.ALL)||EquipmentEffects.accepts(item,EquipmentEffects.Category.ARMOR));
                else matches=item.isEmpty();
                if(!matches) break;
            }
            if(!matches) continue;
            ItemStack item=grid.get((y+1)*width+x+1);
            try(State state=VersionBridge.stack(item)) { if(state.has("paragon")) continue; }
            var metal=Materials.BY_NAME.get(EquipmentEffects.baseMetal(grid.get(y*width+x+1)));
            if(metal==null || metal.name().equals("tartarite") || !metal.hasTools()&&!metal.hasArmor()) continue;
            return new Match(item,metal);
        }
        return null;
    }
    public static boolean matches(List<ItemStack> grid,int width,int height) {var result=match(grid,width,height);return result!=null && it.hurts.metallurgy_reforged.config.ContentAvailability.enabled(VersionBridge.itemId(result.equipment)); }
    public static ItemStack assemble(List<ItemStack> grid,int width,int height) {
        var match=match(grid,width,height);if(match==null || !it.hurts.metallurgy_reforged.config.ContentAvailability.enabled(VersionBridge.itemId(match.equipment))) return ItemStack.EMPTY;
        ItemStack output=match.equipment.copy();output.setCount(1);
        int base=output.getMaxDamage();
        int infused=match.infused.hasTools()?(int)match.infused.tool("durability",0):(int)match.infused.armor("durability",0)*16;
        float ratio=(base+(float)infused)/(base-(float)infused);
        // Modern MAX_DAMAGE components require a positive finite value. The
        //1.12 equation could produce infinity/negative durability. Only that
        // invalid branch uses the combined durability instead of invalid data.
        if(!Float.isFinite(ratio) || ratio<=0) ratio=(base+(float)infused)/Math.max(1,base);
        int maximum=Math.max(1,(int)(ratio*base));
        try(State state=VersionBridge.stack(output)) {
            state.string("paragon",match.infused.name());state.flag("paragon_absorbed",true);state.decimal("durability_boost",ratio);
        }
        VersionBridge.setMaximumDamage(output,maximum);
        return output;
    }
    public static int maximum(ItemStack stack,int base) {
        try(State state=VersionBridge.stack(stack)) { return state.has("durability_boost")?Math.max(1,(int)(state.decimal("durability_boost")*base)):base; }
    }
    private Paragon() {}
}
