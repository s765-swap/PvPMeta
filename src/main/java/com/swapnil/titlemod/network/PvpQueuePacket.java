package com.swapnil.titlemod.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class PvpQueuePacket {
    public static final Identifier ID = new Identifier("titlemod", "pvp_queue");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            String mode = buf.readString();
            server.execute(() -> {
                QueueManager.queuePlayer(player, mode);
            });
        });
    }

    public static PacketByteBuf createBuf(String mode) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(mode);
        return buf;
    }
}