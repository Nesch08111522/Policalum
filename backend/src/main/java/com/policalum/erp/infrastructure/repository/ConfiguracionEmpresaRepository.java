package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.ConfiguracionEmpresa;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Tabla de una sola fila (id = {@link ConfiguracionEmpresa#ID_UNICO}). */
@Repository
public interface ConfiguracionEmpresaRepository extends JpaRepository<ConfiguracionEmpresa, Long> {

    /** Bloquea la fila (FOR UPDATE) para serializar ediciones concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConfiguracionEmpresa c WHERE c.id = :id")
    Optional<ConfiguracionEmpresa> findParaActualizar(@Param("id") Long id);
}
