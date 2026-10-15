package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.ConfiguracionVentas;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Tabla de una sola fila (id = {@link ConfiguracionVentas#ID_UNICO}). */
@Repository
public interface ConfiguracionVentasRepository extends JpaRepository<ConfiguracionVentas, Long> {

    /** Bloquea la fila (FOR UPDATE) para serializar ediciones concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConfiguracionVentas c WHERE c.id = :id")
    Optional<ConfiguracionVentas> findParaActualizar(@Param("id") Long id);
}
