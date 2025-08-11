package com.swapnil.titlemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.swapnil.titlemod.util.MinecraftClientIdentifier;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class ModConfig {

  private static final String CONFIG_FILE = "titlemod_config.json";
  private static ModConfig INSTANCE;

  // Encrypted server addresses
  private String encryptedMatchmakingServerIp;
  private int matchmakingServerPort;
  private String encryptedKitEditorServerIp;
  private int kitEditorServerPort;

  // Default constructor for Gson
  public ModConfig() {
    // Default values - these will be encrypted automatically
    String matchmakingIp = "34.159.92.94";
    String kitEditorIp = "34.159.92.94";

    this.encryptedMatchmakingServerIp = encrypt(matchmakingIp);
    this.matchmakingServerPort = 5000;
    this.encryptedKitEditorServerIp = encrypt(kitEditorIp);
    this.kitEditorServerPort = 25565;

    System.out.println("[TitleMod] Default config created. Encrypted IPs:");
    System.out.println("[TitleMod]   Matchmaking: " + this.encryptedMatchmakingServerIp);
    System.out.println("[TitleMod]   KitEditor: " + this.encryptedKitEditorServerIp);
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
        System.out.println("[TitleMod] Config loaded successfully");
      } catch (IOException e) {
        System.err.println("[TitleMod] Failed to load config: " + e.getMessage());
        INSTANCE = new ModConfig();
        saveConfig(); // Create default config
      }
    } else {
      INSTANCE = new ModConfig();
      saveConfig(); // Create default config
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
      System.out.println("[TitleMod] Config saved successfully");
    } catch (IOException e) {
      System.err.println("[TitleMod] Failed to save config: " + e.getMessage());
    }
  }

  // Getters with decryption
  public String getMatchmakingServerIp() {
    String decrypted = decrypt(encryptedMatchmakingServerIp);
    System.out.println("[TitleMod] Decrypted Matchmaking IP: " + decrypted); // Debug log
    return decrypted;
  }

  public int getMatchmakingServerPort() {
    return matchmakingServerPort;
  }

  public String getKitEditorServerIp() {
    String decrypted = decrypt(encryptedKitEditorServerIp);
    System.out.println("[TitleMod] Decrypted KitEditor IP: " + decrypted); // Debug log
    return decrypted;
  }

  public int getKitEditorServerPort() {
    return kitEditorServerPort;
  }

  // Setters with encryption
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

  public void setKitEditorServerPort(int port) {
    this.kitEditorServerPort = port;
    saveConfig();
  }

  // Advanced encryption/decryption methods
  private String encrypt(String input) {
    System.out.println("[TitleMod] Encrypting: " + input);
    try {
      // Use SHA-256 to derive a key from a hardcoded secret
      // This makes the encryption more complex and harder to reverse-engineer
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] keyBytes =
          digest.digest(
              ("TitleModSecretKey" + MinecraftClientIdentifier.getClientId())
                  .getBytes(StandardCharsets.UTF_8));

      // Use the first 16 bytes of the hash as our encryption key
      byte[] key = new byte[16];
      System.arraycopy(keyBytes, 0, key, 0, 16);

      // Apply multiple rounds of XOR with different derived keys
      byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
      byte[] encrypted = new byte[bytes.length];
      System.arraycopy(bytes, 0, encrypted, 0, bytes.length);

      // First round with original key
      for (int i = 0; i < encrypted.length; i++) {
        encrypted[i] = (byte) (encrypted[i] ^ key[i % key.length]);
      }

      // Second round with modified key
      for (int i = 0; i < key.length; i++) {
        key[i] = (byte) (key[i] ^ 0x3A); // XOR with a constant to create a different key
      }
      for (int i = 0; i < encrypted.length; i++) {
        encrypted[i] = (byte) (encrypted[i] ^ key[(i + 3) % key.length]);
      }
      String encryptedValue = Base64.getEncoder().encodeToString(encrypted);
      System.out.println("[TitleMod] Encrypted: " + encryptedValue);
      return encryptedValue;
    } catch (NoSuchAlgorithmException e) {
      System.err.println("[TitleMod] Encryption failed: " + e.getMessage());
      // Fallback to simple encryption if advanced method fails
      byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
      byte[] key = "TitleModSecretKey".getBytes(StandardCharsets.UTF_8);
      byte[] encrypted = new byte[bytes.length];
      for (int i = 0; i < bytes.length; i++) {
        encrypted[i] = (byte) (bytes[i] ^ key[i % key.length]);
      }
      String encryptedValue = Base64.getEncoder().encodeToString(encrypted);
      System.out.println("[TitleMod] Encrypted (Simple): " + encryptedValue);
      return encryptedValue;
    }
  }

  private String decrypt(String encrypted) {
    System.out.println("[TitleMod] Decrypting: " + encrypted);
    try {
      // Use SHA-256 to derive the same key used for encryption
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] keyBytes =
          digest.digest(
              ("TitleModSecretKey" + MinecraftClientIdentifier.getClientId())
                  .getBytes(StandardCharsets.UTF_8));

      // Use the first 16 bytes of the hash as our decryption key
      byte[] key = new byte[16];
      System.arraycopy(keyBytes, 0, key, 0, 16);

      byte[] bytes = Base64.getDecoder().decode(encrypted);
      byte[] decrypted = new byte[bytes.length];
      System.arraycopy(bytes, 0, decrypted, 0, bytes.length);

      // Reverse the second round of encryption
      for (int i = 0; i < key.length; i++) {
        key[i] = (byte) (key[i] ^ 0x3A); // XOR with the same constant used in encryption
      }
      for (int i = 0; i < decrypted.length; i++) {
        decrypted[i] = (byte) (decrypted[i] ^ key[(i + 3) % key.length]);
      }

      // Reverse the first round of encryption
      for (int i = 0; i < key.length; i++) {
        key[i] = (byte) (key[i] ^ 0x3A); // Revert back to original key
      }
      for (int i = 0; i < decrypted.length; i++) {
        decrypted[i] = (byte) (decrypted[i] ^ key[i % key.length]);
      }

      String decryptedValue = new String(decrypted, StandardCharsets.UTF_8);
      System.out.println("[TitleMod] Decrypted: " + decryptedValue);
      return decryptedValue;
    } catch (Exception e) {
      System.err.println("[TitleMod] Advanced decryption failed: " + e.getMessage());
      try {
        // Fallback to simple decryption
        byte[] bytes = Base64.getDecoder().decode(encrypted);
        byte[] key = "TitleModSecretKey".getBytes(StandardCharsets.UTF_8);
        byte[] decrypted = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
          decrypted[i] = (byte) (bytes[i] ^ key[i % key.length]);
        }
        String decryptedValue = new String(decrypted, StandardCharsets.UTF_8);
        System.out.println("[TitleMod] Decrypted (Simple): " + decryptedValue);
        return decryptedValue;
      } catch (Exception ex) {
        System.err.println("[TitleMod] Simple decryption also failed: " + ex.getMessage());
        return "127.0.0.1"; // Fallback to localhost
      }
    }
  }

  // Generate SHA-256 fingerprint for matchmaking
  public static String generateFingerprint(String username, String kitName) {
    try {
      String input = username + "::" + kitName + "::TitleModSecret";
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      // Convert to hex string
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      System.err.println("[TitleMod] SHA-256 algorithm not available: " + e.getMessage());
      return "";
    }
  }
}