package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.gadget.GadgetLogic;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid="metallurgy",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class GadgetProperties {
    @SubscribeEvent public static void colors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {event.register(DetectorDisplay::tint,Content.ITEM_MAP.get("ore_detector").get());}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {event.enqueueWork(()-> {
        for(String id:java.util.List.of("lemurite_shield","zinc_buckler","vulcanite_buckler","ceruclase_shield","orichalcum_buckler"))
            ItemProperties.register(Content.ITEM_MAP.get(id).get(),new ResourceLocation("minecraft","blocking"),(stack,level,entity,seed)->entity!=null && entity.isUsingItem() && entity.getUseItem()==stack?1:0);
        ItemProperties.register(Content.ITEM_MAP.get("electrum_magnet").get(),new ResourceLocation("minecraft","active"),(stack,level,entity,seed)->GadgetLogic.magnetMode(stack)>0?1:0);
        for(String id:java.util.List.of("celenegil_axe","celenegil_sword","electrum_axe","electrum_pickaxe","electrum_shovel","tartarite_axe","tartarite_pickaxe","tartarite_shovel","tartarite_sword"))
            ItemProperties.register(Content.ITEM_MAP.get(id).get(),new ResourceLocation("minecraft","active"),(stack,level,entity,seed)->ItemVisualState.active(stack)?1:0);
    });}
    private GadgetProperties() {}
}
