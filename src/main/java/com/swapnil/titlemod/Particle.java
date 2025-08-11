package com.swapnil.titlemod;

import net.minecraft.util.math.MathHelper;
import java.util.Random;

public class Particle {
    public float x, y;
    public float speedY;
    public float alpha;
    private final Random random = new Random();

    public Particle(int screenWidth, int screenHeight, boolean randomY) {
        this.x = random.nextFloat() * screenWidth;
        this.y = randomY ? random.nextFloat() * screenHeight : -random.nextFloat() * screenHeight; // Start above or at random Y
        // MODIFIED: speedY for slower, more realistic snowfall
        this.speedY = 0.1F + random.nextFloat() * 0.4F; // Speed between 0.1 and 0.5
        this.alpha = 0.5F + random.nextFloat() * 0.5F; // Alpha between 0.5 and 1.0
    }

    public void update(int screenWidth, int screenHeight) {
        this.y += this.speedY;
        // MODIFIED: Reduced horizontal sway for gentler drift
        this.x += MathHelper.sin(this.y * 0.05F) * 0.2F; // Subtle horizontal sway (reduced from 0.5F to 0.2F)

        // Reset particle if it goes off-screen
        if (this.y > screenHeight) {
            this.y = -8; // Reset to just above the screen
            this.x = random.nextFloat() * screenWidth; // New random X
            // MODIFIED: speedY for slower, more realistic snowfall upon reset
            this.speedY = 0.1F + random.nextFloat() * 0.4F;
            this.alpha = 0.5F + random.nextFloat() * 0.5F;
        }
    }
}
