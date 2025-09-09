package com.swapnil.titlemod.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class StringEncryption {
    private static final String ALGORITHM = "AES";
    private static final String SECRET_KEY = System.getProperty("titlemod.key", "TitleModSecureKey2024!@#$");
    
    public static String encrypt(String plainText) {
        try {
            SecretKeySpec key = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            return plainText;
        }
    }
    
    public static String decrypt(String encryptedText) {
        try {
            SecretKeySpec key = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encryptedText;
        }
    }
    
    private static SecretKeySpec generateKey() throws Exception {
        byte[] key = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        
        byte[] key16 = new byte[16];
        int len = Math.min(key.length, 16);
        System.arraycopy(key, 0, key16, 0, len);
        return new SecretKeySpec(key16, 0, 16, ALGORITHM);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java StringEncryption <string-to-encrypt>");
            return;
        }
        String encrypted = encrypt(args[0]);
        System.out.println("Encrypted: " + encrypted);
    }
}