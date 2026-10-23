package it.hurts.metallurgy_reforged.inventory;

import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class MetalChestMenu extends AbstractContainerMenu {
    private final Container chest;
    public final int capacity, columns, rows;
    public final BlockPos position;
    public MetalChestMenu(int id, Inventory inventory, Container chest, BlockPos pos) {
        super(Content.CHEST_MENU.get(), id);
        this.chest=chest; this.capacity=chest.getContainerSize(); this.position=pos;
        this.columns = capacity == 90 ? 10 : capacity == 108 ? 12 : 9; this.rows = capacity / columns;
        chest.startOpen(inventory.player);
        for(int y=0;y<rows;y++) for(int x=0;x<columns;x++) addSlot(new Slot(chest,x+y*columns,8+x*18,20+y*18));
        int offset = 20 + rows*18 + 14;
        int playerX=8+(columns-9)*9;
        for(int y=0;y<3;y++) for(int x=0;x<9;x++) addSlot(new Slot(inventory,x+y*9+9,playerX+x*18,offset+y*18));
        for(int x=0;x<9;x++) addSlot(new Slot(inventory,x,playerX+x*18,offset+58));
    }
    public static MetalChestMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buf) {
        BlockPos pos=buf.readBlockPos(); int capacity=buf.readVarInt();
        if (!Content.CHEST_SIZES.containsValue(capacity)) throw new IllegalArgumentException("Invalid chest size");
        return new MetalChestMenu(id, inventory, new SimpleContainer(capacity), pos);
    }
    @Override public boolean stillValid(Player player) { return chest.stillValid(player); }
    public boolean isChest(MetalChestEntity entity) { return chest == entity; }
    @Override public void removed(Player player) { super.removed(player); chest.stopOpen(player); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        Slot slot=slots.get(index); if(!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(index<capacity) { if(!moveItemStackTo(stack,capacity,slots.size(),true)) return ItemStack.EMPTY; }
        else if(!moveItemStackTo(stack,0,capacity,false)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if(stack.getCount()==original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player,stack); return original;
    }
}
