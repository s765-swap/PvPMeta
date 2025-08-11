package com.swapnil.titlemod.network;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ArenaManager {
    public static void startMatch(ServerPlayerEntity p1, ServerPlayerEntity p2, String mode) {
        // Match coordinates (must match MatchmakingServer.java)
        int x1 = 100, y = 70, z1 = 100;
        int x2 = 110, z2 = 100;

        p1.teleport(p1.getServerWorld(), x1, y, z1, p1.getYaw(), p1.getPitch());
        p2.teleport(p2.getServerWorld(), x2, y, z2, p2.getYaw(), p2.getPitch());

        p1.sendMessage(Text.of("Match found! Fighting " + p2.getName().getString() + " in " + mode), false);
        p2.sendMessage(Text.of("Match found! Fighting " + p1.getName().getString() + " in " + mode), false);
    }
}