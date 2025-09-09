package com.swapnil.titlemod.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import java.util.function.Supplier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.sound.PositionedSoundInstance;
import com.swapnil.titlemod.TitleMod;
import org.joml.Matrix4f;

public class ModButtonWidget extends ButtonWidget {
    private final Identifier backgroundTexture;
    private boolean wasHovered = false; 
    private float externalAlpha = 1.0F;
    private float currentScale = 1.0F;
    private float externalScale = 1.0F;

    public ModButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, Identifier backgroundTexture) {
        super(x, y, width, height, message, onPress, (Supplier<MutableText> narrationSupplier) -> narrationSupplier.get());
        this.backgroundTexture = backgroundTexture;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
       

        float alphaMultiplier = 1.0F; 
        int textColor = 0xFFFFFF; 

        
        if (this.isHovered() && !wasHovered) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(TitleMod.BUTTON_HOVER_SOUND, 1.0F));
        }
        wasHovered = this.isHovered();

        if (this.active) {
            if (this.isHovered()) {
                
                alphaMultiplier = 0.9F; 
                textColor = 0xFFFFAA; 
            }
        } else {
            
            alphaMultiplier = 0.5F; 
            textColor = 0xA0A0A0; 
        }

        
        float targetScale = 1.0F;
        if (this.active && this.isHovered()) {
            targetScale = 1.03F;
        }
        currentScale += (targetScale - currentScale) * 0.2F;

        
        float renderScale = currentScale * externalScale;

        
        int margin = 6;
        int shadowOffset = 3;
        int shadowAlpha = (int)(0x30 * alphaMultiplier * externalAlpha) << 24 | 0x000000;
        int shadowX = getX() + shadowOffset - margin;
        int shadowY = getY() + shadowOffset - margin;
        int shadowW = getWidth() + margin * 2;
        int shadowH = getHeight() + margin * 2;
        context.fill(shadowX, shadowY, shadowX + shadowW, shadowY + shadowH, shadowAlpha);

        
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alphaMultiplier * externalAlpha)));
        RenderSystem.setShaderTexture(0, backgroundTexture);

        context.getMatrices().push();
        context.getMatrices().translate(getX() + getWidth() / 2.0F, getY() + getHeight() / 2.0F, 0);
        context.getMatrices().scale(renderScale, renderScale, 1.0F);
        context.getMatrices().translate(-(getX() + getWidth() / 2.0F), -(getY() + getHeight() / 2.0F), 0);

        
        context.drawTexture(backgroundTexture, getX() - margin, getY() - margin, 0, 0, getWidth() + margin * 2, getHeight() + margin * 2, getWidth() + margin * 2, getHeight() + margin * 2);

        
        if (this.active && this.isHovered()) {
            long t = System.currentTimeMillis() % 900L;
            float progress = (float)t / 900.0F; 
            int shineWidth = Math.max(8, (int)(getWidth() * 0.18f));
            int sx = getX() - shineWidth + (int)(progress * (getWidth() + shineWidth * 2));
            int sy = getY();
            int ex = sx + shineWidth;
            int ey = getY() + getHeight();
            int a = (int)(0x50 * externalAlpha) << 24; 
            int color = a | 0x00FFFFFF;
            
            context.fill(sx, sy, ex, ey, color);
        }

        
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2, 
                textColor);

        
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        context.getMatrices().pop();
    }

    public void setExternalAlpha(float alpha) {
        this.externalAlpha = alpha;
    }

    public void setExternalScale(float scale) {
        this.externalScale = scale;
    }
}
