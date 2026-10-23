package it.hurts.metallurgy_reforged.machine;
import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;
public final class ChamberBlock extends BaseEntityBlock {
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {it.hurts.metallurgy_reforged.client.ChamberParticles.animate(level,pos,random);}
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public ChamberBlock(Properties properties) { super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,false)); }
    private static final com.mojang.serialization.MapCodec<ChamberBlock> CODEC=simpleCodec(ChamberBlock::new);
    @Override protected com.mojang.serialization.MapCodec<ChamberBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()); }
    @Override public BlockState rotate(BlockState state,Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state,Mirror mirror) { return rotate(state,mirror.getRotation(state.getValue(FACING))); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ChamberEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return createTickerHelper(type,Content.CHAMBER_ENTITY.get(),(world,pos,block,chamber)->chamber.tick()); }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder params) { return List.of(); }
}
