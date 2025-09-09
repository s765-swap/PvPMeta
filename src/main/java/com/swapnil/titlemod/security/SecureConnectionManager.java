package com.swapnil.titlemod.security;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class SecureConnectionManager {
    private static final String VALIDATION_SERVER_IP = "34.159.92.94"; 
    private static final int VALIDATION_SERVER_PORT = 5008; 
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 15000; // 15 seconds
    
    
    public static CompletableFuture<ValidationResult> requestWhitelist(String username, String service) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(VALIDATION_SERVER_IP, VALIDATION_SERVER_PORT), CONNECTION_TIMEOUT);
                socket.setSoTimeout(READ_TIMEOUT);
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                    
                    
                    
                    String payload = "REQUEST_WHITELIST::" + username + "::" + "::";
                    writer.println(payload);
                    
                    String response = reader.readLine();
                    if (response == null) {
                        return new ValidationResult(false, "No response from validation server", ValidationErrorType.SERVER_ERROR);
                    }
                    
                    if (response.startsWith("SUCCESS:")) {
                        return new ValidationResult(true, "Whitelist access granted", null);
                    } else if (response.startsWith("ERROR:")) {
                        String errorMessage = response.substring(6);
                        if (errorMessage.contains("Invalid mod installation")) {
                            return new ValidationResult(false, errorMessage, ValidationErrorType.MOD_UPDATE_REQUIRED);
                        } else {
                            return new ValidationResult(false, errorMessage, ValidationErrorType.VALIDATION_ERROR);
                        }
                    } else {
                        return new ValidationResult(false, "Invalid response from validation server", ValidationErrorType.SERVER_ERROR);
                    }
                    
                } finally {
                    socket.close();
                }
                
            } catch (Exception e) {
                return new ValidationResult(false, "Connection error: " + e.getMessage(), ValidationErrorType.CONNECTION_ERROR);
            }
        });
    }
    

    public static CompletableFuture<ValidationResult> verifyAccess(String username, String service) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(VALIDATION_SERVER_IP, VALIDATION_SERVER_PORT), CONNECTION_TIMEOUT);
                socket.setSoTimeout(READ_TIMEOUT);
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                    
                 
                    String payload = "VERIFY_ACCESS::" + username + "::" + "::";
                    writer.println(payload);
                    
                    String response = reader.readLine();
                    if (response == null) {
                        return new ValidationResult(false, "No response from validation server", ValidationErrorType.SERVER_ERROR);
                    }
                    
                    if (response.startsWith("SUCCESS:")) {
                        return new ValidationResult(true, "Access verified", null);
                    } else if (response.startsWith("ERROR:")) {
                        String errorMessage = response.substring(6);
                        if (errorMessage.contains("not whitelisted")) {
                            return new ValidationResult(false, errorMessage, ValidationErrorType.NOT_WHITELISTED);
                        } else if (errorMessage.contains("expired")) {
                            return new ValidationResult(false, errorMessage, ValidationErrorType.WHITELIST_EXPIRED);
                        } else if (errorMessage.contains("mismatch")) {
                            return new ValidationResult(false, errorMessage, ValidationErrorType.API_KEY_MISMATCH);
                        } else {
                            return new ValidationResult(false, errorMessage, ValidationErrorType.VALIDATION_ERROR);
                        }
                    } else {
                        return new ValidationResult(false, "Invalid response from validation server", ValidationErrorType.SERVER_ERROR);
                    }
                    
                } finally {
                    socket.close();
                }
                
            } catch (Exception e) {
                return new ValidationResult(false, "Connection error: " + e.getMessage(), ValidationErrorType.CONNECTION_ERROR);
            }
        });
    }
    
   
    public static void handleValidationResult(ValidationResult result, String username) {
        if (result.isSuccess()) {
            SecureLogger.logInfo("Validation successful for user: " + username);
            return;
        }
        
        
        SecureLogger.logSecurityEvent("Validation failed for user: " + username + " - " + result.getMessage());
        
       
        switch (result.getErrorType()) {
            case MOD_UPDATE_REQUIRED:
                APIErrorHandler.showModUpdateRequired();
                break;
            case UNAUTHORIZED_ACCESS:
                APIErrorHandler.showUnauthorizedAccess();
                break;
            case CONNECTION_ERROR:
                APIErrorHandler.showServerConnectionError();
                break;
            case NOT_WHITELISTED:
            case WHITELIST_EXPIRED:
            case API_KEY_MISMATCH:
                APIErrorHandler.showAPIErrorPopup("Access Denied", 
                    "You are not authorized to access this service.\n" +
                    "Please ensure you're using the official mod and try again.\n" +
                    "If the problem persists, contact staff.");
                break;
            case VALIDATION_ERROR:
            case SERVER_ERROR:
            default:
                APIErrorHandler.showGenericAPIError();
                break;
        }
    }
    
    
    public static class ValidationResult {
        private final boolean success;
        private final String message;
        private final ValidationErrorType errorType;
        
        public ValidationResult(boolean success, String message, ValidationErrorType errorType) {
            this.success = success;
            this.message = message;
            this.errorType = errorType;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public ValidationErrorType getErrorType() {
            return errorType;
        }
    }
    
  
    public enum ValidationErrorType {
        MOD_UPDATE_REQUIRED,
        UNAUTHORIZED_ACCESS,
        CONNECTION_ERROR,
        NOT_WHITELISTED,
        WHITELIST_EXPIRED,
        API_KEY_MISMATCH,
        VALIDATION_ERROR,
        SERVER_ERROR
    }
}