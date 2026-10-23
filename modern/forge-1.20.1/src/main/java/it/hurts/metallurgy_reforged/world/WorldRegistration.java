package it.hurts.metallurgy_reforged.world;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.*;
public final class WorldRegistration {
    private static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,"metallurgy");
    static {SERIALIZERS.register("tar_dimensions",()->TarBiomeModifier.CODEC);SERIALIZERS.register("ore_realms",()->OreBiomeModifier.CODEC);}
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {SERIALIZERS.register(bus);RetrogenEvents.register(bus);}
    private WorldRegistration() {}
}
