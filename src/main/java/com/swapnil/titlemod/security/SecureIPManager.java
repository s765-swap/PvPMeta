package com.swapnil.titlemod.security;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class SecureIPManager {
    private static final ConcurrentHashMap<String, AtomicInteger> requestCounters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long RATE_LIMIT_WINDOW = 60000; // 1 minute
    
    private static final String OBFUSCATED_MATCHMAKING_IP = "bXQtc2VydmVyLTE=";
    private static final String OBFUSCATED_KIT_EDITOR_IP = "a2l0LXNlcnZlci0x"; 
    
    
    private static final String REQUEST_SECRET = "TitleModSecureRequest2024";
    
   
    public static String getMatchmakingServerIP() {
        
        String decoded = decodeObfuscatedIP(OBFUSCATED_MATCHMAKING_IP);
        logSecureAccess("matchmaking", decoded);
        return decoded;
    }
    
    
    public static String getKitEditorServerIP() {
       
        String decoded = decodeObfuscatedIP(OBFUSCATED_KIT_EDITOR_IP);
        logSecureAccess("kiteditor", decoded);
        return decoded;
    }
    
    
    private static boolean validateRequest(String requestType) {
        String key = requestType + "_" + getClientFingerprint();
        
        
        AtomicInteger counter = requestCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        Long lastTime = lastRequestTime.get(key);
        long currentTime = System.currentTimeMillis();
        
        if (lastTime != null && currentTime - lastTime < RATE_LIMIT_WINDOW) {
            if (counter.get() >= MAX_REQUESTS_PER_MINUTE) {
                SecureLogger.logSecurityEvent("Rate limit exceeded for " + requestType);
                return false;
            }
        } else {
            counter.set(0);
        }
        
        counter.incrementAndGet();
        lastRequestTime.put(key, currentTime);
        
        
        if (AntiTamper.isDebugEnvironment()) {
            SecureLogger.logSecurityEvent("Debug environment detected during " + requestType + " request");
            return false;
        }
        
        return true;
    }
    
    
    private static String decodeObfuscatedIP(String obfuscated) {
        try {
          
            String decoded = new String(Base64.getDecoder().decode(obfuscated));
           
            switch (decoded) {
                case "mt-server-1":
                    return "34.159.92.94";
                case "kit-server-1":
                    return "34.159.92.94";
                default:
                    return "127.0.0.1"; 
            }
        } catch (Exception e) {
            SecureLogger.logSecurityEvent("Failed to decode obfuscated IP: " + e.getMessage());
            return "127.0.0.1";
        }
    }
    
    
    private static void logSecureAccess(String service, String ip) {
        String maskedIP = IPMasker.maskIP(ip);
        SecureLogger.logInfo("Secure access to " + service + " server: " + maskedIP);
    }
    
    
    private static String maskIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "***.***.***.***";
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + ".***.***." + parts[3];
        }
        
        return "***.***.***.***";
    }
    
    
    private static String getClientFingerprint() {
        String clientInfo = System.getProperty("user.name", "unknown") + 
                          System.getProperty("os.name", "unknown") +
                          System.getProperty("java.version", "unknown");
        String compact = clientInfo.replaceAll("\\s+", "");
        return compact.substring(0, Math.min(16, compact.length()));
    }
    
   
    public static String generateRequestToken(String service, long timestamp) {
        
        return service + "::" + timestamp + "::" + REQUEST_SECRET;
    }
    
  
    public static boolean validateRequestToken(String service, long timestamp, String token) {
        String expectedToken = generateRequestToken(service, timestamp);
        return expectedToken.equals(token);
    }
}
