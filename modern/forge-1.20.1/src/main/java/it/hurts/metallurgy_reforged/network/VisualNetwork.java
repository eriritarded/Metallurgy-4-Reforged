package it.hurts.metallurgy_reforged.network;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
public final class VisualNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("metallurgy","visuals"),()->"4","4"::equals,"4"::equals);
    private static final Map<ServerPlayer,Integer> SENT=Collections.synchronizedMap(new WeakHashMap<>());
    private record Height(int height) {}
    private record Activation(int entity,net.minecraft.world.item.ItemStack stack) {}
    public static void register() {
        CHANNEL.messageBuilder(Activation.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder((message,buffer)->{buffer.writeVarInt(message.entity);buffer.writeItem(message.stack);}).decoder(buffer->new Activation(buffer.readVarInt(),buffer.readItem()))
            .consumerMainThread((message,supplier)->{net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->it.hurts.metallurgy_reforged.client.ArmorActivation.show(message.entity,message.stack));supplier.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(it.hurts.metallurgy_reforged.visual.ExplosionVisual.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder((message,buffer)->message.write(buffer)).decoder(it.hurts.metallurgy_reforged.visual.ExplosionVisual::read)
            .consumerMainThread((message,supplier)->{net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->it.hurts.metallurgy_reforged.client.ExplosionParticles.burst(message));supplier.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Height.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((message,buffer)->buffer.writeVarInt(message.height)).decoder(buffer->new Height(buffer.readVarInt()))
            .consumerMainThread((message,supplier)->{net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->it.hurts.metallurgy_reforged.client.HudRenderer.height(message.height));supplier.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(it.hurts.metallurgy_reforged.visual.ParticleBurst.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((message,buffer)->message.write(buffer)).decoder(it.hurts.metallurgy_reforged.visual.ParticleBurst::read)
            .consumerMainThread((message,supplier)->{net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->it.hurts.metallurgy_reforged.client.OreParticles.burst(message));supplier.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(it.hurts.metallurgy_reforged.visual.SoundCue.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((message,buffer)->message.write(buffer)).decoder(it.hurts.metallurgy_reforged.visual.SoundCue::read)
            .consumerMainThread((message,supplier)->{net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->it.hurts.metallurgy_reforged.client.MachineAmbience.cue(message));supplier.get().setPacketHandled(true);}).add();
    }
    public static void height(ServerPlayer player,int height) {int value=Math.max(0,Math.min(27,height));Integer previous=SENT.put(player,value);if(previous==null || previous!=value) CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Height(value));}
    public static void clear(ServerPlayer player) {SENT.remove(player);}
    public static void activation(net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack) {if(entity instanceof ServerPlayer player) CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Activation(entity.getId(),stack.copy()));}
    public static void explosion(net.minecraft.server.level.ServerLevel level,it.hurts.metallurgy_reforged.visual.ExplosionVisual data) {if(data.valid()) for(var player:level.players()) if(player.distanceToSqr(data.x(),data.y(),data.z())<=4096) CHANNEL.send(PacketDistributor.PLAYER.with(()->player),data);}
    public static void burst(net.minecraft.server.level.ServerLevel level,it.hurts.metallurgy_reforged.visual.ParticleBurst data) {if(data.valid()) for(var player:level.players()) if(player.distanceToSqr(data.x(),data.y(),data.z())<=4096) CHANNEL.send(PacketDistributor.PLAYER.with(()->player),data);}
    public static void sound(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos,int kind) {var data=new it.hurts.metallurgy_reforged.visual.SoundCue(it.hurts.metallurgy_reforged.effect.VersionBridge.dimensionId(level),pos,kind);if(data.valid()) for(var player:level.players()) if(player.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<=4096) CHANNEL.send(PacketDistributor.PLAYER.with(()->player),data);}
    private VisualNetwork() {}
}
