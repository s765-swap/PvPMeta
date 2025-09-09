package com.swapnil.titlemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.swapnil.titlemod.util.MinecraftClientIdentifier;
import com.swapnil.titlemod.security.SecureLogger;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.Base64;

public class ModConfig {

  private static final String CONFIG_FILE = "titlemod_config.json";
  private static ModConfig INSTANCE;

  
  private String encryptedMatchmakingServerIp;
  private int matchmakingServerPort;
  private String encryptedKitEditorServerIp;
  private int kitEditorWhitelistPort;
  private int kitEditorMinecraftPort;

 
  public ModConfig() {
    
    String matchmakingIp = "34.159.92.94";
    String kitEditorIp = "34.159.92.94";

    this.encryptedMatchmakingServerIp = encrypt(matchmakingIp);
    this.matchmakingServerPort = 5000; 
    this.encryptedKitEditorServerIp = encrypt(kitEditorIp);
    this.kitEditorWhitelistPort = 5007; 
    this.kitEditorMinecraftPort = 25565; 

    SecureLogger.logInfo("Default config created with encrypted IPs");
  }

  public static ModConfig getInstance() {
    if (INSTANCE == null) {
      loadConfig();
    }
    return INSTANCE;
  }

  public static void loadConfig() {
    File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE);
    if (configFile.exists()) {
      try (FileReader reader = new FileReader(configFile)) {
        Gson gson = new Gson();
        INSTANCE = gson.fromJson(reader, ModConfig.class);
        SecureLogger.logInfo("Config loaded successfully");
      } catch (IOException e) {
        SecureLogger.logSecurityEvent("Failed to load config: " + e.getMessage());
        INSTANCE = new ModConfig();
        saveConfig(); 
      }
    } else {
      INSTANCE = new ModConfig();
      saveConfig(); 
    }
  }

  public static void saveConfig() {
    File configDir = FabricLoader.getInstance().getConfigDir().toFile();
    if (!configDir.exists()) {
      configDir.mkdirs();
    }
    File configFile = new File(configDir, CONFIG_FILE);
    try (FileWriter writer = new FileWriter(configFile)) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      gson.toJson(INSTANCE, writer);
      SecureLogger.logInfo("Config saved successfully");
    } catch (IOException e) {
      SecureLogger.logSecurityEvent("Failed to save config: " + e.getMessage());
    }
  }

  
  public String getMatchmakingServerIp() {
    
    return com.swapnil.titlemod.security.SecureIPManager.getMatchmakingServerIP();
  }

  public int getMatchmakingServerPort() {
    return matchmakingServerPort;
  }

  public String getKitEditorServerIp() {
    
    return com.swapnil.titlemod.security.SecureIPManager.getKitEditorServerIP();
  }

  public int getKitEditorWhitelistPort() {
    
    return kitEditorWhitelistPort > 0 ? kitEditorWhitelistPort : 5007;
  }

  public int getKitEditorMinecraftPort() {
    
    return kitEditorMinecraftPort > 0 ? kitEditorMinecraftPort : 25565;
  }

  
  public void setMatchmakingServerIp(String ip) {
    this.encryptedMatchmakingServerIp = encrypt(ip);
    saveConfig();
  }

  public void setMatchmakingServerPort(int port) {
    this.matchmakingServerPort = port;
    saveConfig();
  }

  public void setKitEditorServerIp(String ip) {
    this.encryptedKitEditorServerIp = encrypt(ip);
    saveConfig();
  }

  public void setKitEditorWhitelistPort(int port) {
    this.kitEditorWhitelistPort = port;
    saveConfig();
  }

  public void setKitEditorMinecraftPort(int port) {
    this.kitEditorMinecraftPort = port;
    saveConfig();
  }

  
  private String encrypt(String input) {
    try {
     
      String secretKey = System.getenv("TITLEMOD_ENCRYPTION_KEY");
      if (secretKey == null || secretKey.isEmpty()) {
        secretKey = "TitleModSecureKey2024!@#$%^&*()";
      }
      
      
      javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
          secretKey.toCharArray(), 
          "TitleModSalt2024".getBytes(StandardCharsets.UTF_8), 
          65536, 
          256
      );
      
      byte[] keyBytes = factory.generateSecret(spec).getEncoded();
      
      
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
      javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
      javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(
          "TitleModIV2024!".getBytes(StandardCharsets.UTF_8)
      );
      
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, ivSpec);
      byte[] encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
      
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      SecureLogger.logSecurityEvent("Encryption failed: " + e.getMessage());
      
      return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
  }

  private String decrypt(String encrypted) {
    try {
      
      String secretKey = System.getenv("TITLEMOD_ENCRYPTION_KEY");
      if (secretKey == null || secretKey.isEmpty()) {
        secretKey = "TitleModSecureKey2024!@#$%^&*()";
      }
      
      
      javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
          secretKey.toCharArray(), 
          "TitleModSalt2024".getBytes(StandardCharsets.UTF_8), 
          65536, 
          256
      );
      
      byte[] keyBytes = factory.generateSecret(spec).getEncoded();
      
      
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
      javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
      javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(
          "TitleModIV2024!".getBytes(StandardCharsets.UTF_8)
      );
      
      cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec);
      byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
      
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      SecureLogger.logSecurityEvent("Decryption failed: " + e.getMessage());
      try {
        
        return new String(Base64.getDecoder().decode(encrypted), StandardCharsets.UTF_8);
      } catch (Exception ex) {
        SecureLogger.logSecurityEvent("Fallback decryption also failed: " + ex.getMessage());
        return "127.0.0.1"; 
      }
    }
  }

  
  public static String generateFingerprint(String username, String kitName) {
    String input = username + "::" + kitName + "::TitleModSecret";
    return input;
  }

  
  public static String generateTimestampedSignature(String username, String kitName, long timestampMs) {
    String input = username + "::" + kitName + "::" + timestampMs + "::TitleModSecret";
    return input;
  }
}