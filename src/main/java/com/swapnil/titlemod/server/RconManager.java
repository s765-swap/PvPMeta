package com.swapnil.titlemod.server;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RconManager {
   private static final String SERVER_IP = "34.159.92.94";
   private static final int SERVER_PORT = 25575;
   private static final String PASSWORD = "98750";
   private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
   private static final Queue<RconManager.RconCommand> commandQueue = new ConcurrentLinkedQueue();
   private static final Map<String, Long> lastCommandTime = new ConcurrentHashMap();
   private static final long COMMAND_COOLDOWN_MS = 1000L;
   private static final AtomicInteger batchSize = new AtomicInteger(0);
   private static final int MAX_BATCH_SIZE = 10;

   public static void addToWhitelist(String var0) {
      queueCommand(new RconManager.RconCommand("whitelist add " + var0, RconManager.CommandType.WHITELIST_ADD, var0));
   }

   public static void removeFromWhitelist(String var0) {
      queueCommand(new RconManager.RconCommand("whitelist remove " + var0, RconManager.CommandType.WHITELIST_REMOVE, var0));
   }

   public static void kickPlayer(String var0, String var1) {
      queueCommand(new RconManager.RconCommand("kick " + var0 + " " + var1, RconManager.CommandType.KICK, var0));
   }

   public static void checkPlayerOnline(String var0, RconManager.Consumer<Boolean> var1) {
      String var2 = "online_check_" + var0;
      long var3 = (Long)lastCommandTime.getOrDefault(var2, 0L);
      if (System.currentTimeMillis() - var3 > 30000L) {
         lastCommandTime.put(var2, System.currentTimeMillis());
         queueCommand(new RconManager.RconCommand("list", RconManager.CommandType.LIST_CHECK, var0, var1));
      }

   }

   private static void queueCommand(RconManager.RconCommand var0) {
      commandQueue.offer(var0);
      if (batchSize.incrementAndGet() >= 10) {
         scheduler.schedule(() -> {
            processCommandQueue();
         }, 100L, TimeUnit.MILLISECONDS);
      }

   }

   private static void processCommandQueue() {
      if (!commandQueue.isEmpty()) {
         try {
            RconClient var0 = new RconClient("34.159.92.94", 25575, "98750");

            label57: {
               try {
                  var0.connect();
                  if (var0.authenticate()) {
                     int var1 = 0;

                     while(!commandQueue.isEmpty() && var1 < 10) {
                        RconManager.RconCommand var2 = (RconManager.RconCommand)commandQueue.poll();
                        if (var2 != null) {
                           try {
                              String var3 = var0.sendCommand(var2.command);
                              var2.onComplete(var3);
                              ++var1;
                              Thread.sleep(1000L);
                           } catch (Exception var5) {
                              String var10001 = var2.command;
                              System.err.println("[RconManager] Command failed: " + var10001 + " - " + var5.getMessage());
                              var2.onError(var5);
                           }
                        }
                     }

                     batchSize.addAndGet(-var1);
                     System.out.println("[RconManager] Processed " + var1 + " commands in batch");
                     break label57;
                  }

                  System.err.println("[RconManager] Failed to authenticate");
               } catch (Throwable var6) {
                  try {
                     var0.close();
                  } catch (Throwable var4) {
                     var6.addSuppressed(var4);
                  }

                  throw var6;
               }

               var0.close();
               return;
            }

            var0.close();
         } catch (Exception var7) {
            System.err.println("[RconManager] Failed to process command queue: " + var7.getMessage());
         }

      }
   }

   public static void shutdown() {
      scheduler.shutdown();

      try {
         if (!scheduler.awaitTermination(5L, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
         }
      } catch (InterruptedException var1) {
         scheduler.shutdownNow();
      }

   }

   static {
      scheduler.scheduleAtFixedRate(() -> {
         processCommandQueue();
      }, 2L, 2L, TimeUnit.SECONDS);
   }

   private static class RconCommand {
      final String command;
      final RconManager.CommandType type;
      final String username;
      final RconManager.Consumer<Boolean> callback;

      RconCommand(String var1, RconManager.CommandType var2, String var3) {
         this(var1, var2, var3, (RconManager.Consumer)null);
      }

      RconCommand(String var1, RconManager.CommandType var2, String var3, RconManager.Consumer<Boolean> var4) {
         this.command = var1;
         this.type = var2;
         this.username = var3;
         this.callback = var4;
      }

      void onComplete(String var1) {
         switch(this.type) {
         case WHITELIST_ADD:
            System.out.println("[RconManager] Added " + this.username + " to whitelist");
            break;
         case WHITELIST_REMOVE:
            System.out.println("[RconManager] Removed " + this.username + " from whitelist");
            break;
         case KICK:
            System.out.println("[RconManager] Kicked " + this.username);
            break;
         case LIST_CHECK:
            if (this.callback != null) {
               boolean var2 = var1 != null && var1.contains(this.username);
               this.callback.accept(var2);
            }
         }

      }

      void onError(Exception var1) {
         String var10001 = this.username;
         System.err.println("[RconManager] Command error for " + var10001 + ": " + var1.getMessage());
      }
   }

   private static enum CommandType {
      WHITELIST_ADD,
      WHITELIST_REMOVE,
      KICK,
      LIST_CHECK;

      // $FF: synthetic method
      private static RconManager.CommandType[] $values() {
         return new RconManager.CommandType[]{WHITELIST_ADD, WHITELIST_REMOVE, KICK, LIST_CHECK};
      }
   }

   @FunctionalInterface
   public interface Consumer<T> {
      void accept(T var1);
   }
}
