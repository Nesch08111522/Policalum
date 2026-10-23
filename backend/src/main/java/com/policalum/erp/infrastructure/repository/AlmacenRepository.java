package com.policalum.erp.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.policalum.erp.domain.model.Almacen;

@Repository
public interface AlmacenRepository extends JpaRepository<Almacen, UUID> {

    /**
     * Búsqueda paginada de almacenes.
     *
     * IMPORTANTE:
     * - Se elimina TRIM(:searchTerm) = '' para evitar btrim(bytea) en PostgreSQL.
     * - :searchTerm se castea a STRING dentro del JPQL para que PostgreSQL sepa que
     *   debe tratarlo como TEXT y no como BYTEA. Sin esto, cuando :searchTerm es null,
     *   PostgreSQL intenta aplicar lower()/like sobre un bytea y falla con:
     *       ERROR: no existe la función lower(bytea)
     * - El servicio normaliza "" a null antes de llamar a este método.
     */
    @EntityGraph(attributePaths = "sede")
    @Query("SELECT a FROM Almacen a WHERE "
            + "(:searchTerm IS NULL OR "
            + " LOWER(a.nombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) OR "
            + " LOWER(COALESCE(a.descripcion, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))) AND "
            + "(:sedeId IS NULL OR a.sede.id = :sedeId) AND "
            + "(:activo IS NULL OR a.activo = :activo)")
    Page<Almacen> search(@Param("searchTerm") String searchTerm,
                         @Param("sedeId") UUID sedeId,
                         @Param("activo") Boolean activo,
                         Pageable pageable);

    @EntityGraph(attributePaths = "sede")
    List<Almacen> findBySede_IdOrderByNombreAsc(UUID sedeId);

    @EntityGraph(attributePaths = "sede")
    Optional<Almacen> findWithSedeById(UUID id);

    boolean existsBySede_IdAndNombreIgnoreCase(UUID sedeId, String nombre);

    Optional<Almacen> findBySede_IdAndNombreIgnoreCase(UUID sedeId, String nombre);

    @Query("SELECT a FROM Almacen a WHERE LOWER(a.nombre) = LOWER(:nombre)")
    List<Almacen> buscarPorNombre(@Param("nombre") String nombre);

    long countByActivo(Boolean activo);

    long countBySede_Id(UUID sedeId);
}