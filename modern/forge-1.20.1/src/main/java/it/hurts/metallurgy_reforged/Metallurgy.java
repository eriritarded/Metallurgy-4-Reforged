package it.hurts.metallurgy_reforged;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.effect.EffectEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
@Mod("metallurgy")
public final class Metallurgy {
    public Metallurgy() {
        it.hurts.metallurgy_reforged.config.ConfigBootstrap.initialize();
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        it.hurts.metallurgy_reforged.config.ContentCondition.register(bus);
        it.hurts.metallurgy_reforged.config.IntegrationCondition.register(bus);
        it.hurts.metallurgy_reforged.recipe.ProcessingRegistration.register(bus);
        it.hurts.metallurgy_reforged.world.WorldRegistration.register(bus);
        it.hurts.metallurgy_reforged.advancement.Advancements.register(bus);
        it.hurts.metallurgy_reforged.compat.OptionalPacks.register(bus);
        it.hurts.metallurgy_reforged.compat.ModularTiers.register();
        if(net.minecraftforge.fml.ModList.get().isLoaded("tconstruct")) it.hurts.metallurgy_reforged.compat.tconstruct.TinkerIntegration.register(bus);
        if(it.hurts.metallurgy_reforged.config.GeneralConfig.spartanWeaponryIntegration && net.minecraftforge.fml.ModList.get().isLoaded("spartanweaponry")) it.hurts.metallurgy_reforged.compat.spartan.SpartanIntegration.register(bus);
        it.hurts.metallurgy_reforged.network.ArmorNetwork.register();
        it.hurts.metallurgy_reforged.network.VisualNetwork.register();
        Content.register(FMLJavaModLoadingContext.get().getModEventBus());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.effect.ItemAttributes.class);
        MinecraftForge.EVENT_BUS.register(EffectEvents.class);
        MinecraftForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.gadget.BlockGadgetEvents.class);
        MinecraftForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.config.GeneralEvents.class);
        MinecraftForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.world.RetrogenEvents.class);
        MinecraftForge.EVENT_BUS.register(it.hurts.metallurgy_reforged.advancement.AdvancementEvents.class);
    }
}
