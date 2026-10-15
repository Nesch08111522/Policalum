package com.policalum.erp.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.Sede;

public record UserResponseDTO(UUID id,
                Rol rol,
                Sede sede,
                String nombreUsuario,
                String nombre,
                String apellido,
                String email,
                String celular,
                Boolean activo,
                LocalDateTime ultimoLogin,
                LocalDateTime createdAt) {
}
