package com.swapnil.titlemod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import com.swapnil.titlemod.data.MatchLogEntryDTO;
import com.swapnil.titlemod.gui.TransparentButtonWidget;
import com.swapnil.titlemod.network.RemoteDataLoader;
import com.swapnil.titlemod.util.Anim;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MatchLogScreen extends Screen {
    private static final Identifier BACKGROUND = new Identifier("titlemod", "textures/gui/matchlog_background.png");
    private static final Identifier GLASS_BACKGROUND = new Identifier("titlemod", "textures/gui/glassbg.png");

    private List<MatchLogEntryDTO> duelEntries = new ArrayList<>();
    private List<MatchLogEntryDTO> filteredDuelEntries = new ArrayList<>();

 
    private double scrollOffset = 0.0;
    private static final int ENTRY_HEIGHT = 40;
    private static final int LIST_PADDING = 5; 
    private static final int SCROLL_SPEED = 10;
    private int maxScrollOffset = 0; 

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Anim.Manager animManager = new Anim.Manager();
    private Anim.Tween uiAlpha;
    private Anim.Tween uiYOffset;
    private Anim.Tween exitAlpha;
    private Anim.Tween exitYOffset;
    private boolean exiting = false;
    private Screen nextScreen = null;

    public MatchLogScreen(Screen parent) {
        super(Text.literal("Match Log"));
    }

    @Override
    protected void init() {

        this.clearChildren();

        this.addDrawableChild(new TransparentButtonWidget(15, 15, 90, 20,
                Text.literal("← Back").formatted(Formatting.BOLD),
                btn -> {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    startExitTo(new CustomTitleScreen());
                }
        ));

      
        updateMaxScrollOffset();

        fetchMatchLogData();

        
        uiAlpha = Anim.Utils.fadeIn(animManager, 800);
        uiYOffset = Anim.Utils.slideIn(animManager, 24f, 850);
    }

    private void startExitTo(Screen screen) {
        if (exiting) return;
        exiting = true;
        nextScreen = screen;
        exitAlpha = Anim.Utils.fadeOut(animManager, 480);
        exitYOffset = Anim.Utils.slideOut(animManager, 20f, 480);
    }

    private void updateMaxScrollOffset() {
        int listHeight = this.height - 80 - 50; 
        int totalContentHeight = filteredDuelEntries.size() * ENTRY_HEIGHT;
        this.maxScrollOffset = Math.max(0, totalContentHeight - listHeight);

  
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScrollOffset));
    }


    private void fetchMatchLogData() {
        String currentPlayerUsername = MinecraftClient.getInstance().getSession().getUsername();

        if (currentPlayerUsername == null || currentPlayerUsername.isEmpty()) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§c[Match Log] §fCould not get current player username to fetch match log."));
            return;
        }

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Match Log Debug] §fAttempting to fetch duel data for username: " + currentPlayerUsername));

        RemoteDataLoader.fetchRealTimeDuels(duels -> {
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Match Log Debug] §fDuel data callback received. Total duels: " + (duels != null ? duels.size() : "null")));
                this.duelEntries.clear();
                this.filteredDuelEntries.clear();

                if (duels != null) {
                    this.duelEntries.addAll(duels);
                    for (MatchLogEntryDTO duel : duels) {
                        if (currentPlayerUsername.equalsIgnoreCase(duel.getWinnerUsername()) || currentPlayerUsername.equalsIgnoreCase(duel.getLoserUsername())) {
                            this.filteredDuelEntries.add(duel);
                        }
                    }
                }
                this.filteredDuelEntries.sort(Comparator.comparing(MatchLogEntryDTO::getTimestamp).reversed());
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§a[Match Log] §fFiltered " + this.filteredDuelEntries.size() + " duels for your profile."));

                updateMaxScrollOffset();
            });
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animManager.update();

        RenderSystem.setShaderTexture(0, BACKGROUND);
        context.drawTexture(BACKGROUND, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        float alpha = uiAlpha == null ? 1f : uiAlpha.get();
        float yOffset = uiYOffset == null ? 0f : uiYOffset.get();
        if (exiting) {
            float ea = exitAlpha == null ? 1f : exitAlpha.get();
            float ey = exitYOffset == null ? 0f : exitYOffset.get();
            alpha *= ea;
            yOffset += ey;
        }

        context.setShaderColor(1F, 1F, 1F, alpha);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.getTitle().getString()).formatted(Formatting.BOLD, Formatting.AQUA), this.width / 2, 25 + (int)yOffset, 0xFFFFFF);
        context.setShaderColor(1F, 1F, 1F, 1F);


        int listX = this.width / 2 - 200;
        int listY = 80;
        int listWidth = 400;
        int listHeight = this.height - listY - 50;

        context.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        context.getMatrices().push();
        context.getMatrices().translate(0, yOffset, 0);
        drawMatchLogEntries(context, mouseX, mouseY, listX, listY, listWidth, listHeight);
        context.getMatrices().pop();
        context.disableScissor();
      
        context.getMatrices().push();
        context.getMatrices().translate(0, yOffset, 0);
        drawScrollbar(context, listX + listWidth + 5, listY, 10, listHeight);
        context.getMatrices().pop();

        super.render(context, mouseX, mouseY, delta);

       
        if (exiting && exitAlpha != null && exitAlpha.isFinished() && nextScreen != null) {
            MinecraftClient.getInstance().setScreen(nextScreen);
            nextScreen = null;
        }
    }

    private void drawMatchLogEntries(DrawContext context, int mouseX, int mouseY, int listX, int listY, int listWidth, int listHeight) {
        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0x40000000);

        if (filteredDuelEntries.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No duel entries found for your profile.").formatted(Formatting.GRAY), this.width / 2, listY + listHeight / 2, 0xFFFFFF);
        }

        String currentPlayerUsername = MinecraftClient.getInstance().getSession().getUsername();

        for (int i = 0; i < filteredDuelEntries.size(); i++) {
            MatchLogEntryDTO entry = filteredDuelEntries.get(i);
            
            int entryY = listY + LIST_PADDING + (i * ENTRY_HEIGHT) - (int) scrollOffset;

            
            if (entryY >= listY && entryY + ENTRY_HEIGHT <= listY + listHeight && mouseX >= listX && mouseX <= listX + listWidth && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT) {
                context.fill(listX, entryY, listX + listWidth, entryY + ENTRY_HEIGHT, 0x30FFFFFF);
            }

            String timestamp = DATE_FORMATTER.format(Instant.ofEpochMilli(entry.getTimestamp()));

            String resultText;
            Formatting resultColor;

            if (entry.getWinnerUsername() != null && entry.getWinnerUsername().equalsIgnoreCase(currentPlayerUsername)) {
                resultText = String.format("WIN against %s (%s)", (entry.getLoserUsername() != null ? entry.getLoserUsername() : "Unknown Player"), timestamp);
                resultColor = Formatting.GREEN;
            } else if (entry.getLoserUsername() != null && entry.getLoserUsername().equalsIgnoreCase(currentPlayerUsername)) {
                resultText = String.format("LOSS to %s (%s)", (entry.getWinnerUsername() != null ? entry.getWinnerUsername() : "Unknown Player"), timestamp);
                resultColor = Formatting.RED;
            } else {
                resultText = String.format("Duel: %s vs %s (%s)",
                        (entry.getPlayer1Username() != null ? entry.getPlayer1Username() : "Unknown"),
                        (entry.getPlayer2Username() != null ? entry.getPlayer2Username() : "Unknown"),
                        timestamp);
                resultColor = Formatting.WHITE;
            }

            context.drawTextWithShadow(this.textRenderer, Text.literal(resultText).formatted(resultColor), listX + 10, entryY + (ENTRY_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFF);
        }
    }

    private void drawScrollbar(DrawContext context, int x, int y, int width, int height) {
        if (maxScrollOffset > 0) {
            context.fill(x, y, x + width, y + height, 0x40808080);

            float thumbHeightRatio = (float) height / (height + maxScrollOffset);
            int thumbHeight = (int) (height * thumbHeightRatio);
            int scrollbarThumbY = (int) (y + (height - thumbHeight) * (scrollOffset / maxScrollOffset));

            context.fill(x, scrollbarThumbY, x + width, scrollbarThumbY + thumbHeight, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
       
        scrollOffset -= amount * SCROLL_SPEED;
        
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
}