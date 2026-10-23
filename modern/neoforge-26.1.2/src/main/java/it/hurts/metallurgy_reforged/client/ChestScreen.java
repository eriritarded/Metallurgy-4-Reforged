package it.hurts.metallurgy_reforged.client;
import it.hurts.metallurgy_reforged.inventory.MetalChestMenu;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
public final class ChestScreen extends AbstractContainerScreen<MetalChestMenu> {
    private final String metal;
    private final Identifier texture;
    private final int textureHeight;
    public ChestScreen(MetalChestMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,menu.columns*18+14,menu.rows*18+116);
        metal=switch(menu.capacity) {case 54->"brass";case 72->"silver";case 81->"electrum";case 90->"rubracium";case 108->"platinum";default->throw new IllegalArgumentException("Chest tier");};
        texture=Identifier.fromNamespaceAndPath("metallurgychests","textures/gui/"+metal+"_chest.png");
        textureHeight=imageHeight>256?imageHeight:256;
        titleLabelX=10;titleLabelY=9;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int x,int y,float partial) {
        super.extractBackground(graphics,x,y,partial);
        graphics.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos,topPos,0,0,imageWidth,imageHeight,256,textureHeight);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics,int x,int y) {
        graphics.text(font,title,titleLabelX,titleLabelY,0xff000000|Materials.BY_NAME.get(metal).color(),false);
    }
}
