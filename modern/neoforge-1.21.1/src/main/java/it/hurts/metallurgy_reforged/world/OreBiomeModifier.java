package it.hurts.metallurgy_reforged.world;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.*;
/** Dimension eligibility is decided by the feature, including custom realms. */
public record OreBiomeModifier(Holder<PlacedFeature> feature) implements BiomeModifier {
    public static final MapCodec<OreBiomeModifier> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(PlacedFeature.CODEC.fieldOf("feature").forGetter(OreBiomeModifier::feature)).apply(instance,OreBiomeModifier::new));
    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if(phase==Phase.ADD) builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES,feature);
    }
    @Override public MapCodec<? extends BiomeModifier> codec() {return CODEC;}
}
