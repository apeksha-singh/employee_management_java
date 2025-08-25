package com.example.employee_managment.service.encryption;

import com.example.employee_managment.annotation.Encrypted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.lang.reflect.Field;

/**
 * Entity listener that automatically encrypts and decrypts fields
 * marked with the @Encrypted annotation.
 * 
 * This listener will:
 * - Encrypt fields before saving to database
 * - Decrypt fields after loading from database
 * - Automatically populate encrypted fields from source fields
 */
@Component
public class EncryptionEntityListener {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptionEntityListener.class);
    
    @Autowired
    private EncryptionService encryptionService;
    
    /**
     * Called before persisting an entity.
     * Encrypts all fields marked with @Encrypted annotation.
     */
    @PrePersist
    public void prePersist(Object entity) {
        logger.info("PrePersist called for entity: {}", entity.getClass().getSimpleName());
        processEncryption(entity, true);
    }
    
    /**
     * Called before updating an entity.
     * Encrypts all fields marked with @Encrypted annotation.
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        processEncryption(entity, true);
    }
    
    /**
     * Called after loading an entity from database.
     * Decrypts all fields marked with @Encrypted annotation.
     * 
     * NOTE: Commented out to keep encrypted values as-is in API responses.
     * Uncomment if you need automatic decryption.
     */
    @PostLoad
    public void postLoad(Object entity) {
        processEncryption(entity, false);
    }
    
    /**
     * Called after persisting an entity.
     * Decrypts fields for immediate use.
     */
    @PostPersist
    public void postPersist(Object entity) {
        processEncryption(entity, false);
    }
    
    /**
     * Called after updating an entity.
     * Decrypts fields for immediate use.
     */
    @PostUpdate
    public void postUpdate(Object entity) {
        processEncryption(entity, false);
    }
    
    /**
     * Processes encryption or decryption for all fields marked with @Encrypted.
     * 
     * @param entity The entity to process
     * @param encrypt True to encrypt, false to decrypt
     */
    private void processEncryption(Object entity, boolean encrypt) {
        if (entity == null) return;
        
        
        Class<?> clazz = entity.getClass();
        Field[] fields = clazz.getDeclaredFields();
        
        for (Field field : fields) {
            Encrypted encryptedAnnotation = field.getAnnotation(Encrypted.class);
            if (encryptedAnnotation == null) continue;
            
            
            try {
                field.setAccessible(true);
                
                if (encrypt) {
                    // Encryption phase
                    if (encryptedAnnotation.autoPopulate() && !encryptedAnnotation.sourceField().isEmpty()) {
                        // Auto-populate from source field
                        String sourceFieldName = encryptedAnnotation.sourceField();
                        
                        Field sourceField = clazz.getDeclaredField(sourceFieldName);
                        sourceField.setAccessible(true);
                        Object sourceValue = sourceField.get(entity);
                        
                        
                        if (sourceValue != null) {
                            String encryptedValue = encryptionService.encrypt(sourceValue.toString());
                            field.set(entity, encryptedValue);
                        }
                    } else {
                        // Encrypt the field's own value
                        Object fieldValue = field.get(entity);
                        if (fieldValue != null) {
                            String encryptedValue = encryptionService.encrypt(fieldValue.toString());
                            field.set(entity, encryptedValue);
                        }
                    }
                } else {
                    // Decryption phase
                    Object fieldValue = field.get(entity);
                    if (fieldValue != null) {
                        String decryptedValue = encryptionService.decrypt(fieldValue.toString());
                        field.set(entity, decryptedValue);
                    }
                }
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to process encryption for field: " + field.getName(), e);
            }
        }
    }
}
