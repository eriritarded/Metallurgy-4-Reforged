package it.hurts.metallurgy_reforged.effect;

import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Original category eligibility and capped, double-weight Paragon armor. */
public final class EquipmentEffects {
    public enum Category { WEAPON, TOOL, PICKAXE, HOE, SWORD, ARMOR, ALL }
    public static final List<EquipmentSlot> ARMOR_SLOTS=List.of(EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD);
    private static final Set<String> TOOLS=Set.of("axe","pickaxe","hoe","shovel","sword");
    public static String form(ItemStack stack) {
        String id=VersionBridge.itemId(stack);
        if(!id.startsWith("metallurgy:")) return "";
        String path=id.substring(11);
        int separator=path.lastIndexOf('_');
        return separator<0?"":path.substring(separator+1);
    }
    public static String baseMetal(ItemStack stack) {
        String id=VersionBridge.itemId(stack);
        if(!id.startsWith("metallurgy:")) return "";
        String path=id.substring(11);
        int separator=path.lastIndexOf('_');
        if(separator<0) return "";
        String metal=path.substring(0,separator);
        return Materials.BY_NAME.containsKey(metal)?metal:"";
    }
    public static String material(ItemStack stack) {
        String metal=baseMetal(stack);
        if(metal.equals("tartarite")) {
            try(State state=VersionBridge.stack(stack)) {
                String paragon=state.string("paragon");
                if(it.hurts.metallurgy_reforged.config.ConfigFiles.effect("tartarite","all") && Materials.BY_NAME.containsKey(paragon) && !paragon.equals("tartarite")) return paragon;
            }
        }
        return metal;
    }
    public static String weapon(LivingEntity entity) {
        String form=form(entity.getMainHandItem());
        return form.equals("sword")||form.equals("axe")?enabledMaterial(entity.getMainHandItem(),Category.WEAPON):"";
    }
    public static String enabledMaterial(ItemStack stack,Category category) {String metal=material(stack);return it.hurts.metallurgy_reforged.config.ConfigFiles.effect(metal,category.name().toLowerCase(Locale.ROOT))?metal:"";}
    public static boolean accepts(ItemStack stack,Category category) {
        String form=form(stack);
        return switch(category) {
            case WEAPON -> form.equals("sword")||form.equals("axe");
            case TOOL -> form.equals("axe")||form.equals("pickaxe")||form.equals("shovel");
            case SWORD -> form.equals("sword");
            case PICKAXE -> form.equals("pickaxe");
            case HOE -> form.equals("hoe");
            case ALL -> TOOLS.contains(form);
            case ARMOR -> Set.of("boots","leggings","chestplate","helmet").contains(form);
        };
    }
    public static int armorLevel(LivingEntity entity,String metal) {
        if(!it.hurts.metallurgy_reforged.config.ConfigFiles.effect(metal,"armor")) return 0;
        return rawArmorLevel(entity,metal);
    }
    private static int rawArmorLevel(LivingEntity entity,String metal) {
        int count=0;
        for(EquipmentSlot slot:ARMOR_SLOTS) {
            ItemStack piece=entity.getItemBySlot(slot);
            if(!accepts(piece,Category.ARMOR)) continue;
            String base=baseMetal(piece);
            if(base.equals(metal)) count++;
            else if(base.equals("tartarite") && material(piece).equals(metal)) count+=2;
        }
        return Math.min(4,count);
    }
    public static int level(LivingEntity entity,String metal,Category category) {
        if(!it.hurts.metallurgy_reforged.config.ConfigFiles.effect(metal,category.name().toLowerCase(Locale.ROOT))) return 0;
        if(category==Category.ARMOR) return armorLevel(entity,metal);
        if(category==Category.ALL) return rawArmorLevel(entity,metal)+(accepts(entity.getMainHandItem(),Category.ALL) && material(entity.getMainHandItem()).equals(metal)?10:0);
        return accepts(entity.getMainHandItem(),category) && material(entity.getMainHandItem()).equals(metal)?10:0;
    }
    private EquipmentEffects() {}
}
