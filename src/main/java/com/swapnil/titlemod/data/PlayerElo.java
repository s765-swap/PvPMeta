package com.swapnil.titlemod.data;

// This class is a simple holder for a player's UUID and their ELO.
// It's designed to be used when parsing the 'elo.json' which is a map of UUIDs to ELOs.
// Gson will handle the map directly, but this class is useful for individual entries or lists.
public class PlayerElo {
    private String uuid;
    private int elo;

    // Default constructor for Gson
    public PlayerElo() {}

    public PlayerElo(String uuid, int elo) {
        this.uuid = uuid;
        this.elo = elo;
    }

    public String getUuid() {
        return uuid;
    }

    public int getElo() {
        return elo;
    }

    // Optional: toString for easy debugging
    @Override
    public String toString() {
        return "PlayerElo{" +
                "uuid='" + uuid + '\'' +
                ", elo=" + elo +
                '}';
    }
}
