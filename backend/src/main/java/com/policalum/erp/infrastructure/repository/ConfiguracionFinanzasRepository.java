package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.ConfiguracionFinanzas;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Tabla de una sola fila (id = {@link ConfiguracionFinanzas#ID_UNICO}). */
@Repository
public interface ConfiguracionFinanzasRepository extends JpaRepository<ConfiguracionFinanzas, Long> {

    /** Bloquea la fila (FOR UPDATE) para serializar ediciones concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConfiguracionFinanzas c WHERE c.id = :id")
    Optional<ConfiguracionFinanzas> findParaActualizar(@Param("id") Long id);
}
