package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.gadget.ShieldLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class GadgetVisuals {
    private static int sprintTimer;
    @SubscribeEvent public static void hidden(RenderPlayerEvent.Pre<?> event) {var level=Minecraft.getInstance().level;if(level!=null && level.getEntity(event.getRenderState().id) instanceof net.minecraft.world.entity.LivingEntity entity && ShieldLogic.invisible(entity)) event.setCanceled(true);}
    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {it.hurts.metallurgy_reforged.gadget.RoadMovement.input(event.getEntity(),event.getInput().getMoveVector().lengthSquared()>0);}
    // InputEvent is an abstract grouping type and cannot be registered with the event bus.
    // Mouse Post and Key run after native key mappings have received the physical input.
    @SubscribeEvent public static void attackMouse(InputEvent.MouseButton.Post event) { attack(); }
    @SubscribeEvent public static void attackKey(InputEvent.Key event) { attack(); }
    private static void attack() {
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
