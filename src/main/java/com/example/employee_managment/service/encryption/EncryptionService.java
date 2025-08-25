package com.example.employee_managment.service.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data.
 * Uses AES encryption with a configurable secret key.
 */
@Service
public class EncryptionService {
    
    @Value("${app.encryption.secret-key:defaultSecretKey123}")
    private String secretKeyString;
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    /**
     * Encrypts a string value using AES encryption.
     * 
     * @param value The value to encrypt
     * @return Base64 encoded encrypted string, or null if input is null
     */
    public String encrypt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            SecretKey secretKey = generateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt value", e);
        }
    }
    
    /**
     * Decrypts an encrypted string value using AES decryption.
     * 
     * @param encryptedValue The Base64 encoded encrypted value
     * @return Decrypted string, or null if input is null
     */
    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.trim().isEmpty()) {
            return null;
        }
        
        try {
            SecretKey secretKey = generateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedValue);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt value", e);
        }
    }
    
    /**
     * Generates a secret key from the configured secret key string.
     * 
     * @return SecretKey for AES encryption
     */
    private SecretKey generateSecretKey() {
        try {
            // Use the configured secret key string to generate a consistent key
            byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
            // Ensure the key is exactly 16, 24, or 32 bytes for AES
            byte[] normalizedKey = new byte[16];
            System.arraycopy(keyBytes, 0, normalizedKey, 0, Math.min(keyBytes.length, 16));
            return new SecretKeySpec(normalizedKey, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate secret key", e);
        }
    }
    
    /**
     * Manually decrypt a field value when needed.
     * Use this method in your service layer when you need the decrypted value.
     * 
     * @param encryptedValue The encrypted value to decrypt
     * @return Decrypted value, or null if input is null
     */
    public String decryptField(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.trim().isEmpty()) {
            return null;
        }
        return decrypt(encryptedValue);
    }
    
    /**
     * Generates a new random secret key (for testing purposes).
     * 
     * @return Base64 encoded random secret key
     */
    public String generateNewSecretKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(128, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate new secret key", e);
        }
    }
}
