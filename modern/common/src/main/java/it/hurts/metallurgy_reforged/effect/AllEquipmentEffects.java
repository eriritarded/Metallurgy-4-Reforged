package it.hurts.metallurgy_reforged.effect;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.*;
import java.util.*;

/** Source ALL-category callbacks. Inventory repair/degradation runs for every
 * carried piece, not just the selected tool. Server mutations are authoritative. */
public final class AllEquipmentEffects {
    private static boolean equipment(ItemStack stack) {
        return EquipmentEffects.accepts(stack,EquipmentEffects.Category.ALL)||EquipmentEffects.accepts(stack,EquipmentEffects.Category.ARMOR);
    }
    private static List<ItemStack> equipped(LivingEntity entity) {
        List<ItemStack> values=new ArrayList<>();
        for(EquipmentSlot slot:EquipmentSlot.values()) values.add(entity.getItemBySlot(slot));
        return values;
    }
    public static void equipped(LivingEntity entity,EquipmentSlot slot,ItemStack stack) {
        if(!equipment(stack)) return;
        String metal=EquipmentEffects.enabledMaterial(stack,EquipmentEffects.Category.ALL);
        if(metal.equals("platinum") && slot==EquipmentSlot.MAINHAND && EquipmentEffects.accepts(stack,EquipmentEffects.Category.ALL)) VersionBridge.unbreakable(stack);
        if(metal.equals("mithril")) {
            int count=VersionBridge.enchantmentCount(stack);
            try(State state=VersionBridge.stack(stack)) {
                if(count>0 || state.has("arcane_boost")) {
                    state.integer("arcane_boost",Math.min(5,count));state.integer("arcane_combat_boost",count);
                }
            }
        }
    }
    private static int baseDurability(ItemStack stack) {
        var material=Materials.BY_NAME.get(EquipmentEffects.baseMetal(stack));
        if(material==null) return stack.getMaxDamage();
        if(EquipmentEffects.accepts(stack,EquipmentEffects.Category.ALL)) return (int)material.tool("durability",0);
        int multiplier=switch(EquipmentEffects.form(stack)) {case "boots"->13;case "leggings"->15;case "chestplate"->16;default->11;};
        return multiplier*(int)material.armor("durability",0);
    }
    private static boolean rebirth(ItemStack stack) {
        if(!equipment(stack) || !EquipmentEffects.enabledMaterial(stack,EquipmentEffects.Category.ALL).equals("haderoth")) return false;
        try(State state=VersionBridge.stack(stack)) {
            if(state.flag("reborn")) return false;
            state.flag("reborn",true);state.decimal("durability_boost",25);state.decimal("attack_boost",2);
        }
        VersionBridge.setMaximumDamage(stack,Math.max(1,baseDurability(stack)*25));stack.setDamageValue(0);return true;
    }
    public static void use(Player player) {
        ItemStack stack=player.getMainHandItem();
        if(stack.getMaxDamage()-stack.getDamageValue()<=1 && rebirth(stack)) VersionBridge.sound(player,"block.anvil.place",0.75F,1.25F);
    }
    public static void hurt(LivingEntity entity,float amount) {
        if(EquipmentEffects.level(entity,"haderoth",EquipmentEffects.Category.ALL)==0) return;
        float expected=Math.max(1,amount/4)*3;
        for(EquipmentSlot slot:EquipmentEffects.ARMOR_SLOTS) {
            ItemStack stack=entity.getItemBySlot(slot);
            if(stack.getDamageValue()+expected<stack.getMaxDamage()) continue;
            ItemStack copy=stack.copy();
            if(rebirth(copy)) {
                try(State state=VersionBridge.stack(copy)) { state.flag("reborn_armor",true); }
                entity.setItemSlot(slot,copy);VersionBridge.sound(entity,"item.totem.use",0.75F,1);VersionBridge.sound(entity,"block.anvil.place",0.5F,1.25F);
            }
        }
    }
    public static float damage(DamageSource source,float amount) {
        if(source.getDirectEntity() instanceof LivingEntity owner && EquipmentEffects.level(owner,"haderoth",EquipmentEffects.Category.ALL)>=10)
            try(State data=VersionBridge.stack(owner.getMainHandItem())) { if(data.flag("reborn")) amount+=data.decimal("attack_boost"); }
        return amount;
    }
    public static float speed(Player player,float original) {
        ItemStack stack=player.getMainHandItem();String metal=EquipmentEffects.enabledMaterial(stack,EquipmentEffects.Category.ALL);
        try(State state=VersionBridge.stack(stack)) {
            if(metal.equals("haderoth") && state.flag("reborn")) return original*2;
            if(metal.equals("mithril") && state.integer("arcane_boost")>0) return original+state.integer("arcane_boost")*1.5F;
        }
        return original;
    }
    public static void tick(Player player) {
        if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        Set<ItemStack> carried=Collections.newSetFromMap(new IdentityHashMap<>());carried.addAll(equipped(player));
        for(int i=0;i<player.getInventory().getContainerSize();i++) carried.add(player.getInventory().getItem(i));
        for(ItemStack stack:carried) {
            if(!equipment(stack)) continue;
            switch(EquipmentEffects.enabledMaterial(stack,EquipmentEffects.Category.ALL)) {
                case "prometheum" -> {
                    if(player.tickCount%100==0 && level.dimension()==Level.OVERWORLD && VersionBridge.day(player) && level.canSeeSky(player.blockPosition()) && Math.random()<0.5 && stack.getDamageValue()>0) stack.setDamageValue(stack.getDamageValue()-1);
                }
                case "shadow_steel" -> {
                    int sky=VersionBridge.day(player)?Math.min(14,level.getBrightness(LightLayer.SKY,player.blockPosition())):0;
                    int block=Math.min(14,level.getBrightness(LightLayer.BLOCK,player.blockPosition()));
                    int interval=75-Math.round(50-Math.max(sky,block)*50F/14);
                    if(interval<=50 && player.tickCount%interval==0 && stack.getDamageValue()>0) stack.setDamageValue(stack.getDamageValue()-1);
                }
                case "adamantine" -> {
                    float food=player.getFoodData().getFoodLevel()/20F;
                    if(food<0.85F && player.tickCount%((1+(int)Math.floor(8*food))*20)==0) {
                        VersionBridge.damageStack(player,stack,2);VersionBridge.sound(player,"entity.painting.break",0.3F,0.8F);it.hurts.metallurgy_reforged.visual.EffectParticles.entity(player,"adamantine",5,1,true,1);
                    }
                }
            }
        }
    }
    public static void eat(LivingEntity entity,ItemStack food) {
        int nutrition=VersionBridge.nutrition(food,entity);if(nutrition<0) return;
        Set<ItemStack> stacks=Collections.newSetFromMap(new IdentityHashMap<>());stacks.addAll(equipped(entity));
        if(entity instanceof Player player) for(int i=0;i<36;i++) stacks.add(player.getInventory().getItem(i));
        for(ItemStack stack:stacks) if(equipment(stack) && EquipmentEffects.enabledMaterial(stack,EquipmentEffects.Category.ALL).equals("adamantine") && stack.getDamageValue()>0) {
            stack.setDamageValue(Math.max(0,stack.getDamageValue()-nutrition*2));VersionBridge.sound(entity,"block.anvil.use",0.5F,3);
        }
    }
    public static void death(LivingEntity victim,DamageSource source) {
        if(source.getDirectEntity() instanceof LivingEntity owner && EquipmentEffects.level(owner,"orichalcum",EquipmentEffects.Category.ALL)>0) {
            for(ItemStack stack:equipped(owner)) if(equipment(stack) && EquipmentEffects.enabledMaterial(stack,EquipmentEffects.Category.ALL).equals("orichalcum")) {
                stack.setDamageValue(Math.max(0,stack.getDamageValue()-(int)victim.getMaxHealth()));it.hurts.metallurgy_reforged.visual.EffectParticles.front(owner,"orichalcum",2,.4F,true,5);
            }
        }
    }
    public static void playerDrops(Player player,Collection<ItemEntity> drops) {
        boolean keep=VersionBridge.keepInventory(player);
        Iterator<ItemEntity> iterator=drops.iterator();
        while(iterator.hasNext()) {
            ItemStack stack=iterator.next().getItem();if(!equipment(stack)) continue;
            String metal=EquipmentEffects.enabledMaterial(stack,EquipmentEffects.Category.ALL);
            if(metal.equals("sanguinite")) iterator.remove();
            else if(!keep && metal.equals("amordrine")) { player.getInventory().add(stack);iterator.remove(); }
        }
    }
    public static void clone(Player original,Player replacement,boolean death) {
        if(!death || VersionBridge.keepInventory(replacement)) return;
        for(int i=0;i<36;i++) {
            ItemStack stack=original.getInventory().getItem(i);
            if(equipment(stack) && EquipmentEffects.enabledMaterial(stack,EquipmentEffects.Category.ALL).equals("amordrine")) replacement.getInventory().add(stack.copy());
        }
    }
    private AllEquipmentEffects() {}
}
