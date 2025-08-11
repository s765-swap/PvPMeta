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
import com.swapnil.titlemod.network.RemoteDataLoader;

public class ProfileScreen extends Screen {
    private static final Identifier BACKGROUND = new Identifier("titlemod", "textures/gui/profile_background.png");
    private static final Identifier GLASS_BACKGROUND = new Identifier("titlemod", "textures/gui/glassbg.png");

    private int playerElo = 1000; // Default ELO, will be updated from remote data

    public ProfileScreen() {
        super(Text.literal("Profile"));
    }

    @Override
    protected void init() {
        // Fetch ELO data when the screen initializes
        fetchPlayerElo();

        // Back button (consistent with CustomTitleScreen's transparent style)
        this.addDrawableChild(new TransparentButtonWidget(15, 15, 90, 20,
                Text.literal("← Back").formatted(Formatting.BOLD),
                btn -> {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    MinecraftClient.getInstance().setScreen(new CustomTitleScreen()); // Always go back to CustomTitleScreen
                }
        ));

        int centerX = this.width / 2;
        int buttonWidth = 150;
        int buttonHeight = 25;
        int buttonSpacing = 10;

        // Action Buttons (Match Log, Player Search) - placed below the player model area
        int buttonsY = this.height / 2 + 80;

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
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§e[Profile] §fPlayer Search feature not yet implemented."));
                },
                GLASS_BACKGROUND
        ));
    }

    private void fetchPlayerElo() {
        if (MinecraftClient.getInstance().getSession() == null) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§c[Profile] §fCould not get player session to fetch ELO. Session is null."));
            return;
        }
        String playerUuidString = MinecraftClient.getInstance().getSession().getUuid(); // This is the UUID without hyphens

        // NEW: Log the client's own UUID
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Profile Debug] §fClient's UUID: " + playerUuidString));


        if (playerUuidString != null && !playerUuidString.isEmpty()) {
            final String finalPlayerUuid = playerUuidString;
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Profile Debug] §fAttempting to fetch ELO for UUID: " + finalPlayerUuid));

            RemoteDataLoader.fetchEloData(eloMap -> {
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Profile Debug] §fELO data callback received. Map size: " + (eloMap != null ? eloMap.size() : "null")));
                    if (eloMap != null) {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Profile Debug] §fReceived ELO Map: " + eloMap.toString())); // Log the full map
                        if (eloMap.containsKey(finalPlayerUuid)) {
                            this.playerElo = eloMap.get(finalPlayerUuid);
                            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§a[Profile] §fSuccessfully fetched ELO: " + this.playerElo + " for UUID: " + finalPlayerUuid));
                        } else {
                            this.playerElo = 1000; // Default if not found
                            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§e[Profile] §fYour ELO not found in data for UUID: " + finalPlayerUuid + ". Defaulting to " + this.playerElo));
                        }
                    } else {
                        this.playerElo = 1000; // Default if map is null
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§c[Profile] §fELO map received was null. Defaulting to " + this.playerElo));
                    }
                });
            });
        } else {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§c[Profile] §fCould not get player UUID to fetch ELO. Session UUID is null or empty."));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, BACKGROUND);
        context.drawTexture(BACKGROUND, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.getTitle().getString()).formatted(Formatting.BOLD, Formatting.AQUA), this.width / 2, 25, 0xFFFFFF);

        drawPlayerModel(context);
        drawPlayerStats(context);
        drawTierModes(context);

        super.render(context, mouseX, mouseY, delta);
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
        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        String ratingText = "Rating: " + this.playerElo;
        String playtimeText = "Playtime: 356 hrs";

        int padding = 20;
        int boxWidth = 200;
        int boxHeight = 60;
        int boxX = this.width / 2 - boxWidth / 2;
        int boxY = this.height / 2 - 120;

        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x40FFFFFF);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(playerName).formatted(Formatting.BOLD, Formatting.YELLOW), boxX + boxWidth / 2, boxY + 8, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, ratingText, boxX + boxWidth / 2, boxY + 28, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, playtimeText, boxX + boxWidth / 2, boxY + 42, 0xFFFFFF);
    }

    private void drawTierModes(DrawContext context) {
        int columnWidth = 150;
        int entryHeight = 20;
        int verticalSpacing = 5;
        int startY = this.height / 2 - 100;
        int leftColumnX = this.width / 2 - 200 - columnWidth / 2;
        int rightColumnX = this.width / 2 + 200 - columnWidth / 2;

        String[] modes = {"Crystal", "NethPot", "Axe", "Sword", "Pot", "UHC", "SMP"};
        String[] times = {"1 week", "1 week", "6 days", "5 days", "4 days", "3 days", "2 days"};

        context.drawTextWithShadow(this.textRenderer, Text.literal("Tier Modes").formatted(Formatting.BOLD, Formatting.GRAY), leftColumnX + columnWidth / 2 - this.textRenderer.getWidth("Tier Modes") / 2, startY - 20, 0xFFFFFF);

        for (int i = 0; i < modes.length; i++) {
            int y = startY + (i * (entryHeight + verticalSpacing));
            int x;

            if (i % 2 == 0) {
                x = leftColumnX;
            } else {
                x = rightColumnX;
            }

            context.fill(x, y, x + columnWidth, y + entryHeight, 0x20FFFFFF);
            context.drawTextWithShadow(this.textRenderer, modes[i], x + 10, y + (entryHeight - 8) / 2, 0xFFFFFF);
            int timeWidth = this.textRenderer.getWidth(times[i]);
            context.drawTextWithShadow(this.textRenderer, times[i], x + columnWidth - timeWidth - 10, y + (entryHeight - 8) / 2, 0xAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
