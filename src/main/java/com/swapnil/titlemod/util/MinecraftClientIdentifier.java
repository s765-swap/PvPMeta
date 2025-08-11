package com.swapnil.titlemod.util;

import net.minecraft.client.MinecraftClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class to generate a unique identifier for the Minecraft client.
 * This helps make encryption more secure by binding it to the specific client installation.
 */
public class MinecraftClientIdentifier {
    private static String clientId = null;
    
    /**
     * Gets a unique identifier for this Minecraft client installation.
     * The ID is based on a combination of system properties and Minecraft installation details.
     * 
     * @return A unique identifier string
     */
    public static String getClientId() {
        if (clientId == null) {
            try {
                // Combine various system properties to create a unique fingerprint
                StringBuilder idBuilder = new StringBuilder();
                
                // Add system properties that are unlikely to change between sessions
                idBuilder.append(System.getProperty("user.name"));
                idBuilder.append(System.getProperty("os.name"));
                idBuilder.append(System.getProperty("os.arch"));
                
                // Add Minecraft-specific information
                idBuilder.append(MinecraftClient.getInstance().getSession().getUuid());
                idBuilder.append(MinecraftClient.getInstance().getGameVersion());
                
                // Add some hardware identifiers that are stable
                idBuilder.append(Runtime.getRuntime().availableProcessors());
                idBuilder.append(System.getProperty("java.vendor"));
                
                // Hash the combined string to create a fixed-length identifier
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(idBuilder.toString().getBytes(StandardCharsets.UTF_8));
                
                // Convert to hex string
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                
                // Use the first 16 characters as the client ID
                clientId = hexString.toString().substring(0, 16);
                
            } catch (Exception e) {
                // Fallback to a simple identifier if anything goes wrong
                clientId = "default_client_id";
                System.err.println("[TitleMod] Failed to generate client ID: " + e.getMessage());
            }
        }
        
        return clientId;
    }
    
    /**
     * Generates an obfuscated version of the client ID for additional security.
     * 
     * @return An obfuscated client identifier
     */
    public static String getObfuscatedClientId() {
        String id = getClientId();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest((id + "TitleModSalt").getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return id; // Fallback to regular client ID
        }
    }
}