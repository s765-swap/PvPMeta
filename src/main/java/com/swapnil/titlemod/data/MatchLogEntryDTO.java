package com.swapnil.titlemod.data;

// This class is a Data Transfer Object (DTO) for duel log entries
// It's designed to match the JSON structure sent by the backend for /real_time_duels.json
// which now includes usernames instead of UUIDs for display.
public class MatchLogEntryDTO {
    private String player1Username;
    private String player2Username;
    private String winnerUsername;
    private String loserUsername;
    private long timestamp;

    // Default constructor for Gson
    public MatchLogEntryDTO() {}

    // Constructor for creating instances (e.g., for dummy data or testing)
    public MatchLogEntryDTO(String player1Username, String player2Username, String winnerUsername, String loserUsername, long timestamp) {
        this.player1Username = player1Username;
        this.player2Username = player2Username;
        this.winnerUsername = winnerUsername;
        this.loserUsername = loserUsername;
        this.timestamp = timestamp;
    }

    // Getters for all fields
    public String getPlayer1Username() {
        return player1Username;
    }

    public String getPlayer2Username() {
        return player2Username;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    public String getLoserUsername() {
        return loserUsername;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Optional: toString for easy debugging
    @Override
    public String toString() {
        return "MatchLogEntryDTO{" +
                "player1Username='" + player1Username + '\'' +
                ", player2Username='" + player2Username + '\'' +
                ", winnerUsername='" + winnerUsername + '\'' +
                ", loserUsername='" + loserUsername + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
