package com.swapnil.titlemod.security;

import java.net.URL;
import java.util.regex.Pattern;


public class IPMasker {
    
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    
    private static final Pattern PORT_PATTERN = Pattern.compile(":\\d{4,5}\\b");
    
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+");
    
    
    public static String maskIP(String input) {
        if (input == null) return input;
        return IP_PATTERN.matcher(input).replaceAll("***.***.***.***");
    }
    
    
    public static String maskPort(String input) {
        if (input == null) return input;
        return PORT_PATTERN.matcher(input).replaceAll(":****");
    }
    
   
    public static String maskURL(String input) {
        if (input == null) return input;
        
       
        String masked = maskIP(input);
        
        masked = maskPort(masked);
        
        
        masked = masked.replaceAll("http://[^/]+", "http://noipleak:****");
        masked = masked.replaceAll("https://[^/]+", "https://noipleak:****");
        
        return masked;
    }
    
    
    public static String maskURL(URL url) {
        if (url == null) return "null";
        return maskURL(url.toString());
    }
    
    
    public static String maskSensitiveData(String input) {
        if (input == null) return input;
        
        
        if (URL_PATTERN.matcher(input).find()) {
            return maskURL(input);
        }
        
        
        if (IP_PATTERN.matcher(input).find()) {
            return maskIP(input);
        }
        
        
        if (PORT_PATTERN.matcher(input).find()) {
            return maskPort(input);
        }
        
        return input;
    }
    
   
    public static String safeLog(String message) {
        return maskSensitiveData(message);
    }
    
   
    public static String safeLog(String format, Object... args) {
        String message = String.format(format, args);
        return maskSensitiveData(message);
    }
}
