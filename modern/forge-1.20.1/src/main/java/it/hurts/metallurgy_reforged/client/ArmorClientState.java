package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.effect.ArmorPhasing;
import net.minecraft.client.Minecraft;
public final class ArmorClientState {
    public static void phase(boolean active) { var player=Minecraft.getInstance().player;if(player!=null) ArmorPhasing.clientFlag(player,active); }
    public static void corpse(int entityId,int stage) { var player=Minecraft.getInstance().player;if(player!=null) it.hurts.metallurgy_reforged.effect.CorpseLifecycle.applyClient(player,entityId,stage); }
    private ArmorClientState() {}
}
