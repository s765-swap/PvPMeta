package com.swapnil.titlemod.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


public final class EnhancedPremiumValidator {
    private static final String MOJANG_PROFILE_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_SESSION_API = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final long CACHE_TTL_MS = 30 * 60 * 1000; 
    private static final String USERNAME_HISTORY_FILE = "username_history.json";
    
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();
    
    private static final Map<String, PremiumValidationResult> validationCache = new ConcurrentHashMap<>();
    private static final Map<String, String> usernameHistory = new HashMap<>();
    private static final Gson gson = new Gson();
    
    private static boolean isInitialized = false;
    
    private EnhancedPremiumValidator() {}
    
  
    public static void initialize() {
        if (isInitialized) return;
        
        loadUsernameHistory();
        isInitialized = true;
        SecureLogger.logInfo("EnhancedPremiumValidator initialized");
    }
    
    
    public static CompletableFuture<PremiumValidationResult> validatePremiumAccount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var session = MinecraftClient.getInstance().getSession();
                if (session == null) {
                    return new PremiumValidationResult(false, "No session available", ValidationErrorType.NO_SESSION);
                }
                
                String username = session.getUsername();
                String uuid = session.getUuidOrNull() != null ? session.getUuidOrNull().toString() : null;
                
                if (username == null || username.trim().isEmpty()) {
                    return new PremiumValidationResult(false, "Invalid username", ValidationErrorType.INVALID_USERNAME);
                }
                
                
                PremiumValidationResult cached = validationCache.get(username);
                if (cached != null && cached.isFresh()) {
                    return cached;
                }
                
                
                PremiumValidationResult result = performComprehensiveValidation(username, uuid);
                
               
                validationCache.put(username, result);
                
                return result;
                
            } catch (Exception e) {
                SecureLogger.logSecurityEvent("Premium validation error: " + e.getMessage());
                return new PremiumValidationResult(false, "Validation error: " + e.getMessage(), ValidationErrorType.VALIDATION_ERROR);
            }
        });
    }
    
   
    private static PremiumValidationResult performComprehensiveValidation(String username, String uuid) {
        try {
            
            if (!isValidMojangUsername(username)) {
                return new PremiumValidationResult(false, "Invalid Mojang username", ValidationErrorType.INVALID_USERNAME);
            }
            
            
            if (!isValidSession(username, uuid)) {
                return new PremiumValidationResult(false, "Invalid session", ValidationErrorType.INVALID_SESSION);
            }
            
            
            if (hasUsernameChanged(username)) {
                return new PremiumValidationResult(false, "Username change detected", ValidationErrorType.USERNAME_CHANGE_DETECTED);
            }
            
            
            if (!isPremiumAccount(username)) {
                return new PremiumValidationResult(false, "Non-premium account", ValidationErrorType.NON_PREMIUM);
            }
            
            
            updateUsernameHistory(username);
            
            return new PremiumValidationResult(true, "Premium account validated", null);
            
        } catch (Exception e) {
            SecureLogger.logSecurityEvent("Comprehensive validation error: " + e.getMessage());
            return new PremiumValidationResult(false, "Validation failed: " + e.getMessage(), ValidationErrorType.VALIDATION_ERROR);
        }
    }
    
    
    private static boolean isValidMojangUsername(String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MOJANG_PROFILE_API + username))
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject profile = JsonParser.parseString(response.body()).getAsJsonObject();
                return profile.has("id") && profile.has("name");
            }
            
            return false;
        } catch (Exception e) {
            SecureLogger.logSecurityEvent("Mojang API check failed: " + e.getMessage());
            return false;
        }
    }
    
    
    private static boolean isValidSession(String username, String uuid) {
        try {
            if (uuid == null || uuid.trim().isEmpty()) {
                return false;
            }
            
            String cleanUuid = uuid.replace("-", "");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MOJANG_SESSION_API + cleanUuid))
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject profile = JsonParser.parseString(response.body()).getAsJsonObject();
                String profileName = profile.get("name").getAsString();
                return username.equals(profileName);
            }
            
            return false;
        } catch (Exception e) {
            SecureLogger.logSecurityEvent("Session validation failed: " + e.getMessage());
            return false;
        }
    }
    
 
    private static boolean hasUsernameChanged(String username) {
        String lastUsername = usernameHistory.get(getClientFingerprint());
        if (lastUsername == null) {
            return false; // First time login
        }
        
        return !username.equals(lastUsername);
    }
    
    
    private static boolean isPremiumAccount(String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MOJANG_PROFILE_API + username))
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            SecureLogger.logSecurityEvent("Premium status check failed: " + e.getMessage());
            return false;
        }
    }
    
   
    private static void updateUsernameHistory(String username) {
        String fingerprint = getClientFingerprint();
        usernameHistory.put(fingerprint, username);
        saveUsernameHistory();
    }
    
    
    private static void loadUsernameHistory() {
        try {
            File file = new File(USERNAME_HISTORY_FILE);
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    Map<String, String> loaded = gson.fromJson(reader, Map.class);
                    if (loaded != null) {
                        usernameHistory.putAll(loaded);
                    }
                }
            }
        } catch (Exception e) {
            SecureLogger.logSecurityEvent("Failed to load username history: " + e.getMessage());
        }
    }
    
    
    private static void saveUsernameHistory() {
        try {
            try (FileWriter writer = new FileWriter(USERNAME_HISTORY_FILE)) {
                gson.toJson(usernameHistory, writer);
            }
        } catch (IOException e) {
            SecureLogger.logSecurityEvent("Failed to save username history: " + e.getMessage());
        }
    }
    
  
    private static String getClientFingerprint() {
        String clientInfo = System.getProperty("user.name", "unknown") + 
                          System.getProperty("os.name", "unknown") +
                          System.getProperty("java.version", "unknown") +
                          System.getProperty("user.home", "unknown");
        String compact = clientInfo.replaceAll("\\s+", "");
        return compact.substring(0, Math.min(16, compact.length()));
    }
    
  
    public static void clearCache() {
        validationCache.clear();
    }
    
   
    public static class PremiumValidationResult {
        private final boolean isValid;
        private final String message;
        private final ValidationErrorType errorType;
        private final long timestamp;
        
        public PremiumValidationResult(boolean isValid, String message, ValidationErrorType errorType) {
            this.isValid = isValid;
            this.message = message;
            this.errorType = errorType;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public ValidationErrorType getErrorType() {
            return errorType;
        }
        
        public boolean isFresh() {
            return System.currentTimeMillis() - timestamp < CACHE_TTL_MS;
        }
    }
    
   
    public enum ValidationErrorType {
        NO_SESSION,
        INVALID_USERNAME,
        INVALID_SESSION,
        USERNAME_CHANGE_DETECTED,
        NON_PREMIUM,
        VALIDATION_ERROR
    }
}
