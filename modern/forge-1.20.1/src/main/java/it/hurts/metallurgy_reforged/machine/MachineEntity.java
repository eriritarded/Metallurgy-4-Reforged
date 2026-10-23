package it.hurts.metallurgy_reforged.machine;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.recipe.ProcessingAccess;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import java.util.function.Consumer;

public final class MachineEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items;
    public final boolean alloy;
    public int burnTime,totalBurnTime,progress,fuelBoost=1,ambience;
    public static Consumer<MachineEntity> clientTick=machine->{};
    private final LazyOptional<IItemHandlerModifiable>[] handlers=SidedInvWrapper.create(this,Direction.UP,Direction.DOWN,Direction.NORTH);
    private final LazyOptional<Object> heater=LazyOptional.of(this::externalHeater);
    private Object heatAdapter;
    private final MachineExperience experience=new MachineExperience();
    public float takeExperience(int slot,ItemStack stack,int removed) {if(!(level instanceof net.minecraft.server.level.ServerLevel)) return 0;float amount=experience.claim(slot,stack,getItem(slot).getCount(),removed,alloy);setChanged();return amount;}
    public Object externalHeater() {if(heatAdapter==null) heatAdapter=it.hurts.metallurgy_reforged.compat.ExternalHeat.wrap(this);return heatAdapter;}
    public MachineEntity(BlockPos pos,BlockState state) { super(Content.MACHINE_ENTITY.get(),pos,state);alloy=((MachineBlock)state.getBlock()).alloy;items=NonNullList.withSize(alloy?4:5,ItemStack.EMPTY); }
    @Override public int getContainerSize() { return alloy?4:5; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items=value; }
    @Override protected Component getDefaultName() { return Component.translatable(alloy?"container.alloyer":"container.crusher"); }
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) { return new MachineMenu(id,inventory,this,worldPosition,alloy,data); }
    public int fuelSlot() { return alloy?2:1; }
    public int firstOutput() { return alloy?3:2; }
    public final ContainerData data=new ContainerData() {
        public int get(int id) { return switch(id) {case 0->burnTime;case 1->totalBurnTime;case 2->progress;default->0;}; }
        public void set(int id,int value) { switch(id) {case 0->burnTime=value;case 1->totalBurnTime=value;case 2->progress=value;} }
        public int getCount() { return 3; }
    };
    @Override public void setItem(int slot,ItemStack stack) {
        ItemStack previous=getItem(slot);
        if(slot<fuelSlot() && !ItemStack.isSameItemSameTags(previous,stack)) progress=0;
        super.setItem(slot,stack);
    }
    @Override public int[] getSlotsForFace(Direction side) { return side==Direction.UP?(alloy?new int[]{0,1}:new int[]{0}):side==Direction.DOWN?(alloy?new int[]{3}:new int[]{2,3,4}):new int[]{fuelSlot()}; }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction direction) { return canPlaceItem(slot,stack); }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction direction) { return slot>=firstOutput(); }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {
        if(slot>=firstOutput()) return false;
        if(slot==fuelSlot()) return MachineBridge.fuel(level,stack)>0 || stack.is(Items.BUCKET)&&!getItem(slot).is(Items.BUCKET);
        if(!alloy) return ProcessingAccess.input(level,false,stack);
        if(!ProcessingAccess.input(level,true,stack)) return false;
        ItemStack current=getItem(slot),other=getItem(1-slot);
        if(!current.isEmpty()) return ItemStack.isSameItemSameTags(current,stack) && current.getCount()<current.getMaxStackSize();
        return other.isEmpty() || ProcessingAccess.pair(level,stack,other);
    }
    private MachineRecipes.Result recipe() { return ProcessingAccess.result(level,alloy,getItem(0),alloy?getItem(1):ItemStack.EMPTY); }
    private int outputSlot(MachineRecipes.Result recipe) {
        if(recipe==null || getItem(0).getCount()<recipe.first() || alloy&&getItem(1).getCount()<recipe.second()) return -1;
        ItemStack output=recipe.output();
        for(int i=firstOutput();i<getContainerSize();i++) {
            ItemStack slot=getItem(i);
            if(slot.isEmpty() || ItemStack.isSameItemSameTags(slot,output) && slot.getCount()+output.getCount()<=Math.min(64,slot.getMaxStackSize())) return i;
        }
        return -1;
    }
    public int externalHeat(int available,boolean redstone,int baseCost,int speedCost) {
        if(!(level instanceof net.minecraft.server.level.ServerLevel) || available<=0) return 0;
        boolean process=outputSlot(recipe())>=0;if(!process && !redstone) return 0;
        int cost=Math.max(1,baseCost);if(available<cost) return 0;
        totalBurnTime=111;burnTime=(int)Math.min(111L,Math.max(0,burnTime)+2L);fuelBoost=1;
        long speed=(long)cost+Math.max(0,speedCost);
        if(process && !redstone && burnTime==111 && progress<140 && speed<=available) {progress++;cost=(int)speed;}
        if(!getBlockState().getValue(MachineBlock.BURNING)) level.setBlock(worldPosition,getBlockState().setValue(MachineBlock.BURNING,true),3);
        setChanged();return cost;
    }
    public void tick() {
        if(level==null) return;
        if(level.isClientSide) { clientTick.accept(this);return; }
        if(!(level instanceof net.minecraft.server.level.ServerLevel server)) return;
        boolean burning=burnTime>0;
        var recipe=recipe();int output=outputSlot(recipe);
        if(burning) {
            burnTime--;
            if(output>=0) {
                if(ambience==0) MachineBridge.sound(this,"windup");
                ambience++;progress=Math.min(140,progress+Math.max(1,fuelBoost));
                if(ambience%(alloy?370:330)==(alloy?90:120)) it.hurts.metallurgy_reforged.network.VisualNetwork.sound(server,worldPosition,alloy?1:0);
                if(progress>=140) {
                    ItemStack existing=getItem(output);
                    experience.produced(output,existing,recipe.output(),recipe.xp(),alloy);
                    if(existing.isEmpty()) items.set(output,recipe.output().copy());else existing.grow(recipe.output().getCount());
                    getItem(0).shrink(recipe.first());if(alloy) getItem(1).shrink(recipe.second());
                    progress=0;MachineBridge.sound(this,"impact");
                }
            } else if(progress==0 && ambience!=0) {ambience=0;it.hurts.metallurgy_reforged.network.VisualNetwork.sound(server,worldPosition,-1);}
        } else {
            ItemStack fuel=getItem(fuelSlot());int value=MachineBridge.fuel(level,fuel);
            if(!fuel.isEmpty() && output>=0 && value>0) {
                String id=it.hurts.metallurgy_reforged.effect.VersionBridge.itemId(fuel);
                fuelBoost=id.equals("metallurgy:thermite_dust")?2:id.equals("metallurgy:ignatius_fuel")?3:1;
                totalBurnTime=burnTime=value*140/200;
                ItemStack remainder=MachineBridge.remainder(fuel);fuel.shrink(1);
                if(fuel.isEmpty()) items.set(fuelSlot(),remainder);
            }
            progress=Math.max(0,progress-(alloy?3:2));
        }
        if(burning!=(burnTime>0)) level.setBlock(worldPosition,getBlockState().setValue(MachineBlock.BURNING,burnTime>0),3);
        if(burning || burnTime>0 || progress>0) setChanged();
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);items=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items);
        burnTime=tag.getInt("burn_time");totalBurnTime=tag.getInt("total_burn_time");progress=tag.getInt("progress");fuelBoost=tag.getInt("fuel_boost");ambience=tag.getInt("ambience_tick");
        experience.load(tag.getString("experience_ledger"));
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);
        tag.putInt("burn_time",burnTime);tag.putInt("total_burn_time",totalBurnTime);tag.putInt("progress",progress);tag.putInt("fuel_boost",fuelBoost);tag.putInt("ambience_tick",ambience);
        tag.putString("experience_ledger",experience.save());
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side) {
        if(side==null && capability==it.hurts.metallurgy_reforged.compat.ExternalHeat.capability()) return heater.cast();
        if(capability==ForgeCapabilities.ITEM_HANDLER && side!=null) return handlers[side==Direction.UP?0:side==Direction.DOWN?1:2].cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps();for(var handler:handlers) handler.invalidate();heater.invalidate(); }
}
