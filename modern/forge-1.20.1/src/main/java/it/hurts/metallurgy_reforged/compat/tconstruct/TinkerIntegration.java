package it.hurts.metallurgy_reforged.compat.tconstruct;

import it.hurts.metallurgy_reforged.config.GeneralConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import java.util.UUID;

/** Loaded only after Forge confirms TConstruct is installed. */
public final class TinkerIntegration {
    private static final UUID UNDERWATER=UUID.fromString("65e286b2-0560-4d7a-a75f-85aa115b8b17");
    public static void register(IEventBus bus) {
        bus.addListener(TinkerIntegration::setup);MinecraftForge.EVENT_BUS.addListener(TinkerIntegration::tick);
        MinecraftForge.EVENT_BUS.addListener(TinkerArmorEvents::tick);MinecraftForge.EVENT_BUS.addListener(TinkerArmorEvents::using);
        it.hurts.metallurgy_reforged.compat.OptionalArmorGuard.register(TinkerArmorEvents::locked);
    }
    private static void setup(FMLCommonSetupEvent event) {event.enqueueWork(()->{
        ModifierModule.LOADER.register(new ResourceLocation("metallurgy","legacy_trait"),LegacyTraitModule.LOADER);
        slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator.addReloadListener(client->{if(!client) LegacyTraitModule.clearRecipes();});
    });}
    private static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || event.player.level().isClientSide) return;
        var player=event.player;var attribute=player.getAttribute(Attributes.ATTACK_SPEED);if(attribute==null) return;
        var stack=player.getMainHandItem();boolean active=false;
        if(GeneralConfig.tiCIntegration && player.isAlive() && player.isUnderWater() && stack.is(TinkerTags.Items.MODIFIABLE)) {
            var tool=ToolStack.from(stack);active=!tool.isBroken() && tool.getModifierLevel(new ModifierId("metallurgy:opistognathus"))>0;
        }
        if(active && attribute.getModifier(UNDERWATER)==null) attribute.addTransientModifier(new AttributeModifier(UNDERWATER,"Metallurgy Opistognathus",2.7,AttributeModifier.Operation.ADDITION));
        else if(!active && attribute.getModifier(UNDERWATER)!=null) attribute.removeModifier(UNDERWATER);
    }
    private TinkerIntegration() {}
}
