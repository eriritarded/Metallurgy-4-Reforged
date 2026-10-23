package it.hurts.metallurgy_reforged.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;


/** Five native inventories in the single metallurgy mod. No doubled chest. */
public final class MetalChestBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public final int capacity;
    private static final VoxelShape SHAPE = Block.box(1,0,1,15,14,15);
    public MetalChestBlock(int capacity, Properties properties) {
        super(properties); this.capacity = capacity;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    private static final com.mojang.serialization.MapCodec<MetalChestBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
        com.mojang.serialization.Codec.INT.fieldOf("capacity").forGetter(block -> block.capacity), propertiesCodec()).apply(instance,MetalChestBlock::new));
    @Override protected com.mojang.serialization.MapCodec<MetalChestBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MetalChestEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type,it.hurts.metallurgy_reforged.content.Content.CHEST_ENTITY.get(),MetalChestEntity::clientTick) : null;
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) { if(level.getBlockEntity(pos) instanceof MetalChestEntity chest) chest.recheckOpeners(); }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int value) {
        super.triggerEvent(state,level,pos,id,value);
        return level.getBlockEntity(pos) instanceof MetalChestEntity chest && chest.triggerEvent(id,value);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof MetalChestEntity chest
                && !level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above())) {
            server.openMenu(chest, buf -> { buf.writeBlockPos(pos); buf.writeVarInt(capacity); });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MetalChestEntity chest) { Containers.dropContents(level, pos, chest); level.updateNeighbourForOutputSignal(pos, this); }
            super.onRemove(state, level, pos, next, moving);
        }
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MetalChestEntity chest ? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(chest) : 0;
    }
}
