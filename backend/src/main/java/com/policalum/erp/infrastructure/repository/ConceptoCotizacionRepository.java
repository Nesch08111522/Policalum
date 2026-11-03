package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.ConceptoCotizacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptoCotizacionRepository extends JpaRepository<ConceptoCotizacion, UUID> {
    List<ConceptoCotizacion> findAllByOrderByOrdenAscNombreAsc();
}
