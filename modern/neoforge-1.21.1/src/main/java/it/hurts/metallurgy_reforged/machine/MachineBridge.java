package it.hurts.metallurgy_reforged.machine;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import it.hurts.metallurgy_reforged.content.Content;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
public final class MachineBridge {
    public static int fuel(Level level,ItemStack stack) {
        return switch(VersionBridge.itemId(stack)) {
            case "metallurgy:tar"->800;case "metallurgy:charcoal_block"->16000;
            case "metallurgy:thermite_dust"->it.hurts.metallurgy_reforged.config.GadgetsConfig.ThermiteDust.thermiteFuelValue*200;case "metallurgy:ignatius_fuel"->it.hurts.metallurgy_reforged.config.GadgetsConfig.IgnatiusFuel.ignatiusFuelValue*200;
            default->stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
        };
    }
    public static ItemStack remainder(ItemStack stack) { return stack.hasCraftingRemainingItem()?stack.getCraftingRemainingItem():ItemStack.EMPTY; }
    public static void sound(MachineEntity machine,String suffix) {
        var sound=Content.SOUND_MAP.get((machine.alloy?"alloyer":"crusher")+"_"+suffix);
        if(sound!=null) machine.getLevel().playSound(null,machine.getBlockPos(),sound.get(),SoundSource.BLOCKS,1,1);
    }
    public static void experience(Player player,ItemStack output,int count,float amount) {
        if(player.level().isClientSide) return;
        int xp=(int)amount;
        if(Math.random()<amount-xp) xp++;
        while(xp>0) { int split=ExperienceOrb.getExperienceValue(xp);xp-=split;player.level().addFreshEntity(new ExperienceOrb(player.level(),player.getX(),player.getY()+0.5,player.getZ()+0.5,split)); }
        output.onCraftedBy(player.level(),player,count);
        net.neoforged.neoforge.event.EventHooks.firePlayerSmeltedEvent(player,output);
    }
    private MachineBridge() {}
}
