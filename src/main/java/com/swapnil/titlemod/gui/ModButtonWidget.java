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

public class ModButtonWidget extends ButtonWidget {
    private final Identifier backgroundTexture;
    private boolean wasHovered = false; // Track hover state for sound

    public ModButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, Identifier backgroundTexture) {
        super(x, y, width, height, message, onPress, (Supplier<MutableText> narrationSupplier) -> narrationSupplier.get());
        this.backgroundTexture = backgroundTexture;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // IMPORTANT: DO NOT call super.render() here, as it draws the default button background.
        // We are completely custom drawing this button.

        float alphaMultiplier = 1.0F; // Multiplier for the texture's inherent alpha
        int textColor = 0xFFFFFF; // Default white text

        // Play sound on hover entry
        if (this.isHovered() && !wasHovered) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(TitleMod.BUTTON_HOVER_SOUND, 1.0F));
        }
        wasHovered = this.isHovered();

        if (this.active) {
            if (this.isHovered()) {
                // Refined Hover Effect: Subtle alpha increase, subtle golden text
                alphaMultiplier = 0.9F; // Slightly more opaque on hover
                textColor = 0xFFFFAA; // Subtle golden/yellow on hover
            }
        } else {
            // Inactive state: dim the button and text
            alphaMultiplier = 0.5F; // Dim the button if inactive
            textColor = 0xA0A0A0; // Gray out text if inactive
        }

        // Apply alpha multiplier to the texture drawing.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alphaMultiplier);
        RenderSystem.setShaderTexture(0, backgroundTexture);
        context.drawTexture(backgroundTexture, getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());

        // Draw the button text centered on top of the background
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2, // 8 is approximate text height
                textColor);

        // Reset shader color for subsequent renders to avoid affecting other UI elements
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
