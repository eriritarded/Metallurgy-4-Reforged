package it.hurts.metallurgy_reforged.compat.hephaestus.mixin;

import com.titammods.hephaestus_tools.materials.MaterialId;
import it.hurts.metallurgy_reforged.compat.hephaestus.HephaestusTraits;
import net.minecraft.network.chat.Component;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.titammods.hephaestus_tools.tools.helper.ToolTooltipBuilder",remap=false)
public abstract class PartTooltipMixin {
    @Inject(method="partStats",at=@At("RETURN"),cancellable=true,remap=false)
    private static void metallurgy$partTraits(MaterialId material,int role,CallbackInfoReturnable<List<Component>> callback) {
        var extra=HephaestusTraits.partLines(material,role);
        if(extra.isEmpty()) return;
        var lines=new ArrayList<>(callback.getReturnValue());lines.addAll(extra);callback.setReturnValue(lines);
    }
}
