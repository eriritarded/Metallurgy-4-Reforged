package it.hurts.metallurgy_reforged.config;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.*;
public record ContentCondition(String content) implements ICondition {
    private static final MapCodec<ContentCondition> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(Codec.STRING.fieldOf("content").forGetter(ContentCondition::content)).apply(instance,ContentCondition::new));
    private static final DeferredRegister<MapCodec<? extends ICondition>> SERIALIZERS=DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS,"metallurgy");
    static {SERIALIZERS.register("content_enabled",()->CODEC);}
    public static void register(net.neoforged.bus.api.IEventBus bus) {SERIALIZERS.register(bus);}
    @Override public boolean test(IContext context) {return ContentAvailability.enabled(content);}
    @Override public MapCodec<? extends ICondition> codec() {return CODEC;}
}
