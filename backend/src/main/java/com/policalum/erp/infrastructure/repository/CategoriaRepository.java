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

import com.policalum.erp.domain.model.Categoria;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    /**
     * Búsqueda paginada de categorías.
     *
     * IMPORTANTE:
     * - Se elimina el TRIM(:searchTerm) = '' que provocaba btrim(bytea) en PostgreSQL.
     * - El parámetro :searchTerm se castea a STRING dentro del JPQL para que PostgreSQL
     *   sepa que debe tratarlo como TEXT y no como BYTEA. Sin esto, cuando :searchTerm
     *   es null, PostgreSQL intenta aplicar lower()/like sobre un bytea y falla con:
     *       ERROR: no existe la función lower(bytea)
     *       ERROR: no existe la función pg_catalog.btrim(bytea)
     * - El servicio normaliza "" a null antes de llamar a este método.
     */
    @Query("SELECT c FROM Categoria c WHERE "
            + "(:searchTerm IS NULL OR "
            + " LOWER(c.nombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) OR "
            + " LOWER(COALESCE(c.slug, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))) AND "
            + "(:activa IS NULL OR c.activa = :activa)")
    Page<Categoria> search(@Param("searchTerm") String searchTerm,
                           @Param("activa") Boolean activa,
                           Pageable pageable);

    @EntityGraph(attributePaths = "parent")
    Optional<Categoria> findWithParentById(UUID id);

    List<Categoria> findByParent_IdOrderByNombreAsc(UUID parentId);

    List<Categoria> findByParentIsNullOrderByNombreAsc();

    Optional<Categoria> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    long countByActiva(Boolean activa);
}