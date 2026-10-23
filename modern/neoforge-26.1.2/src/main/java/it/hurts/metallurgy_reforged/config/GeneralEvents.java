package it.hurts.metallurgy_reforged.config;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.material.Materials;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import java.util.*;
public final class GeneralEvents {
    @SubscribeEvent public static void spawn(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof Mob mob)) return;
        String id=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        if(!Arrays.asList(GeneralConfig.mobsThatCanHaveEquipment).contains(id)) return;
        try(var state=VersionBridge.entity(mob)) {if(state.flag("spawn_equipment_tried")) return;state.flag("spawn_equipment_tried",true);}
        var random=mob.getRandom();if(random.nextFloat()*100>=event.getLevel().getDifficulty().getId()*5) return;
        List<Materials.Definition> candidates=Materials.ALL.stream().filter(Materials.Definition::hasArmor)
            .filter(material->Arrays.stream(GeneralConfig.metalsThatCannotBeEquipped).noneMatch(material.name()::equalsIgnoreCase)).toList();
        if(candidates.isEmpty()) return;var metal=candidates.get(random.nextInt(candidates.size()));
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)) {
            String suffix=switch(slot) {case HEAD->"helmet";case CHEST->"chestplate";case LEGS->"leggings";default->"boots";};
            String item=metal.name()+"_"+suffix;if(mob.getItemBySlot(slot).isEmpty() && ContentAvailability.enabled("metallurgy:"+item)) mob.setItemSlot(slot,Content.stack(item));
        }
        if(metal.hasTools() && mob.getMainHandItem().isEmpty()) {
            String item=metal.name()+(random.nextFloat()<0.25F?"_axe":"_sword");
            if(ContentAvailability.enabled("metallurgy:"+item)) mob.setItemSlot(EquipmentSlot.MAINHAND,Content.stack(item));
        }
    }
    @SubscribeEvent public static void repair(AnvilUpdateEvent event) {
        ItemStack left=event.getLeft(),right=event.getRight();String id=VersionBridge.itemId(left);
        if(!id.startsWith("metallurgy:")) return;
        boolean armor=List.of("_helmet","_chestplate","_leggings","_boots").stream().anyMatch(id::endsWith);
        boolean tool=List.of("_sword","_axe","_pickaxe","_shovel","_hoe").stream().anyMatch(id::endsWith);
        if(!armor && !tool) return;
        String metal=id.substring(11,id.lastIndexOf('_'));
        boolean ingot=VersionBridge.itemId(right).equals("metallurgy:"+metal+"_ingot") || VersionBridge.itemTag(right,"ingots/"+metal);
        if(ingot && (armor && !GeneralConfig.enableAnvilArmorRepair || tool && !GeneralConfig.enableAnvilToolRepair)) event.setCanceled(true);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(!event.getEntity().level().isClientSide() && GeneralConfig.warning)
            for(String key:List.of("message.world_join_message.1","message.world_join_message.3","message.github_repo_url","message.world_join_message.4")) event.getEntity().sendSystemMessage(Component.translatable(key));
    }
    private GeneralEvents() {}
}
