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

    private boolean isReady = false;
    private long timerStart = 0;
    private ButtonWidget readyOrCancelButton;
    private int hoveredIndex = -1;

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
                    MinecraftClient.getInstance().setScreen(parent);
                }
        ));

        createReadyOrCancelButton();
    }

    private void createReadyOrCancelButton() {
        int buttonWidth = 110;
        int imageWidth = 80;
        int spacing = 12;
        int totalWidth = 7 * imageWidth + 6 * spacing;
        int startX = (this.width - totalWidth) / 2;
        int centerIndex = 3;
        int centerX = startX + centerIndex * (imageWidth + spacing) - (buttonWidth - imageWidth) / 2;

        Text buttonText;
        ButtonWidget.PressAction action;

        if (selected.isEmpty()) {
            buttonText = Text.literal("Select a Kit").formatted(Formatting.GRAY);
            action = (btn) -> {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§c[PvP] §fPlease select at least one kit."));
            };
        } else {
            buttonText = Text.literal(isReady ? "✖ Cancel" : "✔ Ready").formatted(Formatting.BOLD, Formatting.WHITE);
            action = (btn) -> {
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
                if (isReady) {
                    isReady = false;
                    selected.clear();
                    timerStart = 0;
                } else {
                    isReady = true;
                    timerStart = System.currentTimeMillis();

                    new Thread(() -> {
                        for (Integer i : selected) {
                            System.out.println("[Client] Sending JOIN: " + KIT_NAMES[i]);
                            MatchmakingClient.xyzabc123(KIT_NAMES[i]);
                        }
                    }).start();
                }
                updateActionButtonState();
            };
        }

        readyOrCancelButton = new ModButtonWidget(
                centerX, this.height / 2 + 130, buttonWidth, 20,
                buttonText,
                action,
                BUTTON_BACKGROUND
        );
        readyOrCancelButton.active = !selected.isEmpty();

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
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                if (isReady) {
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

        hoveredIndex = -1;

        for (int i = 0; i < IMAGES.length; i++) {
            int x = startX + i * (imageWidth + spacing);
            int y = centerY;

            float scale = 1.0f;
            if (flipStartTimes.containsKey(i)) {
                long timePassed = System.currentTimeMillis() - flipStartTimes.get(i);
                if (timePassed < 300) {
                    float t = timePassed / 300f;
                    scale = 1.0f - 0.3f * (float) Math.sin(t * Math.PI);
                } else {
                    flipStartTimes.remove(i);
                }
            }

            if (mouseX >= x && mouseX <= x + imageWidth && mouseY >= y && mouseY <= y + imageHeight) {
                hoveredIndex = i;
                context.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, 0x40FFFFFF);
            }

            context.getMatrices().push();
            context.getMatrices().translate(x + imageWidth / 2.0, y + imageHeight / 2.0, 0);
            context.getMatrices().scale(scale, scale, 1);
            context.getMatrices().translate(-(x + imageWidth / 2.0), -(y + imageHeight / 2.0), 0);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F); // 40% opacity
            RenderSystem.setShaderTexture(0, IMAGES[i]);
            context.drawTexture(IMAGES[i], x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F); // Reset opacity

            context.getMatrices().pop();

            if (selected.contains(i)) {
                context.fill(x, y, x + imageWidth, y + imageHeight, 0x40FFFFFF);
                int borderColor = 0xD0FFFFFF;
                context.fill(x - 2, y - 2, x + imageWidth + 2, y, borderColor);
                context.fill(x - 2, y + imageHeight, x + imageWidth + 2, y + imageHeight + 2, borderColor);
                context.fill(x - 2, y, x, y + imageHeight, borderColor);
                context.fill(x + imageWidth, y, x + imageWidth + 2, y + imageHeight, borderColor);
            }
        }

        if (isReady) {
            long millis = System.currentTimeMillis() - timerStart;
            long seconds = millis / 1000;
            float alpha = Math.min(millis / 500f, 1.0f);
            context.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("⏰ In Queue: " + seconds), this.width / 2, 20, 0xFFFFFF);
            context.setShaderColor(1F, 1F, 1F, 1F);
        }

        if (hoveredIndex >= 0 && hoveredIndex < KIT_NAMES.length) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Kit: " + KIT_NAMES[hoveredIndex]), this.width / 2, centerY - 20, 0xFFFFDD);
        } else if (!selected.isEmpty()) {
            String selectedKitsText = "Selected: " + String.join(", ", selected.stream().map(idx -> KIT_NAMES[idx]).toArray(String[]::new));
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(selectedKitsText), this.width / 2, centerY - 20, 0xAAFFDD);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Select one or more kits to queue."), this.width / 2, centerY - 20, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
