package it.hurts.metallurgy_reforged.fluid;

import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.fluids.*;
import java.util.*;
import java.util.function.Supplier;

public final class FluidContent {
    public static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES,"metallurgy");
    public static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,"metallurgy");
    public static final Map<String,Entry> ENTRIES=new LinkedHashMap<>();
    public static final class Entry {
        public final FluidCatalog.Definition definition;
        public Supplier<FluidType> type;
        public Supplier<? extends BaseFlowingFluid> still,flowing;
        public Supplier<LiquidBlock> block;
        public Supplier<Item> bucket;
        private Entry(FluidCatalog.Definition definition) {
            this.definition=definition;String id=definition.id();
            type=TYPES.register(id,()->new FluidType(FluidType.Properties.create().density(800).viscosity(4000)
                .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY,net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_LAVA)
                .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL,net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA)
                .temperature(definition.temperature()).lightLevel(definition.light()).canConvertToSource(false)
                .canHydrate(definition.tar()).canExtinguish(definition.tar()).canDrown(definition.tar()).canSwim(definition.tar()).supportsBoating(definition.tar())) {
                @Override public boolean isVaporizedOnPlacement(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,FluidStack stack) {
                    return !definition.tar() && level.dimensionType().ultraWarm();
                }

            });
            still=FLUIDS.register(id,()->new BaseFlowingFluid.Source(properties()));
            flowing=FLUIDS.register("flowing_"+id,()->new BaseFlowingFluid.Flowing(properties()));
            block=Content.BLOCKS.register(definition.block(),()->new MetallurgyLiquidBlock(this,BlockBehaviour.Properties.of()
                .noCollission().strength(100).noLootTable().replaceable().liquid().lightLevel(state->definition.light())));
            Content.BLOCK_MAP.put(definition.block(),()->block.get());
            bucket=Content.item(id+"_bucket",()->new MetalBucketItem(still.get(),new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
        }
        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type,still,flowing).bucket(bucket).block(block)
                .slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(20).explosionResistance(100);
        }
    }
    static {for(var definition:FluidCatalog.ALL) ENTRIES.put(definition.id(),new Entry(definition));}
    public static void register(IEventBus bus) {TYPES.register(bus);FLUIDS.register(bus);}
    private FluidContent() {}
}
