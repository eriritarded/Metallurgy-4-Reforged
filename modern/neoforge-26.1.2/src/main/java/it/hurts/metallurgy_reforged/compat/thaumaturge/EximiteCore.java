package it.hurts.metallurgy_reforged.compat.thaumaturge;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Eximite is an End metal. Alienis is not a vis type, so the rod draws on the End instead: like the primal staff it
 * slowly tops up a random primal that is below a tenth of capacity, and it does so three times as fast in the End.
 */
final class EximiteCore implements IWandRodOnUpdate {
    @Override
    public void onUpdate(ItemStack wand, Player player) {
        if (player.level().isClientSide()) return;
        boolean end = player.level().dimension() == Level.END;
        int interval = Math.max(1, (int) ThaumaturgeTuning.get(end ? "eximite_rod.end_interval_ticks" : "eximite_rod.interval_ticks"));
        if (player.tickCount % interval != 0) return;
        int floor = WandVisHelper.capacityOf(wand) / 10;
        List<ResourceKey<IAspect>> low = TTAspects.PRIMALS.stream().filter(primal -> WandVisHelper.storedIn(wand, primal) < floor).toList();
        if (!low.isEmpty()) WandVisHelper.topUp(wand, low.get(player.getRandom().nextInt(low.size())), 1, true);
    }
}
