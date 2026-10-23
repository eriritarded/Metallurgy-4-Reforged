package it.hurts.metallurgy_reforged.machine;
import it.hurts.metallurgy_reforged.content.Content;
import it.hurts.metallurgy_reforged.recipe.ProcessingAccess;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import java.util.*;

/** Original no-menu chamber: exact block batches, fuel-paused effect clock,
 * nearby-player aura and a stateful dropped block item. */
public final class ChamberEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
    public int fuelTime,activeTime,fullDuration,recipeAmplifier;
    public String effect="";
    private final Set<UUID> affected=new HashSet<>();
    private final LazyOptional<IItemHandlerModifiable>[] handlers=SidedInvWrapper.create(this,Direction.UP,Direction.DOWN,Direction.NORTH);
    public ChamberEntity(BlockPos pos,BlockState state) { super(Content.CHAMBER_ENTITY.get(),pos,state); }
    @Override public int getContainerSize() { return 2; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items=value; }
    @Override protected Component getDefaultName() { return Component.translatable("container.chamber"); }
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) { return null; }
    @Override public int[] getSlotsForFace(Direction side) { return side==Direction.UP?new int[]{0}:new int[]{1}; }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side) { return canPlaceItem(slot,stack); }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side) { return slot==1; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {
        if(slot==1) return MachineBridge.fuel(level,stack)>0;
        var recipe=ProcessingAccess.chamber(level,stack,false);return recipe!=null&&getItem(0).getCount()<recipe.amounts().get(0);
    }
    public boolean insert(Player player,InteractionHand hand,Direction face) {
        if(face==null) return false;
        ItemStack incoming=player.getItemInHand(hand);if(incoming.isEmpty()) return false;
        int slot=face==Direction.UP?0:1;if(face==Direction.DOWN) return false;
        ItemStack stored=getItem(slot);int limit;
        if(!stored.isEmpty() && !ItemStack.isSameItemSameTags(stored,incoming)) return false;
        if(slot==0) { var recipe=ProcessingAccess.chamber(level,incoming,false);if(recipe==null) return false;limit=recipe.amounts().get(0); }
        else { if(MachineBridge.fuel(level,incoming)<=0) return false;limit=incoming.getMaxStackSize(); }
        int amount=Math.min(incoming.getCount(),limit-stored.getCount());if(amount<=0) return false;
        ItemStack inserted=incoming.split(amount);if(stored.isEmpty()) items.set(slot,inserted);else stored.grow(amount);
        if(slot==0) VersionBridge.sound(player,"block.metal.place",1,1);
        changed();return true;
    }
    private void active(boolean value) {
        if(getBlockState().getValue(ChamberBlock.LIT)!=value) { level.setBlock(worldPosition,getBlockState().setValue(ChamberBlock.LIT,value),3);changed(); }
    }
    private void changed() { setChanged();if(level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    public void clearEffects() {
        if(level instanceof ServerLevel server && !effect.isEmpty()) for(UUID id:affected) {
            Player player=server.getServer().getPlayerList().getPlayer(id);
            if(player==null) player=server.getPlayerByUUID(id);
            if(player!=null) VersionBridge.removeBuff(player,effect);
        }
        affected.clear();setChanged();
    }
    public void tick() {
        if(level!=null && level.isClientSide) {if(getBlockState().getValue(ChamberBlock.LIT) && activeTime>0 && activeTime<fullDuration) activeTime++;return;}
        if(!(level instanceof ServerLevel server)) return;
        var recipe=effect.isEmpty()?ProcessingAccess.chamber(level,getItem(0),true):null;
        boolean valid=!effect.isEmpty() || recipe!=null;
        if(fuelTime>0) fuelTime--;
        int fuel=MachineBridge.fuel(level,getItem(1));
        if(fuelTime>0) {
            if(valid) {
                if(activeTime==0) {
                    activeTime=1;
                    if(effect.isEmpty()) { effect=recipe.effect();fullDuration=recipe.duration();recipeAmplifier=recipe.amplifier(); }
                    active(true);ChamberBridge.sound(this,"windup");changed();
                } else {
                    if(!getBlockState().getValue(ChamberBlock.LIT)) { active(true);ChamberBridge.sound(this,"windup"); }
                    if(activeTime<fullDuration) {
                        activeTime++;
                        if(activeTime%300==30) it.hurts.metallurgy_reforged.network.VisualNetwork.sound(server,worldPosition,2);
                        Vec3 origin=Vec3.atLowerCornerOf(worldPosition);
                        for(Player player:server.getEntitiesOfClass(Player.class,new AABB(origin,origin).inflate(6))) {
                            if(VersionBridge.amplifier(player,effect)<0) {
                                // The original update method intentionally used
                                // the two-argument PotionEffect constructor:
                                // all applied auras are amplifier0.
                                VersionBridge.buff(player,effect,fullDuration-activeTime,0);affected.add(player.getUUID());
                            }
                        }
                    } else {
                        items.set(0,ItemStack.EMPTY);effect="";activeTime=0;fullDuration=0;active(false);affected.clear();changed();
                    }
                }
            }
        } else if(fuel>0 && valid) {
            ItemStack input=getItem(1),remainder=MachineBridge.remainder(input);input.shrink(1);fuelTime=fuel;
            if(input.isEmpty()) items.set(1,remainder);changed();
        } else { if(!affected.isEmpty()) clearEffects();if(getBlockState().getValue(ChamberBlock.LIT)) it.hurts.metallurgy_reforged.network.VisualNetwork.sound(server,worldPosition,-1);active(false); }
        if(fuelTime>0 || activeTime>0) {setChanged();if(level.getGameTime()%20==0) changed();}
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);items=NonNullList.withSize(2,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items);
        fuelTime=tag.getInt("fuel_time");activeTime=tag.getInt("active_time");fullDuration=tag.getInt("full_duration");recipeAmplifier=tag.getInt("recipe_amplifier");effect=tag.getString("effect");
        affected.clear();for(String id:tag.getString("affected_players").split(",")) if(!id.isEmpty()) try { affected.add(UUID.fromString(id)); } catch(IllegalArgumentException ignored) {}
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);tag.putInt("fuel_time",fuelTime);tag.putInt("active_time",activeTime);tag.putInt("full_duration",fullDuration);tag.putInt("recipe_amplifier",recipeAmplifier);tag.putString("effect",effect);
        tag.putString("affected_players",affected.stream().map(UUID::toString).sorted().collect(java.util.stream.Collectors.joining(",")));
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side) {
        if(capability==ForgeCapabilities.ITEM_HANDLER && side!=null) return handlers[side==Direction.UP?0:side==Direction.DOWN?1:2].cast();return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps();for(var handler:handlers) handler.invalidate(); }
}
