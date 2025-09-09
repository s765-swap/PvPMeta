package com.swapnil.titlemod.security;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class HTTPRequestProtector {
    private static final ConcurrentHashMap<String, AtomicInteger> httpRequestCounters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastHttpRequestTime = new ConcurrentHashMap<>();
    private static final int MAX_HTTP_REQUESTS_PER_MINUTE = 10;
    private static final long HTTP_RATE_LIMIT_WINDOW = 60000; // 1 minute
    
    private static final String HTTP_REQUEST_SECRET = SignatureUtil.SIGNING_SECRET;
    
    private static final String CLIENT_SIGNATURE = SignatureUtil.CLIENT_SIGNATURE;
    
    private static final String[] ALLOWED_DOMAINS = {
        "34.159.92.94", // Your server IP
        "localhost",
        "127.0.0.1"
    };
    
    
    public static HttpURLConnection createSecureConnection(String urlString) throws IOException, SecurityException {
        
        if (!isAllowedURL(urlString)) {
            SecureLogger.logSecurityEvent("Blocked unauthorized HTTP request to: " + maskURL(urlString));
            throw new SecurityException("Unauthorized HTTP request blocked");
        }
        
        
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        
        connection.setRequestProperty("X-Request-Timestamp", String.valueOf(System.currentTimeMillis()));
        
        
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(15000); // 15 seconds
        
       
        connection.setInstanceFollowRedirects(false);
        
        SecureLogger.logInfo("Secure HTTP connection created to: " + IPMasker.maskURL(urlString));
        return connection;
    }
    
   
    private static boolean isAllowedURL(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            
            
            for (String allowed : ALLOWED_DOMAINS) {
                if (host.equals(allowed) || host.equalsIgnoreCase(allowed)) {
                    return true;
                }
            }
            
            
            if (host.contains("..") || host.contains("\\") || host.contains("//")) {
                return false; 
            }
            
            return false; 
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
  
    private static boolean validateHttpRequest(String urlString) {
        String key = "http_" + getClientFingerprint();
        
        AtomicInteger counter = httpRequestCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        Long lastTime = lastHttpRequestTime.get(key);
        long currentTime = System.currentTimeMillis();
        
        if (lastTime != null && currentTime - lastTime < HTTP_RATE_LIMIT_WINDOW) {
            if (counter.get() >= MAX_HTTP_REQUESTS_PER_MINUTE) {
                SecureLogger.logSecurityEvent("HTTP rate limit exceeded");
                return false;
            }
        } else {
            counter.set(0);
        }
        
        counter.incrementAndGet();
        lastHttpRequestTime.put(key, currentTime);
        
        return true;
    }
    
    
    private static String generateRequestSignature(String urlString) {
        long timestamp = System.currentTimeMillis();
        
        return urlString + "::" + timestamp + "::" + HTTP_REQUEST_SECRET;
    }
    
 
    public static boolean validateRequestSignature(String urlString, String signature) {
        try {
            String[] parts = signature.split("::");
            if (parts.length < 3) {
                return false;
            }
            long timestamp = Long.parseLong(parts[1]);
            if (System.currentTimeMillis() - timestamp > 300000) {
                return false;
            }
            String expected = generateRequestSignature(urlString);
            return expected.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
    
  
    private static String getClientFingerprint() {
        String clientInfo = System.getProperty("user.name", "unknown") + 
                          System.getProperty("os.name", "unknown") +
                          System.getProperty("java.version", "unknown");
        String compact = clientInfo.replaceAll("\\s+", "");
        return compact.substring(0, Math.min(16, compact.length()));
    }
    
   
    private static String maskURL(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            String maskedHost = maskIP(host);
            return urlString.replace(host, maskedHost);
        } catch (MalformedURLException e) {
            return "***.***.***.***";
        }
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
    
    
    public static Socket createSecureSocket(String host, int port) throws IOException, SecurityException {
       
        if (!isAllowedHost(host)) {
            SecureLogger.logSecurityEvent("Blocked unauthorized socket connection to: " + maskIP(host));
            throw new SecurityException("Unauthorized socket connection blocked");
        }
        
        
        Socket socket = new Socket();
        socket.setSoTimeout(15000); // 15 second timeout
        
        SecureLogger.logInfo("Secure socket connection created to: " + IPMasker.maskIP(host) + ":" + IPMasker.maskPort(":" + port));
        return socket;
    }
    
   
    private static boolean isAllowedHost(String host) {
        for (String allowed : ALLOWED_DOMAINS) {
            if (host.equals(allowed) || host.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }
    
  
    private static boolean validateSocketRequest(String host) {
        String key = "socket_" + getClientFingerprint();
        
        AtomicInteger counter = httpRequestCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        Long lastTime = lastHttpRequestTime.get(key);
        long currentTime = System.currentTimeMillis();
        
        if (lastTime != null && currentTime - lastTime < HTTP_RATE_LIMIT_WINDOW) {
            if (counter.get() >= MAX_HTTP_REQUESTS_PER_MINUTE) {
                SecureLogger.logSecurityEvent("Socket rate limit exceeded");
                return false;
            }
        } else {
            counter.set(0);
        }
        
        counter.incrementAndGet();
        lastHttpRequestTime.put(key, currentTime);
        
        return true;
    }
    
    
    public static String getClientSignature() {
        return CLIENT_SIGNATURE;
    }
 
    
    public static boolean validateClientSignature(String signature) {
        return CLIENT_SIGNATURE.equals(signature);
    }
}
