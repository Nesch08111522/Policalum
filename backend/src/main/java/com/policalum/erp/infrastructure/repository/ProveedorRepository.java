package com.policalum.erp.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.policalum.erp.domain.model.Proveedor;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, UUID> {

    Optional<Proveedor> findByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

    @Query("SELECT p FROM Proveedor p WHERE " +
            "(CAST(:searchTerm AS string) IS NULL OR " +
            "LOWER(COALESCE(p.nombreRazon, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) OR " +
            "LOWER(COALESCE(p.numeroDocumento, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')))")
    Page<Proveedor> searchByNombreOrDocumento(@Param("searchTerm") String searchTerm, Pageable pageable);

    Page<Proveedor> findByActivo(Boolean activo, Pageable pageable);

    @Query("SELECT p FROM Proveedor p WHERE " +
            "p.email IS NULL OR TRIM(p.email) = '' OR " +
            "p.celular IS NULL OR TRIM(p.celular) = ''")
    List<Proveedor> findWithIncompleteContactData();

    @Query("SELECT p.tipoDocumento, COUNT(p) FROM Proveedor p GROUP BY p.tipoDocumento ORDER BY COUNT(p) DESC")
    List<Object[]> countGroupByTipoDocumento();

    long countByActivo(Boolean activo);

    /**
     * El proveedor figura en compras, productos o cuentas por pagar.
     * En ese caso no se borra: se desactiva para conservar el historial.
     */
    @Query("""
            SELECT CASE WHEN
                EXISTS (SELECT 1 FROM Compra c WHERE c.proveedor.id = :id)
                OR EXISTS (SELECT 1 FROM Producto p WHERE p.proveedor.id = :id)
                OR EXISTS (SELECT 1 FROM FinanzasCuentaPorPagar x WHERE x.proveedor.id = :id)
            THEN TRUE ELSE FALSE END
            """)
    boolean tieneHistorial(@Param("id") UUID id);
}