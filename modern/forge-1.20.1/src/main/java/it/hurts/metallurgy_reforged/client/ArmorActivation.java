package it.hurts.metallurgy_reforged.client;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
/** The native activation animation displays the armor stack that was sacrificed. */
public final class ArmorActivation {
    public static void show(int entityId,ItemStack stack) {var mc=Minecraft.getInstance();if(mc.player!=null && mc.player.getId()==entityId && !stack.isEmpty()) mc.gameRenderer.displayItemActivation(stack);}
    private ArmorActivation() {}
}
