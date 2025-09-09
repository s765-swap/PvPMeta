package com.swapnil.titlemod;

import net.minecraft.util.math.MathHelper;
import java.util.Random;

public class Particle {
    public float x, y;
    public float speedY;
    public float speedX;
    public float alpha;
    public float scale;
    public float rotation;
    public float rotationSpeed;
    public float swayAmount;
    public float swaySpeed;
    public float swayOffset;
    private final Random random = new Random();
    private long lastUpdateTime;
    private float deltaTime;

    public Particle(int screenWidth, int screenHeight, boolean randomY) {
        this.x = random.nextFloat() * screenWidth;
        this.y = randomY ? random.nextFloat() * screenHeight : -random.nextFloat() * screenHeight;
        
       
        this.speedY = 0.05F + random.nextFloat() * 0.15F; 
        this.speedX = (random.nextFloat() - 0.5F) * 0.02F; 
        this.alpha = 0.3F + random.nextFloat() * 0.7F; 
        this.scale = 0.8F + random.nextFloat() * 0.4F; 
        this.rotation = random.nextFloat() * 360F; 
        this.rotationSpeed = (random.nextFloat() - 0.5F) * 0.5F; 
        this.swayAmount = 0.5F + random.nextFloat() * 1.0F; 
        this.swaySpeed = 0.01F + random.nextFloat() * 0.02F; 
        this.swayOffset = random.nextFloat() * (float)Math.PI * 2F; 
        
        this.lastUpdateTime = System.currentTimeMillis();
        this.deltaTime = 0f;
    }

    public void update(int screenWidth, int screenHeight) {
        long currentTime = System.currentTimeMillis();
        this.deltaTime = (currentTime - lastUpdateTime) / 16.67f; 
        this.deltaTime = Math.min(this.deltaTime, 2.0f); 
        this.lastUpdateTime = currentTime;
        
     
        this.y += this.speedY * this.deltaTime;

        float swayX = (float)Math.sin(this.y * this.swaySpeed + this.swayOffset) * this.swayAmount;
        this.x += this.speedX * this.deltaTime + swayX * 0.01f;
        
       
        this.rotation += this.rotationSpeed * this.deltaTime;
        if (this.rotation > 360F) this.rotation -= 360F;
        if (this.rotation < 0F) this.rotation += 360F;
        
       
        float heightFactor = 1.0f - (this.y / screenHeight);
        this.alpha = Math.max(0.1f, Math.min(1.0f, heightFactor * (0.3F + random.nextFloat() * 0.7F)));
        
      
        if (this.y > screenHeight + 10) {
            resetParticle(screenWidth, screenHeight);
        }
        
       
        if (this.x < -10) {
            this.x = screenWidth + 10;
        } else if (this.x > screenWidth + 10) {
            this.x = -10;
        }
    }
    
    private void resetParticle(int screenWidth, int screenHeight) {
        this.y = -10 - random.nextFloat() * 20; 
        this.x = random.nextFloat() * screenWidth;
        
    
        this.speedY = 0.05F + random.nextFloat() * 0.15F;
        this.speedX = (random.nextFloat() - 0.5F) * 0.02F;
        this.alpha = 0.3F + random.nextFloat() * 0.7F;
        this.scale = 0.8F + random.nextFloat() * 0.4F;
        this.rotation = random.nextFloat() * 360F;
        this.rotationSpeed = (random.nextFloat() - 0.5F) * 0.5F;
        this.swayAmount = 0.5F + random.nextFloat() * 1.0F;
        this.swaySpeed = 0.01F + random.nextFloat() * 0.02F;
        this.swayOffset = random.nextFloat() * (float)Math.PI * 2F;
    }
    

    public float getX() { return x; }
    public float getY() { return y; }
    public float getAlpha() { return alpha; }
    public float getScale() { return scale; }
    public float getRotation() { return rotation; }
    public float getDeltaTime() { return deltaTime; }
    
   
    public static Particle createCustomParticle(int screenWidth, int screenHeight, float speedMultiplier, float sizeMultiplier) {
        Particle particle = new Particle(screenWidth, screenHeight, true);
        particle.speedY *= speedMultiplier;
        particle.scale *= sizeMultiplier;
        return particle;
    }
}
