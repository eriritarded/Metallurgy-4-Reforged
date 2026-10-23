package it.hurts.metallurgy_reforged;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.EffectEvents;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
@Mod("metallurgy")
public final class Metallurgy {
    public Metallurgy(IEventBus bus) {
        it.hurts.metallurgy_reforged.config.ConfigBootstrap.initialize();
        it.hurts.metallurgy_reforged.config.ContentCondition.register(bus);
        it.hurts.metallurgy_reforged.config.IntegrationCondition.register(bus);
        it.hurts.metallurgy_reforged.recipe.ProcessingRegistration.register(bus);
        it.hurts.metallurgy_reforged.world.WorldRegistration.register(bus);
        it.hurts.metallurgy_reforged.advancement.Advancements.register(bus);
        it.hurts.metallurgy_reforged.compat.OptionalPacks.register(bus);
        Content.register(bus);
        bus.addListener(it.hurts.metallurgy_reforged.network.ProcessingNetwork::register);
        NeoForge.EVENT_BUS.register(EffectEvents.class);
        NeoForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.gadget.BlockGadgetEvents.class);
        NeoForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.config.GeneralEvents.class);
        NeoForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.world.RetrogenEvents.class);
        NeoForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.advancement.AdvancementEvents.class);
        NeoForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.effect.ItemAttributes.class);
        bus.addListener(this::capabilities);
        bus.addListener(it.hurts.metallurgy_reforged.network.ArmorNetwork::register);
        bus.addListener(it.hurts.metallurgy_reforged.network.VisualNetwork::register);
    }
    private void capabilities(RegisterCapabilitiesEvent event) {
        it.hurts.metallurgy_reforged.compat.ExternalHeat.register(event);
        // NeoForge's fallback only covers exact BucketItem instances, not our placement-gated subclass.
        // FluidContentsTint and native fluid transfer both read the contained fluid through this capability.
        for (var entry:it.hurts.metallurgy_reforged.fluid.FluidContent.ENTRIES.values()) {
            event.registerItem(Capabilities.Fluid.ITEM,
                (stack,access)->new net.neoforged.neoforge.transfer.fluid.BucketResourceHandler(access),entry.bucket.get());
        }
        event.registerBlockEntity(Capabilities.Item.BLOCK,Content.CHEST_ENTITY.get(),(chest,side)->VanillaContainerWrapper.of(chest));
        event.registerBlockEntity(Capabilities.Item.BLOCK,Content.MACHINE_ENTITY.get(),(machine,side)->new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(machine,side));
        event.registerBlockEntity(Capabilities.Item.BLOCK,Content.CHAMBER_ENTITY.get(),(machine,side)->new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(machine,side));
    }
}
