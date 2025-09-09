package com.swapnil.titlemod.security;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;


public class APIKeyManager {
  
    public static void testFakeServerConnection() {
        System.out.println("[TitleMod] Testing connection to fake server: noipleak.com:0000");
        
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("noipleak.com", 0), 5000);
            System.out.println("[TitleMod] Successfully connected to noipleak.com:0000");
        } catch (Exception e) {
            System.err.println("[TitleMod] Failed to connect to noipleak.com:0000");
            System.err.println("[TitleMod] Error: " + e.getMessage());
        }
    }
    
   
    private static void showValidationFailedPopup(String message) {
        System.err.println("[TitleMod] VALIDATION FAILED: " + message);
        System.err.println("[TitleMod] Your API validation failed. Please ensure you're using the official TitleMod.");
        
       
        MinecraftClient.getInstance().execute(() -> {
           
            com.swapnil.titlemod.security.APIErrorHandler.showGenericAPIError();
        });
    }
    
    
    public static void main(String[] args) {
        System.out.println("[TEST] Testing APIKeyManager...");
        testFakeServerConnection();
    }
}
