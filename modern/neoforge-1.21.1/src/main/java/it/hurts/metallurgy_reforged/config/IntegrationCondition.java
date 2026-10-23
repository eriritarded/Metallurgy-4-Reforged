package it.hurts.metallurgy_reforged.config;
import com.mojang.serialization.*;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.*;
public record IntegrationCondition(String integration) implements ICondition {
    private static final MapCodec<IntegrationCondition> CODEC=Codec.STRING.fieldOf("integration").xmap(IntegrationCondition::new,IntegrationCondition::integration);
    private static final DeferredRegister<MapCodec<? extends ICondition>> SERIALIZERS=DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS,"metallurgy");
    static {SERIALIZERS.register("integration_enabled",()->CODEC);}
    public static void register(net.neoforged.bus.api.IEventBus bus) {SERIALIZERS.register(bus);}
    @Override public boolean test(IContext context) {return IntegrationFlags.enabled(integration);}
    @Override public MapCodec<? extends ICondition> codec() {return CODEC;}
}
