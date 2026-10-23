package it.hurts.metallurgy_reforged.gadget;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
public final class RoadBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public RoadBlock(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    private static final com.mojang.serialization.MapCodec<RoadBlock> CODEC=simpleCodec(RoadBlock::new);
    @Override protected com.mojang.serialization.MapCodec<RoadBlock> codec() {return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {return defaultBlockState().setValue(FACING,context.getHorizontalDirection());}
    @Override public BlockState rotate(BlockState state,Rotation rotation) {return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror) {return state.rotate(mirror.getRotation(state.getValue(FACING)));}
}
