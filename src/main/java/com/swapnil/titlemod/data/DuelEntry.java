package com.swapnil.titlemod.data;

// This class defines the structure for a single duel entry
public class DuelEntry {
    private String player1Uuid;
    private String player2Uuid;
    private String winnerUuid;
    private String loserUuid;
    private long timestamp; // Unix timestamp in milliseconds

    // Constructor to initialize a DuelEntry object
    public DuelEntry(String player1Uuid, String player2Uuid, String winnerUuid, String loserUuid, long timestamp) {
        this.player1Uuid = player1Uuid;
        this.player2Uuid = player2Uuid;
        this.winnerUuid = winnerUuid;
        this.loserUuid = loserUuid;
        this.timestamp = timestamp;
    }

    // Getter methods for each field
    public String getPlayer1Uuid() {
        return player1Uuid;
    }

    public String getPlayer2Uuid() {
        return player2Uuid;
    }

    public String getWinnerUuid() {
        return winnerUuid;
    }

    public String getLoserUuid() {
        return loserUuid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Optional: toString method for easier debugging
    @Override
    public String toString() {
        return "DuelEntry{" +
                "player1Uuid='" + player1Uuid + '\'' +
                ", player2Uuid='" + player2Uuid + '\'' +
                ", winnerUuid='" + winnerUuid + '\'' +
                ", loserUuid='" + loserUuid + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
