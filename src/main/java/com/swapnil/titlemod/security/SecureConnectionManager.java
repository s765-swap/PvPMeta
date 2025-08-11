package com.swapnil.titlemod.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class SecureConnectionManager {
    private static final String DECRYPTION_KEY = "TitleModSecureKey2024!@#$";
    private static final String API_ENDPOINT = "http://34.159.92.94:4560";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
    public static String decryptServerIP(String encryptedIP, String sessionToken) {
        try {
            if (!validateSessionToken(sessionToken)) {
                return AntiTamper.getDecoyIP(sessionToken.hashCode());
            }
            
            return decryptIP(encryptedIP);
        } catch (Exception e) {
            return AntiTamper.getDecoyIP(0);
        }
    }
    
    private static boolean validateSessionToken(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_ENDPOINT + "/validate?token=" + token))
                .GET()
                .header("User-Agent", "TitleMod/1.0")
                .timeout(java.time.Duration.ofSeconds(5))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                return json.has("valid") && json.get("valid").getAsBoolean();
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static String decryptIP(String encryptedIP) {
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(encryptedIP);
            byte[] key = DECRYPTION_KEY.getBytes();
            byte[] decrypted = new byte[decoded.length];
            
            for (int i = 0; i < decoded.length; i++) {
                decrypted[i] = (byte) (decoded[i] ^ key[i % key.length]);
            }
            
            return new String(decrypted);
        } catch (Exception e) {
            return com.swapnil.titlemod.config.ModConfig.getInstance().getKitEditorServerIp();
        }
    }
    
    public static String maskIPForLogs(String ip) {
        if (ip == null || !ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return "***.***.***.***";
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return "***.***.***.***";
        
        return parts[0] + ".***.***." + parts[3];
    }
}