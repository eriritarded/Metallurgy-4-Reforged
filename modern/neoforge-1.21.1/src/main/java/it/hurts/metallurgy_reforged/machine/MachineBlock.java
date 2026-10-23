package it.hurts.metallurgy_reforged.machine;
import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;


public final class MachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty BURNING=BooleanProperty.create("burning");
    public final boolean alloy;
    public MachineBlock(boolean alloy,Properties properties) { super(properties);this.alloy=alloy;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(BURNING,false)); }
    private static final com.mojang.serialization.MapCodec<MachineBlock> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance->instance.group(
        com.mojang.serialization.Codec.BOOL.fieldOf("alloy").forGetter(block->block.alloy),propertiesCodec()).apply(instance,MachineBlock::new));
    @Override protected com.mojang.serialization.MapCodec<MachineBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,BURNING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()); }
    @Override public BlockState rotate(BlockState state,Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state,Mirror mirror) { return rotate(state,mirror.getRotation(state.getValue(FACING))); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new MachineEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,Content.MACHINE_ENTITY.get(),(world,pos,block,machine)->machine.tick());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof MachineEntity machine)
            server.openMenu(machine,buf->{buf.writeBlockPos(pos);buf.writeBoolean(alloy);});
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moved) {
        if(!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof MachineEntity machine) {
            Containers.dropContents(level,pos,machine);level.updateNeighbourForOutputSignal(pos,this);
        }
        super.onRemove(state,level,pos,replacement,moved);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) { return AbstractContainerMenuSignal(level,pos); }
    private int AbstractContainerMenuSignal(Level level,BlockPos pos) { return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos)); }
}
