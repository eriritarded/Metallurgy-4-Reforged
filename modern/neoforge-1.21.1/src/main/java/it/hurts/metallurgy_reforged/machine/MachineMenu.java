package it.hurts.metallurgy_reforged.machine;
import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
public final class MachineMenu extends AbstractContainerMenu {
    public final Container machine;
    public final boolean alloy;
    public final BlockPos position;
    public final ContainerData data;
    public MachineMenu(int id,Inventory inventory,Container machine,BlockPos pos,boolean alloy,ContainerData data) {
        super(Content.MACHINE_MENU.get(),id);this.machine=machine;this.alloy=alloy;this.position=pos;this.data=data;
        checkContainerSize(machine,alloy?4:5);checkContainerDataCount(data,3);
        if(alloy) { addSlot(new Slot(machine,0,102,-8));addSlot(new Slot(machine,1,123,-8));addSlot(fuel(inventory,2,111,48));addSlot(result(inventory,3,57,42)); }
        else { addSlot(new Slot(machine,0,61,-4));addSlot(fuel(inventory,1,129,48));addSlot(result(inventory,2,67,36));addSlot(result(inventory,3,48,36));addSlot(result(inventory,4,29,36)); }
        for(int y=0;y<3;y++) for(int x=0;x<9;x++) addSlot(new Slot(inventory,x+y*9+9,8+x*18,84+y*18));
        for(int x=0;x<9;x++) addSlot(new Slot(inventory,x,8+x*18,142));
        addDataSlots(data);
    }
    private Slot fuel(Inventory inventory,int slot,int x,int y) { return new Slot(machine,slot,x,y) { @Override public boolean mayPlace(ItemStack stack) { return MachineBridge.fuel(inventory.player.level(),stack)>0 || stack.is(Items.BUCKET); } }; }
    private Slot result(Inventory inventory,int slot,int x,int y) {
        return new Slot(machine,slot,x,y) {
            private int removed;
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public ItemStack remove(int count) { removed+=Math.min(count,getItem().getCount());return super.remove(count); }
            @Override protected void onQuickCraft(ItemStack stack,int count) { removed+=count; }
            @Override public void onTake(Player player,ItemStack stack) {float xp=machine instanceof MachineEntity entity?entity.takeExperience(slot,stack,removed):0;MachineBridge.experience(player,stack,removed,xp);removed=0;super.onTake(player,stack);}
        };
    }
    public static MachineMenu fromNetwork(int id,Inventory inventory,FriendlyByteBuf buf) {
        BlockPos pos=buf.readBlockPos();boolean alloy=buf.readBoolean();
        return new MachineMenu(id,inventory,new SimpleContainer(alloy?4:5),pos,alloy,new SimpleContainerData(3));
    }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player) && player.distanceToSqr(position.getCenter())<=64; }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();int size=machine.getContainerSize();
        if(index<size) { if(!moveItemStackTo(stack,size,slots.size(),true)) return ItemStack.EMPTY;if(index>=(alloy?3:2)) slot.onQuickCraft(stack,original); }
        else {
            boolean input=it.hurts.metallurgy_reforged.recipe.ProcessingAccess.input(player.level(),alloy,stack);
            if(input) { if(!moveItemStackTo(stack,0,alloy?2:1,false)) return ItemStack.EMPTY; }
            else if(MachineBridge.fuel(player.level(),stack)>0) { if(!moveItemStackTo(stack,alloy?2:1,alloy?3:2,false)) return ItemStack.EMPTY; }
            else if(index<size+27) { if(!moveItemStackTo(stack,size+27,size+36,false)) return ItemStack.EMPTY; }
            else if(!moveItemStackTo(stack,size,size+27,false)) return ItemStack.EMPTY;
        }
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player,original.copyWithCount(original.getCount()-stack.getCount()));return original;
    }
}
