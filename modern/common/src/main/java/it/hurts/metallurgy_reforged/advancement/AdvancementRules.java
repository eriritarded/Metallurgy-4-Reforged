package it.hurts.metallurgy_reforged.advancement;
import it.hurts.metallurgy_reforged.effect.EquipmentEffects;
import it.hurts.metallurgy_reforged.material.MaterialItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class AdvancementRules {
    public static void equipment(ServerPlayer player,EquipmentSlot changed,ItemStack replacement) {
        if(!EquipmentEffects.ARMOR_SLOTS.contains(changed)) return;
        Set<String> unique=new HashSet<>();int yin=0,yang=0;
        for(var slot:EquipmentEffects.ARMOR_SLOTS) {
            var material=MaterialItems.material(slot==changed?replacement:player.getItemBySlot(slot));
            if(material==null) continue;unique.add(material.name());if(material.name().equals("sanguinite")) yin++;else if(material.name().equals("amordrine")) yang++;
        }
        if(unique.size()==4) Advancements.fire(player,"jack_of_all_trades",0,"");
        if(yin==2 && yang==2) Advancements.fire(player,"yin_yang",0,"");
    }
    private AdvancementRules() {}
}
