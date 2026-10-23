package it.hurts.metallurgy_reforged.effect;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Original weapon loot probabilities, including Midasium's zero-looting
 * NaN quirk (+2 at level0, +1 at positive levels), kept explicit. */
public final class WeaponLoot {
    public static void apply(LivingEntity victim,DamageSource source,Collection<ItemEntity> drops) {
        if(!(victim.level() instanceof ServerLevel level)) return;
        LivingEntity direct=source.getDirectEntity() instanceof LivingEntity living?living:null;
        LivingEntity owner=source.getEntity() instanceof LivingEntity living?living:null;
        if(owner!=null && EquipmentEffects.weapon(owner).equals("eximite") && level.dimension()==Level.END) {
            for(ItemEntity drop:drops) drop.getItem().grow(victim.getRandom().nextInt(3));
        }
        if(direct==null) return;
        int looting=VersionBridge.looting(direct);
        String metal=EquipmentEffects.weapon(direct);
        if(metal.equals("ignatius") && Math.random()<(looting+1)/4F) {
            for(ItemEntity drop:drops) {
                ItemStack input=drop.getItem(),result=VersionBridge.smelt(level,input);
                if(!result.isEmpty()) { result.setCount(input.getCount());drop.setItem(result); }
            }
            it.hurts.metallurgy_reforged.visual.EffectParticles.block(level,victim.blockPosition(),metal,15,2,true,3,new net.minecraft.world.phys.Vec3(0,.04,0),new net.minecraft.world.phys.Vec3(.03125,0,.03125));
        } else if(metal.equals("midasium") && Math.random()<=(looting+1)*0.3F) {
            List<ItemStack> equipment=new ArrayList<>();
            for(EquipmentSlot slot:EquipmentSlot.values()) equipment.add(victim.getItemBySlot(slot));
            for(ItemEntity drop:drops) {
                if(equipment.contains(drop.getItem())) continue;
                if(Math.random()<=0.03+looting*0.05) drop.setItem(new ItemStack(Items.GOLD_INGOT,drop.getItem().getCount()));
                else drop.getItem().grow(looting==0?2:1);
            }
            it.hurts.metallurgy_reforged.visual.EffectParticles.block(level,victim.blockPosition(),metal,drops.size(),1,true,5,net.minecraft.world.phys.Vec3.ZERO,new net.minecraft.world.phys.Vec3(.05,.05,.05));
        }
    }
    private WeaponLoot() {}
}
