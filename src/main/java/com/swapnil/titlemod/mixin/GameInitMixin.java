package com.swapnil.titlemod.mixin;

import com.swapnil.titlemod.CustomTitleScreen;
import com.swapnil.titlemod.security.SecureLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(MinecraftClient.class)
public class GameInitMixin {
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        
        
        if (client.world == null) {
           
            if (client.currentScreen instanceof TitleScreen && !(client.currentScreen instanceof CustomTitleScreen)) {
                SecureLogger.logInfo("GameInitMixin detected vanilla TitleScreen, replacing with CustomTitleScreen");
                client.execute(() -> {
                    if (client.currentScreen instanceof TitleScreen && !(client.currentScreen instanceof CustomTitleScreen)) {
                        client.setScreen(new CustomTitleScreen());
                    }
                });
            }
          
            else if (client.currentScreen == null) {
                SecureLogger.logInfo("GameInitMixin detected no screen, setting CustomTitleScreen");
                client.execute(() -> {
                    if (client.currentScreen == null) {
                        client.setScreen(new CustomTitleScreen());
                    }
                });
            }
        }
    }
}
