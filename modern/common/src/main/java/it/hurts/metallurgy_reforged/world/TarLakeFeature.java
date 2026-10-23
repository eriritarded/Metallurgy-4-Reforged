package it.hurts.metallurgy_reforged.world;
import it.hurts.metallurgy_reforged.fluid.FluidContent;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/** Original20-in-500 attempts and0..255 sampling, with the target's native
 * lake carving/terrain validation instead of copying old generator internals. */
public final class TarLakeFeature extends Feature<NoneFeatureConfiguration> {
    public TarLakeFeature() {super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level();var random=context.random();
        if(!java.util.Arrays.asList(it.hurts.metallurgy_reforged.config.WorldGenerationConfig.tarLakeDimensionWhiteList).contains(VersionBridge.dimensionId(world.getLevel())) || random.nextInt(500)+1>it.hurts.metallurgy_reforged.config.WorldGenerationConfig.tarLakePercentage) return false;
        BlockPos probe=context.origin().offset(8+random.nextInt(16),random.nextInt(256)-context.origin().getY(),8+random.nextInt(16));
        while(probe.getY()>0 && world.isEmptyBlock(probe)) probe=probe.below();
        if(probe.getY()<=4) return false;
        var fluid=FluidContent.ENTRIES.get("molten_tar").block.get().defaultBlockState();
        var configuration=new LakeFeature.Configuration(BlockStateProvider.simple(fluid),BlockStateProvider.simple(Blocks.AIR));
        return Feature.LAKE.place(configuration,world,context.chunkGenerator(),random,probe.offset(-8,0,-8));
    }
}
