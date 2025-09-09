    package com.swapnil.titlemod.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MatchmakingServer {
   private static final int SERVER_PORT = 5000;
   private static final Map<String, BlockingQueue<MatchmakingServer.ClientConnection>> queues = new ConcurrentHashMap();
   private static final Map<String, Integer> activeMatchesPerKit = new ConcurrentHashMap();
   private static final Map<String, Long> queueJoinTimes = new ConcurrentHashMap();
   private static final Map<String, String> kitEditorUsers = new ConcurrentHashMap();
   private static final Map<String, Long> whitelistedPlayers = new ConcurrentHashMap();
   private static final Map<String, Integer> failedAttempts = new ConcurrentHashMap();
   private static final Map<String, MatchmakingServer.SimpleRateLimiter> ipRateLimiters = new ConcurrentHashMap();
   private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
   private static final String MINECRAFT_SERVER_IP = "34.159.92.94";
   private static final int MINECRAFT_SERVER_PORT = 25565;
   private static final int RCON_PORT = 25575;
   private static final String RCON_PASSWORD = "98750";
   private static final long MAX_WAIT_TIME_MS = 60000L;
   private static final String WHITELIST_COMMAND = "whitelist add ";
   private static final String WHITELIST_REMOVE = "whitelist remove ";
   private static final String VALIDATION_SERVER_IP = "localhost";
   private static final int VALIDATION_SERVER_PORT = 5008;
   private static final String MATCHMAKING_SHARED_SECRET = "TitleModSecret";
   private static final int RATE_LIMIT_MAX_REQUESTS = 20;
   private static final long RATE_LIMIT_WINDOW_MS = 60000L;
   private static final ConcurrentHashMap<String, MatchmakingServer.SessionInfo> sessions = new ConcurrentHashMap();
   private static final ConcurrentHashMap<String, MatchmakingServer.RateLimitInfo> rateLimits = new ConcurrentHashMap();
   private static final long SESSION_EXPIRY_MS = 3600000L;
   private static final int RATE_LIMIT = 60;
   private static final SecureRandom secureRandom = new SecureRandom();
   private static final int MAX_FIGHTS_PER_KIT = 5;
   private static final int MATCHMAKING_COOLDOWN_MS = 60000; // 1 minute cooldown
   private static final Map<String, Long> recentlyMatched = new ConcurrentHashMap<>();
   private static final Set<String> inMatch = new HashSet<>();
   private static final String INMATCH_FILE = "inmatch.txt";
   private static final Object inMatchLock = new Object();

   private static int getActiveMatches(String var0) {
      Integer var1 = (Integer)activeMatchesPerKit.get(var0);
      return var1 == null ? 0 : var1;
   }

   private static void incrementActiveMatches(String var0) {
      activeMatchesPerKit.merge(var0, 1, Integer::sum);
   }

   private static void decrementActiveMatches(String var0) {
      activeMatchesPerKit.compute(var0, (var0x, var1) -> {
         return var1 != null && var1 > 1 ? var1 - 1 : null;
      });
   }

   private static String generateSessionToken() {
      byte[] var0 = new byte[32];
      secureRandom.nextBytes(var0);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(var0);
   }

   private static void removeUserFromAllQueues(String var0) {
      Iterator var1 = queues.values().iterator();

      while(var1.hasNext()) {
         BlockingQueue var2 = (BlockingQueue)var1.next();
         Iterator var3 = var2.iterator();

         while(var3.hasNext()) {
            MatchmakingServer.ClientConnection var4 = (MatchmakingServer.ClientConnection)var3.next();
            if (var4.getUsername().equalsIgnoreCase(var0)) {
               var3.remove();
            }
         }
      }

   }

   private static void removeUserJoinTimes(String var0) {
      String var1 = var0 + ":";
      queueJoinTimes.entrySet().removeIf((var1x) -> {
         return ((String)var1x.getKey()).startsWith(var1);
      });
   }

   private static boolean queueContainsUser(BlockingQueue var0, String var1) {
      Iterator var2 = var0.iterator();

      MatchmakingServer.ClientConnection var4;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         Object var3 = var2.next();
         var4 = (MatchmakingServer.ClientConnection)var3;
      } while(!var4.getUsername().equalsIgnoreCase(var1));

      return true;
   }

   private static boolean isPlayerActuallyOnline(String username) {
      try {
         RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD);
         rcon.connect();
         if (rcon.authenticate()) {
            String listResp = rcon.sendCommand("list");
            if (listResp != null && listResp.toLowerCase().contains(username.toLowerCase())) {
               System.out.println("[SECURITY] Detected player already online via /list: " + username);
               return true;
            }
         }
         rcon.close();
      } catch (Exception e) {
         System.err.println("[RCON] Error checking online status for " + username + ": " + e.getMessage());
      }
      return false;
   }

   private static void handleClientConnection(Socket var0) {
      String var1 = null;
      String var2 = null;
      System.out.println("[Connection] New client connection from " + var0.getRemoteSocketAddress());

      BufferedReader var3 = null;
      PrintWriter var4 = null;
      try {
         var3 = new BufferedReader(new InputStreamReader(var0.getInputStream()));
         var4 = new PrintWriter(var0.getOutputStream(), true);

         String var5 = var3.readLine();
         if (var5 == null || !var5.contains("::")) {
            var4.println("ERROR: Invalid format. Expected: username::kit::timestamp::signature::api_key::installation_id");
            return;
         }

        String[] var6 = var5.split("::");
        if (var6.length != 6) {
           var4.println("ERROR: Invalid format. Expected: username::kit::timestamp::signature::api_key::installation_id");
           return;
        }

        var1 = var6[0].trim();
        var2 = var6[1].trim();
        System.out.println("[QueueRequest] User: " + var1 + " Kit: " + var2);
        if (var1.isEmpty() || var2.isEmpty()) {
           var4.println("ERROR: Invalid username or kit name");
           return;
        }

        if (isPlayerActuallyOnline(var1)) {
           var4.println("ERROR: You are already playing or detected as an alternate account.");
           System.out.println("[SECURITY] Blocked queue/join for already online or alt user: " + var1);
           return;
        }

        synchronized (inMatchLock) {
           if (inMatch.contains(var1.toLowerCase())) {
              var4.println("ERROR: You are already in a match.");
              System.out.println("[SECURITY] Blocked queue/join for already in-match user: " + var1);
              return;
           }
        }

        BlockingQueue var7;
        synchronized (queues) {
           var7 = (BlockingQueue)queues.computeIfAbsent(var2, (var0x) -> {
              return new LinkedBlockingQueue();
           });
        }

        synchronized (var7) {
           if (queueContainsUser(var7, var1)) {
              var4.println("ERROR: You are already in the queue for this kit");
              System.out.println("[SECURITY] Blocked duplicate queue for user: " + var1 + " kit: " + var2);
              return;
           }

           int var8 = getActiveMatches(var2);
           if (var8 >= 5) {
              var4.println("ERROR: Queue full. 10 players playing in " + var2 + " already. Please wait.");
              return;
           }

           MatchmakingServer.ClientConnection var9 = new MatchmakingServer.ClientConnection(var1, var2, var4, var0);
           try {
              var7.put(var9);
           } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              var4.println("ERROR: Interrupted while joining queue");
              return;
           }
           queueJoinTimes.put(var1 + ":" + var2, System.currentTimeMillis());
           System.out.println("[Queue] " + var1 + " joined queue for kit: " + var2 + " (Total: " + var7.size() + ")");
           var4.println("WAITING: Added to queue for " + var2);
        }

        // Keep the connection alive while the user is waiting
        while (!var0.isClosed()) {
           try {
              Thread.sleep(1000L);
           } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
           }
        }
      } catch (IOException var38) {
         System.err.println("[MatchmakingServer] Error: " + var38.getMessage());
      } finally {
         if (var1 != null) {
            removeUserFromAllQueues(var1);
            removeUserJoinTimes(var1);
         }
         try {
            if (var3 != null) var3.close();
         } catch (IOException ignored) {}
         if (var4 != null) {
            try { var4.close(); } catch (Exception ignored) {}
         }
         try {
            var0.close();
         } catch (IOException var31) {
         }
      }
   }

   private static void whitelistPlayer(String var0) {
      RconManager.addToWhitelist(var0);

      try {
         Thread.sleep(2000L);
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      }

      whitelistedPlayers.put(var0, System.currentTimeMillis());
   }

   private static void removeFromWhitelist(String var0) {
      if (whitelistedPlayers.containsKey(var0)) {
         RconManager.removeFromWhitelist(var0);
         whitelistedPlayers.remove(var0);
      }

   }

   private static boolean isPlayerOnline(String var0) {
      return true;
   }

   private static String calculateFingerprint(String var0, String var1) {
      String var2 = var0 + "::" + var1 + "::TitleModSecret";

      return var2;
   }

   private static String calculateTimestampedSignature(String var0, String var1, long var2) {
      String var4 = var0 + "::" + var1 + "::" + var2 + "::TitleModSecret";

      return var4;
   }

   private static boolean waitForPlayersToJoin(String var0, String var1, long var2) {
      long var4 = System.currentTimeMillis();

      while(System.currentTimeMillis() - var4 < var2) {
         try {
            Thread.sleep(5000L);
            RconClient var6 = new RconClient("34.159.92.94", 25575, "98750");

            boolean var7;
            label66: {
               label65: {
                  try {
                     var6.connect();
                     if (!var6.authenticate()) {
                        break label65;
                     }

                     String var8 = var6.sendCommand("list");
                     if (var8 == null || !var8.contains(var0) || !var8.contains(var1)) {
                        break label65;
                     }

                     if (isKitEditorUser(var0) || isKitEditorUser(var1) || mightBeKitEditorUser(var0) || mightBeKitEditorUser(var1)) {
                        System.out.println("[Match] One or both players are kit editor users, not proceeding with duel");
                        var7 = false;
                        break label66;
                     }

                     var7 = true;
                  } catch (Throwable var11) {
                     try {
                        var6.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }

                     throw var11;
                  }

                  var6.close();
                  return var7;
               }

               var6.close();
               continue;
            }

            var6.close();
            return var7;
         } catch (Exception var12) {
            System.err.println("[RCON] Error checking player status: " + var12.getMessage());
         }
      }

      return false;
   }

   private static void sendRconCommands(String... var0) {
      (new Thread(() -> {
         try {
            RconClient var1 = new RconClient("34.159.92.94", 25575, "98750");

            try {
               var1.connect();
               if (var1.authenticate()) {
                  String[] var2 = var0;
                  int var3 = var0.length;

                  for(int var4 = 0; var4 < var3; ++var4) {
                     String var5 = var2[var4];
                     String var6 = var1.sendCommand(var5);
                     System.out.println("[RCON] Sent: " + var5 + " | Response: " + String.valueOf(var6));
                  }
               }
            } catch (Throwable var8) {
               try {
                  var1.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            var1.close();
         } catch (Exception var9) {
            System.err.println("[RCON] Failed to send commands: " + var9.getMessage());
         }

      })).start();
   }

   private static String sendRconCommandSync(String var0) {
      try {
         RconClient var1 = new RconClient("34.159.92.94", 25575, "98750");

         String var2;
         label46: {
            try {
               var1.connect();
               if (var1.authenticate()) {
                  var2 = var1.sendCommand(var0);
                  break label46;
               }
            } catch (Throwable var6) {
               try {
                  var1.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }

               throw var6;
            }

            var1.close();
            return null;
         }

         var1.close();
         return var2;
      } catch (Exception var7) {
         System.err.println("[RCON] Error sending command: " + var7.getMessage());
         return null;
      }
   }

   public static void handleKitEditorMode(String var0, String var1) {
      kitEditorUsers.put(var0, var1);
      System.out.println("[Kit Editor] User " + var0 + " identified as kit editor for kit: " + var1);
      Iterator var2 = queues.entrySet().iterator();

      while(var2.hasNext()) {
         Entry var3 = (Entry)var2.next();
         String var4 = (String)var3.getKey();
         BlockingQueue var5 = (BlockingQueue)var3.getValue();
         var5.removeIf((var1x) -> {
            MatchmakingServer.ClientConnection conn = (MatchmakingServer.ClientConnection)var1x;
            return conn.getUsername().equals(var0);
         });
         String var6 = var0 + ":" + var4;
         queueJoinTimes.remove(var6);
      }

   }

   public static boolean isKitEditorUser(String var0) {
      return kitEditorUsers.containsKey(var0);
   }

   public static boolean mightBeKitEditorUser(String var0) {
      if (isKitEditorUser(var0)) {
         return true;
      } else {
         long var1 = System.currentTimeMillis();
         Iterator var3 = queueJoinTimes.entrySet().iterator();

         while(var3.hasNext()) {
            Entry var4 = (Entry)var3.next();
            String var5 = (String)var4.getKey();
            if (var5.startsWith(var0 + ":")) {
               long var6 = (Long)var4.getValue();
               if (var1 - var6 < 120000L) {
                  return false;
               }
            }
         }

         if (whitelistedPlayers.containsKey(var0)) {
            long var8 = (Long)whitelistedPlayers.get(var0);
            if (var1 - var8 < 300000L) {
               return true;
            }
         }

         return false;
      }
   }

   public static void removeKitEditorUser(String var0) {
      kitEditorUsers.remove(var0);
      System.out.println("[Kit Editor] User " + var0 + " removed from kit editor mode");
   }

   public static void onPlayerJoin(String var0) {
      if (whitelistedPlayers.containsKey(var0)) {
         long var1 = (Long)whitelistedPlayers.get(var0);
         long var3 = System.currentTimeMillis();
         if (var3 - var1 < 300000L) {
            System.out.println("[Kit Editor] Auto-detected " + var0 + " as kit editor user");
            kitEditorUsers.put(var0, "UNKNOWN");
         }
      }

   }

   public static void onKitEditorWhitelist(String var0) {
      whitelistedPlayers.put(var0, System.currentTimeMillis());
      System.out.println("[Kit Editor] User " + var0 + " whitelisted for kit editing");
   }

   public static void monitorPlayerJoins() {
      (new Thread(() -> {
         while(true) {
            try {
               Thread.sleep(10000L);
               RconClient var0 = new RconClient("34.159.92.94", 25575, "98750");

               try {
                  var0.connect();
                  if (var0.authenticate()) {
                     String var1 = var0.sendCommand("list");
                     if (var1 != null) {
                        String[] var2 = var1.split("\n");
                        String[] var3 = var2;
                        int var4 = var2.length;

                        for(int var5 = 0; var5 < var4; ++var5) {
                           String var6 = var3[var5];
                           if (var6.contains(":")) {
                              String[] var7 = var6.split(":");
                              if (var7.length >= 2) {
                                 String var8 = var7[1].trim();
                                 if (var8.contains("(")) {
                                    String var9 = var8.substring(0, var8.indexOf("(")).trim();
                                    if (!isKitEditorUser(var9) && !mightBeKitEditorUser(var9)) {
                                       onPlayerJoin(var9);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               } catch (Throwable var11) {
                  try {
                     var0.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }

                  throw var11;
               }

               var0.close();
            } catch (Exception var12) {
               System.err.println("[RCON] Error monitoring player joins: " + var12.getMessage());
            }
         }
      })).start();
   }

   private static void monitorPlayersAndCleanup(String var0, String var1) {
      (new Thread(() -> {
         long var2 = -1L;
         long var4 = 2000L;

         while(true) {
            try {
               label86: {
                  RconClient var6 = new RconClient("34.159.92.94", 25575, "98750");

                  label77: {
                     try {
                        var6.connect();
                        if (var6.authenticate()) {
                           String var7 = var6.sendCommand("list");
                           boolean var8 = var7 != null && var7.contains(var0);
                           boolean var9 = var7 != null && var7.contains(var1);
                           if (!var8 && !var9) {
                              if (var2 >= 0L) {
                                 if (System.currentTimeMillis() - var2 > var4) {
                                    removeFromWhitelist(var0);
                                    removeFromWhitelist(var1);
                                    System.out.println("[Whitelist] Cleaned up after match for " + var0 + " and " + var1);
                                    break label77;
                                 }
                              } else {
                                 var2 = System.currentTimeMillis();
                              }
                           } else {
                              var2 = -1L;
                           }
                        }
                     } catch (Throwable var12) {
                        try {
                           var6.close();
                        } catch (Throwable var10) {
                           var12.addSuppressed(var10);
                        }

                        throw var12;
                     }

                     var6.close();
                     break label86;
                  }

                  var6.close();
                  break;
               }
            } catch (Exception var13) {
               System.err.println("[RCON] Monitor error: " + var13.getMessage());
            }

            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var11) {
               break;
            }
         }

      })).start();
   }

   public static void clearFailedAttempts(String var0) {
      failedAttempts.remove(var0);
   }

   public static String getSystemStatus() {
      StringBuilder var0 = new StringBuilder();
      var0.append("Active queues: ").append(queues.size()).append(" | ");
      var0.append("Total players in queues: ");
      int var1 = 0;

      BlockingQueue var2;
      for(Iterator var3 = queues.values().iterator(); var3.hasNext(); var1 += var2.size()) {
         var2 = (BlockingQueue)var3.next();
      }

      var0.append(var1).append(" | ");
      var0.append("Kit editor users: ").append(kitEditorUsers.size()).append(" | ");
      var0.append("Whitelisted players: ").append(whitelistedPlayers.size());
      return var0.toString();
   }

   public static String getDebugInfo() {
      StringBuilder var0 = new StringBuilder();
      var0.append("=== TitleMod Debug Info ===\n");
      var0.append("Server IP: ").append("34.159.92.94").append("\n");
      var0.append("Server Port: ").append(25565).append("\n");
      var0.append("RCON Port: ").append(25575).append("\n");
      var0.append("Max Wait Time: ").append(60000L).append("ms\n");
      var0.append("Active queues: ").append(queues.size()).append("\n");
      Iterator var1 = queues.entrySet().iterator();

      Entry var2;
      while(var1.hasNext()) {
         var2 = (Entry)var1.next();
         String var3 = (String)var2.getKey();
         BlockingQueue var4 = (BlockingQueue)var2.getValue();
         var0.append("  ").append(var3).append(": ").append(var4.size()).append(" players\n");
      }

      var0.append("Kit editor users:\n");
      var1 = kitEditorUsers.entrySet().iterator();

      while(var1.hasNext()) {
         var2 = (Entry)var1.next();
         var0.append("  ").append((String)var2.getKey()).append(" -> ").append((String)var2.getValue()).append("\n");
      }

      var0.append("Whitelisted players:\n");
      var1 = whitelistedPlayers.entrySet().iterator();

      while(var1.hasNext()) {
         var2 = (Entry)var1.next();
         long var5 = System.currentTimeMillis() - (Long)var2.getValue();
         var0.append("  ").append((String)var2.getKey()).append(" (whitelisted ").append(var5 / 1000L).append("s ago)\n");
      }

      return var0.toString();
   }

   private static boolean validateWithCentralizedServer(String var0, String var1, String var2, String var3) {
      try {
         Socket var4 = new Socket();

         boolean var5;
         label83: {
            try {
               PrintWriter var6;
               label88: {
                  var4.connect(new InetSocketAddress("localhost", 5008), 5000);
                  var6 = new PrintWriter(var4.getOutputStream(), true);

                  try {
                     label89: {
                        BufferedReader var7 = new BufferedReader(new InputStreamReader(var4.getInputStream()));

                        label75: {
                           try {
                              String var8 = "REQUEST_WHITELIST::" + var0 + "::" + var1 + "::" + var2;
                              var6.println(var8);
                              String var9 = var7.readLine();
                              if (var9 != null && var9.startsWith("SUCCESS:")) {
                                 System.out.println("[MatchmakingServer] User " + var0 + " validated successfully with centralized server");
                                 var5 = true;
                                 break label75;
                              }

                              System.err.println("[MatchmakingServer] Validation failed for " + var0 + ": " + var9);
                              var5 = false;
                           } catch (Throwable var13) {
                              try {
                                 var7.close();
                              } catch (Throwable var12) {
                                 var13.addSuppressed(var12);
                              }

                              throw var13;
                           }

                           var7.close();
                           break label89;
                        }

                        var7.close();
                        break label88;
                     }
                  } catch (Throwable var14) {
                     try {
                        var6.close();
                     } catch (Throwable var11) {
                        var14.addSuppressed(var11);
                     }

                     throw var14;
                  }

                  var6.close();
                  break label83;
               }

               var6.close();
            } catch (Throwable var15) {
               try {
                  var4.close();
               } catch (Throwable var10) {
                  var15.addSuppressed(var10);
               }

               throw var15;
            }

            var4.close();
            return var5;
         }

         var4.close();
         return var5;
      } catch (Exception var16) {
         System.err.println("[MatchmakingServer] Failed to connect to centralized validation server: " + var16.getMessage());
         return false;
      }
   }

   public static void startAdminConsole() {
      new Thread(() -> {
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
         while (true) {
            try {
               String line = reader.readLine();
               if (line == null) continue;
               if (line.trim().equalsIgnoreCase("status")) {
                  System.out.println("\n=== MatchmakingServer Status ===");
                  System.out.println("Active queues: " + queues.size());
                  for (Map.Entry<String, BlockingQueue<ClientConnection>> entry : queues.entrySet()) {
                     System.out.println("  Kit: " + entry.getKey() + " | Players: " + entry.getValue().size());
                  }
                  synchronized (inMatchLock) {
                     System.out.println("In-match users: " + inMatch.size() + " => " + inMatch);
                  }
                  System.out.println("===============================\n");
               }
            } catch (Exception e) {
               System.err.println("[AdminConsole] Error: " + e.getMessage());
            }
         }
      }, "AdminConsole").start();
   }

   public static void main(String[] var0) {
      System.out.println("[MatchmakingServer] Starting on port 5000...");
      startAdminConsole();

      try {
         scheduler.scheduleAtFixedRate(new MatchmakingServer.MatchmakingEngine(), 0L, 1L, TimeUnit.SECONDS);
         scheduler.scheduleAtFixedRate(new MatchmakingServer.QueueTimeoutChecker(), 10L, 10L, TimeUnit.SECONDS);
      } catch (Exception var5) {
         System.err.println("[MatchmakingServer] Failed to schedule workers: " + var5.getMessage());
      }

      try {
         ServerSocket var1 = new ServerSocket(5000);

         try {
            while(true) {
               while(true) {
                  try {
                     Socket var2 = var1.accept();
                     (new Thread(() -> {
                        handleClientConnection(var2);
                     })).start();
                  } catch (IOException var6) {
                     System.err.println("[MatchmakingServer] Accept error: " + var6.getMessage());
                  }
               }
            }
         } catch (Throwable var7) {
            try {
               var1.close();
            } catch (Throwable var4) {
               var7.addSuppressed(var4);
            }

            throw var7;
         }
      } catch (IOException var8) {
         System.err.println("[MatchmakingServer] Fatal error: " + var8.getMessage());
      }
   }

   static {
      autoPopulateInMatchFromRcon();
      loadInMatch();
      (new Thread(() -> {
         while(true) {
            long var0 = System.currentTimeMillis();
            sessions.entrySet().removeIf((var2) -> {
               return var0 - ((MatchmakingServer.SessionInfo)var2.getValue()).lastActive > 3600000L;
            });
            // Clean up expired cooldowns for recentlyMatched
            recentlyMatched.entrySet().removeIf(entry -> var0 - entry.getValue() > MATCHMAKING_COOLDOWN_MS);
            try {
               Thread.sleep(300000L);
            } catch (InterruptedException var3) {
            }
         }
      }, "SessionCleanup")).start();
   }

   private static void autoPopulateInMatchFromRcon() {
      try {
         RconClient rcon = new RconClient(MINECRAFT_SERVER_IP, RCON_PORT, RCON_PASSWORD);
         rcon.connect();
         if (rcon.authenticate()) {
            String listResp = rcon.sendCommand("list");
            if (listResp != null) {
               Set<String> found = new HashSet<>();
               String[] tokens = listResp.split("[ ,\n]");
               for (String token : tokens) {
                  String name = token.trim();
                  if (!name.isEmpty() && name.matches("^[A-Za-z0-9_]{3,16}$")) {
                     found.add(name.toLowerCase());
                  }
               }
               synchronized (inMatchLock) {
                  inMatch.clear();
                  inMatch.addAll(found);
                  saveInMatch();
               }
               System.out.println("[Startup] Auto-populated inMatch with online users: " + found);
            }
         }
         rcon.close();
      } catch (Exception e) {
         System.err.println("[Startup] Failed to auto-populate inMatch from RCON: " + e.getMessage());
      }
   }

   private static void notifyClientSafe(MatchmakingServer.ClientConnection conn, String message) {
      try {
         if (conn != null && conn.getWriter() != null) {
            conn.getWriter().println(message);
         }
      } catch (Exception e) {
         System.err.println("[Notify] Failed to notify client " + (conn != null ? conn.getUsername() : "null") + ": " + e.getMessage());
      }
   }

   private static class ClientConnection {
      private final String username;
      private final String kit;
      private final PrintWriter writer;
      private final Socket socket;

      public ClientConnection(String var1, String var2, PrintWriter var3, Socket var4) {
         this.username = var1;
         this.kit = var2;
         this.writer = var3;
         this.socket = var4;
      }

      public String getUsername() {
         return this.username;
      }

      public String getKit() {
         return this.kit;
      }

      public PrintWriter getWriter() {
         return this.writer;
      }

      public Socket getSocket() {
         return this.socket;
      }
   }

   private static class MatchmakingEngine implements Runnable {
      public void run() {
         Iterator var1 = MatchmakingServer.queues.entrySet().iterator();
         while(var1.hasNext()) {
            Entry var2 = (Entry)var1.next();
            String var3 = (String)var2.getKey();
            BlockingQueue var4 = (BlockingQueue)var2.getValue();
            synchronized (var4) {
               while (var4.size() >= 2) {
                  // Take two players
                  MatchmakingServer.ClientConnection var6 = (MatchmakingServer.ClientConnection)var4.peek();
                  MatchmakingServer.ClientConnection var7 = null;
                  Iterator it = var4.iterator();
                  if (it.hasNext()) {
                     var6 = (MatchmakingServer.ClientConnection)it.next();
                  }
                  if (it.hasNext()) {
                     var7 = (MatchmakingServer.ClientConnection)it.next();
                  }
                  if (var6 == null || var7 == null) break;
                  // Check if either is already in a match
                  synchronized (inMatchLock) {
                     if (inMatch.contains(var6.getUsername().toLowerCase()) || inMatch.contains(var7.getUsername().toLowerCase())) {
                        // Remove from queue if already in match
                        var4.remove(var6);
                        var4.remove(var7);
                        continue;
                     }
                     // Mark both as in match
                     inMatch.add(var6.getUsername().toLowerCase());
                     inMatch.add(var7.getUsername().toLowerCase());
                     saveInMatch();
                  }
                  // Remove both from queue
                  var4.remove(var6);
                  var4.remove(var7);
                  // Proceed with match logic (existing code follows)
                  // Before whitelisting and sending MATCH_FOUND, check if both clients are still connected
                  if (var6.getSocket().isClosed() || !var6.getSocket().isConnected() || var7.getSocket().isClosed() || !var7.getSocket().isConnected()) {
                     // Notify both clients if possible
                     try { if (var6.getWriter() != null) var6.getWriter().println("ERROR: Opponent left the queue before the match could start."); } catch (Exception e) {}
                     try { if (var7.getWriter() != null) var7.getWriter().println("ERROR: Opponent left the queue before the match could start."); } catch (Exception e) {}
                     removeFromWhitelist(var6.getUsername());
                     removeFromWhitelist(var7.getUsername());
                     continue;
                  }
                  // Whitelist both players synchronously before sending MATCH_FOUND
                  boolean whitelist1 = false;
                  boolean whitelist2 = false;
                  try {
                     whitelistPlayer(var6.getUsername());
                     whitelist1 = true;
                  } catch (Exception e) {
                     System.err.println("[Whitelist] Error whitelisting " + var6.getUsername() + ": " + e.getMessage());
                  }
                  try {
                     whitelistPlayer(var7.getUsername());
                     whitelist2 = true;
                  } catch (Exception e) {
                     System.err.println("[Whitelist] Error whitelisting " + var7.getUsername() + ": " + e.getMessage());
                  }
                  if (!whitelist1 || !whitelist2) {
                     // Inform both clients of error and do not start match
                     try { if (var6.getWriter() != null) var6.getWriter().println("ERROR: Failed to whitelist players. Please try again."); } catch (Exception e) {}
                     try { if (var7.getWriter() != null) var7.getWriter().println("ERROR: Failed to whitelist players. Please try again."); } catch (Exception e) {}
                     removeFromWhitelist(var6.getUsername());
                     removeFromWhitelist(var7.getUsername());
                     continue;
                  }
                  // Now both are whitelisted, send MATCH_FOUND to both, even if they left queue after match found
                  String var8 = MatchmakingServer.generateSessionToken();
                  String var9 = MatchmakingServer.generateSessionToken();
                  try { if (var6.getWriter() != null) var6.getWriter().println("MATCH_FOUND:34.159.92.94:25565:" + var8); } catch (Exception e) {}
                  try { if (var7.getWriter() != null) var7.getWriter().println("MATCH_FOUND:34.159.92.94:25565:" + var9); } catch (Exception e) {}
                  PrintStream var10 = System.out;
                  String var11 = var6.getUsername();
                  var10.println("[Match] " + var11 + " vs " + var7.getUsername() + " in kit: " + var3);
                  System.out.println("[Match] Creating match: " + var6.getUsername() + " vs " + var7.getUsername() + " in kit: " + var3);
                  // Create final copies for lambda
                  final String kitNameFinal = var3;
                  final ClientConnection player1Final = var6;
                  final ClientConnection player2Final = var7;
                  (new Thread(() -> {
                     try {
                        try {
                           MatchmakingServer.incrementActiveMatches(kitNameFinal);
                           if (MatchmakingServer.waitForPlayersToJoin(player1Final.getUsername(), player2Final.getUsername(), 90000L)) {
                              System.out.println("[Match] Both players detected online. Initiating duel sequence.");
                              try {
                                 Thread.sleep(1500L);
                              } catch (InterruptedException ie) {
                                 Thread.currentThread().interrupt();
                                 return;
                              }
                              String duelCmd = String.format("sudo %s duel %s 4 %s", player1Final.getUsername(), player2Final.getUsername(), kitNameFinal.toLowerCase());
                              String acceptCmd = String.format("sudo %s duel accept %s", player2Final.getUsername(), player1Final.getUsername());
                              MatchmakingServer.sendRconCommands(duelCmd, acceptCmd);
                              MatchmakingServer.monitorPlayersAndCleanup(player1Final.getUsername(), player2Final.getUsername());
                              return;
                           }
                           // If not both joined, check who is online and kick with dodge message
                           boolean p1Online = isPlayerOnline(player1Final.getUsername());
                           boolean p2Online = isPlayerOnline(player2Final.getUsername());
                           if (p1Online && !p2Online) {
                              String msg = "Your opponent " + player2Final.getUsername() + " dodged the match with you. Congratulations!";
                              System.out.println("[Kick] " + player1Final.getUsername() + ": " + msg);
                              (new Thread(() -> {
                                 RconManager.kickPlayer(player1Final.getUsername(), msg);
                              })).start();
                              notifyClientSafe(player1Final, "ERROR: Opponent left or dodged the match.");
                              notifyClientSafe(player2Final, "ERROR: You left or dodged the match.");
                           } else if (p2Online && !p1Online) {
                              String msg = "Your opponent " + player1Final.getUsername() + " dodged the match with you. Congratulations!";
                              System.out.println("[Kick] " + player2Final.getUsername() + ": " + msg);
                              (new Thread(() -> {
                                 RconManager.kickPlayer(player2Final.getUsername(), msg);
                              })).start();
                              notifyClientSafe(player2Final, "ERROR: Opponent left or dodged the match.");
                              notifyClientSafe(player1Final, "ERROR: You left or dodged the match.");
                           } else {
                              // Neither joined: timeout
                              notifyClientSafe(player1Final, "ERROR: Match timed out. No players joined the arena.");
                              notifyClientSafe(player2Final, "ERROR: Match timed out. No players joined the arena.");
                           }
                           MatchmakingServer.removeFromWhitelist(player1Final.getUsername());
                           MatchmakingServer.removeFromWhitelist(player2Final.getUsername());
                        } catch (Exception ex) {
                           System.err.println("[Whitelist] Error: " + ex.getMessage());
                           notifyClientSafe(player1Final, "ERROR: Internal server error during match.");
                           notifyClientSafe(player2Final, "ERROR: Internal server error during match.");
                        }
                     } finally {
                        System.out.println("[MatchCleanup] Cleaning up match: " + player1Final.getUsername() + " vs " + player2Final.getUsername() + " in kit: " + kitNameFinal);
                        MatchmakingServer.decrementActiveMatches(kitNameFinal);
                        // Remove from inMatch and update JSON
                        synchronized (inMatchLock) {
                           inMatch.remove(player1Final.getUsername().toLowerCase());
                           inMatch.remove(player2Final.getUsername().toLowerCase());
                           saveInMatch();
                        }
                     }
                  })).start();
               }
            }
         }

      }
   }

   private static class QueueTimeoutChecker implements Runnable {
      public void run() {
         // Disabled: Users can now wait in queue indefinitely. No timeout removal.
         // (Previously, this would remove users after 1 minute or 5 minutes for kit editor users.)
      }
   }

   private static class SessionInfo {
      final String username;
      final String ip;
      volatile long lastActive;

      SessionInfo(String var1, String var2) {
         this.username = var1;
         this.ip = var2;
         this.lastActive = System.currentTimeMillis();
      }
   }

   private static final class SimpleRateLimiter {
      private final int maxRequests;
      private final long windowMs;
      private final ArrayDeque<Long> timestamps = new ArrayDeque();

      private SimpleRateLimiter(int var1, long var2) {
         this.maxRequests = var1;
         this.windowMs = var2;
      }

      synchronized boolean tryAcquire() {
         long var1 = System.currentTimeMillis();

         while(!this.timestamps.isEmpty() && var1 - (Long)this.timestamps.peekFirst() > this.windowMs) {
            this.timestamps.pollFirst();
         }

         if (this.timestamps.size() >= this.maxRequests) {
            return false;
         } else {
            this.timestamps.addLast(var1);
            return true;
         }
      }
   }

   private static class RateLimitInfo {
      volatile int count = 0;
      volatile long windowStart = System.currentTimeMillis();

      RateLimitInfo() {
      }
   }

   private static void saveInMatch() {
      synchronized (inMatchLock) {
         try {
            Path p = Paths.get(INMATCH_FILE);
            List<String> lines = inMatch.stream().toList();
            Files.write(p, lines, StandardCharsets.UTF_8);
         } catch (Exception e) {
            System.err.println("[MatchmakingServer] Failed to save inMatch file: " + e.getMessage());
         }
      }
   }

   private static void loadInMatch() {
      synchronized (inMatchLock) {
         try {
            Path p = Paths.get(INMATCH_FILE);
            if (Files.exists(p)) {
               List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
               inMatch.clear();
               for (String line : lines) {
                  String name = line == null ? "" : line.trim().toLowerCase();
                  if (!name.isEmpty()) {
                     inMatch.add(name);
                  }
               }
            }
         } catch (Exception e) {
            // Ignore read errors on startup
         }
      }
   }
}
    
