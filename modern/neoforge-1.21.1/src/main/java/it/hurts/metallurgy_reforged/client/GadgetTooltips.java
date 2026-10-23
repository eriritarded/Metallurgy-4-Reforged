package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import it.hurts.metallurgy_reforged.gadget.GadgetLogic;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.*;
import java.util.*;
public final class GadgetTooltips {
    public static void append(ItemStack stack,List<Component> lines) {
        String id=VersionBridge.itemId(stack);
        if(id.equals("metallurgy:ore_detector")) {
            var metals=GadgetLogic.detectorMetals(stack);
            if(metals.isEmpty()) lines.add(Component.translatable("tooltip.metallurgy.detector_program").withStyle(ChatFormatting.YELLOW));
            else {
                lines.add(Component.translatable("tooltip.metallurgy.detector_use").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("tooltip.metallurgy.detector_charges",Math.max(0,25*metals.size()-GadgetLogic.detectorUses(stack))).withStyle(ChatFormatting.GRAY));
            }
            for(int i=0;i<metals.size();i++) {String metal=metals.get(i);lines.add(Component.literal("Metal "+(i+1)+": ").withStyle(ChatFormatting.GRAY).append(Component.translatable("item.metallurgy."+metal+"_ingot").withStyle(style->style.withColor(Materials.BY_NAME.get(metal).color()))));}
        } else if(id.equals("metallurgy:electrum_magnet")) {
            translated(lines,"tooltip.metallurgy.electrum_magnet");
            String mode=switch(GadgetLogic.magnetMode(stack)) {case 1->"metal_only";case 2->"enabled";default->"disabled";};
            lines.add(Component.translatable("tooltip.metallurgy.electrum_magnet_mode").append(" ").append(Component.translatable("tooltip.metallurgy.electrum_magnet_"+mode)));
        } else if(id.equals("metallurgy:etherium_goggles") || Set.of("metallurgy:lemurite_shield","metallurgy:zinc_buckler","metallurgy:vulcanite_buckler","metallurgy:ceruclase_shield","metallurgy:orichalcum_buckler").contains(id)) translated(lines,"tooltip.metallurgy."+id.substring(11));
        else if(id.equals("metallurgy:sublimation_chamber")) {
            var component=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);if(component==null) return;var data=component.copyTag();
            String effect=data.getString("effect");if(effect.isEmpty()) return;
            var key=ResourceLocation.tryParse(effect.indexOf(':')<0?"minecraft:"+effect:effect);if(key==null) return;
            var type=BuiltInRegistries.MOB_EFFECT.get(key);if(type==null) return;
            lines.add(Component.literal("Active Effect: ").append(Component.translatable(type.getDescriptionId()).withStyle(type.getCategory()==MobEffectCategory.HARMFUL?ChatFormatting.RED:ChatFormatting.BLUE)));
            int seconds=Math.max(0,data.getInt("active_time"))/20;lines.add(Component.literal("Time Elapsed: "+seconds/60+":"+String.format(Locale.ROOT,"%02d",seconds%60)));
        }
    }
    private static void translated(List<Component> lines,String key) {for(String line:Component.translatable(key).getString().replace("<PC>","%").replace("<NL>","\n").split("\n")) lines.add(Component.literal(line));}
    private GadgetTooltips() {}
}
