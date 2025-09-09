package com.swapnil.titlemod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

import com.swapnil.titlemod.gui.ModButtonWidget;
import com.swapnil.titlemod.gui.TransparentButtonWidget;

public class KitEditorScreen extends Screen {
    private final Screen parent;

    private static final Identifier[] IMAGES = new Identifier[7];
    private static final Identifier BACKGROUND = new Identifier("titlemod", "textures/gui/pvp_background.png");
    private static final Identifier BUTTON_BACKGROUND = new Identifier("titlemod", "textures/gui/buttonbg.png");
    private static final String[] KIT_NAMES = { "NETHPOT", "UHC", "AXE", "POT", "SMP", "SWORD", "CPVP" };

    static {
        for (int i = 0; i < 7; i++) {
            IMAGES[i] = new Identifier("titlemod", "textures/gui/pvp_" + (i + 1) + ".png");
        }
    }

    private int selectedKitIndex = -1;
    private final Map<Integer, Long> flipStartTimes = new HashMap<>();
    private ButtonWidget actionButton;

    public KitEditorScreen(Screen parent) {
        super(Text.literal("Kit Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        
        this.addDrawableChild(new TransparentButtonWidget(10, 10, 90, 20,
                Text.literal("← Back").formatted(Formatting.BOLD),
                btn -> {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    MinecraftClient.getInstance().setScreen(new CustomTitleScreen());
                }
        ));

        createActionButton();
    }


    private void createActionButton() {
        int buttonWidth = 110;
        int imageWidth = 80;
        int spacing = 12;
        int totalWidth = 7 * imageWidth + 6 * spacing;
        int startX = (this.width - totalWidth) / 2;
        int centerIndex = 3;
        int centerX = startX + centerIndex * (imageWidth + spacing) - (buttonWidth - imageWidth) / 2;

        Text buttonText;
        ButtonWidget.PressAction action;

        if (selectedKitIndex != -1) {
            buttonText = Text.literal("✎ Edit Kit").formatted(Formatting.BOLD, Formatting.WHITE);
            action = (btn) -> {
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
                String kitName = KIT_NAMES[selectedKitIndex];
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§a[Kit Editor] §fConnecting to kit editor server (" + com.swapnil.titlemod.config.ModConfig.getInstance().getKitEditorServerIp() + ":" + com.swapnil.titlemod.config.ModConfig.getInstance().getKitEditorMinecraftPort() + ") for kit §b" + kitName + "§f..."));
                
                
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        Thread.sleep(1500); 
                        MatchmakingClient.joinKitEditor(kitName);
                        MinecraftClient.getInstance().setScreen(null);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            };
        } else {
            buttonText = Text.literal("Select a Kit").formatted(Formatting.GRAY);
            action = (btn) -> {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§c[Kit Editor] §fPlease select a kit to edit."));
            };
        }

        actionButton = new ModButtonWidget(
                centerX, this.height / 2 + 130, buttonWidth, 20,
                buttonText,
                action,
                BUTTON_BACKGROUND
        );
        actionButton.active = (selectedKitIndex != -1);

        this.addDrawableChild(actionButton);
    }

    private void updateActionButtonState() {
        this.remove(actionButton);
        createActionButton();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int imageWidth = 80;
        int imageHeight = 115;
        int spacing = 12;
        int totalWidth = 7 * imageWidth + 6 * spacing;
        int startX = (this.width - totalWidth) / 2;
        int centerY = (this.height - imageHeight) / 2;

        for (int i = 0; i < IMAGES.length; i++) {
            int x = startX + i * (imageWidth + spacing);
            int y = centerY;

            if (mouseX >= x && mouseX <= x + imageWidth && mouseY >= y && mouseY <= y + imageHeight) {
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                selectedKitIndex = i;
                flipStartTimes.put(i, System.currentTimeMillis());
                updateActionButtonState();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
       
        RenderSystem.setShaderTexture(0, BACKGROUND);
        context.setShaderColor(1F, 1F, 1F, 1F);
        context.drawTexture(BACKGROUND, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        
        int imageWidth = 80;
        int imageHeight = 115;
        int spacing = 12;
        int totalImages = IMAGES.length;
        int totalWidth = totalImages * imageWidth + (totalImages - 1) * spacing;

        int startX = (this.width - totalWidth) / 2;
        int centerY = (this.height - imageHeight) / 2;
        
        
        int gridX = startX - 20;
        int gridY = centerY - 20;
        int gridWidth = totalWidth + 40;
        int gridHeight = imageHeight + 40;
        
       
        context.fill(gridX, gridY, gridX + gridWidth, gridY + 1, 0x80FFFFFF);
        context.fill(gridX, gridY + gridHeight - 1, gridX + gridWidth, gridY + gridHeight, 0x80FFFFFF);
        context.fill(gridX, gridY, gridX + 1, gridY + gridHeight, 0x80FFFFFF);
        context.fill(gridX + gridWidth - 1, gridY, gridX + gridWidth, gridY + gridHeight, 0x80FFFFFF);

        int hoveredIndex = -1;

        for (int i = 0; i < IMAGES.length; i++) {
            int x = startX + i * (imageWidth + spacing);
            int y = centerY;

            float scale = 1.0f;

            if (flipStartTimes.containsKey(i)) {
                long timePassed = System.currentTimeMillis() - flipStartTimes.get(i);
                if (timePassed < 300) {
                    float flipT = timePassed / 300f;
                    scale = 1.0f - 0.3f * (float) Math.sin(flipT * Math.PI);
                } else {
                    flipStartTimes.remove(i);
                }
            }

            boolean hovered = mouseX >= x && mouseX <= x + imageWidth && mouseY >= y && mouseY <= y + imageHeight;
            if (hovered) {
                hoveredIndex = i;
                int hoverGlow = 0x40FFFFFF;
                context.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, hoverGlow);
            }

            context.getMatrices().push();
            float lift = hovered ? -3.0f : 0.0f;
            float hoverScale = hovered ? 1.03f : 1.0f;
            context.getMatrices().translate(x + imageWidth / 2.0, y + imageHeight / 2.0 + lift, 0);
            context.getMatrices().scale(scale * hoverScale, scale * hoverScale, 1);
            context.getMatrices().translate(-(x + imageWidth / 2.0), -(y + imageHeight / 2.0 + lift), 0);

            RenderSystem.setShaderTexture(0, IMAGES[i]);
            context.setShaderColor(1F, 1F, 1F, 1F);
            context.drawTexture(IMAGES[i], x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

            context.getMatrices().pop();

            
            if (hovered) {
                int shadowColor = 0x30000000;
                context.fill(x + 6, y + imageHeight - 3, x + imageWidth - 6, y + imageHeight + 3, shadowColor);
            }

            if (selectedKitIndex == i) {
                
                context.fill(x, y, x + imageWidth, y + imageHeight, 0x40FFFFFF);
                int borderColor = 0xC0FFFF;
                context.fill(x - 2, y - 2, x + imageWidth + 2, y, borderColor);
                context.fill(x - 2, y + imageHeight, x + imageWidth + 2, y + imageHeight + 2, borderColor);
                context.fill(x - 2, y, x, y + imageHeight, borderColor);
                context.fill(x + imageWidth, y, x + imageWidth + 2, y + imageHeight, borderColor);
            }
        }

        if (hoveredIndex >= 0 && hoveredIndex < KIT_NAMES.length) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Kit: " + KIT_NAMES[hoveredIndex]), this.width / 2, centerY - 20, 0xFFFFDD);
        } else if (selectedKitIndex != -1) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Selected Kit: " + KIT_NAMES[selectedKitIndex]), this.width / 2, centerY - 20, 0xAAFFDD);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Select a kit to edit."), this.width / 2, centerY - 20, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
