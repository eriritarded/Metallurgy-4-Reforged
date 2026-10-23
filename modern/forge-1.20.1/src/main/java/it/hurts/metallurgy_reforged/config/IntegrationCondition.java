package it.hurts.metallurgy_reforged.config;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.*;
public record IntegrationCondition(String integration) implements ICondition {
    private static final ResourceLocation ID=new ResourceLocation("metallurgy","integration_enabled");
    @Override public ResourceLocation getID() {return ID;}
    @Override public boolean test(IContext context) {return IntegrationFlags.enabled(integration);}
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->CraftingHelper.register(new IConditionSerializer<IntegrationCondition>() {
            @Override public void write(JsonObject json,IntegrationCondition value) {json.addProperty("integration",value.integration);}
            @Override public IntegrationCondition read(JsonObject json) {return new IntegrationCondition(json.get("integration").getAsString());}
            @Override public ResourceLocation getID() {return ID;}
        })));
    }
}
