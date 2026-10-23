package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.network.ArmorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
/** Key handling is client-only; the server validates each requested action. */
@Mod.EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class ArmorInput {
    private static boolean held,grounded=true;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) { if(event.phase==TickEvent.Phase.END) update(); }
    private static void update() {
        MachineAmbience.tick();
        DetectorDisplay.tick();
        OreParticles.tick();
        Minecraft minecraft=Minecraft.getInstance();var player=minecraft.player;
        if(player==null || minecraft.screen!=null || minecraft.isPaused()) { held=false;grounded=true;return; }
        boolean jump=minecraft.options.keyJump.isDown();
        if(jump && !held && !grounded && !player.onGround() && it.hurts.metallurgy_reforged.effect.EquipmentEffects.armorLevel(player,"amordrine")>0) ArmorNetwork.send(0);
        held=jump;grounded=player.onGround();
        if(it.hurts.metallurgy_reforged.effect.EquipmentEffects.armorLevel(player,"krik")==4) {
            if(ArmorKeys.UP.isDown()) ArmorNetwork.send(1);else if(ArmorKeys.DOWN.isDown()) ArmorNetwork.send(2);
        }
    }
    private ArmorInput() {}
}
