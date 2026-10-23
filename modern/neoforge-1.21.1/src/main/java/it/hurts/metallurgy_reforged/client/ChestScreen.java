package it.hurts.metallurgy_reforged.client;

import it.hurts.metallurgy_reforged.inventory.MetalChestMenu;
import it.hurts.metallurgy_reforged.material.Materials;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Native screen dimensions and slot locations from the five original GUIs. */
public final class ChestScreen extends AbstractContainerScreen<MetalChestMenu> {
    private final String metal;
    private final ResourceLocation texture;
    private final int textureHeight;
    public ChestScreen(MetalChestMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title);
        metal=switch(menu.capacity) { case 54->"brass";case 72->"silver";case 81->"electrum";case 90->"rubracium";case 108->"platinum";default->throw new IllegalArgumentException("Chest tier"); };
        imageWidth=menu.columns*18+14;
        imageHeight=menu.rows*18+116;
        textureHeight=imageHeight>256?imageHeight:256;
        texture=ResourceLocation.fromNamespaceAndPath("metallurgychests","textures/gui/"+metal+"_chest.png");
        titleLabelX=10;titleLabelY=9;
        inventoryLabelY=imageHeight-94;
    }
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y) {
        graphics.blit(texture,leftPos,topPos,0,0,imageWidth,imageHeight,256,textureHeight);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {
        graphics.drawString(font,title,titleLabelX,titleLabelY,Materials.BY_NAME.get(metal).color(),false);
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial) {
        renderBackground(graphics,x,y,partial);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);
    }
}
