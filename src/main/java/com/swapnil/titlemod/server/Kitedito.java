import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Kitedito {
    private static final int SERVER_PORT = 5007;
    private static final String MINECRAFT_SERVER_IP = "34.159.92.94";
    private static final int RCON_PORT = 25575;
    private static final String RCON_PASSWORD = "98750";
    private static final String WHITELIST_COMMAND = "whitelist add ";
    private static final String WHITELIST_REMOVE = "whitelist remove ";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("[Kitedito] Kit Editor server started on port " + SERVER_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClientConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("[Kitedito] Server error: " + e.getMessage());
        }
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
            System.out.println("[Kitedito] " + username + " connected for kit: " + kit);
            whitelistPlayer(username);
            // Start monitoring the player's presence on the Minecraft server and
            // remove from whitelist after they leave for a grace period
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
                System.out.println("[Kitedito] " + username + " removed from whitelist (disconnected)");
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static boolean isValidUsernameOrKit(String s) {
        return s != null && !s.trim().isEmpty() && s.length() <= 32 && s.matches("^[a-zA-Z0-9_-]+$");
    }

    private static void whitelistPlayer(String username) {
        new Thread(() -> {
            try (RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD)) {
                rcon.connect();
                if (rcon.authenticate()) {
                    rcon.sendCommand(WHITELIST_COMMAND + username);
                    System.out.println("[Kitedito] Added " + username + " to whitelist");
                }
            } catch (Exception e) {
                System.err.println("[Kitedito] Failed to add " + username + ": " + e.getMessage());
            }
        }).start();
    }

    private static void removeFromWhitelist(String username) {
        new Thread(() -> {
            try (RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD)) {
                rcon.connect();
                if (rcon.authenticate()) {
                    rcon.sendCommand(WHITELIST_REMOVE + username);
                    System.out.println("[Kitedito] Removed " + username + " from whitelist");
                }
            } catch (Exception e) {
                System.err.println("[Kitedito] Failed to remove " + username + ": " + e.getMessage());
            }
        }).start();
    }

    private static void monitorPlayerAndCleanup(String username) {
        new Thread(() -> {
            long lastSeenOfflineAt = -1L;
            long gracePeriodMs = 30000; // 30 seconds offline before cleanup
            while (true) {
                try (RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD)) {
                    rcon.connect();
                    if (rcon.authenticate()) {
                        String response = rcon.sendCommand("list");
                        boolean online = response != null && response.contains(username);
                        if (!online) {
                            if (lastSeenOfflineAt < 0) {
                                lastSeenOfflineAt = System.currentTimeMillis();
                            } else if (System.currentTimeMillis() - lastSeenOfflineAt > gracePeriodMs) {
                                removeFromWhitelist(username);
                                System.out.println("[Kitedito] Cleaned up whitelist for " + username + " after disconnect");
                                break;
                            }
                        } else {
                            lastSeenOfflineAt = -1L; // reset if online again
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Kitedito] Monitor error: " + e.getMessage());
                }
                try { Thread.sleep(5000); } catch (InterruptedException ignored) { break; }
            }
        }).start();
    }
}
