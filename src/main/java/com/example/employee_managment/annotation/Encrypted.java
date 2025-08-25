package com.example.employee_managment.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark fields that should be automatically encrypted/decrypted.
 * 
 * Usage:
 * @Encrypted
 * private String sensitiveData;
 * 
 * This annotation will trigger automatic encryption when saving to database
 * and automatic decryption when loading from database.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {
    
    /**
     * Optional: Specify the source field to encrypt from.
     * If not specified, the annotated field itself will be encrypted.
     */
    String sourceField() default "";
    
    /**
     * Optional: Specify the encryption algorithm to use.
     * Default is AES encryption.
     */
    String algorithm() default "AES";
    
    /**
     * Optional: Specify if the field should be automatically populated
     * from the source field when saving.
     */
    boolean autoPopulate() default true;
}
