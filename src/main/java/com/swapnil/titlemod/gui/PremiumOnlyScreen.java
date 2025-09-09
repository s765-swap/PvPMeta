package com.swapnil.titlemod.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import com.swapnil.titlemod.CustomTitleScreen;

public class PremiumOnlyScreen extends Screen {
    private static final Identifier BACKGROUND_TEXTURE = new Identifier("titlemod", "textures/gui/background.png");
    private static final Identifier WARNING_ICON = new Identifier("titlemod", "textures/gui/warning.png");
    
    private final String reason;
    private final boolean isUsernameChange;
    
    public PremiumOnlyScreen(String reason) {
        super(Text.literal("Premium Account Required"));
        this.reason = reason;
        this.isUsernameChange = reason.contains("username change") || reason.contains("cracked");
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal(""), button -> {})
            .dimensions(centerX - 100, centerY - 80, 200, 40)
            .build());
        
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal(""), button -> {})
            .dimensions(centerX - 150, centerY - 20, 300, 60)
            .build());
        
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Return to Main Menu"), button -> {
            MinecraftClient.getInstance().setScreen(new CustomTitleScreen());
        }).dimensions(centerX - 100, centerY + 60, 200, 20).build());
        
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Exit Game"), button -> {
            MinecraftClient.getInstance().scheduleStop();
        }).dimensions(centerX - 100, centerY + 90, 200, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        
        this.renderBackground(context);
        
        
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.drawTexture(BACKGROUND_TEXTURE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        
        context.drawTexture(WARNING_ICON, centerX - 16, centerY - 100, 0, 0, 32, 32, 32, 32);
        
        
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.literal("§c§lPREMIUM ACCOUNT REQUIRED"), 
            centerX, centerY - 80, 0xFFFFFF);
        
        
        String[] lines = reason.split("\n");
        int yOffset = centerY - 20;
        for (String line : lines) {
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal(line), 
                centerX, yOffset, 0xFFFFFF);
            yOffset += 12;
        }
        
        
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.literal("§eThis mod requires a premium Minecraft account."), 
            centerX, centerY + 20, 0xFFFFFF);
        
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.literal("§7Please purchase Minecraft from Mojang/Microsoft"), 
            centerX, centerY + 35, 0xFFFFFF);
        
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.literal("§7to access online features."), 
            centerX, centerY + 50, 0xFFFFFF);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
    
    public static void showPremiumRequired(String reason) {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().setScreen(new PremiumOnlyScreen(reason));
        });
    }
    
    public static void showCrackedAccountDetected() {
        showPremiumRequired("§c§lCracked/Offline Account Detected!\n\n" +
                          "§fThis mod requires a premium Minecraft account.\n" +
                          "§fPlease use a legitimate Microsoft account to continue.");
    }
    
    public static void showUsernameChangeDetected() {
        showPremiumRequired("§c§lUsername Change Detected!\n\n" +
                          "§fYour username has changed since last login.\n" +
                          "§fThis may indicate a cracked account.\n" +
                          "§fPlease use a legitimate Microsoft account.");
    }
}
