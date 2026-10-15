// src/main/java/com/policalum/erp/application/service/TotpService.java
package com.policalum.erp.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class TotpService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_VALIDITY_SECONDS = 60L;
    private static final int TIMESTAMP_BYTES = 8;
    private static final int UUID_BYTES = 16;
    private static final int SIGNATURE_BYTES = 32;
    private static final int TOTAL_BYTES = UUID_BYTES + TIMESTAMP_BYTES + SIGNATURE_BYTES;

    // ============================================
    // ✅ CONSTANTES PARA TOTP ESTÁNDAR
    // ============================================
    public static final long STEP_SECONDS = 60L;  // Cambia cada 60 segundos
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOTP_DIGITS = 6;

    private final byte[] secretKey;

    public TotpService(@Value("${totp.secret-key:defaultSecretKeyChangeMe1234567890!}") String secretKeyBase64) {
        try {
            if (secretKeyBase64 != null && !secretKeyBase64.isEmpty() &&
                    !secretKeyBase64.equals("defaultSecretKeyChangeMe1234567890!")) {
                this.secretKey = Base64.getDecoder().decode(secretKeyBase64);
            } else {
                log.warn("⚠️ Usando clave secreta por defecto para TOTP. ¡Cambiar en producción!");
                this.secretKey = "MiClaveSecretaTOTPSegura1234567890!@#".getBytes(StandardCharsets.UTF_8);
            }
            log.info("✅ TotpService inicializado correctamente. Tamaño de clave: {} bytes", this.secretKey.length);
        } catch (IllegalArgumentException e) {
            log.error("❌ Error al decodificar la clave secreta TOTP", e);
            throw new IllegalStateException("Clave secreta TOTP inválida", e);
        }
    }

    // ============================================
    // ✅ MÉTODOS TOTP ESTÁNDAR (para QR público)
    // ============================================

    /**
     * Genera un secreto TOTP para un empleado
     */
    public String generateSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Ventana actual (basada en tiempo)
     */
    public long currentWindow() {
        return Instant.now().getEpochSecond() / STEP_SECONDS;
    }

    /**
     * Segundos restantes para que cambie el código
     */
    public long secondsRemaining() {
        long epoch = Instant.now().getEpochSecond();
        return STEP_SECONDS - (epoch % STEP_SECONDS);
    }

    /**
     * Timestamp de expiración del código actual
     */
    public long expiresAtEpoch() {
        return (currentWindow() + 1) * STEP_SECONDS;
    }

    /**
     * Genera código TOTP de 6 dígitos
     */
    public String code(String secret, long window) {
        try {
            byte[] key = Base64.getUrlDecoder().decode(secret);
            byte[] counter = ByteBuffer.allocate(8).putLong(window).array();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] hash = mac.doFinal(counter);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return String.format("%06d", binary % 1_000_000);
        } catch (Exception ex) {
            throw new IllegalStateException("Error generando código TOTP", ex);
        }
    }

    /**
     * Verifica si el código TOTP es válido
     * Permite ventana actual y anterior (tolerancia de 60-120 segundos)
     */
    public boolean verify(String secret, String code) {
        long current = currentWindow();
        for (long window = current - 1; window <= current; window++) {
            String expected = code(secret, window);
            if (expected.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica con ventana específica (para el QR que incluye la ventana)
     */
    public boolean verify(String secret, long window, String code) {
        long current = currentWindow();
        return (window == current || window == current - 1) && code(secret, window).equals(code);
    }

    // ============================================
    // ✅ MÉTODOS PARA GENERAR TOKENS CON FIRMA (para QR de empleados)
    // ============================================

    /**
     * Genera un token seguro para el empleado con firma HMAC-SHA256
     */
    public String generarTokenParaEmpleado(UUID empleadoId) {
        try {
            long expiraEn = Instant.now().getEpochSecond() + TOKEN_VALIDITY_SECONDS;

            ByteBuffer buffer = ByteBuffer.allocate(UUID_BYTES + TIMESTAMP_BYTES);
            buffer.putLong(empleadoId.getMostSignificantBits());
            buffer.putLong(empleadoId.getLeastSignificantBits());
            buffer.putLong(expiraEn);

            byte[] payload = buffer.array();
            byte[] signature = hmacSha256(payload);

            ByteBuffer tokenBuffer = ByteBuffer.allocate(payload.length + signature.length);
            tokenBuffer.put(payload);
            tokenBuffer.put(signature);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBuffer.array());

        } catch (Exception e) {
            log.error("❌ Error al generar token TOTP para empleado: {}", empleadoId, e);
            throw new IllegalStateException("No se pudo generar el token TOTP", e);
        }
    }

    /**
     * Valida el token y extrae el UUID del empleado
     */
    public UUID validarTokenYObtenerEmpleadoId(String payload) {
        try {
            byte[] tokenBytes;
            try {
                tokenBytes = Base64.getUrlDecoder().decode(payload);
            } catch (IllegalArgumentException e) {
                log.warn("❌ Token TOTP con formato Base64 inválido");
                return null;
            }

            if (tokenBytes.length != TOTAL_BYTES) {
                log.warn("❌ Token TOTP con tamaño incorrecto: {} bytes (esperado: {})", tokenBytes.length, TOTAL_BYTES);
                return null;
            }

            ByteBuffer buffer = ByteBuffer.wrap(tokenBytes);
            byte[] payloadBytes = new byte[UUID_BYTES + TIMESTAMP_BYTES];
            buffer.get(payloadBytes);
            byte[] signatureReceived = new byte[SIGNATURE_BYTES];
            buffer.get(signatureReceived);

            byte[] expectedSignature = hmacSha256(payloadBytes);
            if (!constantTimeEquals(expectedSignature, signatureReceived)) {
                log.warn("❌ Firma TOTP inválida - posible token manipulado");
                return null;
            }

            ByteBuffer payloadBuffer = ByteBuffer.wrap(payloadBytes);
            long mostSigBits = payloadBuffer.getLong();
            long leastSigBits = payloadBuffer.getLong();
            long expiraEn = payloadBuffer.getLong();

            UUID empleadoId = new UUID(mostSigBits, leastSigBits);

            long ahora = Instant.now().getEpochSecond();
            if (ahora > expiraEn) {
                log.warn("⏰ Token TOTP expirado para empleado: {}", empleadoId);
                return null;
            }

            log.debug("✅ Token TOTP válido para empleado: {}", empleadoId);
            return empleadoId;

        } catch (Exception e) {
            log.warn("❌ Error al validar token TOTP: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el tiempo restante para la expiración del token actual
     */
    public long getTiempoRestante() {
        long ahora = Instant.now().getEpochSecond();
        long proximo = ((ahora / TOKEN_VALIDITY_SECONDS) + 1) * TOKEN_VALIDITY_SECONDS;
        return proximo - ahora;
    }

    /**
     * Calcula la firma HMAC-SHA256 de los datos
     */
    private byte[] hmacSha256(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, HMAC_ALGORITHM);
            mac.init(keySpec);
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("❌ Error en HMAC-SHA256", e);
            throw new IllegalStateException("Error en algoritmo de firma HMAC", e);
        }
    }

    /**
     * Comparación de arrays en tiempo constante para evitar ataques de timing
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Método de utilidad para generar una clave secreta segura para TOTP
     */
    public static String generarClaveSecreta() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().withoutPadding().encodeToString(key);
    }
}