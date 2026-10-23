package it.hurts.metallurgy_reforged.fluid;

import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import net.minecraftforge.fluids.*;
import java.util.*;
import java.util.function.Supplier;

public final class FluidContent {
    public static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,"metallurgy");
    public static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,"metallurgy");
    public static final Map<String,Entry> ENTRIES=new LinkedHashMap<>();
    public static final class Entry {
        public final FluidCatalog.Definition definition;
        public Supplier<FluidType> type;
        public Supplier<? extends ForgeFlowingFluid> still,flowing;
        public Supplier<LiquidBlock> block;
        public Supplier<Item> bucket;
        private Entry(FluidCatalog.Definition definition) {
            this.definition=definition;String id=definition.id();
            type=TYPES.register(id,()->new FluidType(FluidType.Properties.create().density(800).viscosity(4000)
                .sound(net.minecraftforge.common.SoundActions.BUCKET_EMPTY,net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_LAVA)
                .sound(net.minecraftforge.common.SoundActions.BUCKET_FILL,net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA)
                .temperature(definition.temperature()).lightLevel(definition.light()).canConvertToSource(false)
                .canHydrate(definition.tar()).canExtinguish(definition.tar()).canDrown(definition.tar()).canSwim(definition.tar()).supportsBoating(definition.tar())) {
                @Override public boolean isVaporizedOnPlacement(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,FluidStack stack) {
                    return !definition.tar() && level.dimensionType().ultraWarm();
                }
                @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
                    consumer.accept(it.hurts.metallurgy_reforged.client.FluidRendering.extensions(definition));
                }
            });
            still=FLUIDS.register(id,()->new ForgeFlowingFluid.Source(properties()));
            flowing=FLUIDS.register("flowing_"+id,()->new ForgeFlowingFluid.Flowing(properties()));
            block=Content.BLOCKS.register(definition.block(),()->new MetallurgyLiquidBlock(this,BlockBehaviour.Properties.of()
                .noCollission().strength(100).noLootTable().replaceable().liquid().lightLevel(state->definition.light())));
            Content.BLOCK_MAP.put(definition.block(),()->block.get());
            bucket=Content.item(id+"_bucket",()->new MetalBucketItem(still.get(),new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
        }
        private ForgeFlowingFluid.Properties properties() {
            return new ForgeFlowingFluid.Properties(type,still,flowing).bucket(bucket).block(block)
                .slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(20).explosionResistance(100);
        }
    }
    static {for(var definition:FluidCatalog.ALL) ENTRIES.put(definition.id(),new Entry(definition));}
    public static void register(IEventBus bus) {TYPES.register(bus);FLUIDS.register(bus);}
    private FluidContent() {}
}
