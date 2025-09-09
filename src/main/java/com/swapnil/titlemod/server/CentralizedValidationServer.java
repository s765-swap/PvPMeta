/*
 * Decompiled with CFR 0.153-SNAPSHOT (d6f6758-dirty).
 */
package com.swapnil.titlemod.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
// Removed SHA-256 usage
import java.security.SecureRandom;
import java.util.Base64;

public class CentralizedValidationServer {
    private static final Logger LOGGER = Logger.getLogger(CentralizedValidationServer.class.getName());
    private static final int SERVER_PORT = 5008;
    private static final String CLIENT_SIGNATURE = "TitleMod-Client-v1.0.0";
    private static final String API_KEYS_FILE = "centralized_api_keys.json";
    private static final String VALIDATION_LOGS_FILE = "validation_logs.json";
    private static final long TIMESTAMP_WINDOW_MS = 300000L;
    private static final long WHITELIST_TIMEOUT_MS = Long.MAX_VALUE / 4; // Persist until explicit end-session
    private static final String EMBEDDED_SECRET_1 = "TitleModSecureKey2024!@#$%^&*()";
    private static final String EMBEDDED_SECRET_2 = "SwapnilTitleModSecretKey";
    private static final String EMBEDDED_SECRET_3 = "MinecraftFabricModSecurity";
    private static final Map<String, APIKeyData> validAPIKeys = new ConcurrentHashMap<String, APIKeyData>();
    private static final Map<String, WhitelistEntry> whitelistedUsers = new ConcurrentHashMap<String, WhitelistEntry>();
    private static final List<ValidationLogEntry> validationLogs = Collections.synchronizedList(new ArrayList());
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final int KEY_POOL_SIZE = 2000;
    private static final int KEY_LENGTH = 20;
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9]{20}");
    private static final Map<String, String> ipToApiKey = new ConcurrentHashMap<String, String>();
    private static final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, RateLimitInfo> rateLimits = new ConcurrentHashMap<>();
    private static final long SESSION_EXPIRY_MS = 60 * 60 * 1000; // 1 hour
    private static final int RATE_LIMIT = 60; // requests per minute
    private static final long RATE_LIMIT_WINDOW_MS = 60 * 1000;
    private static final SecureRandom secureRandom = new SecureRandom();

    public static void main(String[] stringArray) {
        System.out.println("[CentralizedValidationServer] Starting centralized validation server on port 5008");
        try {
            ServerSocket serverSocket = new ServerSocket(5008);
            try {
                System.out.println("[CentralizedValidationServer] Server started successfully");
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> CentralizedValidationServer.handleClientConnection(socket)).start();
                }
            } catch (Throwable throwable) {
                try {
                    serverSocket.close();
                } catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
        } catch (IOException iOException) {
            System.err.println("[CentralizedValidationServer] Server error: " + iOException.getMessage());
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static void handleClientConnection(Socket socket) {
        String string = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "unknown";
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);){
            String string2 = bufferedReader.readLine();
            if (string2 == null) {
                printWriter.println("ERROR: No input received");
                return;
            }
            String[] stringArray = string2.split("::");
            if (string2.startsWith("REQUEST_API_KEY::")) {
                String payload = string2.substring("REQUEST_API_KEY::".length());
                String[] reqParts = payload.split("::");
                // Expected: publicIP, clientSignature, clientFingerprint, modHash, embeddedSecretHash, modVersion, timestamp
                if (reqParts.length < 7) {
                    printWriter.println("ERROR: Invalid API key request format");
                    return;
                }
                String publicIP = reqParts[0];
                String clientSig = reqParts[1];
                if (!CLIENT_SIGNATURE.equals(clientSig)) {
                    printWriter.println("ERROR: Client signature mismatch");
                    return;
                }
                // End any prior mapping for this IP to enforce fresh session keys
                ipToApiKey.entrySet().removeIf(entry -> entry.getKey().equals(publicIP));
                // Always mint a new key for a new session
                CentralizedValidationServer.ensureKeyPool();
                Optional<APIKeyData> available = validAPIKeys.values().stream().filter(a -> a.isActive && !ipToApiKey.containsValue(a.apiKey)).findFirst();
                if (available.isPresent()) {
                    APIKeyData keyData = available.get();
                    ipToApiKey.put(publicIP, keyData.apiKey);
                    saveAPIKeys();
                    // If installationId is not yet set, generate a simple session-scoped id
                    if (keyData.installationId == null) {
                        keyData.installationId = "inst_" + System.currentTimeMillis();
                    }
                    printWriter.println("SUCCESS:" + keyData.apiKey + ":" + keyData.installationId);
                    return;
                }
                printWriter.println("ERROR: No available API keys. Try again.");
                return;
            }
            if (stringArray.length < 4) {
                printWriter.println("ERROR: Invalid request format");
                return;
            }
            String string5 = stringArray[0];
            String string6 = stringArray[1];
            String string7 = stringArray[2];
            String string8 = stringArray[3];
            if (!CentralizedValidationServer.isValidAPIKey(string7)) {
                CentralizedValidationServer.logValidationEvent("Invalid API key", string, string7, string6, false);
                printWriter.println("ERROR: Invalid API key");
                return;
            }
            switch (string5) {
                case "VALIDATE_MOD": {
                    CentralizedValidationServer.handleModValidation(printWriter, string6, string7, string8, string);
                    return;
                }
                case "REQUEST_WHITELIST": {
                    CentralizedValidationServer.handleWhitelistRequest(printWriter, string6, string7, string8, string);
                    return;
                }
                case "VERIFY_ACCESS": {
                    CentralizedValidationServer.handleAccessVerification(printWriter, string6, string7, string8, string);
                    return;
                }
                case "VALIDATE_API_KEY": {
                    CentralizedValidationServer.handleApiKeyValidation(printWriter, string6, string7, string8, string);
                    return;
                }
                case "END_SESSION": {
                    CentralizedValidationServer.handleEndSession(printWriter, string6, string7, string8, string);
                    return;
                }
                // Remove all handleLogin and handleLogout logic. Only premium/cracked check remains.
            }
            printWriter.println("ERROR: Unknown request type: " + string5);
            return;
        } catch (IOException iOException) {
            LOGGER.warning("Error handling client connection: " + iOException.getMessage());
            return;
        } finally {
            try {
                socket.close();
            } catch (IOException iOException) {}
        }
    }

    private static void handleModValidation(PrintWriter printWriter, String string, String string2, String string3, String string4) {
        try {
            if (!CentralizedValidationServer.validateModInstallation(string2, string3)) {
                CentralizedValidationServer.logValidationEvent("Invalid mod installation", string4, string2, string, false);
                printWriter.println("ERROR: Invalid mod installation. Please use the updated mod.");
                return;
            }
            APIKeyData aPIKeyData = validAPIKeys.get(string2);
            if (aPIKeyData == null || !aPIKeyData.isActive) {
                CentralizedValidationServer.logValidationEvent("Inactive API key", string4, string2, string, false);
                printWriter.println("ERROR: API key is inactive. Please contact staff.");
                return;
            }
            CentralizedValidationServer.updateAPIKeyUsage(string2, string3);
            CentralizedValidationServer.logValidationEvent("Mod validation successful", string4, string2, string, true);
            printWriter.println("SUCCESS: Mod validation successful");
        } catch (Exception exception) {
            LOGGER.severe("Error in mod validation: " + exception.getMessage());
            printWriter.println("ERROR: Validation error occurred");
        }
    }

    private static void handleWhitelistRequest(PrintWriter printWriter, String string, String string2, String string3, String string4) {
        try {
            WhitelistEntry whitelistEntry;
            if (!CentralizedValidationServer.validateModInstallation(string2, string3)) {
                CentralizedValidationServer.logValidationEvent("Invalid mod installation for whitelist", string4, string2, string, false);
                printWriter.println("ERROR: Invalid mod installation. Please use the updated mod.");
                return;
            }
            if (whitelistedUsers.containsKey(string)) {
                whitelistEntry = whitelistedUsers.get(string);
                if (System.currentTimeMillis() - whitelistEntry.timestamp < WHITELIST_TIMEOUT_MS) {
                    whitelistEntry.timestamp = System.currentTimeMillis();
                    printWriter.println("SUCCESS: Whitelist extended");
                    return;
                }
            }
            whitelistEntry = new WhitelistEntry();
            whitelistEntry.username = string;
            whitelistEntry.apiKey = string2;
            whitelistEntry.installationId = string3;
            whitelistEntry.timestamp = System.currentTimeMillis();
            whitelistEntry.clientIP = string4;
            whitelistedUsers.put(string, whitelistEntry);
            CentralizedValidationServer.logValidationEvent("User whitelisted", string4, string2, string, true);
            printWriter.println("SUCCESS: User whitelisted successfully");
        } catch (Exception exception) {
            LOGGER.severe("Error in whitelist request: " + exception.getMessage());
            printWriter.println("ERROR: Whitelist error occurred");
        }
    }

    private static void handleAccessVerification(PrintWriter printWriter, String string, String string2, String string3, String string4) {
        try {
            if (!whitelistedUsers.containsKey(string)) {
                CentralizedValidationServer.logValidationEvent("Access denied - not whitelisted", string4, string2, string, false);
                printWriter.println("ERROR: Access denied. User not whitelisted.");
                return;
            }
            WhitelistEntry whitelistEntry = whitelistedUsers.get(string);
            if (System.currentTimeMillis() - whitelistEntry.timestamp > WHITELIST_TIMEOUT_MS) {
                whitelistedUsers.remove(string);
                CentralizedValidationServer.logValidationEvent("Access denied - whitelist expired", string4, string2, string, false);
                printWriter.println("ERROR: Access denied. Whitelist expired.");
                return;
            }
            if (!whitelistEntry.apiKey.equals(string2)) {
                CentralizedValidationServer.logValidationEvent("Access denied - API key mismatch", string4, string2, string, false);
                printWriter.println("ERROR: Access denied. API key mismatch.");
                return;
            }
            CentralizedValidationServer.logValidationEvent("Access verified successfully", string4, string2, string, true);
            printWriter.println("SUCCESS: Access verified");
        } catch (Exception exception) {
            LOGGER.severe("Error in access verification: " + exception.getMessage());
            printWriter.println("ERROR: Verification error occurred");
        }
    }

    private static void handleApiKeyValidation(PrintWriter out, String apiKey, String publicIP, String fingerprint, String clientIP) {
        try {
            if (!isValidAPIKey(apiKey)) {
                logValidationEvent("Invalid API key validation", clientIP, apiKey, "", false);
                out.println("ERROR: Invalid API key");
                return;
            }
            APIKeyData data = validAPIKeys.get(apiKey);
            data.lastUsed = System.currentTimeMillis();
            logValidationEvent("API key validated", clientIP, apiKey, "", true);
            out.println("VALID: OK");
        } catch (Exception e) {
            LOGGER.severe("Error in API key validation: " + e.getMessage());
            out.println("ERROR: Validation error");
        }
    }

    private static void handleEndSession(PrintWriter out, String apiKey, String installationId, String unused, String clientIP) {
        try {
            APIKeyData data = validAPIKeys.get(apiKey);
            if (data != null) {
                data.isActive = false;
                // Remove any ip mapping for this key
                ipToApiKey.entrySet().removeIf(e -> e.getValue().equals(apiKey));
                saveAPIKeys();
                logValidationEvent("Session ended and key revoked", clientIP, apiKey, "", true);
                out.println("SUCCESS: Session ended");
                return;
            }
            out.println("ERROR: Unknown key");
        } catch (Exception e) {
            LOGGER.severe("Error in end session: " + e.getMessage());
            out.println("ERROR: End session failed");
        }
    }

    private static boolean validateModInstallation(String string, String string2) {
        try {
            if (!validAPIKeys.containsKey(string)) {
                return false;
            }
            APIKeyData aPIKeyData = validAPIKeys.get(string);
            if (!aPIKeyData.isActive) {
                return false;
            }
            if (string2 == null || string2.length() < 16) {
                return false;
            }
            return aPIKeyData.installationId == null || aPIKeyData.installationId.equals(string2);
        } catch (Exception exception) {
            LOGGER.warning("Error in mod installation validation: " + exception.getMessage());
            return false;
        }
    }

    public static boolean isValidAPIKey(String string) {
        if (string == null || string.trim().isEmpty()) {
            return false;
        }
        APIKeyData aPIKeyData = validAPIKeys.get(string);
        return aPIKeyData != null && aPIKeyData.isActive;
    }

    public static boolean isUserWhitelisted(String string) {
        if (string == null || !whitelistedUsers.containsKey(string)) {
            return false;
        }
        WhitelistEntry whitelistEntry = whitelistedUsers.get(string);
        if (System.currentTimeMillis() - whitelistEntry.timestamp > 300000L) {
            whitelistedUsers.remove(string);
            return false;
        }
        return true;
    }

    private static void updateAPIKeyUsage(String string, String string2) {
        APIKeyData aPIKeyData = validAPIKeys.get(string);
        if (aPIKeyData != null) {
            aPIKeyData.lastUsed = System.currentTimeMillis();
            ++aPIKeyData.requestCount;
            aPIKeyData.installationId = string2;
            String string3 = null;
            for (Map.Entry<String, String> entry : ipToApiKey.entrySet()) {
                if (!entry.getValue().equals(string)) continue;
                string3 = entry.getKey();
                break;
            }
            if (string3 != null && !string3.equals(string2)) {
                aPIKeyData.isActive = false;
                ipToApiKey.remove(string3);
                CentralizedValidationServer.ensureKeyPool();
            }
            CentralizedValidationServer.saveAPIKeys();
        }
    }

    private static void logValidationEvent(String event, String clientIP, String apiKey, String username, boolean success) {
        ValidationLogEntry validationLogEntry = new ValidationLogEntry();
        validationLogEntry.event = event;
        validationLogEntry.clientIP = clientIP;
        validationLogEntry.apiKey = CentralizedValidationServer.maskAPIKey(apiKey);
        validationLogEntry.username = username;
        validationLogEntry.timestamp = System.currentTimeMillis();
        validationLogEntry.success = success;
        validationLogs.add(validationLogEntry);
        if (validationLogs.size() > 1000) {
            validationLogs.remove(0);
        }
        CentralizedValidationServer.saveValidationLogs();
        String apiKeyHash = hashAPIKey(apiKey);
        String logMsg = String.format("[%s] %s - User: %s, IP: %s, Success: %s, APIKeyHash: %s", new Date(), event, username, clientIP, success, apiKeyHash);
        if (success) {
            LOGGER.info(logMsg);
        } else {
            LOGGER.warning(String.format("[%s] %s - User: %s, IP: %s, Success: %s", new Date(), event, username, clientIP, success));
        }
    }

    private static String maskAPIKey(String string) {
        if (string == null || string.length() < 8) {
            return "***";
        }
        return string.substring(0, 4) + "***" + string.substring(string.length() - 4);
    }

    private static String hashAPIKey(String apiKey) {
        // SHA-256 hashing removed
        return "sha256_removed";
    }

    private static void startWhitelistCleanup() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask(){

            @Override
            public void run() {
                CentralizedValidationServer.cleanupExpiredWhitelists();
            }
        }, 60000L, 60000L);
    }

    private static void cleanupExpiredWhitelists() {
        long l = System.currentTimeMillis();
        whitelistedUsers.entrySet().removeIf(entry -> l - ((WhitelistEntry)entry.getValue()).timestamp > 300000L);
    }

    private static void loadAPIKeys() {
        try {
            Path path = CentralizedValidationServer.getDataDirectory().resolve(API_KEYS_FILE);
            if (Files.exists(path, new LinkOption[0])) {
                String string = Files.readString((Path)path);
                JsonObject jsonObject = JsonParser.parseString(string).getAsJsonObject();
                for (String string2 : jsonObject.keySet()) {
                    JsonObject jsonObject2 = jsonObject.getAsJsonObject(string2);
                    APIKeyData aPIKeyData = new APIKeyData();
                    aPIKeyData.apiKey = string2;
                    aPIKeyData.installationId = jsonObject2.has("installationId") ? jsonObject2.get("installationId").getAsString() : null;
                    aPIKeyData.clientFingerprint = jsonObject2.has("clientFingerprint") ? jsonObject2.get("clientFingerprint").getAsString() : null;
                    aPIKeyData.createdAt = jsonObject2.has("createdAt") ? jsonObject2.get("createdAt").getAsLong() : System.currentTimeMillis();
                    aPIKeyData.lastUsed = jsonObject2.has("lastUsed") ? jsonObject2.get("lastUsed").getAsLong() : 0L;
                    aPIKeyData.isActive = jsonObject2.has("isActive") ? jsonObject2.get("isActive").getAsBoolean() : true;
                    aPIKeyData.requestCount = jsonObject2.has("requestCount") ? jsonObject2.get("requestCount").getAsInt() : 0;
                    validAPIKeys.put(string2, aPIKeyData);
                }
                LOGGER.info("Loaded " + validAPIKeys.size() + " API keys from file");
            } else {
                CentralizedValidationServer.createSampleAPIKeys();
            }
        } catch (Exception exception) {
            LOGGER.severe("Failed to load API keys: " + exception.getMessage());
            CentralizedValidationServer.createSampleAPIKeys();
        }
    }

    private static void createSampleAPIKeys() {
        CentralizedValidationServer.addAPIKey("sample_key_1", "sample_installation_1", "sample_fingerprint_1");
        CentralizedValidationServer.addAPIKey("sample_key_2", "sample_installation_2", "sample_fingerprint_2");
        LOGGER.info("Created sample API keys for testing");
    }

    public static void addAPIKey(String string, String string2, String string3) {
        APIKeyData aPIKeyData = new APIKeyData();
        aPIKeyData.apiKey = string;
        aPIKeyData.installationId = string2;
        aPIKeyData.clientFingerprint = string3;
        aPIKeyData.createdAt = System.currentTimeMillis();
        aPIKeyData.isActive = true;
        aPIKeyData.requestCount = 0;
        validAPIKeys.put(string, aPIKeyData);
        CentralizedValidationServer.saveAPIKeys();
        LOGGER.info("Added new API key for installation: " + string2);
    }

    private static void saveAPIKeys() {
        try {
            Path path = CentralizedValidationServer.getDataDirectory().resolve(API_KEYS_FILE);
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            JsonObject jsonObject = new JsonObject();
            for (APIKeyData aPIKeyData : validAPIKeys.values()) {
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.addProperty("installationId", aPIKeyData.installationId);
                jsonObject2.addProperty("clientFingerprint", aPIKeyData.clientFingerprint);
                jsonObject2.addProperty("createdAt", aPIKeyData.createdAt);
                jsonObject2.addProperty("lastUsed", aPIKeyData.lastUsed);
                jsonObject2.addProperty("isActive", aPIKeyData.isActive);
                jsonObject2.addProperty("requestCount", aPIKeyData.requestCount);
                for (Map.Entry<String, String> entry : ipToApiKey.entrySet()) {
                    if (!entry.getValue().equals(aPIKeyData.apiKey)) continue;
                    jsonObject2.addProperty("assignedIP", entry.getKey());
                }
                jsonObject.add(aPIKeyData.apiKey, jsonObject2);
            }
            Files.writeString((Path)path, (CharSequence)gson.toJson(jsonObject), (OpenOption[])new OpenOption[0]);
        } catch (Exception exception) {
            LOGGER.severe("Failed to save API keys: " + exception.getMessage());
        }
    }

    private static void loadValidationLogs() {
        try {
            Path path = CentralizedValidationServer.getDataDirectory().resolve(VALIDATION_LOGS_FILE);
            if (Files.exists(path, new LinkOption[0])) {
                String string = Files.readString((Path)path);
                ValidationLogEntry[] validationLogEntryArray = gson.fromJson(string, ValidationLogEntry[].class);
                if (validationLogEntryArray != null) {
                    validationLogs.addAll(Arrays.asList(validationLogEntryArray));
                }
                LOGGER.info("Loaded " + validationLogs.size() + " validation logs from file");
            }
        } catch (Exception exception) {
            LOGGER.severe("Failed to load validation logs: " + exception.getMessage());
        }
    }

    private static void saveValidationLogs() {
        try {
            Path path = CentralizedValidationServer.getDataDirectory().resolve(VALIDATION_LOGS_FILE);
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            Files.writeString((Path)path, (CharSequence)gson.toJson(validationLogs), (OpenOption[])new OpenOption[0]);
        } catch (Exception exception) {
            LOGGER.severe("Failed to save validation logs: " + exception.getMessage());
        }
    }

    private static Path getDataDirectory() {
        return Paths.get(System.getProperty("user.dir"), "data");
    }

    private static void ensureKeyPool() {
        int n = 2000 - (int)validAPIKeys.values().stream().filter(aPIKeyData -> aPIKeyData.isActive && !ipToApiKey.containsValue(aPIKeyData.apiKey)).count();
        for (int i = 0; i < n; ++i) {
            String string;
            while (validAPIKeys.containsKey(string = CentralizedValidationServer.generateRandomKey())) {
            }
            APIKeyData aPIKeyData2 = new APIKeyData();
            aPIKeyData2.apiKey = string;
            aPIKeyData2.isActive = true;
            aPIKeyData2.createdAt = System.currentTimeMillis();
            validAPIKeys.put(string, aPIKeyData2);
        }
        CentralizedValidationServer.saveAPIKeys();
    }

    private static String generateRandomKey() {
        String string = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder stringBuilder = new StringBuilder(20);
        Random random = new Random();
        for (int i = 0; i < 20; ++i) {
            stringBuilder.append(string.charAt(random.nextInt(string.length())));
        }
        return stringBuilder.toString();
    }

    private static String generateSessionToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static {
        CentralizedValidationServer.loadAPIKeys();
        CentralizedValidationServer.loadValidationLogs();
        CentralizedValidationServer.startWhitelistCleanup();
        CentralizedValidationServer.ensureKeyPool();
        // Clean up expired sessions every 5 minutes
        new Thread(() -> {
            while (true) {
                long now = System.currentTimeMillis();
                sessions.entrySet().removeIf(e -> now - e.getValue().lastActive > SESSION_EXPIRY_MS);
                try { Thread.sleep(5 * 60 * 1000); } catch (InterruptedException ignored) {}
            }
        }, "SessionCleanup").start();
    }

    private static class APIKeyData {
        String apiKey;
        String installationId;
        String clientFingerprint;
        long createdAt;
        long lastUsed;
        boolean isActive;
        int requestCount;

        private APIKeyData() {
        }
    }

    private static class WhitelistEntry {
        String username;
        String apiKey;
        String installationId;
        long timestamp;
        String clientIP;

        private WhitelistEntry() {
        }
    }

    private static class ValidationLogEntry {
        String event;
        String clientIP;
        String apiKey;
        String username;
        long timestamp;
        boolean success;

        private ValidationLogEntry() {
        }
    }

    private static class SessionInfo {
        final String username;
        final String ip;
        volatile long lastActive;
        SessionInfo(String username, String ip) {
            this.username = username;
            this.ip = ip;
            this.lastActive = System.currentTimeMillis();
        }
    }
    private static class RateLimitInfo {
        volatile int count;
        volatile long windowStart;
        RateLimitInfo() {
            this.count = 0;
            this.windowStart = System.currentTimeMillis();
        }
    }
}

