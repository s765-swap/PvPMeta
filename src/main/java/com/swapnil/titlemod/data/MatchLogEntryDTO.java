package com.swapnil.titlemod.data;


public class MatchLogEntryDTO {
    private final String player1Username;
    private final String player2Username;
    private final String winnerUsername;
    private final String loserUsername;
    private final long timestamp;

    public MatchLogEntryDTO(String player1Username, String player2Username, 
                           String winnerUsername, String loserUsername, long timestamp) {
        this.player1Username = player1Username;
        this.player2Username = player2Username;
        this.winnerUsername = winnerUsername;
        this.loserUsername = loserUsername;
        this.timestamp = timestamp;
    }

    
    public String getPlayer1Username() { return player1Username; }
    public String getPlayer2Username() { return player2Username; }
    public String getWinnerUsername() { return winnerUsername; }
    public String getLoserUsername() { return loserUsername; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("MatchLogEntryDTO{player1='%s', player2='%s', winner='%s', loser='%s', timestamp=%d}",
                player1Username, player2Username, winnerUsername, loserUsername, timestamp);
    }
}
