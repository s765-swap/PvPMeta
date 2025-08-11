package com.swapnil.titlemod.mixin;

import com.swapnil.titlemod.CustomTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin ensures that the vanilla title screen is replaced with our custom one.
 * It safely intercepts the init method without causing infinite loops.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    
    // Only intercept once during initialization
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInitMethod(CallbackInfo ci) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof TitleScreen && !(currentScreen instanceof CustomTitleScreen)) {
            // Schedule the screen change for the next tick to avoid recursion
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().currentScreen instanceof TitleScreen) {
                    MinecraftClient.getInstance().setScreen(new CustomTitleScreen());
                }
            });
            ci.cancel();
        }
    }
}
