package it.hurts.metallurgy_reforged.compat.hephaestus.mixin;

import it.hurts.metallurgy_reforged.compat.hephaestus.HephaestusTraits;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="com.titammods.hephaestus_tools.tools.helper.ToolDurability",remap=false)
public abstract class DurabilityMixin {
    @ModifyVariable(method="hurt",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private static int metallurgy$wear(int amount,ItemStack tool,int original,ServerLevel level,LivingEntity owner) {
        return HephaestusTraits.wear(tool,amount,level,owner);
    }
}
