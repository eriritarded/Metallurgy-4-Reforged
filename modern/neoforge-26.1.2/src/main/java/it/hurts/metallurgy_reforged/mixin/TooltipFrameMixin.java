package it.hurts.metallurgy_reforged.mixin;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import it.hurts.metallurgy_reforged.client.TooltipTint;
@Mixin(TooltipRenderUtil.class)
public abstract class TooltipFrameMixin {
    @Redirect(method="extractTooltipBackground",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private static void metallurgy$frame(GuiGraphicsExtractor graphics,RenderPipeline pipeline,Identifier sprite,int x,int y,int width,int height) {
        int color=sprite.equals(Identifier.fromNamespaceAndPath("metallurgy","tooltip/metal_frame"))?TooltipTint.color():0xffffffff;
        graphics.blitSprite(pipeline,sprite,x,y,width,height,color);
    }
    @Inject(method="extractTooltipBackground",at=@At("TAIL")) private static void metallurgy$clear(GuiGraphicsExtractor graphics,int x,int y,int width,int height,Identifier style,CallbackInfo callback) {TooltipTint.clear();}
}
