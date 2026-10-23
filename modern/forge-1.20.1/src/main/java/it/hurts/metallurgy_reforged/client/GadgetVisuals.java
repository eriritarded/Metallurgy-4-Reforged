package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.gadget.ShieldLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.*;
@Mod.EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class GadgetVisuals {
    private static int sprintTimer;
    @SubscribeEvent public static void hidden(RenderPlayerEvent.Pre event) {if(ShieldLogic.invisible(event.getEntity())) event.setCanceled(true);}
    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        it.hurts.metallurgy_reforged.gadget.RoadMovement.input(event.getEntity(),event.getInput().forwardImpulse!=0 || event.getInput().leftImpulse!=0);
        if(sprintTimer>0) sprintTimer--;
        if(!ShieldLogic.buckler(event.getEntity().getUseItem())) return;
        var input=event.getInput();input.forwardImpulse*=5;input.leftImpulse*=5;
        if(input.forwardImpulse>0.8) {
            if(sprintTimer!=0 || Minecraft.getInstance().options.keySprint.isDown()) event.getEntity().setSprinting(true);
        } else sprintTimer=7;
    }
    @SubscribeEvent public static void attack(InputEvent event) {
        var mc=Minecraft.getInstance();var player=mc.player;
        if(player==null || mc.gameMode==null || mc.screen!=null || !ShieldLogic.buckler(player.getUseItem())) return;
        while(mc.options.keyAttack.consumeClick()) {
            if(mc.hitResult instanceof EntityHitResult hit) mc.gameMode.attack(player,hit.getEntity());
            else if(mc.hitResult!=null && mc.hitResult.getType()==HitResult.Type.MISS) player.resetAttackStrengthTicker();
            player.swing(InteractionHand.MAIN_HAND);
        }
    }
    private GadgetVisuals() {}
}
