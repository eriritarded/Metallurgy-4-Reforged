package it.hurts.metallurgy_reforged.effect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.bus.api.SubscribeEvent;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class ItemAttributes {
    @SubscribeEvent public static void modifiers(ItemAttributeModifierEvent event) {
        if(EquipmentStats.replacesDefaults(event.getItemStack())) event.clearModifiers();
        for(var entry:EquipmentStats.modifiers(event.getItemStack())) {
            net.minecraft.core.Holder<Attribute> attribute=entry.attribute().equals("reach_distance")?Attributes.BLOCK_INTERACTION_RANGE:
                BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath("minecraft","generic."+entry.attribute())).orElse(null);
            if(attribute==null) continue;
            var id=ResourceLocation.fromNamespaceAndPath("metallurgy",entry.id());
            event.addModifier(attribute,new AttributeModifier(id,entry.amount(),AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.bySlot(entry.slot()));
        }
    }
    private ItemAttributes() {}
}
