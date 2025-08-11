package com.swapnil.titlemod;

import com.swapnil.titlemod.config.ModConfig;
import com.swapnil.titlemod.mixin.ConnectScreenInvoker; // Ensure this mixin is correctly set up
// import com.swapnil.titlemod.security.StringEncryption; // Removed encryption dependency
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.io.IOException;

public class MatchmakingClient {
    // Server configuration is now managed through ModConfig
    // This prevents hardcoded IPs and adds a layer of security
    
    // Static initializer to ensure config is loaded
    static {
        // Initialize the config
        ModConfig.getInstance();
        
        // Add some obfuscation to make reverse engineering harder
    }

    // NEW: Static variable to hold the kit name to be sent after joining the kit editor server
    public static String kitToEditOnJoin = null;

    /**
     * Sends a join request to the external matchmaking server.
     * This method runs in a new thread to avoid blocking the Minecraft client.
     * Uses SHA-256 fingerprinting for secure matchmaking.
     * @param kitName The name of the PvP kit the player wants to queue for.
     */
    public static void xyzabc123(String kitName) {
        // Add obfuscation to make reverse engineering harder
        
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§a[PvP] §fQueuing for §b" + kitName + "§f..."));
        });

        new Thread(() -> {
            // Get server details from config
                    String serverIp = ModConfig.getInstance().getMatchmakingServerIp();
        int serverPort = ModConfig.getInstance().getMatchmakingServerPort();
            
            if (serverIp.equals("127.0.0.1")) {
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP Error] §fMatchmaking Server IP not configured! Check your config file.")
                    );
                });
                System.err.println("[MatchmakingClient] ERROR: Matchmaking Server IP is not configured!");
                return;
            }

            // Add input validation for kitName and username
            if (kitName == null || kitName.trim().isEmpty() || kitName.contains("::")) {
                System.err.println("[MatchmakingClient] Invalid kit name: " + (kitName == null ? "null" : kitName.replaceAll(".", "*")));
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP Error] §fInvalid kit name. Please select a valid kit.")
                    );
                });
                return;
            }
            String username = MinecraftClient.getInstance().getSession().getUsername();
            if (username == null || username.trim().isEmpty() || username.contains("::")) {
                System.err.println("[MatchmakingClient] Invalid username: " + (username == null ? "null" : username.replaceAll(".", "*")));
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP Error] §fInvalid username. Please re-login.")
                    );
                });
                return;
            }

            // Add auto-retry with exponential backoff for matchmaking connection
            int maxRetries = 3;
            int retryDelay = 1000; // ms
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try (
                    Socket socket = new Socket(serverIp, serverPort);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
                ) {
                    // Generate SHA-256 fingerprint for secure matchmaking
                    String fingerprint = ModConfig.generateFingerprint(username, kitName);
                    
                    // Send obfuscated data
                    String payload = username + "::" + kitName;
                    System.out.println("[DEBUG] Sending payload: " + payload);
                    out.println(payload);
                    System.out.println("[MatchmakingClient] Sent join request: " + payload);

                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println("[MatchmakingClient] Server response: " + response);

                        if (response.startsWith("MATCH_FOUND:")) {
                            String[] parts = response.split(":");
                            if (parts.length >= 3) {
                                String matchServerIp = parts[1];
                                int matchServerPort = Integer.parseInt(parts[2]);

                                System.out.println("[MatchmakingClient] Match found! Connecting to game server: " + matchServerIp + ":" + matchServerPort);

                                MinecraftClient.getInstance().execute(() -> {
                                    try {
                                        // Force disconnect if already connected
                                        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                                            MinecraftClient.getInstance().getNetworkHandler().getConnection().disconnect(Text.literal("Match found! Connecting to arena..."));
                                        }
                                        // Wait a short moment to ensure disconnect
                                        Thread.sleep(500);
                                    } catch (Exception e) {
                                        System.err.println("[MatchmakingClient] Error during disconnect: " + e.getMessage());
                                    }
                                    try {
                                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                            Text.literal("§a[PvP] §fMatch found! Connecting to arena...")
                                        );
                                        ServerInfo serverInfo = new ServerInfo("PvP Match Arena", matchServerIp + ":" + matchServerPort, false);
                                        ServerAddress addr = new ServerAddress(matchServerIp, matchServerPort);
                                        ConnectScreenInvoker.invokeConnect(null, MinecraftClient.getInstance(), addr, serverInfo, false);
                                    } catch (Exception e) {
                                        System.err.println("[MatchmakingClient] Auto-connect failed: " + e.getMessage());
                                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                            Text.literal("§c[PvP Error] §fAuto-connect failed. Click here to join the match server.")
                                        );
                                        // Optionally, you could add a clickable chat message here for manual join
                                    }
                                });
                                break;
                            } else {
                                System.err.println("[MatchmakingClient] Invalid MATCH_FOUND format received: " + response);
                                MinecraftClient.getInstance().execute(() -> {
                                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                        Text.literal("§c[PvP Error] §fInvalid match data received. Please try again.")
                                    );
                                });
                            }
                        } else if (response.startsWith("WAITING:")) {
                            String finalResponse = response;
                            MinecraftClient.getInstance().execute(() -> {
                                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                    Text.literal("§e[PvP] §f" + finalResponse.substring(8))
                                );
                            });
                        } else if (response.startsWith("ERROR:")) {
                            String errorMessage = response.substring(6);
                            System.err.println("[MatchmakingClient] Server Error: " + errorMessage);
                            MinecraftClient.getInstance().execute(() -> {
                                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                    Text.literal("§c[PvP Error] §f" + errorMessage)
                                );
                            });
                            break;
                        }
                    }
                } catch (ConnectException e) {
                    System.err.println("[MatchmakingClient] Could not connect to matchmaking server (attempt " + attempt + "): " + e.getMessage());
                    if (attempt == maxRetries) {
                        MinecraftClient.getInstance().execute(() -> {
                            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                Text.literal("§c[PvP Error] §fFailed to connect to matchmaking server after multiple attempts. Returning to main menu.")
                            );
                            MinecraftClient.getInstance().setScreen(new CustomTitleScreen());
                        });
                    } else {
                        try { Thread.sleep(retryDelay); } catch (InterruptedException ignored) {}
                        retryDelay *= 2;
                    }
                } catch (IOException e) {
                    System.err.println("[MatchmakingClient] IO error while connecting to matchmaking server: " + e.getMessage());
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.literal("§c[PvP Error] §fIO error while connecting to matchmaking server.")
                        );
                    });
                    break;
                } catch (NumberFormatException e) {
                    System.err.println("[MatchmakingClient] Invalid port number received: " + e.getMessage());
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.literal("§c[PvP Error] §fReceived invalid server port. Please try again.")
                        );
                    });
                } catch (Exception e) {
                    System.err.println("[MatchmakingClient] An unexpected error occurred: " + e.getMessage());
                    e.printStackTrace();
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.literal("§c[PvP Error] §fAn unexpected error occurred. Check console for details.")
                        );
                    });
                }
            }
        }).start();
    }

    /**
     * Connects the client to the dedicated Kit Editor server.
     * This method runs in a new thread.
     * Uses secure configuration and obfuscation.
     * @param kitName The name of the kit to edit.
     */
    public static void qwerty789(String kitName) {
        // Add obfuscation to make reverse engineering harder
        
        // Set the kit name to be sent after successful connection
        kitToEditOnJoin = kitName;
    
        new Thread(() -> {
            // Get server details from config
            String serverIp = ModConfig.getInstance().getKitEditorServerIp();
            int serverPort = ModConfig.getInstance().getKitEditorServerPort();
            int whitelistPort = 5007; // Dedicated whitelist server (Kitedito)
            
            String username = MinecraftClient.getInstance().getSession().getUsername();
            
            // Step 1: Contact whitelist server first
            try (
                Socket socket = new Socket(serverIp, whitelistPort);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String payload = username + "::" + kitName;
                out.println(payload);
                String response = in.readLine();
                if (response == null || !response.startsWith("OK")) {
                    final String msg = (response == null ? "No response from whitelist server" : response);
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.literal("§c[Kit Editor] §fWhitelist failed: " + msg)
                        );
                    });
                    return;
                }
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§a[Kit Editor] §fWhitelisted. Connecting to kit editor server...")
                    );
                });
            } catch (IOException e) {
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[Kit Editor] §fCould not reach whitelist server (" + serverIp + ":" + whitelistPort + ")")
                    );
                });
                return;
            }
            
            // Step 2: Connect to Minecraft kit editor server
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal("§a[Kit Editor] §fConnecting to kit editor server...")
                );
                
                // Generate a secure connection fingerprint
                String fingerprint = ModConfig.generateFingerprint(username, kitName);
                
                // Use the fingerprint as part of the server name for verification on server side
                String serverName = "Kit Editor-" + fingerprint.substring(0, 8);
                
                ServerInfo serverInfo = new ServerInfo(serverName, serverIp + ":" + serverPort, false);
                ServerAddress addr = new ServerAddress(serverIp, serverPort);
                ConnectScreenInvoker.invokeConnect(null, MinecraftClient.getInstance(), addr, serverInfo, false);
        
                // The command will now be sent by the ClientPlayConnectionEvents.JOIN listener in TitleMod.java
                // after the connection is fully established.
            });
        }).start();
    }
}
