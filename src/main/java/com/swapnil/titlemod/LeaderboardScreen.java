package com.swapnil.titlemod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.swapnil.titlemod.data.MatchLogEntryDTO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.text.OrderedText;
import com.swapnil.titlemod.gui.TransparentButtonWidget;
import com.swapnil.titlemod.network.RemoteDataLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderboardScreen extends Screen {
    private static final Identifier BACKGROUND = new Identifier("titlemod", "textures/gui/leaderboard_background.png");
    private final Screen parent;
    private TextFieldWidget searchField;
    private List<Map.Entry<String, Integer>> leaderboardEntries = new ArrayList<>();
    private List<Map.Entry<String, Integer>> filteredEntries = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 25;
    private static final int VISIBLE_ENTRIES = 15;

    // A local cache for UUID to Username mapping.
    private Map<String, String> cachedUsernames = new ConcurrentHashMap<>();
    private boolean isLoading = false;

    public LeaderboardScreen(Screen parent) {
        super(Text.literal("Leaderboard"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(new TransparentButtonWidget(15, 15, 90, 20,
                Text.literal("← Back").formatted(Formatting.BOLD),
                btn -> {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(ModSounds.BUTTON_CLICK_SOUND_EVENT, 1.0F));
                    MinecraftClient.getInstance().setScreen(this.parent);
                }
        ));

        int searchFieldWidth = 200;
        int searchFieldHeight = 20;
        int searchFieldX = this.width / 2 - searchFieldWidth / 2;
        int searchFieldY = 50;
        this.searchField = new TextFieldWidget(this.textRenderer, searchFieldX, searchFieldY, searchFieldWidth, searchFieldHeight, Text.literal("Search Player Username/UUID"));
        this.searchField.setMaxLength(36);
        this.searchField.setRenderTextProvider((originalText, originalCursor) -> {
            if (originalText.isEmpty() && !searchField.isFocused()) {
                return (OrderedText) Text.literal("Search Player Username/UUID").formatted(Formatting.GRAY);
            }
            return (OrderedText) Text.literal(originalText);
        });
        this.searchField.setChangedListener(this::onSearchFieldChanged);
        this.addDrawableChild(this.searchField);

        fetchLeaderboardData();
    }

    private void fetchLeaderboardData() {
        this.isLoading = true;
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Leaderboard] §fFetching ELO data..."));

        // Step 1: Fetch ELO data from the backend.
        RemoteDataLoader.fetchEloData(eloMap -> {
            // This code runs on the client's main thread.
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Leaderboard] §fELO data received. Fetching usernames..."));

            // Step 2: After ELO data is fetched, fetch the usernames.
            RemoteDataLoader.fetchUsernames(usernameMap -> {
                // This code also runs on the client's main thread.
                if (usernameMap != null) {
                    this.cachedUsernames.clear();
                    this.cachedUsernames.putAll(usernameMap);
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Leaderboard] §fUsername data received. Total usernames: " + this.cachedUsernames.size()));
                }

                // Step 3: Once both sets of data are available, update the leaderboard.
                this.leaderboardEntries.clear();
                if (eloMap != null) {
                    for (Map.Entry<String, Integer> entry : eloMap.entrySet()) {
                        this.leaderboardEntries.add(entry);
                    }
                }

                // Sort the entries by ELO in descending order.
                this.leaderboardEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

                // Update the filtered list and reset scroll position.
                this.filteredEntries.clear();
                this.filteredEntries.addAll(this.leaderboardEntries);
                this.scrollOffset = 0;
                this.isLoading = false;

                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§a[Leaderboard] §fLeaderboard ready. Total entries: " + this.leaderboardEntries.size()));
            });
        });
    }

    private void onSearchFieldChanged(String text) {
        filteredEntries.clear();
        if (text.isEmpty()) {
            filteredEntries.addAll(leaderboardEntries);
        } else {
            String lowercaseText = text.toLowerCase();
            for (Map.Entry<String, Integer> entry : leaderboardEntries) {
                String uuid = entry.getKey();
                // Check if the search text matches either the UUID or the cached username.
                String username = cachedUsernames.getOrDefault(uuid, "Unknown");

                if (uuid.toLowerCase().contains(lowercaseText) || username.toLowerCase().contains(lowercaseText)) {
                    filteredEntries.add(entry);
                }
            }
        }
        scrollOffset = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, BACKGROUND);
        context.drawTexture(BACKGROUND, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.getTitle().getString()).formatted(Formatting.BOLD, Formatting.AQUA), this.width / 2, 25, 0xFFFFFF);

        if (isLoading) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Loading leaderboard...").formatted(Formatting.GRAY), this.width / 2, this.height / 2, 0xFFFFFF);
        } else {
            drawLeaderboardEntries(context, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawLeaderboardEntries(DrawContext context, int mouseX, int mouseY) {
        int listX = this.width / 2 - 150;
        int listY = 80;
        int listWidth = 300;
        int listHeight = this.height - listY - 50;

        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0x40000000);

        int startEntry = scrollOffset;
        int endEntry = Math.min(scrollOffset + VISIBLE_ENTRIES, filteredEntries.size());

        if (filteredEntries.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No leaderboard entries found.").formatted(Formatting.GRAY), this.width / 2, listY + listHeight / 2, 0xFFFFFF);
        }

        for (int i = startEntry; i < endEntry; i++) {
            Map.Entry<String, Integer> entry = filteredEntries.get(i);
            int y = listY + (i - scrollOffset) * ENTRY_HEIGHT;

            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= y && mouseY <= y + ENTRY_HEIGHT) {
                context.fill(listX, y, listX + listWidth, y + ENTRY_HEIGHT, 0x30FFFFFF);
            }

            String rank = "#" + (i + 1);
            String uuid = entry.getKey();
            // Use the cached username with a fallback to a shortened UUID snippet
            String displayName = cachedUsernames.getOrDefault(uuid, "Unknown");
            if (displayName.equals("Unknown") && uuid.length() > 8) {
                displayName = uuid.substring(0, 8) + "...";
            }
            String elo = String.valueOf(entry.getValue());

            context.drawTextWithShadow(this.textRenderer, Text.literal(rank).formatted(Formatting.GOLD), listX + 10, y + (ENTRY_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, Text.literal(displayName).formatted(Formatting.WHITE), listX + 50, y + (ENTRY_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, Text.literal(elo).formatted(Formatting.AQUA), listX + listWidth - textRenderer.getWidth(elo) - 10, y + (ENTRY_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFF);
        }

        if (filteredEntries.size() > VISIBLE_ENTRIES) {
            int scrollbarHeight = listHeight;
            int scrollbarThumbHeight = (int) (scrollbarHeight * ((float) VISIBLE_ENTRIES / filteredEntries.size()));
            int scrollbarThumbY = listY + (int) ((scrollbarHeight - scrollbarThumbHeight) * ((float) scrollOffset / (filteredEntries.size() - VISIBLE_ENTRIES)));

            context.fill(listX + listWidth + 5, listY, listX + listWidth + 10, listY + listHeight, 0x40808080);
            context.fill(listX + listWidth + 5, scrollbarThumbY, listX + listWidth + 10, scrollbarThumbY + scrollbarThumbHeight, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (filteredEntries.size() <= VISIBLE_ENTRIES) {
            return false; // No scrolling needed if all entries fit
        }
        
        if (amount > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (amount < 0) {
            scrollOffset = Math.min(filteredEntries.size() - VISIBLE_ENTRIES, scrollOffset + 1);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
