package it.hurts.metallurgy_reforged.effect;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Extra attributes read by the original JsonMaterialHelper, with per-piece
 * armor division and absorbed Paragon health/movement/reach contributions. */
public final class EquipmentStats {
    public record Entry(String attribute,String id,double amount,EquipmentSlot slot) {}
    public static boolean replacesDefaults(ItemStack stack) {
        if(!EquipmentEffects.accepts(stack,EquipmentEffects.Category.ARMOR) || !EquipmentEffects.material(stack).equals("haderoth")) return false;
        try(State state=VersionBridge.stack(stack)) { return state.flag("reborn_armor"); }
    }
    public static List<Entry> modifiers(ItemStack stack) {
        if(it.hurts.metallurgy_reforged.gadget.GadgetLogic.is(stack,"brass_knuckles") || it.hurts.metallurgy_reforged.gadget.GadgetLogic.is(stack,"rubracium_gauntlet"))
            return List.of(new Entry("attack_damage","gadget_damage",it.hurts.metallurgy_reforged.gadget.GadgetLogic.is(stack,"brass_knuckles")?it.hurts.metallurgy_reforged.config.GadgetsConfig.BrassKnuckles.attackDamage:it.hurts.metallurgy_reforged.config.GadgetsConfig.Gauntlet.gauntletAttackDamage,EquipmentSlot.MAINHAND),new Entry("attack_speed","gadget_speed",it.hurts.metallurgy_reforged.gadget.GadgetLogic.is(stack,"brass_knuckles")?it.hurts.metallurgy_reforged.config.GadgetsConfig.BrassKnuckles.attackSpeed:it.hurts.metallurgy_reforged.config.GadgetsConfig.Gauntlet.gauntletAttackSpeed,EquipmentSlot.MAINHAND));
        String base=EquipmentEffects.baseMetal(stack);
        var material=Materials.BY_NAME.get(base);
        if(material==null) return List.of();
        List<Entry> result=new ArrayList<>();
        if(EquipmentEffects.accepts(stack,EquipmentEffects.Category.ARMOR)) {
            EquipmentSlot slot=switch(EquipmentEffects.form(stack)) {case "helmet"->EquipmentSlot.HEAD;case "chestplate"->EquipmentSlot.CHEST;case "leggings"->EquipmentSlot.LEGS;default->EquipmentSlot.FEET;};
            if(replacesDefaults(stack)) {
                int index=switch(slot) {case FEET->0;case LEGS->1;case CHEST->2;default->3;};
                add(result,"armor","reborn_armor_"+slot.getName(),material.defense(index)+2,slot);
                add(result,"armor_toughness","reborn_toughness_"+slot.getName(),material.armor("toughness",0)+0.5F,slot);
                return result;
            }
            for(String attribute:List.of("max_health","knockback_resistance","movement_speed")) add(result,attribute,"armor_"+slot.getName()+"_"+attribute,material.armor(attribute,0)/4,slot);
            String infused=EquipmentEffects.material(stack);
            if(base.equals("tartarite") && !infused.equals(base)) {
                var other=Materials.BY_NAME.get(infused);
                for(String attribute:List.of("max_health","knockback_resistance","movement_speed")) add(result,attribute,"paragon_"+slot.getName()+"_"+attribute,other.armor(attribute,0)/4,slot);
            }
        } else if(EquipmentEffects.accepts(stack,EquipmentEffects.Category.ALL)) {
            for(String attribute:List.of("max_health","movement_speed","attack_damage","attack_speed","reach_distance")) add(result,attribute,"tool_"+attribute,material.tool(attribute,0),EquipmentSlot.MAINHAND);
            String infused=EquipmentEffects.material(stack);
            if(base.equals("tartarite") && !infused.equals(base)) {
                var other=Materials.BY_NAME.get(infused);
                for(String attribute:List.of("max_health","movement_speed","reach_distance")) add(result,attribute,"paragon_tool_"+attribute,other.tool(attribute,0),EquipmentSlot.MAINHAND);
            }
        }
        if(it.hurts.metallurgy_reforged.config.ConfigFiles.effect("mithril","all") && EquipmentEffects.material(stack).equals("mithril")) {
            int count=VersionBridge.enchantmentCount(stack);boolean active;
            try(State state=VersionBridge.stack(stack)) { active=count>0 || state.has("arcane_boost"); }
            if(active) {
                String form=EquipmentEffects.form(stack);
                if(EquipmentEffects.accepts(stack,EquipmentEffects.Category.ARMOR)) {
                    EquipmentSlot slot=switch(form) {case "helmet"->EquipmentSlot.HEAD;case "chestplate"->EquipmentSlot.CHEST;case "leggings"->EquipmentSlot.LEGS;default->EquipmentSlot.FEET;};
                    // Source used one UUID across slots for each arcane bonus.
                    add(result,"armor","mithril_armor_protection",count,slot);
                    add(result,"armor_toughness","mithril_armor_toughness",count*0.5F,slot);
                } else if(EquipmentEffects.accepts(stack,EquipmentEffects.Category.ALL)) {
                    add(result,"attack_damage","mithril_attack",3+count,EquipmentSlot.MAINHAND);
                    double speed=switch(form) {case "sword"->-2.4;case "axe"->-2.5-material.tool("damage",0)/10;case "pickaxe"->-2.8;case "shovel"->-3;default->material.tool("damage",0)-3;};
                    add(result,"attack_speed","mithril_speed",-2.4-speed-material.tool("attack_speed",0),EquipmentSlot.MAINHAND);
                }
            }
        }
        return result;
    }
    private static void add(List<Entry> list,String attribute,String id,double amount,EquipmentSlot slot) {
        if(amount!=0) list.add(new Entry(attribute,id,amount,slot));
    }
    private EquipmentStats() {}
}
