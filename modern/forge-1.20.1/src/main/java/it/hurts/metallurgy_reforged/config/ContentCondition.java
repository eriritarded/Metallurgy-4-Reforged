package it.hurts.metallurgy_reforged.config;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.*;
public record ContentCondition(String content) implements ICondition {
    private static final ResourceLocation ID=new ResourceLocation("metallurgy","content_enabled");
    @Override public ResourceLocation getID() {return ID;}
    @Override public boolean test(IContext context) {return ContentAvailability.enabled(content);}
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->CraftingHelper.register(new IConditionSerializer<ContentCondition>() {
            @Override public void write(JsonObject json,ContentCondition value) {json.addProperty("content",value.content);}
            @Override public ContentCondition read(JsonObject json) {return new ContentCondition(json.get("content").getAsString());}
            @Override public ResourceLocation getID() {return ID;}
        })));
    }
}
