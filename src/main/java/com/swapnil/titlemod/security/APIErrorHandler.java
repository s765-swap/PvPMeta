package com.swapnil.titlemod.security;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;


public class APIErrorHandler {
    
  
    public static void showAPIErrorPopup(String errorType, String details) {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().setScreen(new APIErrorScreen(errorType, details));
        });
    }
    
   
    public static void showGenericAPIError() {
        showAPIErrorPopup("API Validation Failed", 
            "The mod could not validate with the server.\n" +
            "This usually means you need to update the mod or there's a server issue.");
    }
    
    public static void showModUpdateRequired() {
        showAPIErrorPopup("Mod Update Required", 
            "Your mod version is outdated.\n" +
            "Please download the latest version from the official source.\n" +
            "If the problem persists, contact staff for assistance.");
    }
    
   
    public static void showServerConnectionError() {
        showAPIErrorPopup("Server Connection Error", 
            "Could not connect to the validation server.\n" +
            "Please check your internet connection and try again.\n" +
            "If the problem persists, contact staff.");
    }
    
    
    public static void showUnauthorizedAccess() {
        showAPIErrorPopup("Unauthorized Access", 
            "Access denied. Your mod installation is not authorized.\n" +
            "Please use the official mod from the correct source.\n" +
            "If you believe this is an error, contact staff.");
    }
    
    
    public static class APIErrorScreen extends Screen {
        private final String errorType;
        private final String details;
        private int centerX;
        private int centerY;
        
        public APIErrorScreen(String errorType, String details) {
            super(Text.literal("API Error"));
            this.errorType = errorType;
            this.details = details;
        }
        
        @Override
        protected void init() {
            super.init();
            
            centerX = width / 2;
            centerY = height / 2;
            
           
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Retry"), button -> {
                
                close();
                
            }).dimensions(centerX - 100, centerY + 60, 90, 20).build());
            
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Return to Menu"), button -> {
               
                MinecraftClient.getInstance().setScreen(new TitleScreen());
            }).dimensions(centerX + 10, centerY + 60, 90, 20).build());
            
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Contact Staff"), button -> {
                
                showStaffContactInfo();
            }).dimensions(centerX - 45, centerY + 85, 90, 20).build());
        }
        
        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            
            this.renderBackground(context);
            
            
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(errorType), centerX, centerY - 40, 0xFF5555);
            
            
            String[] lines = details.split("\n");
            int yOffset = centerY - 10;
            for (String line : lines) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(line), centerX, yOffset, 0xFFFFFF);
                yOffset += 12;
            }
            
            
            context.drawCenteredTextWithShadow(textRenderer, 
                Text.literal("If the problem persists, please contact staff for assistance."), 
                centerX, centerY + 35, 0xAAAAAA);
            
            super.render(context, mouseX, mouseY, delta);
        }
        
        @Override
        public boolean shouldPause() {
            return false;
        }
        
        private void showStaffContactInfo() {
           
            MinecraftClient.getInstance().setScreen(new StaffContactScreen());
        }
    }
    
    
    public static class StaffContactScreen extends Screen {
        private int centerX;
        private int centerY;
        
        public StaffContactScreen() {
            super(Text.literal("Contact Staff"));
        }
        
        @Override
        protected void init() {
            super.init();
            
            centerX = width / 2;
            centerY = height / 2;
            
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
                close();
            }).dimensions(centerX - 45, centerY + 80, 90, 20).build());
        }
        
        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            
            
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Contact Staff"), centerX, centerY - 40, 0x55AAFF);
            
            
            String[] contactInfo = {
                "If you're experiencing issues with the mod:",
                "",
                "1. Make sure you're using the official mod",
                "2. Check that your mod is up to date",
                "3. Verify your internet connection",
                "4. Contact staff through:",
                "   - Discord: [Your Discord Server]",
                "   - Email: [Your Email]",
                "   - Website: [Your Website]",
                "",
                "Please include:",
                "- Your Minecraft username",
                "- Error message details",
                "- Mod version",
                "- System information"
            };
            
            int yOffset = centerY - 20;
            for (String line : contactInfo) {
                if (line.startsWith("1.") || line.startsWith("2.") || line.startsWith("3.") || line.startsWith("4.")) {
                    context.drawCenteredTextWithShadow(textRenderer, Text.literal(line), centerX, yOffset, 0x55FF55);
                } else if (line.startsWith("-")) {
                    context.drawCenteredTextWithShadow(textRenderer, Text.literal(line), centerX, yOffset, 0xFFFF55);
                } else if (line.contains("Discord:") || line.contains("Email:") || line.contains("Website:")) {
                    context.drawCenteredTextWithShadow(textRenderer, Text.literal(line), centerX, yOffset, 0x55AAFF);
                } else {
                    context.drawCenteredTextWithShadow(textRenderer, Text.literal(line), centerX, yOffset, 0xFFFFFF);
                }
                yOffset += 12;
            }
            
            super.render(context, mouseX, mouseY, delta);
        }
        
        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
