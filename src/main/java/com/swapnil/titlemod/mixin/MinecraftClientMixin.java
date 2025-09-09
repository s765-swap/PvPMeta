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
public class MinecraftClientMixin {
    
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        
        if (screen instanceof TitleScreen && !(screen instanceof CustomTitleScreen)) {
            SecureLogger.logInfo("MinecraftClientMixin intercepted setScreen call for vanilla TitleScreen, replacing with CustomTitleScreen");
            
            ci.cancel();
            
            
            MinecraftClient client = (MinecraftClient) (Object) this;
            client.execute(() -> {
                client.setScreen(new CustomTitleScreen());
            });
        }
    }
    
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        
       
        if (client.currentScreen instanceof TitleScreen && !(client.currentScreen instanceof CustomTitleScreen)) {
            SecureLogger.logInfo("MinecraftClientMixin detected vanilla TitleScreen being rendered, replacing with CustomTitleScreen");
            client.execute(() -> {
                if (client.currentScreen instanceof TitleScreen && !(client.currentScreen instanceof CustomTitleScreen)) {
                    client.setScreen(new CustomTitleScreen());
                }
            });
        }
    }
}
