package it.hurts.metallurgy_reforged.effect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class ItemAttributes {
    @SubscribeEvent public static void modifiers(ItemAttributeModifierEvent event) {
        if(EquipmentStats.replacesDefaults(event.getItemStack())) event.clearModifiers();
        for(var entry:EquipmentStats.modifiers(event.getItemStack())) {
            if(entry.slot()!=event.getSlotType()) continue;
            Attribute attribute=entry.attribute().equals("reach_distance")?net.minecraftforge.common.ForgeMod.BLOCK_REACH.get():
                BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation("minecraft","generic."+entry.attribute()));
            if(attribute==null) continue;
            UUID id=UUID.nameUUIDFromBytes(("metallurgy:"+entry.id()).getBytes(StandardCharsets.UTF_8));
            event.addModifier(attribute,new AttributeModifier(id,"metallurgy:"+entry.id(),entry.amount(),AttributeModifier.Operation.ADDITION));
        }
    }
    private ItemAttributes() {}
}
