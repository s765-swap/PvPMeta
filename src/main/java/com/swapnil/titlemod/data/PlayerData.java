package com.swapnil.titlemod.data;

import java.util.HashMap;
import java.util.Map;


public class PlayerData {
    private String uuid;
    private String username;
    private int elo;
    private long lastSeen;
    private long sessionStartTime;
    private long totalPlaytime;
    private Map<String, Long> lastPlayedTime;
    private Map<String, Long> modePlaytime;

    public PlayerData(String uuid, String username, int elo, long lastSeen) {
        this.uuid = uuid;
        this.username = username;
        this.elo = elo;
        this.lastSeen = lastSeen;
        this.sessionStartTime = System.currentTimeMillis();
        this.totalPlaytime = 0;
        this.lastPlayedTime = new HashMap<>();
        this.modePlaytime = new HashMap<>();
    }

   
    public String getUuid() { return uuid; }
    public String getUsername() { return username; }
    public int getElo() { return elo; }
    public long getLastSeen() { return lastSeen; }
    public long getSessionStartTime() { return sessionStartTime; }
    public long getTotalPlaytime() { return totalPlaytime; }
    
    
    public String getPlayerName() { return username; }
    
    
    public void setElo(int elo) { this.elo = elo; }
    public void setUsername(String username) { this.username = username; }
    
    
    public void startSession() {
        this.sessionStartTime = System.currentTimeMillis();
    }
    
    public void endSession() {
        long sessionTime = System.currentTimeMillis() - sessionStartTime;
        totalPlaytime += sessionTime;
    }
    
    public void addSessionPlaytime(long playtime) {
        totalPlaytime += playtime;
    }
    
    
    public void updateLastPlayedTime(String mode) {
        lastPlayedTime.put(mode, System.currentTimeMillis());
    }
    
    public long getTimeSinceLastPlayed(String mode) {
        Long lastPlayed = lastPlayedTime.get(mode);
        if (lastPlayed == null) return -1;
        return System.currentTimeMillis() - lastPlayed;
    }
    
    public void addModePlaytime(String mode, long playtime) {
        modePlaytime.put(mode, modePlaytime.getOrDefault(mode, 0L) + playtime);
    }
    
    public long getModePlaytime(String mode) {
        return modePlaytime.getOrDefault(mode, 0L);
    }
    
    
    public String getFormattedPlaytime() {
        long hours = totalPlaytime / (1000 * 60 * 60);
        long minutes = (totalPlaytime % (1000 * 60 * 60)) / (1000 * 60);
        return String.format("%dh %dm", hours, minutes);
    }

    @Override
    public String toString() {
        return String.format("PlayerData{uuid='%s', username='%s', elo=%d, lastSeen=%d, totalPlaytime=%d}",
                uuid, username, elo, lastSeen, totalPlaytime);
    }
}
