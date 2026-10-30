package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.ProyectoPortafolio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoPortafolioRepository extends JpaRepository<ProyectoPortafolio, UUID> {
    List<ProyectoPortafolio> findByPublicadoTrueOrderByOrdenAscTituloAsc();
    List<ProyectoPortafolio> findAllByOrderByOrdenAscTituloAsc();
}
