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
    private boolean wasHovered = false; 
    private float externalAlpha = 1.0F;
    private float externalYOffset = 0.0F;
    private float externalXOffset = 0.0F;

    public TransparentButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, (Supplier<MutableText> narrationSupplier) -> narrationSupplier.get());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      



        int textColor = 0xFFFFFF; 
        int backgroundColor = 0x00000000; 

        
        if (this.isHovered() && !wasHovered) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(TitleMod.BUTTON_HOVER_SOUND, 1.0F));
        }
        wasHovered = this.isHovered();

        if (this.active) {
            if (this.isHovered()) {
               
                textColor = 0xFFFFAA;
                backgroundColor = 0x20FFFFFF; 

            }
        } else {
            textColor = 0x808080;
        }

        
        context.getMatrices().push();
        context.getMatrices().translate(externalXOffset, externalYOffset, 0);

        
        int finalBg = backgroundColor;
        if (finalBg != 0x00000000) {
            int a = (finalBg >>> 24) & 0xFF;
            int scaledA = Math.max(0, Math.min(255, (int)(a * externalAlpha)));
            finalBg = (scaledA << 24) | (finalBg & 0x00FFFFFF);
        }
        int finalText = textColor;
        {
            int baseA = 0xFF;
            int scaledA = Math.max(0, Math.min(255, (int)(baseA * externalAlpha)));
            finalText = (scaledA << 24) | (textColor & 0x00FFFFFF);
        }

     
        if (finalBg != 0x00000000) {
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), finalBg);
        }

        
        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                this.getX() + this.getWidth() / 2,
                this.getY() + (this.getHeight() - 8) / 2,
                finalText 
        );

        context.getMatrices().pop();
    }

    public void setExternalAlpha(float alpha) {
        this.externalAlpha = alpha;
    }

    public void setExternalYOffset(float yOffset) {
        this.externalYOffset = yOffset;
    }

    public void setExternalXOffset(float xOffset) {
        this.externalXOffset = xOffset;
    }
}
