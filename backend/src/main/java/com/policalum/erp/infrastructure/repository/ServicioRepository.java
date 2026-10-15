package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;
@Repository
public interface ServicioRepository extends JpaRepository<Servicio, UUID> {
    /**
     * Valida si existe un servicio con el mismo nombre para evitar duplicados.
     */
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Valida duplicidad de nombre excluyendo el ID actual (usado en actualizaciones).
     */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

    /**
     * Búsqueda paginada flexible por nombre/descripción y rango de precios.
     */
    @Query("SELECT s FROM Servicio s WHERE " +
            "(:searchTerm IS NULL OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(s.descripcion) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:activo IS NULL OR s.activo = :activo) AND " +
            "(:precioMin IS NULL OR s.precio >= :precioMin) AND " +
            "(:precioMax IS NULL OR s.precio <= :precioMax)")
    Page<Servicio> search(
            @Param("searchTerm") String searchTerm,
            @Param("activo") Boolean activo,
            @Param("precioMin") BigDecimal precioMin,
            @Param("precioMax") BigDecimal precioMax,
            Pageable pageable
    );

    /**
     * Métricas rápidas para paneles de control / facturación.
     */
    long countByActivoTrue();
}
