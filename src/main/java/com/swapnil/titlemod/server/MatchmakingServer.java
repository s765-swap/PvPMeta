import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.*;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class MatchmakingServer {
    private static final int SERVER_PORT = 5000;
    private static final Map<String, BlockingQueue<MatchmakingServer.ClientConnection>> queues = new ConcurrentHashMap();
    private static final Map<String, Long> queueJoinTimes = new ConcurrentHashMap();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final String MINECRAFT_SERVER_IP = "34.159.92.94";
    private static final int MINECRAFT_SERVER_PORT = 25565;
    private static final int RCON_PORT = 25575;
    private static final String RCON_PASSWORD = "98750";
    private static final long MAX_WAIT_TIME_MS = 60000; // 1 minute
    private static final String WHITELIST_COMMAND = "whitelist add ";
    private static final String WHITELIST_REMOVE = "whitelist remove ";

    public static void main(String[] args) {
        if ("34.159.92.94".equals("YOUR_MINECRAFT_SERVER_IP") || "98750".equals("YOUR_RCON_PASSWORD")) {
            System.err.println("CRITICAL ERROR: Please configure MINECRAFT_SERVER_IP and RCON_PASSWORD!");
            System.exit(1);
        }

        scheduler.scheduleAtFixedRate(new MatchmakingServer.MatchmakingEngine(), 0L, 2L, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(new MatchmakingServer.QueueTimeoutChecker(), 0L, 5L, TimeUnit.SECONDS);

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("[MatchmakingServer] Server started on port " + SERVER_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClientConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("[MatchmakingServer] Server error: " + e.getMessage());
        } finally {
            scheduler.shutdown();
        }
    }

    private static boolean isValidUsernameOrKit(String s) {
        return s != null && !s.trim().isEmpty() && s.length() <= 32 && s.matches("^[a-zA-Z0-9_-]+$");
    }

    private static String obfuscateIP(String ip) {
        try {
            byte[] ipBytes = ip.getBytes();
            byte[] key = "TitleModSecureKey2024!@#$".getBytes();
            byte[] encrypted = new byte[ipBytes.length];
            
            for (int i = 0; i < ipBytes.length; i++) {
                encrypted[i] = (byte) (ipBytes[i] ^ key[i % key.length]);
            }
            
            return java.util.Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return ip;
        }
    }

    private static String generateSessionToken(String username) {
        return username + "_" + System.currentTimeMillis() + "_" + 
               java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    private static void handleClientConnection(Socket socket) {
        String username = null;
        String kit = null;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            
            String input = reader.readLine();
            if (input == null || !input.contains("::")) {
                writer.println("ERROR: Invalid format. Expected: username::kit");
                return;
            }

            String[] parts = input.split("::");
            if (parts.length != 2) {
                writer.println("ERROR: Invalid format. Expected: username::kit");
                return;
            }

            username = parts[0].trim();
            kit = parts[1].trim();

            if (!isValidUsernameOrKit(username) || !isValidUsernameOrKit(kit)) {
                writer.println("ERROR: Invalid username or kit name");
                return;
            }

            final String finalUsername = username;
            
            // Remove player from any existing queues
            queues.values().forEach(queue -> 
                queue.removeIf(client -> client.getUsername().equalsIgnoreCase(finalUsername))
            );

            BlockingQueue<MatchmakingServer.ClientConnection> queue = queues.computeIfAbsent(kit, k -> new LinkedBlockingQueue<>());
            
            boolean alreadyInQueue = queue.stream()
                .anyMatch(client -> client.getUsername().equalsIgnoreCase(finalUsername));
            
            if (alreadyInQueue) {
                writer.println("ERROR: You are already in the queue for this kit");
                return;
            }

            MatchmakingServer.ClientConnection connection = new MatchmakingServer.ClientConnection(username, kit, writer, socket);
            queue.put(connection);
            queueJoinTimes.put(username + ":" + kit, System.currentTimeMillis());
            
            // Whitelist immediately upon queuing so the player can join the Minecraft server
            whitelistPlayer(username);
            
            System.out.println("[Queue] " + username + " joined queue for kit: " + kit + " (Total: " + queue.size() + ")");
            writer.println("WAITING: Added to queue for " + kit);
            
            while (!socket.isClosed()) {
                Thread.sleep(1000);
            }
            
        } catch (SocketException e) {
            // Client disconnected normally
        } catch (IOException | InterruptedException e) {
            System.err.println("[MatchmakingServer] Error: " + e.getMessage());
        } finally {
            if (username != null) {
                final String finalUsernameForCleanup = username;
                queues.values().forEach(queue -> 
                    queue.removeIf(client -> client.getUsername().equalsIgnoreCase(finalUsernameForCleanup))
                );
                queueJoinTimes.entrySet().removeIf(entry -> entry.getKey().startsWith(finalUsernameForCleanup + ":"));
                
                // Remove player from whitelist when they leave or timeout
                removeFromWhitelist(finalUsernameForCleanup);
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private static void whitelistPlayer(String username) {
        new Thread(() -> {
            try (RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD)) {
                rcon.connect();
                if (rcon.authenticate()) {
                    rcon.sendCommand(WHITELIST_COMMAND + username);
                    System.out.println("[Whitelist] Added " + username + " to whitelist");
                }
            } catch (Exception e) {
                System.err.println("[Whitelist] Failed to add " + username + ": " + e.getMessage());
            }
        }).start();
    }

    private static void removeFromWhitelist(String username) {
        new Thread(() -> {
            try (RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD)) {
                rcon.connect();
                if (rcon.authenticate()) {
                    rcon.sendCommand(WHITELIST_REMOVE + username);
                    System.out.println("[Whitelist] Removed " + username + " from whitelist");
                }
            } catch (Exception e) {
                System.err.println("[Whitelist] Failed to remove " + username + ": " + e.getMessage());
            }
        }).start();
    }

    private static boolean waitForPlayersToJoin(String username1, String username2, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                Thread.sleep(5000); // Check every 5 seconds
                
                try (RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD)) {
                    rcon.connect();
                    if (rcon.authenticate()) {
                        String response = rcon.sendCommand("list");
                        if (response != null && response.contains(username1) && response.contains(username2)) {
                            return true; // Both players joined
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[RCON] Error checking player status: " + e.getMessage());
            }
        }
        return false; // Timeout reached
    }

    private static class QueueTimeoutChecker implements Runnable {
        public void run() {
            long currentTime = System.currentTimeMillis();
            
            for (Map.Entry<String, BlockingQueue<MatchmakingServer.ClientConnection>> entry : queues.entrySet()) {
                String kit = entry.getKey();
                BlockingQueue<MatchmakingServer.ClientConnection> queue = entry.getValue();
                
                if (queue.size() == 1) {
                    // Check if the single player has been waiting too long
                    for (MatchmakingServer.ClientConnection client : queue) {
                        String key = client.getUsername() + ":" + kit;
                        Long joinTime = queueJoinTimes.get(key);
                        
                        if (joinTime != null && (currentTime - joinTime) > MAX_WAIT_TIME_MS) {
                            // Remove from queue and kick player
                            queue.remove(client);
                            queueJoinTimes.remove(key);
                            
                            System.out.println("[Timeout] " + client.getUsername() + " timed out waiting for opponent in kit: " + kit);
                            client.getWriter().println("TIMEOUT: No opponent found within 1 minute");
                            
                            // Kick player from server
                            new Thread(() -> {
                                try (RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD)) {
                                    rcon.connect();
                                    if (rcon.authenticate()) {
                                        rcon.sendCommand("kick " + client.getUsername() + " No opponent found within 1 minute");
                                        System.out.println("[RCON] Kicked " + client.getUsername() + " for timeout");
                                    }
                                } catch (Exception e) {
                                    System.err.println("[RCON] Failed to kick player: " + e.getMessage());
                                }
                            }).start();
                            
                            try {
                                client.getSocket().close();
                            } catch (IOException ignored) {}
                        }
                    }
                }
            }
        }
    }

    private static void sendRconCommands(String... commands) {
        new Thread(() -> {
            try (RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD)) {
                rcon.connect();
                if (rcon.authenticate()) {
                    for (String command : commands) {
                        rcon.sendCommand(command);
                        System.out.println("[RCON] Sent command: " + command);
                    }
                }
            } catch (Exception e) {
                System.err.println("[RCON] Failed to send commands: " + e.getMessage());
            }
        }).start();
    }

    private static class MatchmakingEngine implements Runnable {
        public void run() {
            for (Map.Entry<String, BlockingQueue<MatchmakingServer.ClientConnection>> entry : queues.entrySet()) {
                String kit = entry.getKey();
                BlockingQueue<MatchmakingServer.ClientConnection> queue = entry.getValue();

                if (queue.size() >= 2) {
                    synchronized (queue) {
                        if (queue.size() >= 2) {
                            try {
                                ClientConnection player1 = queue.take();
                                ClientConnection player2 = queue.take();

                                if (player1.getSocket().isClosed() || player2.getSocket().isClosed()) {
                                    if (!player1.getSocket().isClosed()) queue.put(player1);
                                    if (!player2.getSocket().isClosed()) queue.put(player2);
                                    continue;
                                }

                                // Add players to whitelist
                                whitelistPlayer(player1.getUsername());
                                whitelistPlayer(player2.getUsername());

                                // Generate secure session tokens
                                String token1 = generateSessionToken(player1.getUsername());
                                String token2 = generateSessionToken(player2.getUsername());

                                // Send match found response with correct format
                                player1.getWriter().println("MATCH_FOUND:" + MINECRAFT_SERVER_IP + ":" + MINECRAFT_SERVER_PORT + ":" + token1);
                                player2.getWriter().println("MATCH_FOUND:" + MINECRAFT_SERVER_IP + ":" + MINECRAFT_SERVER_PORT + ":" + token2);
                                System.out.println("[Match] " + player1.getUsername() + " vs " + player2.getUsername() + " in kit: " + kit);

                                // Do NOT close the sockets here! Let the client disconnect naturally after joining the Minecraft server.
                                // try {
                                //     player1.getSocket().close();
                                //     player2.getSocket().close();
                                // } catch (IOException ignored) {}

                                new Thread(() -> {
                                    try {
                                        if (waitForPlayersToJoin(player1.getUsername(), player2.getUsername(), 60000)) {
                                            // Match started - keep whitelist active
                                            System.out.println("[Whitelist] Match started - keeping whitelist active");

                                            // Dynamically generate and send duel commands based on kit
                                            String duelCommand1 = String.format("sudo %s duel %s 4 %s", player1.getUsername(), player2.getUsername(), kit.toLowerCase());
                                            String duelCommand2 = String.format("sudo %s duel accept %s", player2.getUsername(), player1.getUsername());
                                            sendRconCommands(duelCommand1, duelCommand2);

                                            // Monitor players and remove whitelist when both have left the Minecraft server
                                            monitorPlayersAndCleanup(player1.getUsername(), player2.getUsername());

                                        } else {
                                            // Remove from whitelist after timeout
                                            removeFromWhitelist(player1.getUsername());
                                            removeFromWhitelist(player2.getUsername());
                                        }
                                    } catch (Exception e) {
                                        System.err.println("[Whitelist] Error: " + e.getMessage());
                                    }
                                }).start();

                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void monitorPlayersAndCleanup(String username1, String username2) {
        new Thread(() -> {
            long lastSeenBothOfflineAt = -1L;
            long gracePeriodMs = 30000; // 30 seconds of both offline before cleanup
            while (true) {
                try (RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD)) {
                    rcon.connect();
                    if (rcon.authenticate()) {
                        String response = rcon.sendCommand("list");
                        boolean u1Online = response != null && response.contains(username1);
                        boolean u2Online = response != null && response.contains(username2);
                        if (!u1Online && !u2Online) {
                            if (lastSeenBothOfflineAt < 0) {
                                lastSeenBothOfflineAt = System.currentTimeMillis();
                            } else if (System.currentTimeMillis() - lastSeenBothOfflineAt > gracePeriodMs) {
                                removeFromWhitelist(username1);
                                removeFromWhitelist(username2);
                                System.out.println("[Whitelist] Cleaned up after match for " + username1 + " and " + username2);
                                break;
                            }
                        } else {
                            lastSeenBothOfflineAt = -1L; // reset if any is online
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[RCON] Monitor error: " + e.getMessage());
                }
                try { Thread.sleep(5000); } catch (InterruptedException ignored) { break; }
            }
        }).start();
    }

    private static class ClientConnection {
        private final String username;
        private final String kit;
        private final PrintWriter writer;
        private final Socket socket;

        public ClientConnection(String username, String kit, PrintWriter writer, Socket socket) {
            this.username = username;
            this.kit = kit;
            this.writer = writer;
            this.socket = socket;
        }

        public String getUsername() {
            return username;
        }

        public String getKit() {
            return kit;
        }

        public PrintWriter getWriter() {
            return writer;
        }

        public Socket getSocket() {
            return socket; 
        }
    }
}
 