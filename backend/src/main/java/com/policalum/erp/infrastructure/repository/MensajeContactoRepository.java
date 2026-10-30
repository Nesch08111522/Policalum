package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.enums.EstadoMensajeWeb;
import com.policalum.erp.domain.model.MensajeContacto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeContactoRepository extends JpaRepository<MensajeContacto, UUID> {
    Page<MensajeContacto> findByEstadoOrderByCreatedAtDesc(EstadoMensajeWeb estado, Pageable pageable);
    Page<MensajeContacto> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByEstado(EstadoMensajeWeb estado);
}
