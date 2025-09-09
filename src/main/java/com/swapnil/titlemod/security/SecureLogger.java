package com.swapnil.titlemod.security;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class SecureLogger {
    
    private static final String LOG_FILE = "logs/titlemod.log";
    private static final String SECURITY_LOG_FILE = "logs/titlemod_security.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    
    private static final ConcurrentHashMap<String, AtomicInteger> logFrequency = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastLogTime = new ConcurrentHashMap<>();
    private static final int MAX_LOGS_PER_MINUTE = 100;
    private static final long LOG_RESET_INTERVAL = 60000; // 1 minute
    
    public enum LogLevel {
        DEBUG(0), INFO(1), WARN(2), ERROR(3), SECURITY(4);
        
        private final int level;
        
        LogLevel(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    private static LogLevel currentLogLevel = LogLevel.INFO;
    
   
    public static void setLogLevel(LogLevel level) {
        currentLogLevel = level;
    }
    
 
    public static void debug(String message) {
        log(LogLevel.DEBUG, "[DEBUG]", IPMasker.maskSensitiveData(message));
    }
    
   
    public static void info(String message) {
        log(LogLevel.INFO, "[INFO]", IPMasker.maskSensitiveData(message));
    }
    
  
    public static void warn(String message) {
        log(LogLevel.WARN, "[WARN]", IPMasker.maskSensitiveData(message));
    }
    
    
    public static void error(String message) {
        log(LogLevel.ERROR, "[ERROR]", IPMasker.maskSensitiveData(message));
    }
    
    
    public static void security(String message) {
        log(LogLevel.SECURITY, "[SECURITY]", IPMasker.maskSensitiveData(message));
    }
    
    
    public static void logSecurityEvent(String message) {
        log(LogLevel.SECURITY, "[SECURITY]", IPMasker.maskSensitiveData(message));
    }
    
   
    public static void logInfo(String message) {
        log(LogLevel.INFO, "[INFO]", IPMasker.maskSensitiveData(message));
    }
    
 
    private static void log(LogLevel level, String prefix, String message) {
        if (level.getLevel() < currentLogLevel.getLevel()) {
            return; 
        }
        
        
        if (!checkLogFrequency(prefix + message)) {
            return; 
        }
        
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format("%s %s %s", timestamp, prefix, message);
        
        
        if (isDevelopmentMode()) {
            System.out.println(logMessage);
        }
        
        
        try {
            writeToLogFile(logMessage, level == LogLevel.SECURITY ? SECURITY_LOG_FILE : LOG_FILE);
        } catch (IOException e) {
            
            if (isDevelopmentMode()) {
                System.err.println("[LOGGER ERROR] Failed to write to log file: " + e.getMessage());
            }
        }
    }
    
   
    private static boolean isDevelopmentMode() {
        return Boolean.getBoolean("titlemod.development") || 
               System.getProperty("java.class.path").contains("gradle");
    }
    
   
    private static boolean checkLogFrequency(String messageHash) {
        long currentTime = System.currentTimeMillis();
        String key = messageHash.substring(0, Math.min(messageHash.length(), 50)); 
        
        AtomicInteger count = logFrequency.computeIfAbsent(key, k -> new AtomicInteger(0));
        Long lastTime = lastLogTime.get(key);
        
        if (lastTime != null && (currentTime - lastTime) < LOG_RESET_INTERVAL) {
            if (count.incrementAndGet() > MAX_LOGS_PER_MINUTE) {
                return false; 
            }
        } else {
            
            count.set(1);
            lastLogTime.put(key, currentTime);
        }
        
        return true;
    }
    
   
    private static void writeToLogFile(String message, String filename) throws IOException {
       
        File logDir = new File("logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        
        
        try (FileWriter fw = new FileWriter(filename, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(message);
        }
    }
    
   
    public static void securityEvent(String eventType, String username, String details) {
        String message = String.format("Event: %s | User: %s | Details: %s", eventType, username, details);
        security(message);
    }
    
   
    public static void matchmaking(String message) {
        info("[Matchmaking] " + message);
    }
    
    
    public static void kitEditor(String message) {
        info("[Kit Editor] " + message);
    }
    
    
    public static void whitelist(String message) {
        info("[Whitelist] " + message);
    }
    
    
    public static void rcon(String message) {
        info("[RCON] " + message);
    }
    
  
    public static void queue(String message) {
        info("[Queue] " + message);
    }
    
    
    public static void timeout(String message) {
        warn("[Timeout] " + message);
    }
    
    
    public static void mixin(String message) {
        debug("[Mixin] " + message);
    }
    
    
    public static void config(String message) {
        info("[Config] " + message);
    }
    
   
    public static void debug(String category, String message) {
        if (isDevelopmentMode()) {
            debug("[" + category + "] " + message);
        }
    }
}
