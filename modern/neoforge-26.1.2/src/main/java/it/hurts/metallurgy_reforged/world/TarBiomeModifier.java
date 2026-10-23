package it.hurts.metallurgy_reforged.world;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.*;
public record TarBiomeModifier(Holder<PlacedFeature> feature) implements BiomeModifier {
    public static final MapCodec<TarBiomeModifier> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(PlacedFeature.CODEC.fieldOf("feature").forGetter(TarBiomeModifier::feature)).apply(instance,TarBiomeModifier::new));
    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if(phase==Phase.ADD) builder.getGenerationSettings().addFeature(GenerationStep.Decoration.LAKES,feature);
    }
    @Override public MapCodec<? extends BiomeModifier> codec() {return CODEC;}
}
