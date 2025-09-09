package com.swapnil.titlemod;

import com.swapnil.titlemod.config.ModConfig;
import com.swapnil.titlemod.mixin.ConnectScreenInvoker; 
import com.swapnil.titlemod.security.SecureLogger; 
import com.swapnil.titlemod.security.SecureConnectionManager; 
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
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

public class MatchmakingClient {
   
    private static final String MATCHMAKING_SERVER_IP = "34.159.92.94";
    private static final int MATCHMAKING_SERVER_PORT = 5000;
    
    
    

    static {
       
        ModConfig.getInstance();
        
       
    }

    
    public static String kitToEditOnJoin = null;

    
    /**
     * @param kitName The name of the PvP kit the player wants to queue for.
     */
    public static void joinMatchmakingQueue(String kitName) {
   
        
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§a[PvP] §fQueuing for §b" + kitName + "§f..."));
        });

        new Thread(() -> {
          
            String serverIp = ModConfig.getInstance().getMatchmakingServerIp();
            int serverPort = ModConfig.getInstance().getMatchmakingServerPort();
            
            if (serverIp.equals("127.0.0.1")) {
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP Error] §fMatchmaking Server IP not configured! Check your config file.")
                    );
                });
                SecureLogger.logSecurityEvent("Matchmaking Server IP not configured");
                return;
            }

         
            if (kitName == null || kitName.trim().isEmpty() || kitName.contains("::")) {
                SecureLogger.logSecurityEvent("Invalid kit name provided for matchmaking");
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP Error] §fInvalid kit name. Please select a valid kit.")
                    );
                });
                return;
            }
            String username = MinecraftClient.getInstance().getSession().getUsername();
            
    
            com.swapnil.titlemod.security.EnhancedPremiumValidator.validatePremiumAccount()
                .thenAccept(result -> {
                    if (!result.isValid()) {
                        MinecraftClient.getInstance().execute(() -> {
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
                        return;
                    }
                    
                  
                    proceedWithMatchmaking(username, kitName, serverIp, serverPort);
                })
                .exceptionally(throwable -> {
                    com.swapnil.titlemod.security.SecureLogger.logSecurityEvent(
                        "Premium validation failed during matchmaking: " + throwable.getMessage()
                    );
                    return null;
                });
            
            return;
            
        }).start();
    }
    
   
    private static void proceedWithMatchmaking(String username, String kitName, String serverIp, int serverPort) {
       
        int maxRetries = 3;
        int retryDelay = 1000; // ms
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                
                Socket socket = com.swapnil.titlemod.security.HTTPRequestProtector.createSecureSocket(serverIp, serverPort);
                socket.connect(new java.net.InetSocketAddress(serverIp, serverPort), 10000);
                
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
               
                long timestamp = System.currentTimeMillis();
                String signature = "NA";
                
           
                String payload = username + "::" + kitName + "::" + timestamp + "::" + signature + "::" + 
                               "NA" + "::" + "NA";
               
                SecureLogger.logInfo("Sending secure matchmaking request for kit: " + kitName);
                out.println(payload);
                SecureLogger.logInfo("Matchmaking request sent successfully");

                String response;
                while ((response = in.readLine()) != null) {
            
                    SecureLogger.logInfo("Received matchmaking response");

                    if (response.startsWith("MATCH_FOUND:")) {
                        String[] parts = response.split(":");
                        if (parts.length >= 3) {
                            String matchServerIp = parts[1];
                            int matchServerPort = Integer.parseInt(parts[2]);

                            SecureLogger.logInfo("Match found! Connecting to game server");

                            MinecraftClient.getInstance().execute(() -> {
                             
                                com.swapnil.titlemod.data.PlayerDataManager.getInstance().updateLastPlayedTime(kitName);
                              
                                com.swapnil.titlemod.queue.QueueManager.getInstance().onMatchFound(kitName);
                                new Thread(() -> {
                                    try {
                                        
                                        Thread.sleep(1500);
                                    } catch (InterruptedException e) {
                                        
                                    }
                                    MinecraftClient.getInstance().execute(() -> {
                                        try {
                                            
                                            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                                                MinecraftClient.getInstance().getNetworkHandler().getConnection().disconnect(Text.literal("Match found! Connecting to arena..."));
                                            }
                                          
                                            Thread.sleep(500);
                                        } catch (Exception e) {
                                            SecureLogger.logSecurityEvent("Error during disconnect: " + e.getMessage());
                                        }
                                        try {
                                            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                                Text.literal("§a[PvP] §fMatch found! Connecting to arena...")
                                            );
                                            ServerInfo serverInfo = new ServerInfo("PvP Match Arena", matchServerIp + ":" + matchServerPort, false);
                                            ServerAddress addr = new ServerAddress(matchServerIp, matchServerPort);
                                            ConnectScreenInvoker.invokeConnect(null, MinecraftClient.getInstance(), addr, serverInfo, false);
                                        } catch (Exception e) {
                                            SecureLogger.logSecurityEvent("Auto-connect failed: " + e.getMessage());
                                            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                                Text.literal("§c[PvP Error] §fAuto-connect failed. Click here to join the match server.")
                                            );
                                           
                                        }
                                    });
                                }).start();
                            });
                            break;
                        } else {
                            SecureLogger.logSecurityEvent("Invalid MATCH_FOUND format received from server");
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
                                                 SecureLogger.logSecurityEvent("Server Error: " + errorMessage);
                        MinecraftClient.getInstance().execute(() -> {
                            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                Text.literal("§c[PvP Error] §f" + errorMessage)
                            );
                        });
                        
                        break;
                    }
                }
                         } catch (ConnectException e) {
                 SecureLogger.logSecurityEvent("Could not connect to matchmaking server (attempt " + attempt + "): " + e.getMessage());
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
                 SecureLogger.logSecurityEvent("IO error while connecting to matchmaking server: " + e.getMessage());
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP Error] §fIO error while connecting to matchmaking server.")
                    );
                });
                break;
                         } catch (NumberFormatException e) {
                 SecureLogger.logSecurityEvent("Invalid port number received: " + e.getMessage());
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP Error] §fReceived invalid server port. Please try again.")
                    );
                });
                         } catch (Exception e) {
                 SecureLogger.logSecurityEvent("An unexpected error occurred: " + e.getMessage());
                 e.printStackTrace();
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("§c[PvP Error] §fAn unexpected error occurred. Check console for details.")
                    );
                });
            }
        }
    }

    /**
     * @param kitName The name of the kit to edit.
     */
    public static void joinKitEditor(String kitName) {
 
        com.swapnil.titlemod.data.PlayerDataManager.getInstance().updateLastPlayedTime(kitName);
        
     
        kitToEditOnJoin = kitName;
    
        new Thread(() -> {
       
            String serverIp = ModConfig.getInstance().getKitEditorServerIp();
            int whitelistPort = ModConfig.getInstance().getKitEditorWhitelistPort(); 
            int serverPort = ModConfig.getInstance().getKitEditorMinecraftPort(); 
            
            String username = MinecraftClient.getInstance().getSession().getUsername();
            
           
            com.swapnil.titlemod.security.EnhancedPremiumValidator.validatePremiumAccount()
                .thenAccept(result -> {
                    if (!result.isValid()) {
                        MinecraftClient.getInstance().execute(() -> {
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
                        return;
                    }
                    
                   
                    proceedWithKitEditor(username, kitName, serverIp, whitelistPort, serverPort);
                })
                .exceptionally(throwable -> {
                    com.swapnil.titlemod.security.SecureLogger.logSecurityEvent(
                        "Premium validation failed during kit editor: " + throwable.getMessage()
                    );
                    return null;
                });
            
            return; 
            
        }).start();
    }
    
 
    private static void proceedWithKitEditor(String username, String kitName, String serverIp, int whitelistPort, int serverPort) {
        
        try {
           
            Socket socket = com.swapnil.titlemod.security.HTTPRequestProtector.createSecureSocket(serverIp, whitelistPort);
            socket.connect(new java.net.InetSocketAddress(serverIp, whitelistPort), 10000);
            
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
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
                    Text.literal("§c[Kit Editor] §fCould not reach whitelist server")
                );
            });
            SecureLogger.logSecurityEvent("Failed to connect to kit editor whitelist server");
            return;
        }
        
        
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                Text.literal("§a[Kit Editor] §fConnecting to kit editor server...")
            );
            
            
            String fingerprint = ModConfig.generateFingerprint(username, kitName);
            
           
            String serverName = "Kit Editor-" + fingerprint.substring(0, 8);
            
            ServerInfo serverInfo = new ServerInfo(serverName, serverIp + ":" + serverPort, false);
            ServerAddress addr = new ServerAddress(serverIp, serverPort);
            ConnectScreenInvoker.invokeConnect(null, MinecraftClient.getInstance(), addr, serverInfo, false);
    
            
            new Thread(() -> {
                try {
                
                    Thread.sleep(2000);
                    
             
                    if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                        MinecraftClient.getInstance().getNetworkHandler().sendChatMessage("§k[KIT_EDITOR_MODE]§r " + kitName);
                                                 SecureLogger.logInfo("Sent kit editor mode message for kit: " + kitName);
                    }
                                 } catch (Exception e) {
                     SecureLogger.logSecurityEvent("Failed to send kit editor mode message: " + e.getMessage());
                 }
            }).start();
        });
    }

  
    public static void joinQueue(String kit) {
                 try {
             SecureLogger.logInfo("QueueManager sending JOIN: " + kit);
            
         
            String username = MinecraftClient.getInstance().getSession().getUsername();
            
                    proceedWithQueueJoin(username, kit);
        
        } catch (Exception e) {
            SecureLogger.logSecurityEvent("Unexpected error during matchmaking: " + e.getMessage());
            e.printStackTrace();
            handleQueueJoinError("Unexpected error: " + e.getMessage());
        }
    }
    
      
     private static void handleQueueJoinSuccess(String kit) {
        
         SecureLogger.logInfo("Queue join successful for kit: " + kit);
     }
    
       
     private static void handleQueueJoinError(String errorMessage) {
         SecureLogger.logSecurityEvent("Queue join failed: " + errorMessage);
         
     }
    
    
    private static void proceedWithQueueJoin(String username, String kit) {
        try {
        
             SecureLogger.logInfo("Connecting to matchmaking server: " + MATCHMAKING_SERVER_IP + ":" + MATCHMAKING_SERVER_PORT);
            
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(MATCHMAKING_SERVER_IP, MATCHMAKING_SERVER_PORT), 10000);
                socket.setSoTimeout(15000);
                
                                 SecureLogger.logInfo("Connected to matchmaking server successfully");
                
                try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    
          
                    long timestamp = System.currentTimeMillis();
                 
                    
                    
                    String request = String.format("%s::%s::%s", username, kit, timestamp);
                    
                                         SecureLogger.logInfo("Sending matchmaking request to " + MATCHMAKING_SERVER_IP + ":" + MATCHMAKING_SERVER_PORT);
                     SecureLogger.logInfo("Request: " + request.substring(0, Math.min(50, request.length())) + "...");
                     SecureLogger.logInfo("Username: " + username);
                     SecureLogger.logInfo("Kit: " + kit);
                     SecureLogger.logInfo("Timestamp: " + timestamp);
                   
                    writer.println(request);
                    
                                        
                     String response = reader.readLine();
                     SecureLogger.logInfo("Matchmaking server response: " + response);
                    
                    if (response != null && response.startsWith("WAITING:")) {
                                                 SecureLogger.logInfo("Successfully added to queue for " + kit);
                        
                        handleQueueJoinSuccess(kit);
                                         } else if (response != null && response.startsWith("ERROR:")) {
                         String errorMsg = response.substring("ERROR:".length());
                         SecureLogger.logSecurityEvent("Failed to join queue: " + errorMsg);
                        handleQueueJoinError(errorMsg);
                                         } else {
                         SecureLogger.logSecurityEvent("Unexpected response from matchmaking server: " + response);
                        handleQueueJoinError("Unexpected server response");
                    }
                }
                         } catch (ConnectException e) {
                 SecureLogger.logSecurityEvent("Failed to connect to matchmaking server: " + MATCHMAKING_SERVER_IP + ":" + MATCHMAKING_SERVER_PORT);
                 SecureLogger.logSecurityEvent("Connection error: " + e.getMessage());
                handleQueueJoinError("Failed to connect to matchmaking server");
                         } catch (SocketTimeoutException e) {
                 SecureLogger.logSecurityEvent("Connection timeout to matchmaking server: " + MATCHMAKING_SERVER_IP + ":" + MATCHMAKING_SERVER_PORT);
                handleQueueJoinError("Connection timeout to matchmaking server");
                         } catch (IOException e) {
                 SecureLogger.logSecurityEvent("IO error during matchmaking: " + e.getMessage());
                handleQueueJoinError("IO error during matchmaking: " + e.getMessage());
            }
            
        } catch (Exception e) {
            SecureLogger.logSecurityEvent("Unexpected error during matchmaking: " + e.getMessage());
            e.printStackTrace();
            handleQueueJoinError("Unexpected error: " + e.getMessage());
        }
    }
    
    
    private static String generateSignature(String username, String kit, long timestamp) {
        return "NA";
    }
}
