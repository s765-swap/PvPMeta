package com.swapnil.titlemod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.swapnil.titlemod.gui.ModButtonWidget;
import com.swapnil.titlemod.gui.TransparentButtonWidget;
import com.swapnil.titlemod.queue.QueueManager;

public class PvPModeScreen extends Screen {
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

    private final Set<Integer> selected = new HashSet<>();
    private final Map<Integer, Long> flipStartTimes = new HashMap<>();

    private ButtonWidget readyOrCancelButton;
    private int hoveredIndex = -1;
    private final QueueManager queueManager = QueueManager.getInstance();
    
    // Server status tracking
    private boolean serverDown = false;
    private long lastServerCheck = 0;
    private static final long SERVER_CHECK_INTERVAL = 10000; // Check every 10 seconds

    public PvPModeScreen(Screen parent) {
        super(Text.literal("PvP Mode"));
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

        // Check server status
        checkServerStatus();
        
        createReadyOrCancelButton();
    }
    
    
    private void checkServerStatus() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastServerCheck > SERVER_CHECK_INTERVAL) {
            lastServerCheck = currentTime;
            
            // Check if matchmaking server is reachable
            queueManager.checkServerStatus(status -> {
                serverDown = !status;
                if (serverDown) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP] §fMatchmaking server is down. Please try again later.")
                    );
                }
            });
        }
    }
    
    @Override
    public void close() {
    
        super.close();
    }

    private void createReadyOrCancelButton() {
        int buttonWidth = 96;
        int buttonHeight = 26;
        int imageWidth = 80;
        int spacing = 12;
        int totalWidth = 7 * imageWidth + 6 * spacing;
        int startX = (this.width - totalWidth) / 2;
        int centerIndex = 3;
        int centerX = startX + centerIndex * (imageWidth + spacing) - (buttonWidth - imageWidth) / 2;

        Text buttonText;
        ButtonWidget.PressAction action;

        if (serverDown) {
            buttonText = Text.literal("⚠ Server Down").formatted(Formatting.BOLD, Formatting.RED);
            action = (btn) -> {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal("§c[PvP] §fMatchmaking server is currently down. Please try again later.")
                );
                checkServerStatus(); // Re-check server status
            };
        } else if (queueManager.isInQueue()) {
            buttonText = Text.literal("✖ Cancel Queue").formatted(Formatting.BOLD, Formatting.RED);
            action = (btn) -> {
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
                queueManager.leaveQueue();
                selected.clear();
                updateActionButtonState();
            };
        } else if (selected.isEmpty()) {
            buttonText = Text.literal("Select a Kit").formatted(Formatting.GRAY);
            action = (btn) -> {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§c[PvP] §fPlease select at least one kit."));
            };
        } else {
            buttonText = Text.literal("✔ Join Queue").formatted(Formatting.BOLD, Formatting.GREEN);
            action = (btn) -> {
                if (serverDown) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP] §fCannot join queue - matchmaking server is down.")
                    );
                    return;
                }
                
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
                Set<String> selectedKits = new HashSet<>();
                for (Integer i : selected) {
                    selectedKits.add(KIT_NAMES[i]);
                }
                queueManager.joinQueue(selectedKits);
                updateActionButtonState();
            };
        }

        readyOrCancelButton = new ModButtonWidget(
                centerX, this.height / 2 + 148, buttonWidth, buttonHeight,
                buttonText,
                action,
                BUTTON_BACKGROUND
        );
        readyOrCancelButton.active = !serverDown && (queueManager.isInQueue() || !selected.isEmpty());
        this.addDrawableChild(readyOrCancelButton);
    }

    private void updateActionButtonState() {
        this.remove(readyOrCancelButton);
        createReadyOrCancelButton();
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
                if (serverDown) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP] §fCannot select kits - matchmaking server is down.")
                    );
                    return true;
                }
                
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                if (queueManager.isInQueue()) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§c[PvP] §fCannot change selection while in queue. Click Cancel first."));
                    return true;
                }
                if (selected.contains(i)) {
                    selected.remove(i);
                } else {
                    selected.add(i);
                }

                flipStartTimes.put(i, System.currentTimeMillis());
                updateActionButtonState();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public String getSelectedKit() {
        if (!selected.isEmpty()) {
            int index = selected.iterator().next();
            return KIT_NAMES[index];
        }
        return null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
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
        
        // Kit grid area border
        int gridX = startX - 20;
        int gridY = centerY - 20;
        int gridWidth = totalWidth + 40;
        int gridHeight = imageHeight + 40;
        
        // Simple border
        context.fill(gridX, gridY, gridX + gridWidth, gridY + 1, 0x80FFFFFF);
        context.fill(gridX, gridY + gridHeight - 1, gridX + gridWidth, gridY + gridHeight, 0x80FFFFFF);
        context.fill(gridX, gridY, gridX + 1, gridY + gridHeight, 0x80FFFFFF);
        context.fill(gridX + gridWidth - 1, gridY, gridX + gridWidth, gridY + gridHeight, 0x80FFFFFF);

        // Check server status periodically
        checkServerStatus();

        hoveredIndex = -1;

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
                // Simple hover effect
                int hoverGlow = 0x60FFFFFF;
                context.fill(x - 3, y - 3, x + imageWidth + 3, y + imageHeight + 3, hoverGlow);
            }

            context.getMatrices().push();
            float lift = hovered ? -3.0f : 0.0f;
            float hoverScale = hovered ? 1.03f : 1.0f;
            context.getMatrices().translate(x + imageWidth / 2.0, y + imageHeight / 2.0 + lift, 0);
            context.getMatrices().scale(scale * hoverScale, scale * hoverScale, 1);
            context.getMatrices().translate(-(x + imageWidth / 2.0), -(y + imageHeight / 2.0 + lift), 0);

            // Apply alpha with server state consideration
            if (serverDown) {
                RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 0.4F); // Grayed out
            } else {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            
            RenderSystem.setShaderTexture(0, IMAGES[i]);
            context.drawTexture(IMAGES[i], x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F); // Reset opacity

            context.getMatrices().pop();

            if (selected.contains(i)) {
          
                int selectionOverlay = 0x60FFFFFF;
                context.fill(x, y, x + imageWidth, y + imageHeight, selectionOverlay);
            }
        }

        // Server status display
        if (serverDown) {
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("⚠ Matchmaking Server Down").formatted(Formatting.RED, Formatting.BOLD), 
                this.width / 2, 20, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("Please try again later").formatted(Formatting.GRAY), 
                this.width / 2, 35, 0xFFFFFF);
        } else if (queueManager.isInQueue()) {
            String queueTime = queueManager.getFormattedQueueTime();
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("⏰ In Queue: " + queueTime).formatted(Formatting.YELLOW, Formatting.BOLD), 
                this.width / 2, 20, 0xFFFFFF);
        }

        if (hoveredIndex >= 0 && hoveredIndex < KIT_NAMES.length) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Kit: " + KIT_NAMES[hoveredIndex]), this.width / 2, centerY - 20, 0xFFFFDD);
        } else if (!selected.isEmpty()) {
            String selectedKitsText = "Selected: " + String.join(", ", selected.stream().map(idx -> KIT_NAMES[idx]).toArray(String[]::new));
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(selectedKitsText), this.width / 2, centerY - 20, 0xAAFFDD);
        } else if (!serverDown) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Select one or more kits to queue."), this.width / 2, centerY - 20, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
