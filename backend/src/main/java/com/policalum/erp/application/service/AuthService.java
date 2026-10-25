// src/main/java/com/policalum/erp/application/service/AuthService.java
package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.LoginRequestDTO;
import com.policalum.erp.application.dto.response.LoginResponseDTO;
import com.policalum.erp.domain.model.RefreshToken;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.RefreshTokenRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.security.JwtUtil;
import com.policalum.erp.infrastructure.security.TokenDenylist;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenDenylist tokenDenylist;

    @Value("${jwt.refresh-expiration:1209600000}")
    private long refreshExpiration;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request, String ip) {
        // 1. Buscar usuario por email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas. Verifica tu email y contraseña."));

        if (!user.getActivo()) {
            throw new AccessDeniedException("Tu cuenta está inactiva. Contacta al administrador del sistema.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas. Verifica tu email y contraseña.");
        }

        user.setUltimoLogin(LocalDateTime.now());
        if (ip != null && !ip.isBlank()) user.setUltimoIp(ip.length() > 45 ? ip.substring(0, 45) : ip);
        userRepository.save(user);

        return respuesta(user);
    }

    @Transactional
    public LoginResponseDTO refrescar(String refreshToken) {
        RefreshToken guardado = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new BadCredentialsException("La sesión ya no es válida. Vuelve a iniciar sesión."));
        if (guardado.isRevocado() || guardado.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("La sesión expiró. Vuelve a iniciar sesión.");
        }
        User user = userRepository.findById(guardado.getUserId())
                .orElseThrow(() -> new BadCredentialsException("La sesión ya no es válida. Vuelve a iniciar sesión."));
        int version = user.getSesionVersion() == null ? 0 : user.getSesionVersion();
        if (!user.getActivo() || version != guardado.getSesionVersion()) {
            guardado.setRevocado(true);
            throw new AccessDeniedException("La sesión fue cerrada. Vuelve a iniciar sesión.");
        }
        guardado.setRevocado(true);
        return respuesta(user);
    }

    /** Revoca solo el refresh de este navegador y su access token. Las demás sesiones siguen. */
    @Transactional
    public void cerrarSesion(String refreshToken, String accessToken) {
        tokenDenylist.revocar(accessToken);
        if (refreshToken == null || refreshToken.isBlank()) return;
        refreshTokenRepository.findByTokenHash(hash(refreshToken)).ifPresent(guardado -> {
            guardado.setRevocado(true);
            refreshTokenRepository.save(guardado);
        });
    }

    /** Tras cerrar las otras sesiones, este navegador recibe un refresh nuevo. */
    @Transactional
    public String reemplazarRefresh(User user) {
        refreshTokenRepository.revocarDe(user.getId());
        return emitirRefresh(user);
    }

    private LoginResponseDTO respuesta(User user) {
        String token = jwtUtil.generateToken(user);
        String refresh = emitirRefresh(user);
        return new LoginResponseDTO(
                token,
                user.getId(),
                user.getNombre() + " " + user.getApellido(),
                user.getEmail(),
                user.getRol().getNombre().name(),
                user.getTotpEnabled() != null && user.getTotpEnabled(),
                refresh
        );
    }

    private String emitirRefresh(User user) {
        String raw = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(raw))
                .sesionVersion(user.getSesionVersion() == null ? 0 : user.getSesionVersion())
                .expiresAt(LocalDateTime.now().plusSeconds(Math.max(refreshExpiration, 1000) / 1000))
                .revocado(false)
                .build();
        refreshTokenRepository.save(token);
        return raw;
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo proteger el token de refresco", e);
        }
    }

    // ✅ Método para validar si un usuario está activo
    public boolean isUserActive(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getActivo)
                .orElse(false);
    }

    // ✅ Método para validar si un email existe y está activo
    public boolean existsActiveUserByEmail(String email) {
        return userRepository.existsByEmailAndActivoTrue(email);
    }
}