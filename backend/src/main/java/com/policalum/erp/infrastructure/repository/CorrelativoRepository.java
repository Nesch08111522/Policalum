package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.Correlativo;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CorrelativoRepository extends JpaRepository<Correlativo, String> {

    /** Crea la clave en 0 si no existe. ON CONFLICT evita el choque de dos primeros usos simultáneos. */
    @Modifying
    @Query(value = "INSERT INTO correlativos (clave, ultimo, updated_at) VALUES (:clave, 0, now()) " +
            "ON CONFLICT (clave) DO NOTHING", nativeQuery = true)
    int crearSiNoExiste(@Param("clave") String clave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Correlativo c WHERE c.clave = :clave")
    Optional<Correlativo> findParaActualizar(@Param("clave") String clave);
}
