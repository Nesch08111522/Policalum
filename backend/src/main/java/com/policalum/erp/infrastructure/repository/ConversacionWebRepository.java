package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.ConversacionWeb;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversacionWebRepository extends JpaRepository<ConversacionWeb, UUID> {
    Optional<ConversacionWeb> findByClave(String clave);
    List<ConversacionWeb> findAllByOrderByUltimoMensajeAtDesc();
    Optional<ConversacionWeb> findFirstByCorreoIgnoreCaseOrderByUltimoMensajeAtDesc(String correo);
    Optional<ConversacionWeb> findFirstByTelefonoOrderByUltimoMensajeAtDesc(String telefono);
    Optional<ConversacionWeb> findFirstByDispositivoOrderByUltimoMensajeAtDesc(String dispositivo);
    List<ConversacionWeb> findByClienteId(UUID clienteId);
}
