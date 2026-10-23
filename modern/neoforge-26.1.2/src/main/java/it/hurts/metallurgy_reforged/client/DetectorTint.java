package it.hurts.metallurgy_reforged.client;
import com.mojang.serialization.*;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public record DetectorTint(int index) implements ItemTintSource {
    public static final MapCodec<DetectorTint> CODEC=Codec.intRange(0,2).fieldOf("index").xmap(DetectorTint::new,DetectorTint::index);
    @Override public int calculate(ItemStack stack,ClientLevel level,LivingEntity owner) {return DetectorDisplay.tint(stack,index);}
    @Override public MapCodec<DetectorTint> type() {return CODEC;}
    @SubscribeEvent public static void register(RegisterColorHandlersEvent.ItemTintSources event) {event.register(Identifier.fromNamespaceAndPath("metallurgy","detector"),CODEC);}
    @SubscribeEvent public static void pipelines(RegisterRenderPipelinesEvent event) {event.registerPipeline(GadgetRenderTypes.PIPELINE);}
}
