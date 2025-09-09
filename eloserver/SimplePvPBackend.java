    import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SimplePvPBackend {
   private static final Map<String, Integer> playerElo = new ConcurrentHashMap();
   private static final List<DuelEntry> duelLog = new CopyOnWriteArrayList();
   private static final Map<String, String> uuidToUsername = new ConcurrentHashMap();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final int PORT = 4560;
   private static final int DEFAULT_INITIAL_ELO = 1000;
   private static final String ELO_FILE = "playerElo.json";
   private static final String DUEL_LOG_FILE = "duelLog.json";
   private static final String UUID_USERNAME_FILE = "uuidUsernameMap.json";
       private static final String VALIDATION_SERVER_IP = "34.159.92.94";
   private static final int VALIDATION_SERVER_PORT = 5008;
   private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

   // REMOVED: Session and rate limiting code (SessionInfo, RateLimitInfo, sessions, rateLimits, generateSessionToken, handleLogin, handleLogout, validateSession, checkRateLimit, and session cleanup thread)

   public static void main(String[] var0) throws IOException {
      loadData();
      scheduler.scheduleAtFixedRate(SimplePvPBackend::saveData, 5L, 5L, TimeUnit.MINUTES);
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         System.out.println("Server shutting down. Initiating final data save...");
         saveData();
         scheduler.shutdown();

         try {
            if (!scheduler.awaitTermination(10L, TimeUnit.SECONDS)) {
               System.err.println("Scheduler did not terminate gracefully within 10 seconds. Forcing shutdown.");
               scheduler.shutdownNow();
            }
         } catch (InterruptedException var1) {
            System.err.println("Shutdown hook interrupted during scheduler termination.");
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
         }

         System.out.println("Server shutdown complete.");
      }));
      HttpServer var1 = HttpServer.create(new InetSocketAddress(4560), 0);
      System.out.println("PvP Backend Server starting on port 4560...");
      System.out.println("[SimplePvPBackend] Using centralized validation server on " + VALIDATION_SERVER_IP + ":" + VALIDATION_SERVER_PORT);
      var1.createContext("/elo.json", SimplePvPBackend::handleEloRequest);
      var1.createContext("/real_time_duels.json", SimplePvPBackend::handleDuelLogRequest);
      var1.createContext("/update_duel", SimplePvPBackend::handleUpdateDuel);
      var1.createContext("/update_username", SimplePvPBackend::handleUpdateUsername);
      var1.createContext("/adjust_elo", SimplePvPBackend::handleAdjustElo);
      var1.setExecutor((Executor)null);
      var1.start();
      System.out.println("Server started successfully. Awaiting requests on port 4560...");
   }

   private static void loadData() {
      FileReader var0;
      Type var1;
      Map var2;
      try {
         var0 = new FileReader("playerElo.json");

         try {
            var1 = (new TypeToken<ConcurrentHashMap<String, Integer>>() {
            }).getType();
            var2 = (Map)GSON.fromJson(var0, var1);
            if (var2 != null) {
               playerElo.putAll(var2);
               System.out.println("Loaded ELO data from playerElo.json. Entries: " + playerElo.size());
            }
         } catch (Throwable var10) {
            try {
               var0.close();
            } catch (Throwable var5) {
               var10.addSuppressed(var5);
            }

            throw var10;
         }

         var0.close();
      } catch (IOException var11) {
         System.err.println("Could not load ELO data from playerElo.json: " + var11.getMessage() + ". Starting with empty data.");
      }

      try {
         var0 = new FileReader("duelLog.json");

         try {
            var1 = (new TypeToken<CopyOnWriteArrayList<DuelEntry>>() {
            }).getType();
            List var12 = (List)GSON.fromJson(var0, var1);
            if (var12 != null) {
               duelLog.addAll(var12);
               System.out.println("Loaded Duel Log data from duelLog.json. Entries: " + duelLog.size());
            }
         } catch (Throwable var8) {
            try {
               var0.close();
            } catch (Throwable var4) {
               var8.addSuppressed(var4);
            }

            throw var8;
         }

         var0.close();
      } catch (IOException var9) {
         System.err.println("Could not load Duel Log data from duelLog.json: " + var9.getMessage() + ". Starting with empty data.");
      }

      try {
         var0 = new FileReader("uuidUsernameMap.json");

         try {
            var1 = (new TypeToken<ConcurrentHashMap<String, String>>() {
            }).getType();
            var2 = (Map)GSON.fromJson(var0, var1);
            if (var2 != null) {
               uuidToUsername.putAll(var2);
               System.out.println("Loaded UUID-Username map from uuidUsernameMap.json. Entries: " + uuidToUsername.size());
            }
         } catch (Throwable var6) {
            try {
               var0.close();
            } catch (Throwable var3) {
               var6.addSuppressed(var3);
            }

            throw var6;
         }

         var0.close();
      } catch (IOException var7) {
         System.err.println("Could not load UUID-Username map from uuidUsernameMap.json: " + var7.getMessage() + ". Starting with empty data.");
      }

   }

   private static synchronized void saveData() {
      FileWriter var0;
      try {
         var0 = new FileWriter("playerElo.json");

         try {
            GSON.toJson(playerElo, var0);
            System.out.println("Saved ELO data to playerElo.json");
         } catch (Throwable var10) {
            try {
               var0.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }

            throw var10;
         }

         var0.close();
      } catch (IOException var11) {
         System.err.println("Error saving ELO data to playerElo.json: " + var11.getMessage());
      }

      try {
         var0 = new FileWriter("duelLog.json");

         try {
            GSON.toJson(duelLog, var0);
            System.out.println("Saved Duel Log data to duelLog.json");
         } catch (Throwable var7) {
            try {
               var0.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         var0.close();
      } catch (IOException var8) {
         System.err.println("Error saving Duel Log data to duelLog.json: " + var8.getMessage());
      }

      try {
         var0 = new FileWriter("uuidUsernameMap.json");

         try {
            GSON.toJson(uuidToUsername, var0);
            System.out.println("Saved UUID-Username map to uuidUsernameMap.json");
         } catch (Throwable var4) {
            try {
               var0.close();
            } catch (Throwable var3) {
               var4.addSuppressed(var3);
            }

            throw var4;
         }

         var0.close();
      } catch (IOException var5) {
         System.err.println("Error saving UUID-Username map to uuidUsernameMap.json: " + var5.getMessage());
      }

   }

   private static String normalizeUuid(String var0) {
      return var0 != null ? var0.replace("-", "").toLowerCase() : null;
   }

   private static void ensurePlayerExists(String var0, String var1) {
      if (var0 != null) {
         playerElo.putIfAbsent(var0, 1000);
         if ((Integer)playerElo.get(var0) == 1000 && !uuidToUsername.containsKey(var0)) {
            System.out.println("Player " + (var1 != null ? var1 : var0) + " (" + var0 + ") initialized with ELO: 1000");
         }

         if (var1 != null && !var1.isEmpty()) {
            String var2 = (String)uuidToUsername.put(var0, var1);
            if (var2 == null) {
               System.out.println("Added new username mapping for " + var0 + " to " + var1);
            } else if (!var2.equals(var1)) {
               System.out.println("Updated username mapping for " + var0 + " from " + var2 + " to " + var1);
            }
         }

      }
   }

   private static void handleEloRequest(HttpExchange var0) throws IOException {
      if ("GET".equals(var0.getRequestMethod())) {
         // REMOVED: API validation and centralized validation
         String var1 = var0.getRequestURI().getQuery();
         String var2 = null;
         String var3 = null;
         if (var1 != null) {
            String[] var4 = var1.split("&");
            int var5 = var4.length;
            for(int var6 = 0; var6 < var5; ++var6) {
               String var7 = var4[var6];
               String[] var8 = var7.split("=");
               if (var8.length == 2 && "uuid".equals(var8[0])) {
                  try {
                     var2 = URLDecoder.decode(var8[1], StandardCharsets.UTF_8.toString());
                     var3 = normalizeUuid(var2);
                  } catch (Exception var10) {
                     System.err.println("Error decoding UUID parameter in ELO request: " + var10.getMessage());
                  }
                  break;
               }
            }
         }
         if (var3 != null) {
            ensurePlayerExists(var3, (String)null);
         }
         String var11 = GSON.toJson(playerElo);
         sendResponse(var0, 200, var11);
         PrintStream var10000 = System.out;
         int var10001 = playerElo.size();
         var10000.println("Served /elo.json request. Current ELO map size: " + var10001 + ". Raw Requested UUID: " + (var2 != null ? var2 : "N/A") + ". Normalized: " + (var3 != null ? var3 : "N/A"));
      } else {
         sendResponse(var0, 405, "Method Not Allowed");
      }
   }

   private static void handleDuelLogRequest(HttpExchange var0) throws IOException {
      if ("GET".equals(var0.getRequestMethod())) {
         // REMOVED: API validation and centralized validation
         List var1 = (List)duelLog.stream().sorted(Comparator.comparing(DuelEntry::getTimestamp).reversed()).map((var0x) -> {
            return new DuelLogEntryDTO((String)uuidToUsername.getOrDefault(var0x.getPlayer1Uuid(), var0x.getPlayer1Uuid()), (String)uuidToUsername.getOrDefault(var0x.getPlayer2Uuid(), var0x.getPlayer2Uuid()), (String)uuidToUsername.getOrDefault(var0x.getWinnerUuid(), var0x.getWinnerUuid()), (String)uuidToUsername.getOrDefault(var0x.getLoserUuid(), var0x.getLoserUuid()), var0x.getTimestamp());
         }).collect(Collectors.toList());
         String var2 = GSON.toJson(var1);
         sendResponse(var0, 200, var2);
         System.out.println("Served /real_time_duels.json request with usernames. Total duel entries: " + duelLog.size());
      } else {
         sendResponse(var0, 405, "Method Not Allowed");
      }
   }

   private static void handleUpdateDuel(HttpExchange var0) throws IOException {
      if ("POST".equals(var0.getRequestMethod())) {
         // REMOVED: API validation and centralized validation
         try {
            InputStreamReader var1 = new InputStreamReader(var0.getRequestBody(), StandardCharsets.UTF_8);
            try {
               JsonObject var2 = JsonParser.parseReader(var1).getAsJsonObject();
               String var3 = var2.has("player1Uuid") ? var2.get("player1Uuid").getAsString() : null;
               String var4 = var2.has("player2Uuid") ? var2.get("player2Uuid").getAsString() : null;
               String var5 = var2.has("winnerUuid") ? var2.get("winnerUuid").getAsString() : null;
               String var6 = var2.has("loserUuid") ? var2.get("loserUuid").getAsString() : null;
               long var7 = var2.has("timestamp") ? var2.get("timestamp").getAsLong() : System.currentTimeMillis();
               String var9 = var2.has("player1Username") ? var2.get("player1Username").getAsString() : null;
               String var10 = var2.has("player2Username") ? var2.get("player2Username").getAsString() : null;
               String var11 = var2.has("winnerUsername") ? var2.get("winnerUsername").getAsString() : null;
               String var12 = var2.has("loserUsername") ? var2.get("loserUsername").getAsString() : null;
               System.out.println("Received raw UUIDs from plugin for duel update: P1: " + var3 + " (" + var9 + "), P2: " + var4 + " (" + var10 + "), Winner: " + var5 + " (" + var11 + "), Loser: " + var6 + " (" + var12 + ")");
               String var13 = normalizeUuid(var3);
               String var14 = normalizeUuid(var4);
               String var15 = normalizeUuid(var5);
               String var16 = normalizeUuid(var6);
               ensurePlayerExists(var13, var9);
               ensurePlayerExists(var14, var10);
               ensurePlayerExists(var15, var11);
               ensurePlayerExists(var16, var12);
               DuelEntry var17 = new DuelEntry(var13, var14, var15, var16, var7);
               duelLog.add(var17);
               System.out.println("Received and added new duel entry: " + var17.toString());
               int var18;
               int var19;
               if (var2.has("winner_elo") && var2.has("loser_elo")) {
                  var18 = var2.get("winner_elo").getAsInt();
                  var19 = var2.get("loser_elo").getAsInt();
                  if (var15 != null) {
                     playerElo.put(var15, var18);
                  }
                  if (var16 != null) {
                     playerElo.put(var16, var19);
                  }
                  System.out.println("Updated ELO for " + (var11 != null ? var11 : var15) + " to " + var18 + ", for " + (var12 != null ? var12 : var16) + " to " + var19);
               } else {
                  var18 = (Integer)playerElo.getOrDefault(var17.getWinnerUuid(), 1000);
                  var19 = (Integer)playerElo.getOrDefault(var17.getLoserUuid(), 1000);
                  byte var20 = 30;
                  double var21 = 1.0D / (1.0D + Math.pow(10.0D, (double)(var19 - var18) / 400.0D));
                  double var23 = 1.0D - var21;
                  int var25 = (int)((double)var20 * (1.0D - var21));
                  int var26 = (int)((double)var20 * (0.0D - var23));
                  if (var17.getWinnerUuid() != null) {
                     playerElo.put(var17.getWinnerUuid(), var18 + var25);
                  }
                  if (var17.getLoserUuid() != null) {
                     playerElo.put(var17.getLoserUuid(), var19 + var26);
                  }
                  System.out.println("Calculated ELO change for " + (var11 != null ? var11 : var15) + ": +" + var25 + ", for " + (var12 != null ? var12 : var16) + ": " + var26);
               }
               sendResponse(var0, 200, "Duel data received and processed.");
            } catch (Throwable var28) {
               try {
                  var1.close();
               } catch (Throwable var27) {
                  var28.addSuppressed(var27);
               }
               throw var28;
            }
            var1.close();
         } catch (Exception var29) {
            System.err.println("Error processing update_duel request: " + var29.getMessage());
            var29.printStackTrace();
            sendResponse(var0, 400, "Invalid JSON or processing error: " + var29.getMessage());
         }
      } else {
         sendResponse(var0, 405, "Method Not Allowed");
      }
   }

   private static void handleUpdateUsername(HttpExchange var0) throws IOException {
      if ("POST".equals(var0.getRequestMethod())) {
         // REMOVED: API validation and centralized validation
         try {
            InputStreamReader var1 = new InputStreamReader(var0.getRequestBody(), StandardCharsets.UTF_8);
            try {
               JsonObject var2 = JsonParser.parseReader(var1).getAsJsonObject();
               String var3 = var2.has("uuid") ? var2.get("uuid").getAsString() : null;
               String var4 = var2.has("username") ? var2.get("username").getAsString() : null;
               System.out.println("Received raw UUID and username for update: UUID=" + var3 + ", Username=" + var4);
               if (var3 != null && var4 != null) {
                  String var5 = normalizeUuid(var3);
                  ensurePlayerExists(var5, var4);
                  sendResponse(var0, 200, "Username updated successfully for " + var4 + " (" + var5 + ")");
                  System.out.println("Username updated: " + var4 + " for UUID " + var5);
               } else {
                  sendResponse(var0, 400, "Missing 'uuid' or 'username' in payload.");
                  System.err.println("Failed to update username: Missing 'uuid' or 'username' in payload.");
               }
            } catch (Throwable var7) {
               try {
                  var1.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
               throw var7;
            }
            var1.close();
         } catch (Exception var8) {
            System.err.println("Error processing /update_username request: " + var8.getMessage());
            var8.printStackTrace();
            sendResponse(var0, 400, "Invalid JSON or processing error: " + var8.getMessage());
         }
      } else {
         sendResponse(var0, 405, "Method Not Allowed");
      }
   }

   private static void handleAdjustElo(HttpExchange var0) throws IOException {
      if ("POST".equals(var0.getRequestMethod())) {
         // REMOVED: API validation and centralized validation
         try {
            InputStreamReader var1 = new InputStreamReader(var0.getRequestBody(), StandardCharsets.UTF_8);
            label53: {
               try {
                  JsonObject var2 = JsonParser.parseReader(var1).getAsJsonObject();
                  String var3 = var2.has("uuid") ? var2.get("uuid").getAsString() : null;
                  int var4 = var2.has("eloChange") ? var2.get("eloChange").getAsInt() : 0;
                  String var5 = var2.has("username") ? var2.get("username").getAsString() : null;
                  if (var3 != null) {
                     String var6 = normalizeUuid(var3);
                     ensurePlayerExists(var6, var5);
                     int var7 = (Integer)playerElo.getOrDefault(var6, 1000);
                     int var8 = var7 + var4;
                     playerElo.put(var6, var8);
                     String var9 = (String)uuidToUsername.getOrDefault(var6, var6);
                     sendResponse(var0, 200, "ELO adjusted successfully for " + var9 + " (" + var6 + "). Old ELO: " + var7 + ", Change: " + var4 + ", New ELO: " + var8);
                     System.out.println("ELO adjusted for " + var9 + " (" + var6 + "). Old: " + var7 + ", Change: " + var4 + ", New: " + var8);
                     break label53;
                  }
                  sendResponse(var0, 400, "Missing 'uuid' in payload.");
                  System.err.println("Failed to adjust ELO: Missing 'uuid' in payload.");
               } catch (Throwable var11) {
                  try {
                     var1.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
                  throw var11;
               }
               var1.close();
               return;
            }
            var1.close();
         } catch (Exception var12) {
            System.err.println("Error processing /adjust_elo request: " + var12.getMessage());
            var12.printStackTrace();
            sendResponse(var0, 400, "Invalid JSON or processing error: " + var12.getMessage());
         }
      } else {
         sendResponse(var0, 405, "Method Not Allowed");
      }
   }

   private static void sendResponse(HttpExchange var0, int var1, String var2) throws IOException {
      var0.getResponseHeaders().set("Content-Type", "application/json");
      var0.sendResponseHeaders(var1, (long)var2.getBytes(StandardCharsets.UTF_8).length);
      OutputStream var3 = var0.getResponseBody();

      try {
         var3.write(var2.getBytes(StandardCharsets.UTF_8));
      } catch (Throwable var7) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (var3 != null) {
         var3.close();
      }

   }

   // REMOVED: validateWithCentralizedServer method
}
