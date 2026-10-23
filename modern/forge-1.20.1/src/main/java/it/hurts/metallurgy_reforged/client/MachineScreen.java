package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final ResourceLocation texture;
    public MachineScreen(MachineMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);texture=new ResourceLocation("metallurgy","textures/gui/"+(menu.alloy?"alloyer":"crusher")+".png");
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial) { renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y); }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {
        int color=it.hurts.metallurgy_reforged.material.Materials.BY_NAME.get(menu.alloy?"osmium":"oureclase").color();
        graphics.drawString(font,title,10,-22,color,false);
    }
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y) {
        graphics.blit(texture,leftPos,topPos-31,0,0,175,196);
        int total=menu.data.get(1),burn=menu.data.get(0),progress=menu.data.get(2);
        if(burn>0) {
            int k=burn*(menu.alloy?17:16)/(total==0?140:total);
            graphics.blit(texture,leftPos+(menu.alloy?110:128),topPos+46-k,176,77-k,17,k+(menu.alloy?2:1));
        }
        int l=progress*33/140;
        if(l>0) {
            graphics.blit(texture,leftPos+(menu.alloy?40:93),topPos+66-l,176,32-l,7,l);
            if(menu.alloy) graphics.blit(texture,leftPos+52,topPos+1,176,81,10,1+progress*70/140);
            else graphics.blit(texture,leftPos+60,topPos+14,177,33,21,18);
        }
    }
}
