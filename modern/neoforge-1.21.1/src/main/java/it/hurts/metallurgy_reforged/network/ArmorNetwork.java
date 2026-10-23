package it.hurts.metallurgy_reforged.network;
import it.hurts.metallurgy_reforged.effect.ArmorControls;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
public final class ArmorNetwork {
    public record Action(int id) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","armor_actions"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Action::id,Action::new);
        @Override public Type<Action> type() { return TYPE; }
    }
    public record Phase(boolean active) implements CustomPacketPayload {
        public static final Type<Phase> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","etherium_phase"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Phase> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,Phase::active,Phase::new);
        @Override public Type<Phase> type() { return TYPE; }
    }
    public record Corpse(int entityId,int stage) implements CustomPacketPayload {
        public static final Type<Corpse> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metallurgy","corpse_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Corpse> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Corpse::entityId,ByteBufCodecs.VAR_INT,Corpse::stage,Corpse::new);
        @Override public Type<Corpse> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("3").playToServer(Action.TYPE,Action.CODEC,(message,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer player) ArmorControls.action(player,message.id());}));
        event.registrar("3").playToClient(Phase.TYPE,Phase.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.effect.ArmorPhasing.clientFlag(context.player(),message.active())));
        event.registrar("3").playToClient(Corpse.TYPE,Corpse.CODEC,(message,context)->context.enqueueWork(()->it.hurts.metallurgy_reforged.effect.CorpseLifecycle.applyClient(context.player(),message.entityId(),message.stage())));
    }
    public static void send(int action) { PacketDistributor.sendToServer(new Action(action)); }
    public static void phase(ServerPlayer player,boolean active) { PacketDistributor.sendToPlayer(player,new Phase(active)); }
    public static void corpse(net.minecraft.world.entity.LivingEntity entity,int stage) { PacketDistributor.sendToPlayersTrackingEntity(entity,new Corpse(entity.getId(),stage)); }
    public static void corpse(ServerPlayer player,net.minecraft.world.entity.LivingEntity entity,int stage) { PacketDistributor.sendToPlayer(player,new Corpse(entity.getId(),stage)); }
    private ArmorNetwork() {}
}
