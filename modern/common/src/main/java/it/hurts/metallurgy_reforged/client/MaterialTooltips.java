package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.config.*;
import it.hurts.metallurgy_reforged.effect.*;
import it.hurts.metallurgy_reforged.material.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Source SHIFT statistics, CTRL effect descriptions and native RGB styles. */
public final class MaterialTooltips {
    private static final ChatFormatting[] COLORS={ChatFormatting.DARK_RED,ChatFormatting.RED,ChatFormatting.GOLD,ChatFormatting.YELLOW,ChatFormatting.DARK_GREEN,ChatFormatting.AQUA,ChatFormatting.LIGHT_PURPLE};
    private record Effect(String metal,String category) {}
    private static final List<Effect> EFFECTS;
    static {
        List<Effect> values=new ArrayList<>();
        for(var field:EffectsRosterConfig.class.getFields()) if(field.getType()==boolean.class) {
            String[] parts=field.getName().split("Effect",2);String metal=parts[0].replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
            values.add(new Effect(metal,parts[1].toLowerCase(Locale.ROOT)));
        }
        values.add(new Effect("quicksilver","all"));EFFECTS=List.copyOf(values);
    }
    public static void append(ItemStack stack,List<Component> lines,boolean shift,boolean control) {
        var material=MaterialItems.material(stack);if(material==null) return;
        if(!lines.isEmpty()) lines.set(0,lines.get(0).copy().withStyle(style->style.withColor(material.color())));
        if(EquipmentEffects.form(stack).equals("ingot")) {int tier=it.hurts.metallurgy_reforged.machine.MachineRecipes.ingotTier(material.name());lines.add(Component.literal("Tier: ").append(Component.literal(Integer.toString(tier)).withStyle(COLORS[Math.max(0,Math.min(6,tier))])));}
        boolean armor=EquipmentEffects.accepts(stack,EquipmentEffects.Category.ARMOR),tool=EquipmentEffects.accepts(stack,EquipmentEffects.Category.ALL);
        if(!armor && !tool) return;
        String form=EquipmentEffects.form(stack);
        if(!GeneralConfig.hideStatsTooltip) {
            if(!shift) lines.add(Component.translatable("tooltip.metallurgy.press_shift"));
            else {
                if(form.equals("pickaxe")) {int tier=Math.max(1,Math.min(7,(int)material.tool("harvest_level",1)));lines.add(Component.translatable("tooltip.metallurgy.stats.harvest_level",Component.literal("\u2b51".repeat(tier)).withStyle(COLORS[tier-1])));}
                int maximum=stack.getMaxDamage(),remaining=maximum-stack.getDamageValue();float ratio=maximum==0?0:remaining/(float)maximum;
                lines.add(Component.translatable("tooltip.metallurgy.stats.durability",Component.literal(remaining+"/"+maximum).withStyle(ratio<0.33F?ChatFormatting.RED:ratio<0.66F?ChatFormatting.YELLOW:ChatFormatting.GREEN)));
                if(List.of("axe","pickaxe","shovel").contains(form)) {float speed=material.tool("efficiency",0);int color=(int)Math.ceil(Math.max(0,Math.min(6,speed-6)));lines.add(Component.translatable("tooltip.metallurgy.stats.efficiency",Component.literal(Float.toString(speed)).withStyle(COLORS[color])));}
            }
        }
        boolean any=effects(material.name(),stack,lines,control);
        String infused=EquipmentEffects.material(stack);if(!infused.equals(material.name())) any|=effects(infused,stack,lines,control);
        if(any && !control) lines.add(Component.translatable("tooltip.metallurgy.press_ctrl"));
    }
    private static boolean effects(String metal,ItemStack stack,List<Component> lines,boolean expanded) {
        boolean any=false,armor=EquipmentEffects.accepts(stack,EquipmentEffects.Category.ARMOR);
        for(Effect effect:EFFECTS) {
            if(!effect.metal.equals(metal) || !ConfigFiles.effect(metal,effect.category)) continue;
            boolean eligible=effect.category.equals("all")?(!metal.equals("platinum") || !armor):EquipmentEffects.accepts(stack,EquipmentEffects.Category.valueOf(effect.category.toUpperCase(Locale.ROOT)));
            if(!eligible) continue;
            String key="tooltip.metallurgy.effect."+metal+"_"+effect.category;
            lines.add(Component.translatable(key).withStyle(style->style.withColor(Materials.BY_NAME.get(metal).color())));
            if(expanded) for(String line:Component.translatable(key+".tooltip").getString().replace("<PC>","%").replace("<NL>","\n").split("\n")) lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            any=true;
        }
        return any;
    }
    private MaterialTooltips() {}
}
