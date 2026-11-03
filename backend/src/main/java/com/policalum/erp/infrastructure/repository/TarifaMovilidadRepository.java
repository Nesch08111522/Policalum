package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.TarifaMovilidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarifaMovilidadRepository extends JpaRepository<TarifaMovilidad, UUID> {
    Optional<TarifaMovilidad> findByDistritoIgnoreCase(String distrito);
}
