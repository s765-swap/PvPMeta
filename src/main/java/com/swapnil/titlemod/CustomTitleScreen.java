package com.swapnil.titlemod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.swapnil.titlemod.gui.IconButtonWidget;
import com.swapnil.titlemod.gui.ModButtonWidget;
import com.swapnil.titlemod.gui.TransparentButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CustomTitleScreen extends Screen {
    private static final Identifier COMMON_BACKGROUND_TEXTURE = new Identifier("titlemod", "textures/gui/background.png");
    private static final Identifier SNOWFLAKE = new Identifier("titlemod", "textures/particles/snowflake.png");
    private static final Identifier ICON_SETTINGS = new Identifier("titlemod", "textures/gui/settings.png");
    private static final Identifier ICON_LEADERBOARD = new Identifier("titlemod", "textures/gui/leaderboard.png");
    private static final Identifier ICON_CLOSE = new Identifier("titlemod", "textures/gui/close.png");
    private static final Identifier BUTTON_BACKGROUND = new Identifier("titlemod", "textures/gui/buttonbg.png");
    private static final Identifier GLASS_BACKGROUND = new Identifier("titlemod", "textures/gui/glassbg.png");

    private final List<Particle> particles = new ArrayList<>();
    private final int iconY = 10;
    private final int iconSize = 20;

    public CustomTitleScreen() {
        super(Text.literal(""));
    }

    @Override
    protected void init() {
        // Start music using the new MusicManager
        MusicManager.startMusic("menu_theme");

        particles.clear();
        for (int i = 0; i < 100; i++) {
            particles.add(new Particle(this.width, this.height, true));
        }

        int spacing = 25;
        int leftX = 20;
        int buttonWidth = 120;
        int buttonHeight = 20;
        int startY = this.height / 2 - spacing;

        // Main action buttons
        addTransparentButtonWithGlassHover("Play PvP", leftX, startY, buttonWidth, buttonHeight,
                btn -> MinecraftClient.getInstance().setScreen(new PvPModeScreen(this))
        );
        addTransparentButtonWithGlassHover("menu.singleplayer", leftX, startY + spacing, buttonWidth, buttonHeight,
                btn -> MinecraftClient.getInstance().setScreen(new SelectWorldScreen(this))
        );
        addTransparentButtonWithGlassHover("menu.multiplayer", leftX, startY + spacing * 2, buttonWidth, buttonHeight,
                btn -> MinecraftClient.getInstance().setScreen(new MultiplayerScreen(this))
        );
        addTransparentButtonWithGlassHover("Kit Editor", leftX, startY + spacing * 3, buttonWidth, buttonHeight,
                btn -> MinecraftClient.getInstance().setScreen(new KitEditorScreen(this))
        );

        // Quit Button
        this.addDrawableChild(new TransparentButtonWidget(this.width / 2 - 45, this.height - 30, 90, 20,
                Text.literal("Quit"),
                btn -> MinecraftClient.getInstance().scheduleStop()
        ));

        // Top Bar Elements
        int padding = 10;
        int currentX = padding;

        // Settings Icon Button
        this.addDrawableChild(new IconButtonWidget(currentX, iconY, iconSize, iconSize, ICON_SETTINGS,
                btn -> MinecraftClient.getInstance().setScreen(new OptionsScreen(this, MinecraftClient.getInstance().options))
        ));
        currentX += iconSize + 8;

        // Leaderboard Icon Button
        this.addDrawableChild(new IconButtonWidget(currentX, iconY, iconSize, iconSize, ICON_LEADERBOARD,
                btn -> MinecraftClient.getInstance().setScreen(new LeaderboardScreen(this))
        ));

        // Right side of top bar
        int rightX = this.width - padding;

        // Close Icon Button
        rightX -= iconSize;
        this.addDrawableChild(new IconButtonWidget(rightX, iconY, iconSize, iconSize, ICON_CLOSE,
                btn -> MinecraftClient.getInstance().scheduleStop()
        ));
        rightX -= (iconSize + 8);

        // Profile Button
        MinecraftClient client = MinecraftClient.getInstance();
        String profileName = client.getSession().getUsername();
        int profileButtonWidth = this.textRenderer.getWidth(profileName) + 10;
        int profileButtonHeight = iconSize + 4;
        rightX -= profileButtonWidth;

        this.addDrawableChild(new ModButtonWidget(
                rightX, iconY - 2,
                profileButtonWidth, profileButtonHeight,
                Text.literal(profileName),
                btn -> MinecraftClient.getInstance().setScreen(new ProfileScreen()),
                BUTTON_BACKGROUND
        ));
    }

    private void addTransparentButtonWithGlassHover(String key, int x, int y, int width, int height, ButtonWidget.PressAction action) {
        this.addDrawableChild(new TransparentButtonWidget(x, y, width, height, Text.translatable(key), action) {
            @Override
            public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                int textColor = 0xFFFFFF;
                float backgroundAlpha = 0.0F;

                if (this.active) {
                    if (this.isHovered()) {
                        textColor = 0xFFFFAA;
                        backgroundAlpha = 0.8F;
                    }
                } else {
                    textColor = 0x808080;
                }

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, backgroundAlpha);
                RenderSystem.setShaderTexture(0, GLASS_BACKGROUND);
                context.drawTexture(GLASS_BACKGROUND, getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                context.drawCenteredTextWithShadow(
                        MinecraftClient.getInstance().textRenderer,
                        this.getMessage(),
                        this.getX() + this.getWidth() / 2,
                        this.getY() + (this.getHeight() - 8) / 2,
                        textColor
                );
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderZoomedBackground(context, this.width, this.height, delta);

        for (Particle p : particles) {
            p.update(this.width, this.height);
            context.setShaderColor(1F, 1F, 1F, p.alpha);
            RenderSystem.setShaderTexture(0, SNOWFLAKE);
            context.drawTexture(SNOWFLAKE, (int) p.x, (int) p.y, 0, 0, 8, 8, 8, 8);
        }

        context.setShaderColor(1F, 1F, 1F, 1F);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("").formatted(net.minecraft.util.Formatting.BOLD), this.width / 2, 40, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    public static void renderZoomedBackground(DrawContext context, int screenWidth, int screenHeight, float delta) {
        RenderSystem.setShaderTexture(0, COMMON_BACKGROUND_TEXTURE);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        float scale = 1.1F;
        float time = (float) (MinecraftClient.getInstance().world == null ? System.currentTimeMillis() / 1000.0 : MinecraftClient.getInstance().world.getTime() / 20.0);
        float offsetX = MathHelper.sin(time * 0.05F) * 10;
        float offsetY = MathHelper.cos(time * 0.04F) * 10;

        int scaledWidth = (int) (screenWidth * scale);
        int scaledHeight = (int) (screenHeight * scale);
        int x = (int) (-(scaledWidth - screenWidth) / 2 + offsetX);
        int y = (int) (-(scaledHeight - screenHeight) / 2 + offsetY);

        context.drawTexture(COMMON_BACKGROUND_TEXTURE, x, y, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
