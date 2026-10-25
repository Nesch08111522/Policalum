package com.policalum.erp.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Evita leer PostgreSQL en cada request. Se invalida al cerrar otras sesiones. */
@Component
@RequiredArgsConstructor
public class SesionUsuarioCache {

    private final UserRepository userRepository;
    private final Cache<String, Estado> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(20))
            .maximumSize(1_000)
            .build();

    public Estado consultar(String email) {
        return cache.get(email, this::cargar);
    }

    public void invalidar(String email) {
        if (email != null && !email.isBlank()) cache.invalidate(email);
    }

    private Estado cargar(String email) {
        return userRepository.findByEmail(email)
                .map(user -> new Estado(true, Boolean.TRUE.equals(user.getActivo()),
                        user.getSesionVersion() == null ? 0 : user.getSesionVersion()))
                .orElseGet(() -> new Estado(false, false, 0));
    }

    public record Estado(boolean existe, boolean activo, int version) {}
}
