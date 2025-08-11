package com.swapnil.titlemod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TitleScreenOverlay extends Screen {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final float iconOpacity = 0.4f; // 40% opacity

    public TitleScreenOverlay() {
        super(Text.literal("Custom Title Screen"));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        int x = 20;
        int y = 20;
        int width = 32;
        int height = 32;

        // Opacity logic: 0.4 = 40%
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, iconOpacity);

        for (int i = 1; i <= 7; i++) {
            Identifier icon = new Identifier("yourmodid", "textures/gui/pvp_" + i + ".png");
            drawContext.drawTexture(icon, x + (i * 40), y, 0, 0, width, height, width, height);
        }

        // Reset opacity to default after drawing
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
