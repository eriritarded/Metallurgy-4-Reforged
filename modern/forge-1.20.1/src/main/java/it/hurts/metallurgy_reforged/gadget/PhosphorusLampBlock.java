package it.hurts.metallurgy_reforged.gadget;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
public final class PhosphorusLampBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public PhosphorusLampBlock(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(FACING);}
    private boolean supported(LevelReader level,BlockPos pos,Direction facing) {
        BlockPos support=pos.relative(facing.getOpposite());return level.hasChunkAt(support) && level.getBlockState(support).isCollisionShapeFullBlock(level,support);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {return defaultBlockState().setValue(FACING,supported(context.getLevel(),context.getClickedPos(),context.getClickedFace())?context.getClickedFace():Direction.UP);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return switch(state.getValue(FACING)) {
            case NORTH->Shapes.box(0.3,0.09,0.45,0.7,0.6,0.85);case SOUTH->Shapes.box(0.3,0.09,0.15,0.7,0.6,0.55);
            case WEST->Shapes.box(0.45,0.09,0.3,0.85,0.6,0.7);case EAST->Shapes.box(0.15,0.09,0.3,0.55,0.6,0.7);
            case UP->Shapes.box(0.30,0,0.30,0.69,0.5,0.69);case DOWN->Shapes.box(0.30,0.13,0.30,0.69,0.63,0.69);
        };
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return it.hurts.metallurgy_reforged.config.GadgetsConfig.PhosphorusLamp.enableLanternCollision?getShape(state,level,pos,context):Shapes.empty();}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved) {
        if(level instanceof ServerLevel server) {LampRange.add(server,pos);server.scheduleTick(pos,this,1);}
    }
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {level.scheduleTick(pos,this,1);return state;}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!level.hasChunkAt(pos.relative(state.getValue(FACING).getOpposite()))) {level.scheduleTick(pos,this,20);return;}
        if(!supported(level,pos,state.getValue(FACING))) {
            // Reuse native falling-block persistence, placement, drops and networking.
            LampRange.remove(level,pos);net.minecraft.world.entity.item.FallingBlockEntity.fall(level,pos,state.setValue(FACING,Direction.UP));
        }
    }
    @Override public BlockState rotate(BlockState state,Rotation rotation) {return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror) {return state.rotate(mirror.getRotation(state.getValue(FACING)));}
}
