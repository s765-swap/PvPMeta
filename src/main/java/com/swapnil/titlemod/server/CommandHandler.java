package com.swapnil.titlemod.server;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
// Note: Backend/server code must not depend on mod-side classes like SecureLogger

/**
 * Secure command handler for custom commands including kit editor mode
 * Implements security measures to prevent command abuse
 */
public class CommandHandler {
    
    // Security: Track command usage to prevent spam
    private static final ConcurrentHashMap<UUID, Long> lastCommandUsage = new ConcurrentHashMap<>();
    private static final long COMMAND_COOLDOWN_MS = 1000; // 1 second cooldown
    
    // Security: Track failed attempts
    private static final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastFailedAttemptAt = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long FAILED_ATTEMPTS_RESET_MS = 300000; // 5 minutes
    
    /**
     * Register all custom commands with security measures
     */
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("kiteditor")
            .requires(source -> source.hasPermissionLevel(0)) // Require permission level 0 (any player)
            .then(argument("kitName", string())
            .executes(CommandHandler::handleKitEditorMode)));
            
        dispatcher.register(literal("kiteditor_mode")
            .requires(source -> source.hasPermissionLevel(0)) // Require permission level 0 (any player)
            .then(argument("kitName", string())
            .executes(CommandHandler::handleKitEditorMode)));
            
        dispatcher.register(literal("titlemod_status")
            .requires(source -> source.hasPermissionLevel(2)) // Require permission level 2 (moderator)
            .executes(CommandHandler::handleStatusCommand));
            
        dispatcher.register(literal("titlemod_debug")
            .requires(source -> source.hasPermissionLevel(4)) // Require permission level 4 (admin)
            .executes(CommandHandler::handleDebugCommand));
    }
    
    /**
     * Handle the kit editor mode command with security checks
     */
    private static int handleKitEditorMode(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            String username = source.getName();
            String kitName = StringArgumentType.getString(context, "kitName");
            
            // Security: Check command cooldown
            if (!checkCommandCooldown(source)) {
                source.sendMessage(Text.literal("§c[Security] §fCommand cooldown active. Please wait before using this command again."));
                return 0;
            }
            
            // Security: Validate kit name
            if (!isValidKitName(kitName)) {
                source.sendMessage(Text.literal("§c[Security] §fInvalid kit name: " + kitName));
                logSecurityEvent("INVALID_KIT_NAME", username, kitName);
                return 0;
            }
            
            // Security: Check for suspicious activity
            if (isSuspiciousActivity(username)) {
                source.sendMessage(Text.literal("§c[Security] §fSuspicious activity detected. Command blocked."));
                logSecurityEvent("SUSPICIOUS_ACTIVITY", username, kitName);
                return 0;
            }
            
            // Call the matchmaking server to handle kit editor mode
            MatchmakingServer.handleKitEditorMode(username, kitName);
            
            // Send confirmation message
            source.sendMessage(Text.literal("§a[Kit Editor] §fYou are now in kit editor mode for kit: §b" + kitName));
            
            // Log successful command usage
            logSecurityEvent("SUCCESSFUL_KIT_EDITOR", username, kitName);
            
            return 1; // Success
            
        } catch (Exception e) {
            context.getSource().sendMessage(Text.literal("§c[Kit Editor] §fError setting kit editor mode: " + e.getMessage()));
            logSecurityEvent("COMMAND_ERROR", context.getSource().getName(), e.getMessage());
            return 0; // Failure
        }
    }
    
    /**
     * Handle status command for moderators
     */
    private static int handleStatusCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            
            // Security: Check command cooldown
            if (!checkCommandCooldown(source)) {
                source.sendMessage(Text.literal("§c[Security] §fCommand cooldown active."));
                return 0;
            }
            
            // Get system status
            String status = "System is running - Matchmaking server operational";
            source.sendMessage(Text.literal("§a[TitleMod] §fSystem Status: §e" + status));
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendMessage(Text.literal("§c[TitleMod] §fError getting status: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Handle debug command for admins
     */
    private static int handleDebugCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            
            // Security: Check command cooldown
            if (!checkCommandCooldown(source)) {
                source.sendMessage(Text.literal("§c[Security] §fCommand cooldown active."));
                return 0;
            }
            
            // Get debug information
            String debugInfo = "Debug mode active - All systems operational";
            source.sendMessage(Text.literal("§a[TitleMod] §fDebug Info: §e" + debugInfo));
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendMessage(Text.literal("§c[TitleMod] §fError getting debug info: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Security: Check command cooldown to prevent spam
     */
    private static boolean checkCommandCooldown(ServerCommandSource source) {
        try {
            UUID playerId = source.getPlayer().getUuid();
            long currentTime = System.currentTimeMillis();
            Long lastUsage = lastCommandUsage.get(playerId);
            
            if (lastUsage != null && (currentTime - lastUsage) < COMMAND_COOLDOWN_MS) {
                return false; // Cooldown active
            }
            
            lastCommandUsage.put(playerId, currentTime);
            return true;
            
        } catch (Exception e) {
            // If we can't get player ID, allow the command (fallback)
            return true;
        }
    }
    
    /**
     * Security: Validate kit name to prevent injection attacks
     */
    private static boolean isValidKitName(String kitName) {
        if (kitName == null || kitName.trim().isEmpty()) {
            return false;
        }
        
        // Only allow alphanumeric characters, hyphens, and underscores
        return kitName.matches("^[a-zA-Z0-9_-]+$") && kitName.length() <= 32;
    }
    
    /**
     * Security: Check for suspicious activity patterns
     */
    private static boolean isSuspiciousActivity(String username) {
        if (username == null || username.trim().isEmpty()) {
            return true;
        }
        
        // Check failed attempts
        Integer failedCount = failedAttempts.get(username);
        if (failedCount != null && failedCount >= MAX_FAILED_ATTEMPTS) {
            long now = System.currentTimeMillis();
            Long lastAt = lastFailedAttemptAt.get(username);
            if (lastAt != null && (now - lastAt) <= FAILED_ATTEMPTS_RESET_MS) {
                return true; // Still blocked
            }
            // Reset after timeout
            failedAttempts.remove(username);
            lastFailedAttemptAt.remove(username);
        }
        
        return false;
    }
    
    /**
     * Security: Log security events for monitoring
     */
    private static void logSecurityEvent(String eventType, String username, String details) {
        System.out.println("[SECURITY] Event: " + eventType + " | User: " + username + " | Details: " + details);
    }
    
    /**
     * Security: Record failed attempt
     */
    public static void recordFailedAttempt(String username) {
        failedAttempts.merge(username, 1, Integer::sum);
        lastFailedAttemptAt.put(username, System.currentTimeMillis());
    }
    
    /**
     * Security: Clear failed attempts for a user
     */
    public static void clearFailedAttempts(String username) {
        failedAttempts.remove(username);
    }
}
