package com.swapnil.titlemod.data;


public class DuelEntry {
    private final String player1Uuid;
    private final String player2Uuid;
    private final String winnerUuid;
    private final String loserUuid;
    private final long timestamp;

    public DuelEntry(String player1Uuid, String player2Uuid, String winnerUuid, String loserUuid, long timestamp) {
        this.player1Uuid = player1Uuid;
        this.player2Uuid = player2Uuid;
        this.winnerUuid = winnerUuid;
        this.loserUuid = loserUuid;
        this.timestamp = timestamp;
    }

 
    public String getPlayer1Uuid() { return player1Uuid; }
    public String getPlayer2Uuid() { return player2Uuid; }
    public String getWinnerUuid() { return winnerUuid; }
    public String getLoserUuid() { return loserUuid; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("DuelEntry{player1='%s', player2='%s', winner='%s', loser='%s', timestamp=%d}",
                player1Uuid, player2Uuid, winnerUuid, loserUuid, timestamp);
    }
}
