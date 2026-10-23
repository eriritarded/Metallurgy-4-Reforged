package it.hurts.metallurgy_reforged.inventory;

import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

public final class MetalChestEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items;
    private LazyOptional<IItemHandler> automation = LazyOptional.of(() -> new InvWrapper(this));
    private final ChestLidController lid = new ChestLidController();
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        protected void onOpen(Level level, BlockPos pos, BlockState state) { level.playSound(null,pos,SoundEvents.CHEST_OPEN,SoundSource.BLOCKS,0.5F,level.random.nextFloat()*0.1F+0.9F); }
        protected void onClose(Level level, BlockPos pos, BlockState state) { level.playSound(null,pos,SoundEvents.CHEST_CLOSE,SoundSource.BLOCKS,0.5F,level.random.nextFloat()*0.1F+0.9F); }
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state,int previous,int current) { level.blockEvent(pos,state.getBlock(),1,current); }
        protected boolean isOwnContainer(Player player) { return player.containerMenu instanceof MetalChestMenu menu && menu.isChest(MetalChestEntity.this); }
    };
    public MetalChestEntity(BlockPos pos, BlockState state) {
        super(Content.CHEST_ENTITY.get(), pos, state);
        items = NonNullList.withSize(((MetalChestBlock)state.getBlock()).capacity, ItemStack.EMPTY);
    }
    @Override public int getContainerSize() { return items.size(); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new MetalChestMenu(id, inventory, this, worldPosition); }
    @Override public void load(CompoundTag tag) {
        super.load(tag); items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items);
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return !isRemoved() && capability == ForgeCapabilities.ITEM_HANDLER ? automation.cast() : super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); automation.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); automation = LazyOptional.of(() -> new InvWrapper(this)); }
    @Override public void startOpen(Player player) { if(!isRemoved() && !player.isSpectator()) openers.incrementOpeners(player,level,worldPosition,getBlockState()); }
    @Override public void stopOpen(Player player) { if(!isRemoved() && !player.isSpectator()) openers.decrementOpeners(player,level,worldPosition,getBlockState()); }
    public void recheckOpeners() { if(!isRemoved()) openers.recheckOpeners(level,worldPosition,getBlockState()); }
    @Override public boolean triggerEvent(int id,int value) { if(id==1) { lid.shouldBeOpen(value>0);return true; } return super.triggerEvent(id,value); }
    public float openness(float partial) { return lid.getOpenness(partial); }
    public static void clientTick(Level level,BlockPos pos,BlockState state,MetalChestEntity chest) { chest.lid.tickLid(); }
}
