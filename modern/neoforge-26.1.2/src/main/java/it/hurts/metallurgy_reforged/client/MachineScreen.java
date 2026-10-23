package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final Identifier texture;
    public MachineScreen(MachineMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,176,166);texture=Identifier.fromNamespaceAndPath("metallurgy","textures/gui/"+(menu.alloy?"alloyer":"crusher")+".png");
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics,int x,int y) {
        int color=it.hurts.metallurgy_reforged.material.Materials.BY_NAME.get(menu.alloy?"osmium":"oureclase").color();
        graphics.text(font,title,10,-22,0xff000000|color,false);
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int x,int y,float partial) {
        super.extractBackground(graphics,x,y,partial);
        graphics.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos,topPos-31,0,0,175,196,256,256);
        int total=menu.data.get(1),burn=menu.data.get(0),progress=menu.data.get(2);
        if(burn>0) {
            int k=burn*(menu.alloy?17:16)/(total==0?140:total);
            graphics.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+(menu.alloy?110:128),topPos+46-k,176,77-k,17,k+(menu.alloy?2:1),256,256);
        }
        int l=progress*33/140;
        if(l>0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+(menu.alloy?40:93),topPos+66-l,176,32-l,7,l,256,256);
            if(menu.alloy) graphics.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+52,topPos+1,176,81,10,1+progress*70/140,256,256);
            else graphics.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+60,topPos+14,177,33,21,18,256,256);
        }
    }
}
