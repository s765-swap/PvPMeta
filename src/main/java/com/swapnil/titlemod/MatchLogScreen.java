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

    // The current scroll offset, in pixels. This is a much better way to handle it.
    private double scrollOffset = 0.0;
    private static final int ENTRY_HEIGHT = 40;
    private static final int LIST_PADDING = 5; // Added a small padding for visuals
    private static final int SCROLL_SPEED = 10; // Pixels to scroll per mouse wheel click
    private int maxScrollOffset = 0; // Max scrollable pixel value

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public MatchLogScreen(Screen parent) {
        super(Text.literal("Match Log"));
    }

    @Override
    protected void init() {
        // Clear all previous widgets to prevent duplicates on resize
        this.clearChildren();

        this.addDrawableChild(new TransparentButtonWidget(15, 15, 90, 20,
                Text.literal("← Back").formatted(Formatting.BOLD),
                btn -> {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    MinecraftClient.getInstance().setScreen(new CustomTitleScreen());
                }
        ));

        // Re-calculate max scroll offset on screen initialization or resize
        updateMaxScrollOffset();

        fetchMatchLogData();
    }

    private void updateMaxScrollOffset() {
        int listHeight = this.height - 80 - 50; // listY to bottom padding
        int totalContentHeight = filteredDuelEntries.size() * ENTRY_HEIGHT;
        this.maxScrollOffset = Math.max(0, totalContentHeight - listHeight);

        // Clamp the scroll offset in case the number of entries has changed
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

                // Recalculate max scroll offset after fetching new data
                updateMaxScrollOffset();
            });
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, BACKGROUND);
        context.drawTexture(BACKGROUND, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.getTitle().getString()).formatted(Formatting.BOLD, Formatting.AQUA), this.width / 2, 25, 0xFFFFFF);

        // --- THE CRITICAL FIX: ENABLE SCISSORING BEFORE DRAWING THE LIST ---
        // This clips all rendering to only happen inside the specified rectangle.
        int listX = this.width / 2 - 200;
        int listY = 80;
        int listWidth = 400;
        int listHeight = this.height - listY - 50;

        context.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        drawMatchLogEntries(context, mouseX, mouseY, listX, listY, listWidth, listHeight);
        context.disableScissor();
        // --- END OF FIX ---

        // Render a simple scrollbar to visualize the position, outside of the scissor region
        drawScrollbar(context, listX + listWidth + 5, listY, 10, listHeight);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawMatchLogEntries(DrawContext context, int mouseX, int mouseY, int listX, int listY, int listWidth, int listHeight) {
        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0x40000000);

        if (filteredDuelEntries.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No duel entries found for your profile.").formatted(Formatting.GRAY), this.width / 2, listY + listHeight / 2, 0xFFFFFF);
        }

        String currentPlayerUsername = MinecraftClient.getInstance().getSession().getUsername();

        for (int i = 0; i < filteredDuelEntries.size(); i++) {
            MatchLogEntryDTO entry = filteredDuelEntries.get(i);
            // We use the scrollOffset to shift the position of each entry
            int entryY = listY + LIST_PADDING + (i * ENTRY_HEIGHT) - (int) scrollOffset;

            // Only draw hover effect for entries that are visible and within the bounds
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
        // Adjust scroll offset based on mouse wheel movement
        scrollOffset -= amount * SCROLL_SPEED;
        // Clamp the scroll offset to prevent scrolling past the start or end
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
}