package com.swapnil.titlemod.client;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class SecureConnectionManager {
    private static final String DECRYPTION_KEY = "TitleMod2024Secure";
    private static final String API_ENDPOINT = "http://" + com.swapnil.titlemod.config.ModConfig.getInstance().getKitEditorServerIp() + ":4560";
    private static final Logger LOGGER = Logger.getLogger(SecureConnectionManager.class.getName());
    
    public static String decryptServerIP(String encryptedToken) {
        try {
            // First, validate the session token
            if (!validateSessionToken(encryptedToken)) {
                throw new SecurityException("Invalid session token");
            }
            
            // Decrypt the actual IP
            byte[] key = DECRYPTION_KEY.getBytes();
            byte[] encrypted = Base64.getDecoder().decode(encryptedToken.split(":")[0]);
            byte[] result = new byte[encrypted.length];
            
            for (int i = 0; i < encrypted.length; i++) {
                result[i] = (byte) (encrypted[i] ^ key[i % key.length]);
            }
            
            return new String(result);
        } catch (Exception e) {
            LOGGER.severe("Failed to decrypt server IP: " + e.getMessage());
            return "localhost"; // Fallback
        }
    }
    
    private static boolean validateSessionToken(String token) {
        try {
            URL url = new URL(API_ENDPOINT + "/validate?token=" + token);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            LOGGER.warning("Failed to validate session token: " + e.getMessage());
            return false;
        }
    }
    
    // Obfuscate the actual IP in logs
    public static String maskIPForLogs(String ip) {
        return "***.***.***.***"; // Completely masked in logs
    }
}