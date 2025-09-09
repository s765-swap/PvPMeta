package com.swapnil.titlemod;

import com.swapnil.titlemod.security.SecureLogger;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

public class TitleMod implements ModInitializer, ClientModInitializer {
    public static final Identifier MENU_THEME_ID = new Identifier("titlemod", "menu_theme");
    public static final SoundEvent MENU_THEME = SoundEvent.of(MENU_THEME_ID);

    // NEW: Hover Sound
    public static final Identifier BUTTON_HOVER_SOUND_ID = new Identifier("titlemod", "button_hover");
    public static final SoundEvent BUTTON_HOVER_SOUND = SoundEvent.of(BUTTON_HOVER_SOUND_ID);

    private boolean initialTitleScreenSet = false;
    private boolean forceCustomTitleScreen = true; // Always force custom title screen
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        // Initialize security systems first
        if (!com.swapnil.titlemod.security.AntiTamper.isValidEnvironment()) {
            SecureLogger.logSecurityEvent("Security check failed - mod will not initialize");
            return;
        }
        
        // Initialize secure logger
        com.swapnil.titlemod.security.SecureLogger.logInfo("TitleMod initializing with enhanced security");
        
        // Initialize enhanced premium validator
        com.swapnil.titlemod.security.EnhancedPremiumValidator.initialize();
        
        // Initialize configuration
        com.swapnil.titlemod.config.ModConfig.getInstance();
        
        // Initialize player data manager
        com.swapnil.titlemod.data.PlayerDataManager.getInstance();
        
        // Register sound events
        Registry.register(Registries.SOUND_EVENT, MENU_THEME_ID, MENU_THEME);
        Registry.register(Registries.SOUND_EVENT, BUTTON_HOVER_SOUND_ID, BUTTON_HOVER_SOUND);
        
        // Register custom sounds
        ModSounds.registerSounds();
        
        // Log successful initialization
        com.swapnil.titlemod.security.SecureLogger.logInfo("TitleMod initialized successfully");
    }

    @Override
    public void onInitializeClient() {
        // Delay sending JOIN event by 30 seconds after mod start
        new Thread(() -> {
            try {
                Thread.sleep(30000); // 30 seconds
                String username = MinecraftClient.getInstance().getSession().getUsername();
                String publicIp = fetchPublicIp();
                String modVersion = "pvpmeta-beta1";
                String payload = "JOIN::" + username + "::" + modVersion + "::" + publicIp;
                System.out.println("[Mod Logger] Attempting to send: " + payload);
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("34.159.92.94", 5050), 5000);
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out.println(payload);
                    String response = in.readLine();
                    System.out.println("[Mod Logger] Logger server response: " + response);
                    if (response != null && response.startsWith("POPUP:")) {
                        String msg = response.substring(6).trim();
                        MinecraftClient.getInstance().execute(() -> {
                            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                Text.literal("§c[Mod Logger] §f" + msg)
                            );
                        });
                    }
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.literal("§a[Mod Logger] JOIN event sent to logger server."));
                    });
                    SecureLogger.logInfo("JOIN event sent to logger server: " + payload);
                }
            } catch (Exception e) {
                System.out.println("[Mod Logger] Failed to send JOIN event: " + e.getMessage());
                SecureLogger.logSecurityEvent("Failed to send JOIN event to logger server: " + e.getMessage());
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[Mod Logger] Failed to send JOIN event to logger server: " + e.getMessage())
                    );
                });
            }
        }).start();
        // Force the custom title screen on every tick if vanilla title screen is detected
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;
            
            // Only enforce if we're not in a game world and force is enabled
            // Wait a few ticks for the client to fully initialize
            if (forceCustomTitleScreen && client.world == null && tickCounter > 10) {
                if (client.currentScreen instanceof TitleScreen && !(client.currentScreen instanceof CustomTitleScreen)) {
                    SecureLogger.logInfo("Detected vanilla title screen, forcing CustomTitleScreen");
                    client.setScreen(new CustomTitleScreen());
                    initialTitleScreenSet = true;
                } else if (client.currentScreen == null && client.world == null) {
                    // If no screen is set and we're not in a world, set our custom title screen
                    SecureLogger.logInfo("No screen detected, setting CustomTitleScreen");
                    client.setScreen(new CustomTitleScreen());
                    initialTitleScreenSet = true;
                }
            }
            
            // Perform premium validation periodically
            if (tickCounter % 600 == 0) { // Every 30 seconds (20 ticks per second * 30)
                performPremiumValidation(client);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SecureLogger.logInfo("Detected disconnect from server. Forcing CustomTitleScreen.");
            // End player session
            com.swapnil.titlemod.data.PlayerDataManager.getInstance().endSession();

            // Send QUIT event to logger server
            new Thread(() -> {
                try {
                    String username = MinecraftClient.getInstance().getSession().getUsername();
                    String publicIp = fetchPublicIp();
                    String modVersion = "pvpmeta-beta1";
                    String payload = "QUIT::" + username + "::" + modVersion + "::" + publicIp;
                    System.out.println("[Mod Logger] Attempting to send: " + payload);
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress("34.159.92.94", 5050), 5000);
                        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out.println(payload);
                        System.out.println("[Mod Logger] QUIT event sent to logger server.");
                        MinecraftClient.getInstance().execute(() -> {
                            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                Text.literal("§a[Mod Logger] QUIT event sent to logger server."));
                        });
                        SecureLogger.logInfo("QUIT event sent to logger server: " + payload);
                    }
                } catch (Exception e) {
                    System.out.println("[Mod Logger] Failed to send QUIT event: " + e.getMessage());
                    SecureLogger.logSecurityEvent("Failed to send QUIT event to logger server: " + e.getMessage());
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.literal("§c[Mod Logger] Failed to send QUIT event to logger server: " + e.getMessage())
                        );
                    });
                }
            }).start();

            client.execute(() -> {
                // Always return to custom title screen after disconnect
                forceCustomTitleScreen = true;
                client.setScreen(new CustomTitleScreen());
                MatchmakingClient.kitToEditOnJoin = null;
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Start player session
            com.swapnil.titlemod.data.PlayerDataManager.getInstance().startSession();
            
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
    
    /**
     * Perform premium validation and show appropriate screens
     */
    private static void performPremiumValidation(MinecraftClient client) {
        if (client.world != null) {
            // Only validate when not in a world to avoid interrupting gameplay
            return;
        }
        
        com.swapnil.titlemod.security.EnhancedPremiumValidator.validatePremiumAccount()
            .thenAccept(result -> {
                if (!result.isValid()) {
                    client.execute(() -> {
                        switch (result.getErrorType()) {
                            case USERNAME_CHANGE_DETECTED:
                                com.swapnil.titlemod.gui.PremiumOnlyScreen.showUsernameChangeDetected();
                                break;
                            case NON_PREMIUM:
                            case INVALID_USERNAME:
                            case INVALID_SESSION:
                                com.swapnil.titlemod.gui.PremiumOnlyScreen.showCrackedAccountDetected();
                                break;
                            default:
                                com.swapnil.titlemod.gui.PremiumOnlyScreen.showPremiumRequired(
                                    "§c§lPremium Account Required!\n\n" +
                                    "§f" + result.getMessage() + "\n" +
                                    "§fPlease use a legitimate Microsoft account."
                                );
                                break;
                        }
                    });
                }
            })
            .exceptionally(throwable -> {
                com.swapnil.titlemod.security.SecureLogger.logSecurityEvent(
                    "Premium validation failed: " + throwable.getMessage()
                );
                return null;
            });
    }

    // Utility to fetch public IP
    private static String fetchPublicIp() {
        try {
            URL url = new URL("https://api.ipify.org");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return in.readLine();
            }
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}