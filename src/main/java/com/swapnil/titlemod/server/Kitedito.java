package com.swapnil.titlemod.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.swapnil.titlemod.server.RconManager;

public class Kitedito {
    private static final int SERVER_PORT = 5007;
    private static final String MINECRAFT_SERVER_IP = "34.159.92.94";
    private static final int RCON_PORT = 25575;
    private static final String RCON_PASSWORD = "98750";
    private static final String WHITELIST_COMMAND = "whitelist add ";
    private static final String WHITELIST_REMOVE = "whitelist remove ";
    
    // Track active kit editor users and their validation status
    private static final ConcurrentHashMap<String, KitEditorUser> activeUsers = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Active kit editor users

    // No session/validation logic required

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("[Kitedito] Kit Editor server started on port " + SERVER_PORT);
            
            // Start cleanup task for expired users
            scheduler.scheduleAtFixedRate(new UserCleanupTask(), 30, 30, TimeUnit.SECONDS);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClientConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("[Kitedito] Server error: " + e.getMessage());
        } finally {
            scheduler.shutdown();
        }
    }

    private static void handleClientConnection(Socket socket) {
        String username = null;
        String kit = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            final String clientIp = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "unknown";
            
            String input = reader.readLine();
            if (input == null || !input.contains("::")) {
                writer.println("ERROR: Invalid format. Expected: username::kit");
                return;
            }

            String[] parts = input.split("::");
            if (parts.length == 2) {
                // Current client sends: username::kit
                username = parts[0].trim();
                kit = parts[1].trim();
            } else {
                writer.println("ERROR: Invalid format. Expected: username::kit");
                return;
            }

            if (!isValidUsernameOrKit(username) || !isValidUsernameOrKit(kit)) {
                writer.println("ERROR: Invalid username or kit name");
                return;
            }

            // Accept all requests unconditionally and proceed to whitelist
            System.out.println("[Kitedito] " + username + " connected for kit: " + kit);
            whitelistPlayer(username);

            // Track this user as a kit editor user
            KitEditorUser user = new KitEditorUser(username, kit, clientIp);
            activeUsers.put(username, user);

            System.out.println("[Kitedito] Kit editor user " + username + " connected for kit: " + kit);

            // Start monitoring the player's presence on the Minecraft server
            monitorPlayerAndCleanup(username);
            writer.println("OK: Whitelisted for kit editing");
            
            // Keep connection open until client disconnects
            while (!socket.isClosed()) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        } catch (SocketException e) {
            // Client disconnected
        } catch (IOException e) {
            System.err.println("[Kitedito] Error: " + e.getMessage());
        } finally {
            if (username != null) {
                removeFromWhitelist(username);
                activeUsers.remove(username);
                System.out.println("[Kitedito] " + username + " removed from whitelist (disconnected)");
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static boolean isValidUsernameOrKit(String s) {
        return s != null && !s.trim().isEmpty() && s.length() <= 32 && s.matches("^[a-zA-Z0-9_-]+$");
    }

    private static void whitelistPlayer(String username) {
        RconManager.addToWhitelist(username);
    }

    private static void removeFromWhitelist(String username) {
        RconManager.removeFromWhitelist(username);
    }

    private static void monitorPlayerAndCleanup(String username) {
        // Event-driven approach: Only check when player disconnects
        // Remove continuous polling - this will be handled by the Minecraft server plugin in next beta
        System.out.println("[Kitedito] Monitoring " + username + " (event-driven mode)");
        
        // Schedule a single cleanup check after 5 minutes instead of continuous monitoring
        scheduler.schedule(() -> {
            RconManager.checkPlayerOnline(username, online -> {
                if (!online) {
                    removeFromWhitelist(username);
                    System.out.println("[Kitedito] Cleaned up whitelist for " + username + " after timeout");
                }
            });
        }, 5, TimeUnit.MINUTES);
    }

    /**
     * Kit Editor User class to track active users
     */
    private static class KitEditorUser {
        final String username;
        final String kit;
        final String clientIp;
        final long connectedAt;

        KitEditorUser(String username, String kit, String clientIp) {
            this.username = username;
            this.kit = kit;
            this.clientIp = clientIp;
            this.connectedAt = System.currentTimeMillis();
        }
    }

    /**
     * Task to clean up expired users
     */
    private static class UserCleanupTask implements Runnable {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long maxAge = 300000; // 5 minutes

            activeUsers.entrySet().removeIf(entry -> {
                KitEditorUser user = entry.getValue();
                if (currentTime - user.connectedAt > maxAge) {
                    System.out.println("[Kitedito] Cleaning up expired user: " + user.username);
                    removeFromWhitelist(user.username);
                    return true;
                }
                return false;
            });
        }
    }
}
