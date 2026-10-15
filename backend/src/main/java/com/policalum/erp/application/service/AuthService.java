// src/main/java/com/policalum/erp/application/service/AuthService.java
package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.LoginRequestDTO;
import com.policalum.erp.application.dto.response.LoginResponseDTO;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ✅ MÉTODO LOGIN
    public LoginResponseDTO login(LoginRequestDTO request) {
        // 1. Buscar usuario por email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("❌ Credenciales inválidas. Verifica tu email y contraseña."));

        // 2. ✅ VERIFICAR QUE ESTÉ ACTIVO
        if (!user.getActivo()) {
            throw new RuntimeException("❌ Tu cuenta está INACTIVA. Por favor, contacta al administrador del sistema.");
        }

        // 3. Verificar contraseña
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("❌ Credenciales inválidas. Verifica tu email y contraseña.");
        }

        // 4. Generar token JWT
        String token = jwtUtil.generateToken(user);

        // 5. Retornar respuesta exitosa
        return new LoginResponseDTO(
                token,
                user.getId(),
                user.getNombre() + " " + user.getApellido(),
                user.getEmail(),
                user.getRol().getNombre().name(),
                user.getTotpEnabled() != null && user.getTotpEnabled()
        );
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