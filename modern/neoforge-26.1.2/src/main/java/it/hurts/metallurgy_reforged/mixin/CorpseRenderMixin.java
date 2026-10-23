package it.hurts.metallurgy_reforged.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.metallurgy_reforged.client.CorpseRenderData;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntityRenderer.class)
public abstract class CorpseRenderMixin {
    @Unique private final java.util.Map<LivingEntityRenderState,Boolean> metallurgy$corpses=new java.util.WeakHashMap<>();
    @Shadow protected abstract int getModelTint(LivingEntityRenderState state);
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void metallurgy$extract(LivingEntity entity,LivingEntityRenderState state,float partial,CallbackInfo callback) {metallurgy$corpses.put(state,CorpseRenderData.active(entity));}
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("HEAD"))
    private void metallurgy$shake(LivingEntityRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo callback) {if(Boolean.TRUE.equals(metallurgy$corpses.get(state))) pose.translate(CorpseRenderData.jitter(),0,CorpseRenderData.jitter());}
    @Redirect(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getModelTint(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)I"))
    private int metallurgy$tint(LivingEntityRenderer<?,?,?> renderer,LivingEntityRenderState state) {int original=getModelTint(state);return Boolean.TRUE.equals(metallurgy$corpses.get(state))?CorpseRenderData.tint(original):original;}
}
