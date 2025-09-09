package com.swapnil.titlemod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.swapnil.titlemod.gui.ModButtonWidget;
import com.swapnil.titlemod.gui.TransparentButtonWidget;
import com.swapnil.titlemod.gui.QueueTimerWidget;
import com.swapnil.titlemod.network.RemoteDataLoader;
import com.swapnil.titlemod.data.PlayerDataManager;
import com.swapnil.titlemod.data.PlayerData;
import com.swapnil.titlemod.util.Anim;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.io.InputStream;
import net.minecraft.client.texture.NativeImage;

public class ProfileScreen extends Screen {
    private static final Identifier BACKGROUND = new Identifier("titlemod", "textures/gui/profile_background.png");
    private static final Identifier BUTTON_BACKGROUND = new Identifier("titlemod", "textures/gui/buttonbg.png");
    private static final Identifier GLASS_BACKGROUND = new Identifier("titlemod", "textures/gui/glassbg.png");
    
    
    private static final Identifier[] GAMEMODE_ICONS = {
        new Identifier("titlemod", "textures/gui/gamemodes/cpvp.png"),
        new Identifier("titlemod", "textures/gui/gamemodes/nethpot.png"),
        new Identifier("titlemod", "textures/gui/gamemodes/uhc.png"),
        new Identifier("titlemod", "textures/gui/gamemodes/axe.png"),
        new Identifier("titlemod", "textures/gui/gamemodes/pot.png"),
        new Identifier("titlemod", "textures/gui/gamemodes/smp.png"),
        new Identifier("titlemod", "textures/gui/gamemodes/sword.png")
    };

    private PlayerData playerData;
    private QueueTimerWidget queueTimerWidget;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 1000; 
    private final Anim.Manager animManager = new Anim.Manager();
    private Anim.Tween uiAlpha;
    private Anim.Tween uiYOffset;
    private Anim.Tween exitAlpha;
    private Anim.Tween exitYOffset;
    private boolean exiting = false;
    private Screen nextScreen = null;
    private Anim.Tween[] buttonAlphas;
    private Anim.Tween[] buttonScales;
    private long entranceStartMs;
    private Identifier playerHeadTexture;
    private Identifier playerSkinTexture;
    private Anim.Tween borderPulse;
    private Anim.Tween borderGlow;

    public ProfileScreen() {
        super(Text.literal("Profile"));
    }

    @Override
    protected void init() {
        
        this.playerData = PlayerDataManager.getInstance().getCurrentPlayerData();
        if (this.playerData != null) {
            this.playerData.startSession(); 
        }
        
        
        this.queueTimerWidget = new QueueTimerWidget(10, 50, 200, 50);
        this.addDrawableChild(queueTimerWidget);

        int centerX = this.width / 2;
        int buttonWidth = 150;
        int buttonHeight = 25;
        int buttonSpacing = 10;
       
        int buttonsY = this.height / 2 + 80;
        int statsBoxY = this.height / 2 - 120;
        int statsBoxHeight = 60;

        
        this.addDrawableChild(new TransparentButtonWidget(15, 15, 90, 22,
                Text.literal("← Back").formatted(Formatting.BOLD),
                btn -> {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    startExitTo(new CustomTitleScreen());
                }
        ));

        buttonAlphas = new Anim.Tween[2];
        buttonScales = new Anim.Tween[2];
        entranceStartMs = System.currentTimeMillis();
        for (int i = 0; i < 2; i++) {
            buttonAlphas[i] = animManager.add(new Anim.Tween(0f, 1f, 900, Anim.Easings.EASE_OUT_QUINT));
            buttonScales[i] = animManager.add(new Anim.Tween(0.92f, 1f, 900, Anim.Easings.EASE_OUT_BACK));
        }

        this.addDrawableChild(new ModButtonWidget(centerX - buttonWidth / 2, buttonsY, buttonWidth, buttonHeight,
                Text.literal("Match Log"),
                btn -> {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    MinecraftClient.getInstance().setScreen(new MatchLogScreen(this));
                },
                GLASS_BACKGROUND
        ));

        this.addDrawableChild(new ModButtonWidget(centerX - buttonWidth / 2, buttonsY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight,
                Text.literal("Player Search"),
                btn -> {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    MinecraftClient.getInstance().setScreen(new PlayerSearchScreen(this));
                },
                GLASS_BACKGROUND
        ));

        
        fetchPlayerElo();
        
        fetchPlayerHead();
        
       
        uiAlpha = Anim.Utils.fadeIn(animManager, 700);
        uiYOffset = Anim.Utils.slideIn(animManager, 24f, 750);
        
       
        borderPulse = animManager.add(new Anim.Tween(0f, 1f, 2000, Anim.Easings.EASE_IN_OUT_SINE));
        borderGlow = Anim.Utils.fadeIn(animManager, 1000);
    }
    
    private void startExitTo(Screen screen) {
        if (exiting) return;
        exiting = true;
        nextScreen = screen;
        exitAlpha = Anim.Utils.fadeOut(animManager, 420);
        exitYOffset = Anim.Utils.slideOut(animManager, 20f, 420);
    }

    private void fetchPlayerElo() {
        if (MinecraftClient.getInstance().getSession() == null) {
            return;
        }
        String playerUuidString = MinecraftClient.getInstance().getSession().getUuid();

        if (playerUuidString != null && !playerUuidString.isEmpty()) {
            final String finalPlayerUuid = playerUuidString;
            RemoteDataLoader.fetchEloData(eloMap -> {
                MinecraftClient.getInstance().execute(() -> {
                    if (eloMap != null && eloMap.containsKey(finalPlayerUuid)) {
                        this.playerData.setElo(eloMap.get(finalPlayerUuid));
                    } else {
                        this.playerData.setElo(1000); 
                    }
                });
            });
        }
    }

    private void fetchPlayerHead() {
        String uuid = MinecraftClient.getInstance().getSession().getUuid();
        if (uuid != null && !uuid.isEmpty()) {
            String url = "https://crafatar.com/avatars/" + uuid + "?size=32&overlay";
            playerHeadTexture = new Identifier("titlemod", "head_" + uuid);
            try (InputStream in = new URL(url).openStream()) {
                NativeImage img = NativeImage.read(in);
                NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                MinecraftClient.getInstance().getTextureManager().registerTexture(playerHeadTexture, tex);
            } catch (Exception e) {
                playerHeadTexture = null;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animManager.update();
        
        
        RenderSystem.setShaderTexture(0, BACKGROUND);
        context.drawTexture(BACKGROUND, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        
    
        float overlayAlpha = 0.12f * (uiAlpha == null ? 1f : uiAlpha.get());
        context.fill(0, 0, this.width, this.height, (int)(0x18 * overlayAlpha) << 24 | 0x000000);
        
    
        float borderAlpha = uiAlpha == null ? 1f : uiAlpha.get();
        float pulse = borderPulse == null ? 0.5f : borderPulse.get();
        float glow = borderGlow == null ? 1f : borderGlow.get();
        
       
        int contentX = 16; // was 20
        int contentY = 48; // was 60
        int contentWidth = this.width - 32; // was -40
        int contentHeight = this.height - 96; // was -120
        
        Anim.BorderRenderer.renderGlowBorder(context, contentX, contentY, contentWidth, contentHeight, borderAlpha, glow);
        Anim.BorderRenderer.renderGradientBorder(context, contentX + 2, contentY + 2, contentWidth - 4, contentHeight - 4, borderAlpha * 0.7f);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.getTitle().getString()).formatted(Formatting.BOLD, Formatting.AQUA), this.width / 2, 25, 0xFFFFFF);

    
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > UPDATE_INTERVAL) {
            lastUpdateTime = currentTime;
          
        }

        
        float alpha = uiAlpha == null ? 1f : uiAlpha.get();
        float yOffset = uiYOffset == null ? 0f : uiYOffset.get();
        
        if (exiting) {
            float ea = exitAlpha == null ? 1f : exitAlpha.get();
            float ey = exitYOffset == null ? 0f : exitYOffset.get();
            alpha *= ea;
            yOffset += ey;
        }
        
      
        for (var drawable : this.children()) {
            if (drawable instanceof TransparentButtonWidget tbw) {
                tbw.setExternalAlpha(alpha);
                tbw.setExternalYOffset(yOffset);
            } else if (drawable instanceof ModButtonWidget mbw) {
                mbw.setExternalAlpha(alpha);
            }
        }
        
 
        context.setShaderColor(1F, 1F, 1F, alpha);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.getTitle().getString()).formatted(Formatting.BOLD, Formatting.AQUA), this.width / 2, 25 + (int)yOffset, 0xFFFFFF);
        context.setShaderColor(1F, 1F, 1F, 1F);
        
        drawPlayerModel(context);
        drawPlayerStats(context);
        drawTierModes(context, alpha, yOffset);

        super.render(context, mouseX, mouseY, delta);
        
      
        if (exiting && exitAlpha != null && exitAlpha.isFinished() && nextScreen != null) {
            MinecraftClient.getInstance().setScreen(nextScreen);
            nextScreen = null;
        }
    }

    private void drawPlayerModel(DrawContext context) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            int x = this.width / 2;
            int y = this.height / 2 + 30;

            EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadows(false);
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(x, y, 100);
            matrices.scale(-1.0F, 1.0F, 1.0F);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180.0F + (float) (System.currentTimeMillis() % 36000L) / 200.0F));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
            matrices.scale(80.0F, 80.0F, 80.0F);
            matrices.translate(0.0, -player.getHeight() / 2.0, 0.0);

            dispatcher.render(player, 0.0, 0.0, 0.0, 0.0F, 1.0F, matrices, context.getVertexConsumers(), 15728880);
            matrices.pop();
            dispatcher.setRenderShadows(true);
        }
    }

    private void drawPlayerStats(DrawContext context) {
        if (playerData == null) return;
        String playerName = playerData.getPlayerName();
        String ratingText = "Rating: " + playerData.getElo();
        String playtimeText = "Playtime: " + playerData.getFormattedPlaytime();
        int padding = 20;
        int boxWidth = 300;
        int boxHeight = 60;
        int boxX = this.width / 2 - boxWidth / 2;
        int boxY = this.height / 2 - 120;
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x40FFFFFF);
        
        if (playerHeadTexture != null) {
            RenderSystem.setShaderTexture(0, playerHeadTexture);
            context.drawTexture(playerHeadTexture, boxX + padding, boxY + 8, 32, 32, 0, 0, 32, 32, 32, 32);
        }
      
        int nameX = boxX + padding + 40;
        context.drawTextWithShadow(this.textRenderer, Text.literal(playerName).formatted(Formatting.BOLD, Formatting.YELLOW), nameX, boxY + 8, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, ratingText, boxX + boxWidth / 2, boxY + 28, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, playtimeText, boxX + boxWidth / 2, boxY + 42, 0xFFFFFF);
    }

        private void drawTierModes(DrawContext context, float alpha, float yOffset) {
        if (playerData == null) return;
        
        int columnWidth = 200; 
        int entryHeight = 25; 
        int verticalSpacing = 5;
       
        int startY = this.height / 2 - 100 + 32;
        int leftColumnX = this.width / 2 - 200 - columnWidth / 2;
        int rightColumnX = this.width / 2 + 200 - columnWidth / 2;

        String[] modes = {"CPVP", "NETHPOT", "UHC", "AXE", "POT", "SMP", "SWORD"};

        context.setShaderColor(1F, 1F, 1F, alpha);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Tier Modes").formatted(Formatting.BOLD, Formatting.GRAY), leftColumnX + columnWidth / 2 - this.textRenderer.getWidth("Tier Modes") / 2, startY - 20 + (int)yOffset, 0xFFFFFF);

        for (int i = 0; i < modes.length; i++) {
            int y = startY + (i * (entryHeight + verticalSpacing));
            int x;

            if (i % 2 == 0) {
                x = leftColumnX;
            } else {
                x = rightColumnX;
            }

            String mode = modes[i];
            String timeSince = formatTimeSince(playerData.getTimeSinceLastPlayed(mode));

            
            int entryColor = (int)(0x25 * alpha) << 24 | 0x00111111;
            context.fill(x, y + (int)yOffset, x + columnWidth, y + entryHeight + (int)yOffset, entryColor);
           
            int entryBorder = (int)(0x15 * alpha) << 24 | 0x00222222;
            context.fill(x - 1, y - 1 + (int)yOffset, x + columnWidth + 1, y + (int)yOffset, entryBorder);
            context.fill(x - 1, y + entryHeight + (int)yOffset, x + columnWidth + 1, y + entryHeight + 1 + (int)yOffset, entryBorder);
            context.fill(x - 1, y + (int)yOffset, x, y + entryHeight + (int)yOffset, entryBorder);
            context.fill(x + columnWidth, y + (int)yOffset, x + columnWidth + 1, y + entryHeight + (int)yOffset, entryBorder);
            
      
            try {
                context.drawTexture(GAMEMODE_ICONS[i], x + 5, y + 2 + (int)yOffset, 0, 0, 16, 16, 16, 16);
            } catch (Exception e) {
                
                int iconColor = (int)(0x30 * alpha) << 24 | 0x00333333;
                context.fill(x + 5, y + 2 + (int)yOffset, x + 21, y + 18 + (int)yOffset, iconColor);
            }
            
        
            context.drawTextWithShadow(this.textRenderer, mode, x + 25, y + (entryHeight - 8) / 2 + (int)yOffset, 0xFFFFFF);
            int timeWidth = this.textRenderer.getWidth(timeSince);
            
          
            Formatting timeFormatting = Formatting.GRAY;
            if (timeSince.equals("Just now")) {
                timeFormatting = Formatting.GREEN;
            } else if (timeSince.contains("minute") || timeSince.contains("hour")) {
                timeFormatting = Formatting.YELLOW;
            } else if (timeSince.equals("Never")) {
                timeFormatting = Formatting.RED;
            }
            
            context.drawTextWithShadow(this.textRenderer, 
                Text.literal(timeSince).formatted(timeFormatting), 
                x + columnWidth - timeWidth - 10, y + (entryHeight - 8) / 2 + (int)yOffset, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
    

    private String formatTimeSince(long timeSince) {
        if (timeSince < 0) return "Never";
        if (timeSince < 60000) return "Just now"; // Less than 1 minute
        
        long minutes = timeSince / 60000;
        if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        }
        
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        }
        
        long days = hours / 24;
        return days + " day" + (days == 1 ? "" : "s") + " ago";
    }

    @Override
    public void close() {
     
        if (playerData != null) {
        
            long currentSessionTime = System.currentTimeMillis() - playerData.getSessionStartTime();
            playerData.addSessionPlaytime(currentSessionTime);
        }
        super.close();
    }
}

class PlayerSearchScreen extends Screen {
    private final Screen parent;
    public PlayerSearchScreen(Screen parent) {
        super(Text.literal("Player Search"));
        this.parent = parent;
    }
    @Override
    protected void init() {
        this.addDrawableChild(new TransparentButtonWidget(20, 20, 100, 25,
                Text.literal("← Back").formatted(Formatting.BOLD),
                btn -> MinecraftClient.getInstance().setScreen(parent)
        ));
    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        context.fill(0, 0, this.width, this.height, 0xA0000000);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Player Search").formatted(Formatting.BOLD, Formatting.AQUA), this.width / 2, 40, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Player Search will be implemented in Beta 2 release.").formatted(Formatting.GRAY), this.width / 2, this.height / 2, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
