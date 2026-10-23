package it.hurts.metallurgy_reforged.network;
import it.hurts.metallurgy_reforged.effect.ArmorControls;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class ArmorNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("metallurgy","armor_actions"),()->"3","3"::equals,"3"::equals);
    private record Action(int id) {}
    private record Phase(boolean active) {}
    private record Corpse(int entityId,int stage) {}
    public static void register() {
        CHANNEL.messageBuilder(Action.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((message,buffer)->buffer.writeVarInt(message.id)).decoder(buffer->new Action(buffer.readVarInt()))
            .consumerMainThread((message,supplier)->{ var context=supplier.get();if(context.getSender()!=null) ArmorControls.action(context.getSender(),message.id);context.setPacketHandled(true); }).add();
        CHANNEL.messageBuilder(Phase.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((message,buffer)->buffer.writeBoolean(message.active)).decoder(buffer->new Phase(buffer.readBoolean()))
            .consumerMainThread((message,supplier)->{ net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->it.hurts.metallurgy_reforged.client.ArmorClientState.phase(message.active));supplier.get().setPacketHandled(true); }).add();
        CHANNEL.messageBuilder(Corpse.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((message,buffer)->{buffer.writeVarInt(message.entityId);buffer.writeVarInt(message.stage);}).decoder(buffer->new Corpse(buffer.readVarInt(),buffer.readVarInt()))
            .consumerMainThread((message,supplier)->{ net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->it.hurts.metallurgy_reforged.client.ArmorClientState.corpse(message.entityId,message.stage));supplier.get().setPacketHandled(true); }).add();
    }
    public static void send(int action) { CHANNEL.sendToServer(new Action(action)); }
    public static void phase(net.minecraft.server.level.ServerPlayer player,boolean active) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Phase(active)); }
    public static void corpse(net.minecraft.world.entity.LivingEntity entity,int stage) { CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->entity),new Corpse(entity.getId(),stage)); }
    public static void corpse(net.minecraft.server.level.ServerPlayer player,net.minecraft.world.entity.LivingEntity entity,int stage) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Corpse(entity.getId(),stage)); }
    private ArmorNetwork() {}
}
