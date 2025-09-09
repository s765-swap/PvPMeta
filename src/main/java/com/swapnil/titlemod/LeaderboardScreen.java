package com.swapnil.titlemod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.swapnil.titlemod.data.MatchLogEntryDTO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import com.swapnil.titlemod.gui.TransparentButtonWidget;
import com.swapnil.titlemod.network.RemoteDataLoader;
import com.swapnil.titlemod.util.Anim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class LeaderboardScreen extends Screen {
    private static final Identifier BACKGROUND = new Identifier("titlemod", "textures/gui/leaderboard_background.png");
    private final Screen parent;
    private List<Map.Entry<String, Integer>> leaderboardEntries = new ArrayList<>();
    private List<Map.Entry<String, Integer>> filteredEntries = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 35; 
    private static final int VISIBLE_ENTRIES = 12; 

    
    private Map<String, String> cachedUsernames = new ConcurrentHashMap<>();
    private boolean isLoading = false;
    private long lastFetchTime = 0;
    private static final long REFRESH_INTERVAL = 30000; // 30 seconds
    private final Anim.Manager animManager = new Anim.Manager();
    private Anim.Tween borderPulse;
    private Anim.Tween borderGlow;
    private Anim.Tween uiAlpha;
    private Anim.Tween uiYOffset;
    private Anim.Tween exitAlpha;
    private Anim.Tween exitYOffset;
    private boolean exiting = false;
    private Screen nextScreen = null;
    
   
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT_UUID_PATTERN = Pattern.compile("^[0-9a-f]{32}$", Pattern.CASE_INSENSITIVE);

    public LeaderboardScreen(Screen parent) {
        super(Text.literal("Leaderboard"));
        this.parent = parent;
    }

    @Override
    protected void init() {
       
        this.addDrawableChild(new TransparentButtonWidget(20, 20, 100, 25,
                Text.literal("← Back").formatted(Formatting.BOLD),
                btn -> {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(ModSounds.BUTTON_CLICK_SOUND_EVENT, 1.0F));
                    startExitTo(this.parent);
                }
        ));

        
        this.addDrawableChild(new TransparentButtonWidget(this.width - 120, 20, 100, 25,
                Text.literal("🔄 Refresh").formatted(Formatting.BOLD),
                btn -> {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(ModSounds.BUTTON_CLICK_SOUND_EVENT, 1.0F));
                    fetchLeaderboardData();
                }
        ));

        

        
        if (System.currentTimeMillis() - lastFetchTime > REFRESH_INTERVAL || leaderboardEntries.isEmpty()) {
            fetchLeaderboardData();
        }
        
        
        uiAlpha = Anim.Utils.fadeIn(animManager, 800);
        uiYOffset = Anim.Utils.slideIn(animManager, 24f, 850);
        borderPulse = animManager.add(new Anim.Tween(0f, 1f, 2200, Anim.Easings.EASE_IN_OUT_SINE));
        borderGlow = Anim.Utils.fadeIn(animManager, 1200);
    }

    private void startExitTo(Screen screen) {
        if (exiting) return;
        exiting = true;
        nextScreen = screen;
        exitAlpha = Anim.Utils.fadeOut(animManager, 480);
        exitYOffset = Anim.Utils.slideOut(animManager, 20f, 480);
    }

    private void fetchLeaderboardData() {
        this.isLoading = true;
        this.lastFetchTime = System.currentTimeMillis();
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Leaderboard] §fFetching ELO data..."));

        
        RemoteDataLoader.fetchEloData(eloMap -> {
            
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Leaderboard] §fELO data received. Fetching usernames..."));

            
            RemoteDataLoader.fetchUsernames(usernameMap -> {
               
                if (usernameMap != null) {
                    this.cachedUsernames.clear();
                    this.cachedUsernames.putAll(usernameMap);
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§b[Leaderboard] §fUsername data received. Total usernames: " + this.cachedUsernames.size()));
                }

               
                this.leaderboardEntries.clear();
                if (eloMap != null) {
                    for (Map.Entry<String, Integer> entry : eloMap.entrySet()) {
                        this.leaderboardEntries.add(entry);
                    }
                }

               
                this.leaderboardEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

                
                this.filteredEntries.clear();
                this.filteredEntries.addAll(this.leaderboardEntries);
                this.scrollOffset = 0;
                this.isLoading = false;

                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§a[Leaderboard] §fLeaderboard ready. Total entries: " + this.leaderboardEntries.size()));
            });
        });
    }



    
    private String getDisplayName(String uuid) {
       
        String cachedName = cachedUsernames.get(uuid);
        if (cachedName != null && !cachedName.equals("Unknown") && !cachedName.matches("^[0-9a-fA-F-]{32,36}$")) {
            return cachedName;
        }
        
        if (isValidUUID(uuid)) {
            return "Player" + Math.abs(uuid.hashCode() % 10000);
        } else {
            
            return uuid;
        }
    }
    
   
    private boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        
       
        if (UUID_PATTERN.matcher(uuid).matches()) {
            return true;
        }
        
       
        if (SHORT_UUID_PATTERN.matcher(uuid).matches()) {
            return true;
        }
        
        return false;
    }
    
    
    private String generateFallbackUsername(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return "Unknown";
        }
        
        
        String cleanUuid = uuid.replace("-", "");
        
       
        String base = cleanUuid.substring(0, Math.min(8, cleanUuid.length()));
        
        
        StringBuilder username = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (Character.isLetter(c)) {
                username.append(Character.toUpperCase(c));
            } else if (Character.isDigit(c)) {
                username.append(c);
            }
        }
        
      
        if (username.length() < 3) {
            username.append("Player");
        }
        
        
        int suffix = Math.abs(uuid.hashCode() % 1000);
        username.append(suffix);
        
        
        return username.toString() + " [C]";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animManager.update();
        
        RenderSystem.setShaderTexture(0, BACKGROUND);
        context.drawTexture(BACKGROUND, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        
     
        float borderAlpha = 1f;
        float pulse = borderPulse == null ? 0.5f : borderPulse.get();
        float glow = borderGlow == null ? 1f : borderGlow.get();
        
  
        int listX = this.width / 2 - 200;
        int listY = 110;
        int listWidth = 400;
        int listHeight = this.height - listY - 50;
        
        Anim.BorderRenderer.renderGlowBorder(context, listX - 10, listY - 10, listWidth + 20, listHeight + 20, borderAlpha, glow);
        Anim.BorderRenderer.renderGradientBorder(context, listX - 5, listY - 5, listWidth + 10, listHeight + 10, borderAlpha * 0.6f);

  
        float alpha = uiAlpha == null ? 1f : uiAlpha.get();
        float yOffset = uiYOffset == null ? 0f : uiYOffset.get();
        if (exiting) {
            float ea = exitAlpha == null ? 1f : exitAlpha.get();
            float ey = exitYOffset == null ? 0f : exitYOffset.get();
            alpha *= ea;
            yOffset += ey;
        }

       
        context.setShaderColor(1F, 1F, 1F, alpha);
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.literal("🏆 LEADERBOARD").formatted(Formatting.BOLD, Formatting.GOLD), 
            this.width / 2, 25 + (int)yOffset, 0xFFFFFF);
        context.setShaderColor(1F, 1F, 1F, 1F);

        if (isLoading) {
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("⏳ Loading leaderboard...").formatted(Formatting.GRAY), 
                this.width / 2, this.height / 2 + (int)yOffset, 0xFFFFFF);
        } else {
           
            context.getMatrices().push();
            context.getMatrices().translate(0, yOffset, 0);
            drawLeaderboardEntries(context, mouseX, mouseY);
            context.getMatrices().pop();
        }

        super.render(context, mouseX, mouseY, delta);

        
        if (exiting && exitAlpha != null && exitAlpha.isFinished() && nextScreen != null) {
            MinecraftClient.getInstance().setScreen(nextScreen);
            nextScreen = null;
        }
    }

    private void drawLeaderboardEntries(DrawContext context, int mouseX, int mouseY) {
        int listX = this.width / 2 - 200;
        int listY = 110; 
        int listWidth = 400; 
        int listHeight = this.height - listY - 50;

        
        context.fill(listX - 5, listY - 5, listX + listWidth + 5, listY + listHeight + 5, 0x80000000);
        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0x40000000);

        
        context.fill(listX, listY, listX + listWidth, listY + 30, 0x60000000);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Rank").formatted(Formatting.GOLD, Formatting.BOLD), listX + 15, listY + 8, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Player").formatted(Formatting.GOLD, Formatting.BOLD), listX + 80, listY + 8, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("ELO").formatted(Formatting.GOLD, Formatting.BOLD), listX + listWidth - 60, listY + 8, 0xFFFFFF);

        int startEntry = scrollOffset;
        int endEntry = Math.min(scrollOffset + VISIBLE_ENTRIES, filteredEntries.size());

        if (filteredEntries.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("No leaderboard entries found.").formatted(Formatting.GRAY), 
                this.width / 2, listY + listHeight / 2, 0xFFFFFF);
        }

        for (int i = startEntry; i < endEntry; i++) {
            Map.Entry<String, Integer> entry = filteredEntries.get(i);
            int y = listY + 30 + (i - scrollOffset) * ENTRY_HEIGHT;

           
            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= y && mouseY <= y + ENTRY_HEIGHT) {
                context.fill(listX, y, listX + listWidth, y + ENTRY_HEIGHT, 0x30FFFFFF);
            }

          
            String rank = "#" + (i + 1);
            Formatting rankFormatting = Formatting.WHITE;
            if (i == 0) rankFormatting = Formatting.GOLD; // 1st place
            else if (i == 1) rankFormatting = Formatting.GRAY; // 2nd place  
            else if (i == 2) rankFormatting = Formatting.YELLOW; // 3rd place

            String uuid = entry.getKey();
            String displayName = getDisplayName(uuid);
            
            String elo = String.valueOf(entry.getValue());

            // Rank
            context.drawTextWithShadow(this.textRenderer, 
                Text.literal(rank).formatted(rankFormatting, Formatting.BOLD), 
                listX + 15, y + (ENTRY_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFF);
            
          
            Formatting nameFormatting = Formatting.WHITE;
            boolean isFallbackName = !cachedUsernames.containsKey(uuid) || cachedUsernames.get(uuid).equals("Unknown");
            if (isFallbackName || displayName.contains("[C]")) {
                nameFormatting = Formatting.GRAY;
            }
            context.drawTextWithShadow(this.textRenderer, 
                Text.literal(displayName).formatted(nameFormatting), 
                listX + 80, y + (ENTRY_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFF);
            
            
            Formatting eloFormatting = Formatting.AQUA;
            int eloValue = entry.getValue();
            if (eloValue >= 1500) eloFormatting = Formatting.GOLD;
            else if (eloValue >= 1200) eloFormatting = Formatting.GREEN;
            else if (eloValue >= 1000) eloFormatting = Formatting.YELLOW;
            else eloFormatting = Formatting.RED;
            
            context.drawTextWithShadow(this.textRenderer, 
                Text.literal(elo).formatted(eloFormatting, Formatting.BOLD), 
                listX + listWidth - textRenderer.getWidth(elo) - 15, y + (ENTRY_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFF);
        }

        
        if (filteredEntries.size() > VISIBLE_ENTRIES) {
            int scrollbarHeight = listHeight - 30; 
            int scrollbarThumbHeight = (int) (scrollbarHeight * ((float) VISIBLE_ENTRIES / filteredEntries.size()));
            int scrollbarThumbY = listY + 30 + (int) ((scrollbarHeight - scrollbarThumbHeight) * ((float) scrollOffset / (filteredEntries.size() - VISIBLE_ENTRIES)));

            context.fill(listX + listWidth + 5, listY + 30, listX + listWidth + 10, listY + 30 + scrollbarHeight, 0x40808080);
            context.fill(listX + listWidth + 5, scrollbarThumbY, listX + listWidth + 10, scrollbarThumbY + scrollbarThumbHeight, 0xFFFFFFFF);
        }

       
        String statsText = String.format("Showing %d of %d players", filteredEntries.size(), leaderboardEntries.size());
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.literal(statsText).formatted(Formatting.GRAY), 
            this.width / 2, listY + listHeight + 10, 0xFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (filteredEntries.size() <= VISIBLE_ENTRIES) {
            return false;
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
