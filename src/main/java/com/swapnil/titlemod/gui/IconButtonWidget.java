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
    private boolean wasHovered = false; // Track hover state for sound

    public IconButtonWidget(int x, int y, int width, int height, Identifier iconTexture, PressAction onPress) {
        // Pass empty text to super, as this button is primarily an icon
        super(x, y, width, height, Text.empty(), onPress);
        this.iconTexture = iconTexture;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Play sound on hover entry
        if (this.isHovered() && !wasHovered) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(TitleMod.BUTTON_HOVER_SOUND, 1.0F));
        }
        wasHovered = this.isHovered();

        // Determine icon color/alpha based on hover state
        float red = 1.0F, green = 1.0F, blue = 1.0F, alpha = 0.8F; // Default subtle transparency

        if (this.active) {
            if (this.isHovered()) {
                // EXAGGERATED ICON GLOW EFFECT FOR DEBUGGING: Make it very bright/white
                red = 2.0F; // Significantly over 1.0 for a very strong glow
                green = 2.0F;
                blue = 2.0F;
                alpha = 1.0F; // Fully opaque on hover
            }
        } else {
            // Inactive state: dim the icon
            alpha = 0.4F; // More transparent if inactive
        }

        // Apply color and alpha to the icon drawing
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderSystem.setShaderTexture(0, iconTexture);

        // Draw the icon within the button bounds
        context.drawTexture(iconTexture, getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());

        // Reset shader color to default for subsequent renders
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // IMPORTANT: DO NOT call super.render() here if you want *only* the icon to glow.
        // If super.render() is called, it would draw the TransparentButtonWidget's default background/overlay.
        // We are intentionally skipping it to ensure only the icon itself is affected.
    }
}
