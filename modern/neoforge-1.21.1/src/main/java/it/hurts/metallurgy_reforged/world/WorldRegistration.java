package it.hurts.metallurgy_reforged.world;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.*;
public final class WorldRegistration {
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS=DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,"metallurgy");
    static {SERIALIZERS.register("tar_dimensions",()->TarBiomeModifier.CODEC);SERIALIZERS.register("ore_realms",()->OreBiomeModifier.CODEC);}
    public static void register(net.neoforged.bus.api.IEventBus bus) {SERIALIZERS.register(bus);RetrogenEvents.register(bus);}
    private WorldRegistration() {}
}
