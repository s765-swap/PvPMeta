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


@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    
   
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInitMethod(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen currentScreen = client.currentScreen;
        
        
        if (currentScreen instanceof TitleScreen && !(currentScreen instanceof CustomTitleScreen)) {
            SecureLogger.logInfo("Mixin intercepted vanilla TitleScreen init, replacing with CustomTitleScreen");
            
          
            client.execute(() -> {
                if (client.currentScreen instanceof TitleScreen && !(client.currentScreen instanceof CustomTitleScreen)) {
                    client.setScreen(new CustomTitleScreen());
                }
            });
            
          
            ci.cancel();
        }
    }
}
