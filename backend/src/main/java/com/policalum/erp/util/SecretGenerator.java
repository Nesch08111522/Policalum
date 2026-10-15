package com.policalum.erp.util;

import java.security.SecureRandom;
import java.util.Base64;

public class SecretGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int SECRET_LENGTH  = 32; //=> 256 bits

    public static String generateSecureSecret(){
        byte[] bytes = new byte[SECRET_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    }

    public static void main(String[] args) {
        System.out.println("🔑" +
                "Secreto Público TOTP:");
        System.out.println(generateSecureSecret());
        System.out.println();
        System.out.println("Copia este valor para application.properties");
    }
}
