package com.swapnil.titlemod.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import com.swapnil.titlemod.queue.QueueManager;
import java.util.function.Supplier;

public class QueueTimerWidget extends ButtonWidget {
    private final QueueManager queueManager;
    
    public QueueTimerWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty(), (button) -> {
            QueueManager.getInstance().leaveQueue();
        }, (Supplier<MutableText> narrationSupplier) -> narrationSupplier.get());
        this.queueManager = QueueManager.getInstance();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!queueManager.isInQueue()) {
            return;
        }
        
        
        context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x80000000);
        
       
        context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + 1, 0xFFFFFFFF);
        context.fill(this.getX(), this.getY() + this.getHeight() - 1, this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFFFFFFFF);
        context.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.getHeight(), 0xFFFFFFFF);
        context.fill(this.getX() + this.getWidth() - 1, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFFFFFFFF);
        
       
        String queueTime = queueManager.getFormattedQueueTime();
        String queueText = "⏰ In Queue: " + queueTime;
        
        int textX = this.getX() + (this.getWidth() - MinecraftClient.getInstance().textRenderer.getWidth(queueText)) / 2;
        int textY = this.getY() + 8; 
        
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, 
            Text.literal(queueText).formatted(Formatting.YELLOW, Formatting.BOLD), 
            textX, textY, 0xFFFFFF);
        
        
        if (!queueManager.getQueuedKits().isEmpty()) {
            String kitsText = "Kits: " + String.join(", ", queueManager.getQueuedKits());
            int kitsTextX = this.getX() + (this.getWidth() - MinecraftClient.getInstance().textRenderer.getWidth(kitsText)) / 2;
            int kitsTextY = textY + 12;
            
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, 
                Text.literal(kitsText).formatted(Formatting.GRAY), 
                kitsTextX, kitsTextY, 0xFFFFFF);
        }
        
        
        String cancelText = "Click to Cancel";
        int cancelTextX = this.getX() + (this.getWidth() - MinecraftClient.getInstance().textRenderer.getWidth(cancelText)) / 2;
        int cancelTextY = textY + 24;
        
        
        int cancelColor = this.isMouseOver(mouseX, mouseY) ? 0xFFFF5555 : 0xFFFF8888;
        
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, 
            Text.literal(cancelText).formatted(Formatting.BOLD), 
            cancelTextX, cancelTextY, cancelColor);
    }
}
