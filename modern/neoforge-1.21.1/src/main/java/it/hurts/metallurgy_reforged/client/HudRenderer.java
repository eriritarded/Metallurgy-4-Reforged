package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.effect.EquipmentEffects;
import it.hurts.metallurgy_reforged.machine.ChamberEntity;
import it.hurts.metallurgy_reforged.material.MaterialItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
@EventBusSubscriber(modid="metallurgy",value=Dist.CLIENT)
public final class HudRenderer {
    private static final ResourceLocation KRIK=ResourceLocation.fromNamespaceAndPath("metallurgy","textures/gui/krik_overlay.png");
    private static java.util.UUID owner;
    private static int selectedHeight;
    public static void height(int value) {var player=Minecraft.getInstance().player;if(player!=null) {owner=player.getUUID();selectedHeight=Math.max(0,Math.min(27,value));}}
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();var player=mc.player;
        if(player==null || mc.level==null || mc.options.hideGui || mc.screen instanceof ChatScreen) return;
        var graphics=event.getGuiGraphics();int width=graphics.guiWidth(),height=graphics.guiHeight();
        if(EquipmentEffects.armorLevel(player,"krik")==4) {
            int selected=player.getUUID().equals(owner)?selectedHeight:0,maximum=0;
            for(int slot=9;slot<36;slot++) if(player.getInventory().getItem(slot).isEmpty()) maximum++;
            blit(graphics,KRIK,width-15,height-60,0,0,8,58,128,128);
            if(selected>0) blit(graphics,KRIK,width-13,height-4-selected*2,10,0,4,selected*2,128,128);
            blit(graphics,KRIK,width-13,height-6-maximum*2,22,0,4,2,128,128);
        }
        if(mc.hitResult instanceof BlockHitResult hit && mc.level.getBlockEntity(hit.getBlockPos()) instanceof ChamberEntity chamber) {
            int x=width/2,y=height/2;var metal=chamber.getItem(0);var fuel=chamber.getItem(1);
            if(!metal.isEmpty()) {item(graphics,metal,x-40,y-7);text(graphics,Integer.toString(metal.getCount()),x-50,y-3,0xffffffff);}
            if(!fuel.isEmpty()) {item(graphics,fuel,x-5,y+17);text(graphics,Integer.toString(fuel.getCount()),x-15,y+20,0xffffffff);}
            if(!chamber.effect.isEmpty()) {
                var material=MaterialItems.material(metal);int color=material==null?0xffffffff:0xff000000|material.color();
                graphics.fill(x+30,y-10,x+54,y+14,0xc0202020);effect(graphics,chamber.effect,x+33,y-7);
                text(graphics,chamber.activeTime/20+" "+Component.translatable("gui.jei_compat.sublimation_chamber.seconds").getString(),x-33,y-30,color);
                text(graphics,effectName(chamber.effect).getString(),x+30,y+25,color);
            }
        }
    }
    private static Component effectName(String name) {var key=it.hurts.metallurgy_reforged.effect.VersionBridge.effectKey(name);var effect=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(key);return effect==null?Component.literal(name):Component.translatable(effect.getDescriptionId());}
    private static void effect(GuiGraphics graphics,String name,int x,int y) {var key=it.hurts.metallurgy_reforged.effect.VersionBridge.effectKey(name);blit(graphics,ResourceLocation.fromNamespaceAndPath(key.getNamespace(),"textures/mob_effect/"+key.getPath()+".png"),x,y,0,0,18,18,18,18);}
    private static void blit(GuiGraphics graphics,ResourceLocation texture,int x,int y,int u,int v,int width,int height,int textureWidth,int textureHeight) {graphics.blit(texture,x,y,u,v,width,height,textureWidth,textureHeight);}
    private static void text(GuiGraphics graphics,String value,int x,int y,int color) {graphics.drawString(Minecraft.getInstance().font,value,x,y,color,true);}
    private static void item(GuiGraphics graphics,net.minecraft.world.item.ItemStack stack,int x,int y) {graphics.renderItem(stack,x,y);}
    private HudRenderer() {}
}
