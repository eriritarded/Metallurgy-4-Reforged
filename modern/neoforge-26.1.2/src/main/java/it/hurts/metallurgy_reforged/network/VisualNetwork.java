package it.hurts.metallurgy_reforged.network;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
public final class VisualNetwork {
    private static final Map<ServerPlayer,Integer> SENT=Collections.synchronizedMap(new WeakHashMap<>());
    public record Debris(it.hurts.metallurgy_reforged.visual.ExplosionVisual data) implements CustomPacketPayload {
        public static final Type<Debris> TYPE=new Type<>(Identifier.fromNamespaceAndPath("metallurgy","explosion_particles"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Debris> CODEC=StreamCodec.of((buffer,message)->message.data.write(buffer),buffer->new Debris(it.hurts.metallurgy_reforged.visual.ExplosionVisual.read(buffer)));
        @Override public Type<Debris> type() {return TYPE;}
    }
    public record Activation(int entity,net.minecraft.world.item.ItemStack stack) implements CustomPacketPayload {
        public static final Type<Activation> TYPE=new Type<>(Identifier.fromNamespaceAndPath("metallurgy","armor_activation"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Activation> CODEC=StreamCodec.of((buffer,message)->{buffer.writeVarInt(message.entity);net.minecraft.world.item.ItemStack.STREAM_CODEC.encode(buffer,message.stack);},buffer->new Activation(buffer.readVarInt(),net.minecraft.world.item.ItemStack.STREAM_CODEC.decode(buffer)));
        @Override public Type<Activation> type() {return TYPE;}
    }
    public record Height(int height) implements CustomPacketPayload {
        public static final Type<Height> TYPE=new Type<>(Identifier.fromNamespaceAndPath("metallurgy","krik_height"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Height> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Height::height,Height::new);
        @Override public Type<Height> type() {return TYPE;}
    }
    public record Particles(it.hurts.metallurgy_reforged.visual.ParticleBurst data) implements CustomPacketPayload {
        public static final Type<Particles> TYPE=new Type<>(Identifier.fromNamespaceAndPath("metallurgy","ore_particles"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Particles> CODEC=StreamCodec.of((buffer,message)->message.data.write(buffer),buffer->new Particles(it.hurts.metallurgy_reforged.visual.ParticleBurst.read(buffer)));
        @Override public Type<Particles> type() {return TYPE;}
    }
    public record Sound(it.hurts.metallurgy_reforged.visual.SoundCue data) implements CustomPacketPayload {
        public static final Type<Sound> TYPE=new Type<>(Identifier.fromNamespaceAndPath("metallurgy","machine_sound"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Sound> CODEC=StreamCodec.of((buffer,message)->message.data.write(buffer),buffer->new Sound(it.hurts.metallurgy_reforged.visual.SoundCue.read(buffer)));
        @Override public Type<Sound> type() {return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("4").playToClient(Activation.TYPE,Activation.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.ArmorActivation.show(message.entity,message.stack)));
        event.registrar("4").playToClient(Debris.TYPE,Debris.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.ExplosionParticles.burst(message.data)));
        event.registrar("4").playToClient(Height.TYPE,Height.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.HudRenderer.height(message.height)));
        event.registrar("4").playToClient(Particles.TYPE,Particles.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.OreParticles.burst(message.data)));
        event.registrar("4").playToClient(Sound.TYPE,Sound.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.client.MachineAmbience.cue(message.data)));
    }
    public static void height(ServerPlayer player,int height) {int value=Math.max(0,Math.min(27,height));Integer previous=SENT.put(player,value);if(previous==null || previous!=value) PacketDistributor.sendToPlayer(player,new Height(value));}
    public static void explosion(net.minecraft.server.level.ServerLevel level,it.hurts.metallurgy_reforged.visual.ExplosionVisual data) {if(data.valid()) for(var player:level.players()) if(player.distanceToSqr(data.x(),data.y(),data.z())<=4096) PacketDistributor.sendToPlayer(player,new Debris(data));}
    public static void activation(net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack) {if(entity instanceof ServerPlayer player) PacketDistributor.sendToPlayer(player,new Activation(entity.getId(),stack.copy()));}
    public static void clear(ServerPlayer player) {SENT.remove(player);}
    public static void burst(net.minecraft.server.level.ServerLevel level,it.hurts.metallurgy_reforged.visual.ParticleBurst data) {if(data.valid()) for(var player:level.players()) if(player.distanceToSqr(data.x(),data.y(),data.z())<=4096) PacketDistributor.sendToPlayer(player,new Particles(data));}
    public static void sound(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos,int kind) {var data=new it.hurts.metallurgy_reforged.visual.SoundCue(it.hurts.metallurgy_reforged.effect.VersionBridge.dimensionId(level),pos,kind);if(data.valid()) for(var player:level.players()) if(player.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<=4096) PacketDistributor.sendToPlayer(player,new Sound(data));}
    private VisualNetwork() {}
}
