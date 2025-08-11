package com.swapnil.titlemod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.text.Text;

public class TitleMod implements ModInitializer, ClientModInitializer {
    public static final Identifier MENU_THEME_ID = new Identifier("titlemod", "menu_theme");
    public static final SoundEvent MENU_THEME = SoundEvent.of(MENU_THEME_ID);

    // NEW: Hover Sound
    public static final Identifier BUTTON_HOVER_SOUND_ID = new Identifier("titlemod", "button_hover");
    public static final SoundEvent BUTTON_HOVER_SOUND = SoundEvent.of(BUTTON_HOVER_SOUND_ID);

    private boolean initialTitleScreenSet = false;

    @Override
    public void onInitialize() {
        // Initialize configuration
        com.swapnil.titlemod.config.ModConfig.getInstance();
        
        // Register sound events
        Registry.register(Registries.SOUND_EVENT, MENU_THEME_ID, MENU_THEME);
        Registry.register(Registries.SOUND_EVENT, BUTTON_HOVER_SOUND_ID, BUTTON_HOVER_SOUND); // Register new sound
        
        // Register custom sounds
        ModSounds.registerSounds();
        
        // Add some obfuscation (removed)
        // com.swapnil.titlemod.util.ObfuscationUtil.obfuscateExecution(0x1234);
        
        // PvpQueuePacket.register(); // If you still use this for internal queueing, keep it.
    }

    @Override
    public void onInitializeClient() {
        // Force the custom title screen on every tick if vanilla title screen is detected
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
                    if (client.currentScreen instanceof TitleScreen && !(client.currentScreen instanceof CustomTitleScreen)) {
            System.out.println("[TitleMod] Detected vanilla title screen, forcing CustomTitleScreen");
            client.setScreen(new CustomTitleScreen());
                initialTitleScreenSet = true;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            System.out.println("[TitleMod] Detected disconnect from server. Forcing CustomTitleScreen.");
            client.execute(() -> {
                client.setScreen(new CustomTitleScreen());
                MatchmakingClient.kitToEditOnJoin = null;
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (MatchmakingClient.kitToEditOnJoin != null) {
                final String kitName = MatchmakingClient.kitToEditOnJoin;
                MatchmakingClient.kitToEditOnJoin = null;

                client.execute(() -> {
                    if (client.player != null && client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatCommand("kiteditor " + kitName);
                        client.inGameHud.getChatHud().addMessage(Text.literal("§a[Kit Editor] §fSent command: /kiteditor " + kitName + " to server."));
                    } else {
                        client.inGameHud.getChatHud().addMessage(Text.literal("§c[Kit Editor] §fFailed to send kit editor command after join. Player or NetworkHandler not ready."));
                    }
                });
            }
        });
    }
}