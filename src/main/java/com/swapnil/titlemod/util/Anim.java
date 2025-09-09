package com.swapnil.titlemod.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class Anim {
    public interface Easing {
        float ease(float t);
    }

    public static final class Easings {
        public static final Easing LINEAR = t -> t;
        
        
        public static final Easing EASE_IN_QUAD = t -> t * t;
        public static final Easing EASE_OUT_QUAD = t -> t * (2f - t);
        public static final Easing EASE_IN_OUT_QUAD = t -> (t < 0.5f) ? 2f * t * t : -1f + (4f - 2f * t) * t;
        
        
        public static final Easing EASE_IN_CUBIC = t -> t * t * t;
        public static final Easing EASE_OUT_CUBIC = t -> {
            float u = t - 1f;
            return u * u * u + 1f;
        };
        public static final Easing EASE_IN_OUT_CUBIC = t -> (t < 0.5f)
                ? 4f * t * t * t
                : (t - 1f) * (2f * t - 2f) * (2f * t - 2f) + 1f;
        
        
        public static final Easing EASE_IN_QUART = t -> t * t * t * t;
        public static final Easing EASE_OUT_QUART = t -> 1f - (float)Math.pow(1f - t, 4);
        public static final Easing EASE_IN_OUT_QUART = t -> (t < 0.5f)
                ? 8f * t * t * t * t
                : 1f - 8f * (float)Math.pow(-t + 1f, 4);
        
        
        public static final Easing EASE_IN_QUINT = t -> t * t * t * t * t;
        public static final Easing EASE_OUT_QUINT = t -> 1f - (float)Math.pow(1f - t, 5);
        public static final Easing EASE_IN_OUT_QUINT = t -> (t < 0.5f)
                ? 16f * t * t * t * t * t
                : 1f - (float)Math.pow(-2f * t + 2f, 5) / 2f;
        
        
        public static final Easing EASE_IN_SINE = t -> 1f - (float)Math.cos((t * Math.PI) / 2.0);
        public static final Easing EASE_OUT_SINE = t -> (float)Math.sin((t * Math.PI) / 2.0);
        public static final Easing EASE_IN_OUT_SINE = t -> -(float)(Math.cos(Math.PI * t) - 1f) / 2f;
        
        
        public static final Easing EASE_IN_EXPO = t -> t == 0f ? 0f : (float)Math.pow(2f, 10f * t - 10f);
        public static final Easing EASE_OUT_EXPO = t -> t == 1f ? 1f : 1f - (float)Math.pow(2f, -10f * t);
        public static final Easing EASE_IN_OUT_EXPO = t -> {
            if (t == 0f) return 0f;
            if (t == 1f) return 1f;
            if (t < 0.5f) return (float)Math.pow(2f, 20f * t - 10f) / 2f;
            return (2f - (float)Math.pow(2f, -20f * t + 10f)) / 2f;
        };
        
       
        public static final Easing EASE_IN_CIRC = t -> 1f - (float)Math.sqrt(1f - t * t);
        public static final Easing EASE_OUT_CIRC = t -> (float)Math.sqrt(1f - (t - 1f) * (t - 1f));
        public static final Easing EASE_IN_OUT_CIRC = t -> {
            if (t < 0.5f) return (1f - (float)Math.sqrt(1f - 4f * t * t)) / 2f;
            return ((float)Math.sqrt(1f - 4f * (t - 1f) * (t - 1f)) + 1f) / 2f;
        };
        
       
        public static final Easing EASE_IN_ELASTIC = t -> {
            if (t == 0f) return 0f;
            if (t == 1f) return 1f;
            return -(float)Math.pow(2f, 10f * t - 10f) * (float)Math.sin((t * 10f - 10.75f) * ((2f * Math.PI) / 3f));
        };
        public static final Easing EASE_OUT_ELASTIC = t -> {
            if (t == 0f) return 0f;
            if (t == 1f) return 1f;
            return (float)Math.pow(2f, -10f * t) * (float)Math.sin((t * 10f - 0.75f) * ((2f * Math.PI) / 3f)) + 1f;
        };
        
        
        public static final Easing EASE_IN_BACK = t -> {
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            return c3 * t * t * t - c1 * t * t;
        };
        public static final Easing EASE_OUT_BACK = t -> {
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            return 1f + c3 * (float)Math.pow(t - 1f, 3) + c1 * (float)Math.pow(t - 1f, 2);
        };
        public static final Easing EASE_IN_OUT_BACK = t -> {
            float c1 = 1.70158f;
            float c2 = c1 * 1.525f;
            if (t < 0.5f) return ((2f * t) * (2f * t) * ((c2 + 1f) * 2f * t - c2)) / 2f;
            return ((2f * t - 2f) * (2f * t - 2f) * ((c2 + 1f) * (t * 2f - 2f) + c2) + 2f) / 2f;
        };
        
        
        public static final Easing EASE_OUT_BOUNCE = t -> {
            if (t < 1f / 2.75f) return 7.5625f * t * t;
            if (t < 2f / 2.75f) return 7.5625f * (t -= 1.5f / 2.75f) * t + 0.75f;
            if (t < 2.5f / 2.75f) return 7.5625f * (t -= 2.25f / 2.75f) * t + 0.9375f;
            return 7.5625f * (t -= 2.625f / 2.75f) * t + 0.984375f;
        };
        public static final Easing EASE_IN_BOUNCE = t -> 1f - EASE_OUT_BOUNCE.ease(1f - t);
        public static final Easing EASE_IN_OUT_BOUNCE = t -> {
            if (t < 0.5f) return EASE_IN_BOUNCE.ease(2f * t) / 2f;
            return EASE_OUT_BOUNCE.ease(2f * t - 1f) / 2f + 0.5f;
        };
    }

    public static final class Tween {
        private final float from;
        private final float to;
        private final long durationMs;
        private final long startTimeMs;
        private final Easing easing;
        private float value;
        private boolean finished;
        private boolean paused;
        private long pauseTimeMs;
        private long totalPauseTimeMs;

        public Tween(float from, float to, long durationMs, Easing easing) {
            this.from = from;
            this.to = to;
            this.durationMs = Math.max(1, durationMs);
            this.easing = easing == null ? Easings.LINEAR : easing;
            this.startTimeMs = System.currentTimeMillis();
            this.value = from;
            this.finished = false;
            this.paused = false;
            this.pauseTimeMs = 0;
            this.totalPauseTimeMs = 0;
        }

        public float get() {
            if (finished) return value;
            if (paused) return value;
            
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTimeMs - totalPauseTimeMs;
            float t = Math.min(1f, Math.max(0f, (float) elapsed / (float) durationMs));
            float k = easing.ease(t);
            value = from + (to - from) * k;
            
            if (elapsed >= durationMs) {
                finished = true;
                value = to;
            }
            return value;
        }

        public boolean isFinished() {
            return finished;
        }
        
        public void pause() {
            if (!paused && !finished) {
                paused = true;
                pauseTimeMs = System.currentTimeMillis();
            }
        }
        
        public void resume() {
            if (paused && !finished) {
                paused = false;
                totalPauseTimeMs += System.currentTimeMillis() - pauseTimeMs;
            }
        }
        
        public void reset() {
            finished = false;
            paused = false;
            value = from;
            totalPauseTimeMs = 0;
        }
        
        public void setValue(float newValue) {
            value = newValue;
        }
        
        public float getProgress() {
            if (finished) return 1f;
            if (paused) return (float)(pauseTimeMs - startTimeMs - totalPauseTimeMs) / (float)durationMs;
            
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTimeMs - totalPauseTimeMs;
            return Math.min(1f, Math.max(0f, (float) elapsed / (float) durationMs));
        }
    }

    public static final class Manager {
        private final List<Tween> tweens = new ArrayList<>();
        private final List<Tween> pendingAdd = new ArrayList<>();
        private final List<Tween> pendingRemove = new ArrayList<>();

        public Tween add(Tween tween) {
            pendingAdd.add(tween);
            return tween;
        }
        
        public void remove(Tween tween) {
            pendingRemove.add(tween);
        }

        public void update() {
            
            if (!pendingAdd.isEmpty()) {
                tweens.addAll(pendingAdd);
                pendingAdd.clear();
            }
            
            
            if (!pendingRemove.isEmpty()) {
                tweens.removeAll(pendingRemove);
                pendingRemove.clear();
            }
            
            
            Iterator<Tween> it = tweens.iterator();
            while (it.hasNext()) {
                Tween t = it.next();
                t.get();
                if (t.isFinished()) {
                    it.remove();
                }
            }
        }
        
        public void update(float deltaTime) {
           
            update();
        }

        public boolean isIdle() {
            return tweens.isEmpty() && pendingAdd.isEmpty();
        }
        
        public void clear() {
            tweens.clear();
            pendingAdd.clear();
            pendingRemove.clear();
        }
        
        public int getActiveTweenCount() {
            return tweens.size();
        }
        
        public void pauseAll() {
            for (Tween tween : tweens) {
                tween.pause();
            }
        }
        
        public void resumeAll() {
            for (Tween tween : tweens) {
                tween.resume();
            }
        }
    }
    
    
    public static class Utils {
        public static Tween fadeIn(Manager manager, long duration) {
            return manager.add(new Tween(0f, 1f, duration, Easings.EASE_OUT_SINE));
        }
        
        public static Tween fadeOut(Manager manager, long duration) {
            return manager.add(new Tween(1f, 0f, duration, Easings.EASE_IN_SINE));
        }
        
        public static Tween slideIn(Manager manager, float distance, long duration) {
            return manager.add(new Tween(distance, 0f, duration, Easings.EASE_OUT_QUINT));
        }
        
        public static Tween slideOut(Manager manager, float distance, long duration) {
            return manager.add(new Tween(0f, distance, duration, Easings.EASE_IN_QUINT));
        }
        
        public static Tween scaleIn(Manager manager, long duration) {
            return manager.add(new Tween(0.8f, 1f, duration, Easings.EASE_OUT_BACK));
        }
        
        public static Tween scaleOut(Manager manager, long duration) {
            return manager.add(new Tween(1f, 0.8f, duration, Easings.EASE_IN_BACK));
        }
        
        public static Tween bounce(Manager manager, long duration) {
            return manager.add(new Tween(0f, 1f, duration, Easings.EASE_OUT_BOUNCE));
        }
        
        public static Tween elastic(Manager manager, long duration) {
            return manager.add(new Tween(0f, 1f, duration, Easings.EASE_OUT_ELASTIC));
        }
    }
    
    
    public static class BorderRenderer {
        private static final Identifier BORDER_CORNER = new Identifier("titlemod", "textures/gui/border_corner.png");
        private static final Identifier BORDER_EDGE = new Identifier("titlemod", "textures/gui/border_edge.png");
        
        public static void renderAnimatedBorder(DrawContext context, int x, int y, int width, int height, float alpha, float pulse) {
            int borderSize = 8;
            int cornerSize = 16;
            
            
            float pulseAlpha = alpha * (0.7f + 0.3f * pulse);
            int borderColor = (int)(0x80 * pulseAlpha) << 24 | 0x00FFFFFF;
            int cornerColor = (int)(0xC0 * pulseAlpha) << 24 | 0x00FFFFFF;
            
            
            context.fill(x, y, x + cornerSize, y + cornerSize, cornerColor);
            context.fill(x + width - cornerSize, y, x + width, y + cornerSize, cornerColor);
            context.fill(x, y + height - cornerSize, x + cornerSize, y + height, cornerColor);
            context.fill(x + width - cornerSize, y + height - cornerSize, x + width, y + height, cornerColor);
            
            
            context.fill(x + cornerSize, y, x + width - cornerSize, y + borderSize, borderColor);
            context.fill(x + cornerSize, y + height - borderSize, x + width - cornerSize, y + height, borderColor);
            context.fill(x, y + cornerSize, x + borderSize, y + height - cornerSize, borderColor);
            context.fill(x + width - borderSize, y + cornerSize, x + width, y + height - cornerSize, borderColor);
        }
        
        public static void renderGradientBorder(DrawContext context, int x, int y, int width, int height, float alpha) {
            int borderSize = 3;
            
            
            for (int i = 0; i < borderSize; i++) {
                float gradientAlpha = alpha * (0.3f - 0.2f * (i / (float)borderSize));
                int color = (int)(0x40 * gradientAlpha) << 24 | 0x00FFFFFF;
                context.fill(x, y + i, x + width, y + i + 1, color);
            }
            
           
            for (int i = 0; i < borderSize; i++) {
                float gradientAlpha = alpha * (0.3f - 0.2f * (i / (float)borderSize));
                int color = (int)(0x40 * gradientAlpha) << 24 | 0x00FFFFFF;
                context.fill(x, y + height - i - 1, x + width, y + height - i, color);
            }
            
            
            for (int i = 0; i < borderSize; i++) {
                float gradientAlpha = alpha * (0.3f - 0.2f * (i / (float)borderSize));
                int color = (int)(0x40 * gradientAlpha) << 24 | 0x00FFFFFF;
                context.fill(x + i, y, x + i + 1, y + height, color);
            }
            
           
            for (int i = 0; i < borderSize; i++) {
                float gradientAlpha = alpha * (0.3f - 0.2f * (i / (float)borderSize));
                int color = (int)(0x40 * gradientAlpha) << 24 | 0x00FFFFFF;
                context.fill(x + width - i - 1, y, x + width - i, y + height, color);
            }
        }
        
        public static void renderGlowBorder(DrawContext context, int x, int y, int width, int height, float alpha, float glowIntensity) {
            int glowSize = 8;
            
            
            for (int i = 0; i < glowSize; i++) {
                float glowAlpha = alpha * glowIntensity * (1f - i / (float)glowSize) * 0.3f;
                int glowColor = (int)(0x40 * glowAlpha) << 24 | 0x00FFFFFF;
                
                context.fill(x - i, y - i, x + width + i, y - i + 1, glowColor); // Top
                context.fill(x - i, y + height + i - 1, x + width + i, y + height + i, glowColor); // Bottom
                context.fill(x - i, y - i, x - i + 1, y + height + i, glowColor); // Left
                context.fill(x + width + i - 1, y - i, x + width + i, y + height + i, glowColor); // Right
            }
            
            // Inner border
            int borderColor = (int)(0x80 * alpha) << 24 | 0x00FFFFFF;
            context.fill(x, y, x + width, y + 1, borderColor);
            context.fill(x, y + height - 1, x + width, y + height, borderColor);
            context.fill(x, y, x + 1, y + height, borderColor);
            context.fill(x + width - 1, y, x + width, y + height, borderColor);
        }
        
        public static void renderRoundedBorder(DrawContext context, int x, int y, int width, int height, float alpha, int cornerRadius) {
            int borderColor = (int)(0xA0 * alpha) << 24 | 0x00FFFFFF;
            
            
            for (int i = 0; i < cornerRadius; i++) {
                for (int j = 0; j < cornerRadius; j++) {
                    float distance = (float)Math.sqrt(i * i + j * j);
                    if (distance <= cornerRadius) {
                        float cornerAlpha = alpha * (1f - distance / cornerRadius);
                        int color = (int)(0xA0 * cornerAlpha) << 24 | 0x00FFFFFF;
                        
                       
                        context.fill(x + i, y + j, x + i + 1, y + j + 1, color);
                        
                        context.fill(x + width - i - 1, y + j, x + width - i, y + j + 1, color);
                       
                        context.fill(x + i, y + height - j - 1, x + i + 1, y + height - j, color);
                       
                        context.fill(x + width - i - 1, y + height - j - 1, x + width - i, y + height - j, color);
                    }
                }
            }
            
            
            context.fill(x + cornerRadius, y, x + width - cornerRadius, y + 1, borderColor);
            context.fill(x + cornerRadius, y + height - 1, x + width - cornerRadius, y + height, borderColor);
            context.fill(x, y + cornerRadius, x + 1, y + height - cornerRadius, borderColor);
            context.fill(x + width - 1, y + cornerRadius, x + width, y + height - cornerRadius, borderColor);
        }
    }
}


