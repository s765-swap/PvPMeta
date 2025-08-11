package com.swapnil.titlemod.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import java.util.function.Supplier;
import net.minecraft.client.sound.PositionedSoundInstance;
import com.swapnil.titlemod.TitleMod;

public class TransparentButtonWidget extends ButtonWidget {
    private boolean wasHovered = false; // Track hover state for sound

    public TransparentButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, (Supplier<MutableText> narrationSupplier) -> narrationSupplier.get());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // IMPORTANT: DO NOT call super.render() here, as it draws the default button background.
        // We are completely custom drawing this button.

        int textColor = 0xFFFFFF; // Default white text
        int backgroundColor = 0x00000000; // Fully transparent background by default

        // Play sound on hover entry
        if (this.isHovered() && !wasHovered) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(TitleMod.BUTTON_HOVER_SOUND, 1.0F));
        }
        wasHovered = this.isHovered();

        if (this.active) {
            if (this.isHovered()) {
                // Refined Hover Effect: Faint white overlay, subtle golden text
                textColor = 0xFFFFAA; // Subtle golden/yellow on hover
                backgroundColor = 0x20FFFFFF; // Faint white overlay on hover (20% opacity)
            }
        } else {
            textColor = 0x808080; // Gray out text if inactive
        }

        // Draw subtle background fill if it's not fully transparent (i.e., on hover)
        if (backgroundColor != 0x00000000) {
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), backgroundColor);
        }

        // Draw the button text centered
        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                this.getX() + this.getWidth() / 2,
                this.getY() + (this.getHeight() - 8) / 2,
                textColor // Use the determined text color
        );
    }
}
