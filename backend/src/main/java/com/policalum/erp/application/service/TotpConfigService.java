// src/main/java/com/policalum/erp/application/service/TotpConfigService.java
package com.policalum.erp.application.service;

import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TotpConfigService {

    private final UserRepository userRepository;
    private final TotpService totpService;

    /**
     * Configura TOTP para un empleado
     * Genera un secreto y lo guarda en la BD
     */
    @Transactional
    public String configurarTOTP(UUID empleadoId) {
        User empleado = userRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        // Generar nuevo secreto
        String secret = totpService.generateSecret();
        empleado.setTotpSecret(secret);
        empleado.setTotpEnabled(true);
        userRepository.save(empleado);

        log.info("✅ TOTP configurado para empleado: {}", empleadoId);
        return secret;
    }

    /**
     * Verifica si un empleado tiene TOTP configurado
     */
    public boolean tieneTOTPConfigurado(UUID empleadoId) {
        User empleado = userRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        return empleado.getTotpSecret() != null && !empleado.getTotpSecret().isEmpty();
    }

    /**
     * Desactiva TOTP para un empleado
     */
    @Transactional
    public void desactivarTOTP(UUID empleadoId) {
        User empleado = userRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        empleado.setTotpSecret(null);
        empleado.setTotpEnabled(false);
        userRepository.save(empleado);
        log.info("❌ TOTP desactivado para empleado: {}", empleadoId);
    }
}