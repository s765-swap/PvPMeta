package com.swapnil.titlemod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.swapnil.titlemod.gui.IconButtonWidget;
import com.swapnil.titlemod.gui.ModButtonWidget;
import com.swapnil.titlemod.gui.TransparentButtonWidget;
import com.swapnil.titlemod.gui.QueueTimerWidget;
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
import org.joml.Matrix4f;
import net.minecraft.util.math.RotationAxis;
import com.swapnil.titlemod.util.Anim;

public class CustomTitleScreen extends Screen {
    private static final Identifier COMMON_BACKGROUND_TEXTURE = new Identifier("titlemod", "textures/gui/background.png");
    private static final Identifier SNOWFLAKE = new Identifier("titlemod", "textures/particles/snowflake.png");
    private static final Identifier ICON_SETTINGS = new Identifier("titlemod", "textures/gui/settings.png");
    private static final Identifier ICON_LEADERBOARD = new Identifier("titlemod", "textures/gui/leaderboard.png");
    private static final Identifier ICON_CLOSE = new Identifier("titlemod", "textures/gui/close.png");
    private static final Identifier BUTTON_BACKGROUND = new Identifier("titlemod", "textures/gui/buttonbg.png");
    private static final Identifier GLASS_BACKGROUND = new Identifier("titlemod", "textures/gui/glassbg.png");
    private static final Identifier ICON_DROPDOWN = new Identifier("titlemod", "textures/gui/dropdown.png");

    private final List<Particle> particles = new ArrayList<>();
    private final int iconY = 10;
    private final int iconSize = 20;
    private QueueTimerWidget queueTimerWidget;
    private final Anim.Manager animManager = new Anim.Manager();
    private Anim.Tween uiAlpha;
    private Anim.Tween uiYOffset;
    private long entranceStartMs;
    private Anim.Tween exitAlpha;
    private Anim.Tween exitYOffset;
    private boolean exiting = false;
    private Screen nextScreen = null;
    private Anim.Tween exitDirectionX;
    private Anim.Tween exitDirectionY;

    public CustomTitleScreen() {
        super(Text.literal(""));
    }

    @Override
    protected void init() {
        
        this.queueTimerWidget = new QueueTimerWidget(10, 50, 200, 50);
        this.addDrawableChild(queueTimerWidget);

        particles.clear();
        for (int i = 0; i < 100; i++) {
            particles.add(new Particle(this.width, this.height, true));
        }

        int spacing = 25;
        int leftX = 20;
        int buttonWidth = 120;
        int buttonHeight = 20;
        int startY = this.height / 2 - spacing;

        
        addTransparentButtonWithGlassHover("Play PvP", leftX, startY, buttonWidth, buttonHeight, btn -> startExitTo(new PvPModeScreen(this)));
        addTransparentButtonWithGlassHover("menu.singleplayer", leftX, startY + spacing, buttonWidth, buttonHeight, btn -> startExitTo(new SelectWorldScreen(this)));
        addTransparentButtonWithGlassHover("menu.multiplayer", leftX, startY + spacing * 2, buttonWidth, buttonHeight, btn -> startExitTo(new MultiplayerScreen(this)));
        addTransparentButtonWithGlassHover("Kit Editor", leftX, startY + spacing * 3, buttonWidth, buttonHeight, btn -> startExitTo(new KitEditorScreen(this)));

        
        this.addDrawableChild(new TransparentButtonWidget(this.width / 2 - 45, this.height - 30, 90, 20,
                Text.literal("Quit"),
                btn -> MinecraftClient.getInstance().scheduleStop()
        ));

        
        int padding = 10;
        int currentX = padding;

        
        this.addDrawableChild(new IconButtonWidget(currentX, iconY, iconSize, iconSize, ICON_SETTINGS,
                btn -> MinecraftClient.getInstance().setScreen(new OptionsScreen(this, MinecraftClient.getInstance().options))
        ));
        currentX += iconSize + 8;

        
        this.addDrawableChild(new IconButtonWidget(currentX, iconY, iconSize, iconSize, ICON_LEADERBOARD,
                btn -> MinecraftClient.getInstance().setScreen(new LeaderboardScreen(this))
        ));

        
        int rightX = this.width - padding;

        
        rightX -= iconSize;
        this.addDrawableChild(new IconButtonWidget(rightX, iconY, iconSize, iconSize, ICON_CLOSE,
                btn -> MinecraftClient.getInstance().scheduleStop()
        ));
        rightX -= (iconSize + 8);

        
        MinecraftClient client = MinecraftClient.getInstance();
        String profileName = client.getSession().getUsername();
        int profileButtonWidth = this.textRenderer.getWidth(profileName) + 40;
        int profileButtonHeight = iconSize + 4;
        rightX -= profileButtonWidth;
        this.addDrawableChild(new ModButtonWidget(
                rightX, iconY - 2,
                profileButtonWidth, profileButtonHeight,
                Text.literal(profileName),
                btn -> startExitTo(new ProfileScreen()),
                BUTTON_BACKGROUND
        ));

        
        uiAlpha = Anim.Utils.fadeIn(animManager, 800);
        uiYOffset = Anim.Utils.slideIn(animManager, 24f, 850);
        entranceStartMs = System.currentTimeMillis();
    }

    private void startExitTo(Screen screen) {
        if (exiting) return;
        exiting = true;
        nextScreen = screen;
        exitAlpha = Anim.Utils.fadeOut(animManager, 480);
        exitYOffset = animManager.add(new Anim.Tween(0f, -28f, 480, Anim.Easings.EASE_OUT_BACK));
        exitDirectionX = animManager.add(new Anim.Tween(0f, 12f, 480, Anim.Easings.EASE_OUT_QUINT));
        exitDirectionY = animManager.add(new Anim.Tween(0f, -8f, 480, Anim.Easings.EASE_OUT_QUINT));
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
        animManager.update();
        renderZoomedBackground(context, this.width, this.height, delta);

        for (Particle p : particles) {
            p.update(this.width, this.height);
            context.setShaderColor(1F, 1F, 1F, p.getAlpha());
            RenderSystem.setShaderTexture(0, SNOWFLAKE);
            
            
            context.getMatrices().push();
            context.getMatrices().translate(p.getX() + 4, p.getY() + 4, 0);
            context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(p.getRotation()));
            context.getMatrices().scale(p.getScale(), p.getScale(), 1.0f);
            context.getMatrices().translate(-4, -4, 0);
            
            context.drawTexture(SNOWFLAKE, 0, 0, 0, 0, 8, 8, 8, 8);
            context.getMatrices().pop();
        }

        context.setShaderColor(1F, 1F, 1F, 1F);

        
        float alpha = uiAlpha == null ? 1f : uiAlpha.get();
        float yOffset = uiYOffset == null ? 0f : uiYOffset.get();
        if (exiting) {
            float ea = exitAlpha == null ? 1f : exitAlpha.get();
            float ey = exitYOffset == null ? 0f : exitYOffset.get();
            float ex = exitDirectionX == null ? 0f : exitDirectionX.get();
            float ey2 = exitDirectionY == null ? 0f : exitDirectionY.get();
            alpha *= ea;
            yOffset += ey + ey2;
            
            for (var drawable : this.children()) {
                if (drawable instanceof TransparentButtonWidget tbw) {
                    tbw.setExternalXOffset(ex);
                }
            }
        }

        
        for (var drawable : this.children()) {
            if (drawable instanceof TransparentButtonWidget tbw) {
                tbw.setExternalAlpha(alpha);
                tbw.setExternalYOffset(yOffset);
            } else if (drawable instanceof ModButtonWidget mbw) {
                mbw.setExternalAlpha(alpha);
            } else if (drawable instanceof IconButtonWidget) {
                
            }
        }


        long now = System.currentTimeMillis();
        long delayPer = 90L;
        int baseY = this.height / 2 - 25; 
        for (var drawable : this.children()) {
            if (drawable instanceof TransparentButtonWidget tbw) {
                int idx = (tbw.getY() - baseY) / 25; 
                long localElapsed = Math.max(0L, now - entranceStartMs - idx * delayPer);
                float t = Math.min(1f, localElapsed / 650f);
                float eased = Anim.Easings.EASE_OUT_SINE.ease(t);
                tbw.setExternalAlpha(alpha * eased);
                tbw.setExternalYOffset(yOffset + (1f - eased) * 20f);
            }
        }

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("").formatted(net.minecraft.util.Formatting.BOLD), this.width / 2, 40, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);

        
        if (exiting && exitAlpha != null && exitAlpha.isFinished() && nextScreen != null) {
            MinecraftClient.getInstance().setScreen(nextScreen);
            nextScreen = null;
        }
    }

    public static void renderZoomedBackground(DrawContext context, int screenWidth, int screenHeight, float delta) {
        RenderSystem.setShaderTexture(0, COMMON_BACKGROUND_TEXTURE);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        float time = (float) (MinecraftClient.getInstance().world == null ? System.currentTimeMillis() / 1000.0 : MinecraftClient.getInstance().world.getTime() / 20.0);

        
        float scale = 1.12F + (float)Math.sin(time * 0.07F) * 0.04F;
       
        float offsetX = MathHelper.sin(time * 0.025F) * 24;
        float offsetY = MathHelper.cos(time * 0.021F) * 18;
        
        float rotation = (float)Math.sin(time * 0.018F) * 2.5F; 

        int scaledWidth = (int) (screenWidth * scale);
        int scaledHeight = (int) (screenHeight * scale);
        int x = (int) (-(scaledWidth - screenWidth) / 2 + offsetX);
        int y = (int) (-(scaledHeight - screenHeight) / 2 + offsetY);

        
        context.getMatrices().push();
        context.getMatrices().translate(screenWidth / 2.0f, screenHeight / 2.0f, 0);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        context.getMatrices().translate(-screenWidth / 2.0f, -screenHeight / 2.0f, 0);

        context.drawTexture(COMMON_BACKGROUND_TEXTURE, x, y, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
        context.getMatrices().pop();
    }

    public static void renderBlurVeil(DrawContext context, int screenWidth, int screenHeight, float alpha) {
        if (alpha <= 0f) return;
        alpha = Math.max(0f, Math.min(1f, alpha));
        RenderSystem.setShaderTexture(0, GLASS_BACKGROUND);
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
        context.drawTexture(GLASS_BACKGROUND, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
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
    
    @Override
    public boolean shouldCloseOnEsc() {
       
        return false;
    }
    
    @Override
    public boolean shouldPause() {
       
        return false;
    }
    
    @Override
    public void close() {
        
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen != this) {
            super.close();
        }
    }
}
