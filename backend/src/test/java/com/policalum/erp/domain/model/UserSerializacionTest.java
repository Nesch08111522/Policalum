package com.policalum.erp.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Módulo 10 (seguridad): el hash de la contraseña y el secreto TOTP nunca deben salir en el JSON. */
class UserSerializacionTest {

    @Test
    void serializar_noExponePasswordHashNiTotpSecret() {
        User u = User.builder().id(UUID.randomUUID()).nombre("Ana").apellido("Ruiz").email("ana@policalum.com")
                .passwordHash("$2a$10$hashSecreto").totpSecret("JBSWY3DPEHPK3PXP").build();

        String json = JsonMapper.builder().build().writeValueAsString(u);

        assertTrue(json.contains("ana@policalum.com"));
        assertFalse(json.contains("hashSecreto"));
        assertFalse(json.contains("JBSWY3DPEHPK3PXP"));
        assertFalse(json.contains("passwordHash"));
    }
}
