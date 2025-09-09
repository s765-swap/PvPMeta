package com.swapnil.titlemod.util;

import com.swapnil.titlemod.security.SecureLogger;
import net.minecraft.client.MinecraftClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MinecraftClientIdentifier {
    private static String clientId = null;
    
    /**
     * @return A unique identifier string
     */
    public static String getClientId() {
        if (clientId == null) {
            try {
                
                StringBuilder idBuilder = new StringBuilder();
                
                
                idBuilder.append(System.getProperty("user.name"));
                idBuilder.append(System.getProperty("os.name"));
                idBuilder.append(System.getProperty("os.arch"));
                
                
                idBuilder.append(MinecraftClient.getInstance().getSession().getUuid());
                idBuilder.append(MinecraftClient.getInstance().getGameVersion());
                
                
                idBuilder.append(Runtime.getRuntime().availableProcessors());
                idBuilder.append(System.getProperty("java.vendor"));
                
               
                String combined = idBuilder.toString().replaceAll("\\s+", "");
                clientId = combined.substring(0, Math.min(16, combined.length()));
                
            } catch (Exception e) {
                
                clientId = "default_client_id";
                SecureLogger.logSecurityEvent("Failed to generate client ID: " + e.getMessage());
            }
        }
        
        return clientId;
    }
    
    /**
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
            return id; 
        }
    }
}