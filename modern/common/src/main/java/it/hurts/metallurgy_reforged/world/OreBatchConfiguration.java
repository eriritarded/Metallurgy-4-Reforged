package it.hurts.metallurgy_reforged.world;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
public record OreBatchConfiguration(String dimension) implements FeatureConfiguration {
    public static final Codec<OreBatchConfiguration> CODEC=RecordCodecBuilder.create(instance->instance.group(Codec.STRING.fieldOf("dimension").forGetter(OreBatchConfiguration::dimension)).apply(instance,OreBatchConfiguration::new));
}
