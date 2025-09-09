package com.swapnil.titlemod.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class PlayerDataManager {
    private static final Map<String, PlayerData> playerCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
   
    private static PlayerDataManager instance;
    
    
    private PlayerData currentPlayerData;
    
    private PlayerDataManager() {
        
    }
    
    public static PlayerDataManager getInstance() {
        if (instance == null) {
            instance = new PlayerDataManager();
        }
        return instance;
    }
    
    static {
        
        scheduler.scheduleAtFixedRate(PlayerDataManager::cleanupCache, 5, 5, TimeUnit.MINUTES);
        
       
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    
    public PlayerData getPlayerData(String uuid, String username) {
        return playerCache.computeIfAbsent(uuid, k -> new PlayerData(uuid, username, 1000, System.currentTimeMillis()));
    }

    
    public void updatePlayerElo(String uuid, int newElo) {
        PlayerData existing = playerCache.get(uuid);
        if (existing != null) {
            PlayerData updated = new PlayerData(uuid, existing.getUsername(), newElo, System.currentTimeMillis());
            playerCache.put(uuid, updated);
        }
    }

   
    public PlayerData getCachedPlayerData(String uuid) {
        return playerCache.get(uuid);
    }

    
    public PlayerData getCurrentPlayerData() {
        if (currentPlayerData == null) {
            
            currentPlayerData = new PlayerData("current", "Player", 1000, System.currentTimeMillis());
        }
        return currentPlayerData;
    }

    
    public void startSession() {
        if (currentPlayerData != null) {
            currentPlayerData.startSession();
        }
    }

    
    public void endSession() {
        if (currentPlayerData != null) {
            currentPlayerData.endSession();
        }
    }

    
    public void updateLastPlayedTime(String mode) {
        if (currentPlayerData != null) {
            currentPlayerData.updateLastPlayedTime(mode);
        }
    }

   
    public void clearCache() {
        playerCache.clear();
    }

    
    private static void cleanupCache() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        playerCache.entrySet().removeIf(entry -> entry.getValue().getLastSeen() < cutoffTime);
    }

    
    public int getCacheSize() {
        return playerCache.size();
    }
}
