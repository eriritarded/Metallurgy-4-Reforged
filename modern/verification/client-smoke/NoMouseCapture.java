package dev.miles.metallurgy.qa.mixin;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** QA only: suppress the cursor capture/warp path throughout the private-desktop run. */
@Mixin(MouseHandler.class)
abstract class NoMouseCapture {
    @Inject(method="grabMouse", at=@At("HEAD"), cancellable=true)
    private void neverCaptureUserMouse(CallbackInfo callback) { callback.cancel(); }
}
