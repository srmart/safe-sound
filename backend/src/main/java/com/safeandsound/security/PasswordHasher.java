package com.safeandsound.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.security.MessageDigest;

@Component
public class PasswordHasher {

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH = 256;

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(char[] password) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
                password,
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] hash = factory.generateSecret(spec).getEncoded();

            // guarda los parámetros necesarios para poder verificar la contraseña posteriormente.
            return ITERATIONS
                    + ":"
                    + Base64.getEncoder().encodeToString(salt)
                    + ":"
                    + Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo generar el hash de la contraseña", e);

        } finally {
            spec.clearPassword();
        }
    }

    //metodo para chequear contraseña
    public boolean matches(char[] password, String storedHash) {

        String[] parts = storedHash.split(":");

        if (parts.length != 3) {
            return false;
        }

        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

        PBEKeySpec spec = new PBEKeySpec(
                password,
                salt,
                iterations,
                expectedHash.length * 8
        );

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] actualHash = factory.generateSecret(spec).getEncoded();

            return MessageDigest.isEqual(expectedHash, actualHash);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(
                    "No se pudo verificar la contraseña",
                    e
            );

        } finally {
            spec.clearPassword();
        }
    }
}