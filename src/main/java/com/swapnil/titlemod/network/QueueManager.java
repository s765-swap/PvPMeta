package com.swapnil.titlemod.network;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class QueueManager {
    private static final Map<String, Queue<ServerPlayerEntity>> modeQueues = new HashMap<>();

    public static void queuePlayer(ServerPlayerEntity player, String mode) {
        modeQueues.putIfAbsent(mode, new LinkedList<>());
        Queue<ServerPlayerEntity> queue = modeQueues.get(mode);

        queue.add(player);
        player.sendMessage(Text.of("Queued for " + mode + "..."), false);

        if (queue.size() >= 2) {
            ServerPlayerEntity player1 = queue.poll();
            ServerPlayerEntity player2 = queue.poll();

            ArenaManager.startMatch(player1, player2, mode);
        }
    }
}