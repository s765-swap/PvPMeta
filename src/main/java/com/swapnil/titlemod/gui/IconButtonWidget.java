package com.swapnil.titlemod.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.sound.PositionedSoundInstance;
import com.swapnil.titlemod.TitleMod;

public class IconButtonWidget extends TransparentButtonWidget {
    private final Identifier iconTexture;
    private boolean wasHovered = false; 
    private float currentScale = 1.0F;

    public IconButtonWidget(int x, int y, int width, int height, Identifier iconTexture, PressAction onPress) {
        
        super(x, y, width, height, Text.empty(), onPress);
        this.iconTexture = iconTexture;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
       
        if (this.isHovered() && !wasHovered) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(TitleMod.BUTTON_HOVER_SOUND, 1.0F));
        }
        wasHovered = this.isHovered();

     
        float red = 1.0F, green = 1.0F, blue = 1.0F, alpha = 0.8F; 

        if (this.active) {
            if (this.isHovered()) {
                
                red = 2.0F; 
                green = 2.0F;
                blue = 2.0F;
                alpha = 1.0F; 
            }
        } else {
           
            alpha = 0.4F; 
        }

       
        float targetScale = (this.active && this.isHovered()) ? 1.08F : 1.0F;
        currentScale += (targetScale - currentScale) * 0.25F;

        
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderSystem.setShaderTexture(0, iconTexture);

        
        context.getMatrices().push();
        context.getMatrices().translate(getX() + getWidth() / 2.0F, getY() + getHeight() / 2.0F, 0);
        context.getMatrices().scale(currentScale, currentScale, 1.0F);
        context.getMatrices().translate(-(getX() + getWidth() / 2.0F), -(getY() + getHeight() / 2.0F), 0);
        context.drawTexture(iconTexture, getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
        context.getMatrices().pop();

        
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }
}
