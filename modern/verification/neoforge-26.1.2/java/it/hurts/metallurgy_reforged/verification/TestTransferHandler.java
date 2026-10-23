package it.hurts.metallurgy_reforged.verification;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
/** Test adapter exercises native committed and rolled-back transactions. */
public record TestTransferHandler(ResourceHandler<ItemResource> handler) {
    public TestTransferHandler { java.util.Objects.requireNonNull(handler,"Missing native item capability"); }
    public int getSlots() { return handler.size(); }
    public ItemStack extractItem(int slot,int amount,boolean simulate) {
        var resource=handler.getResource(slot);
        try(var transaction=Transaction.openRoot()) {
            int extracted=handler.extract(slot,resource,amount,transaction);
            if(!simulate) transaction.commit();return resource.toStack(extracted);
        }
    }
    public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
        try(var transaction=Transaction.openRoot()) {
            int inserted=handler.insert(slot,ItemResource.of(stack),stack.getCount(),transaction);
            if(!simulate) transaction.commit();return stack.copyWithCount(stack.getCount()-inserted);
        }
    }
}
