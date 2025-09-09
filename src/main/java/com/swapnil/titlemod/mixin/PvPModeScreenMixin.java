package com.swapnil.titlemod.mixin;

import com.swapnil.titlemod.MatchmakingClient;
import com.swapnil.titlemod.PvPModeScreen;
import com.swapnil.titlemod.security.SecureLogger;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PvPModeScreen.class)
public abstract class PvPModeScreenMixin extends Screen {

    protected PvPModeScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        
        PvPModeScreen screen = (PvPModeScreen) (Object) this;

        
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Quick Join").formatted(net.minecraft.util.Formatting.BOLD),
                btn -> {
                    this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    String selectedKit = screen.getSelectedKit();
                    if (selectedKit != null) {
                        MatchmakingClient.joinMatchmakingQueue(selectedKit);
                    } else {
                        SecureLogger.logInfo("No kit selected for PvP mode");
                    }
                }
        ).dimensions(this.width / 2 - 60, this.height - 40, 120, 20).build());
    }
}
