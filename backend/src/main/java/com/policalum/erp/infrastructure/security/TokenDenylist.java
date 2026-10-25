package com.policalum.erp.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Access tokens revocados en este proceso. El otro navegador conserva el suyo. */
@Component
public class TokenDenylist {

    private final Cache<String, Boolean> revocados = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(8))
            .maximumSize(10_000)
            .build();

    public void revocar(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return;
        revocados.put(hash(accessToken), Boolean.TRUE);
    }

    public boolean revocado(String accessToken) {
        return accessToken != null && revocados.getIfPresent(hash(accessToken)) != null;
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo marcar el token", e);
        }
    }
}
