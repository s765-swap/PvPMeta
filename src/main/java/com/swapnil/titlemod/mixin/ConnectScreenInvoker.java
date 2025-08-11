package com.swapnil.titlemod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin interface to expose the private `connect` method of `ConnectScreen`.
 * This allows `MatchmakingClient` to programmatically connect to a Minecraft server.
 */
@Mixin(ConnectScreen.class)
public interface ConnectScreenInvoker {
    /**
     * Invokes the private static `connect` method of `ConnectScreen`.
     * @param parent The parent screen to return to after connection. Can be null.
     * @param client The MinecraftClient instance.
     * @param address The ServerAddress to connect to.
     * @param info The ServerInfo for the server (can be null if not a saved server).
     * @param quickPlay If the connection is for quick play.
     */
    @Invoker("connect")
    static void invokeConnect(Screen parent, MinecraftClient client, ServerAddress address, ServerInfo info, boolean quickPlay) {
        throw new AssertionError(); // Mixin will replace this
    }
}
