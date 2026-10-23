package it.hurts.metallurgy_reforged.recipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
public final class ProcessingRegistration {
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,"metallurgy");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,"metallurgy");
    public static final Supplier<RecipeType<ProcessingRecipe>> TYPE=TYPES.register("processing",()->new RecipeType<ProcessingRecipe>() {@Override public String toString() {return "metallurgy:processing";}});
    public static final Supplier<RecipeSerializer<ProcessingRecipe>> SERIALIZER=SERIALIZERS.register("processing",ProcessingRecipe.Serializer::create);
    public static void register(net.neoforged.bus.api.IEventBus bus) {TYPES.register(bus);SERIALIZERS.register(bus);}
    private ProcessingRegistration() {}
}
