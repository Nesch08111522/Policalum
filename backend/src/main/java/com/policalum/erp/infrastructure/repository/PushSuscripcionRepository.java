package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.PushSuscripcion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSuscripcionRepository extends JpaRepository<PushSuscripcion, UUID> {
    List<PushSuscripcion> findByUsuarioId(UUID usuarioId);
    Optional<PushSuscripcion> findByEndpoint(String endpoint);
}
