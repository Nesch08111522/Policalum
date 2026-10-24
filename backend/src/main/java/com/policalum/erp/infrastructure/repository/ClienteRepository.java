package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    // ============ CONSULTAS BÁSICAS Y DE VALIDACIÓN ============

    /**
     * Busca cliente por número de documento (único)
     * Útil para validaciones de duplicados y búsquedas rápidas
     */
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    /**
     * Verifica si existe un cliente con ese número de documento
     * Para validaciones antes de crear/actualizar
     */
    boolean existsByNumeroDocumento(String numeroDocumento);

    /**
     * Verifica si existe un cliente con ese email
     * Para validaciones de unicidad de email
     */
    boolean existsByEmail(String email);

    /**
     * Busca cliente por email (para autenticación o comunicación)
     */
    Optional<Cliente> findByEmail(String email);

    // ============ CONSULTAS DE BÚSQUEDA Y FILTRADO ============

    /**
     * Búsqueda flexible por nombre o número de documento
     * Ideal para autocompletado y búsquedas rápidas en CRM
     */
    @Query("SELECT c FROM Cliente c WHERE " +
            "(CAST(:searchTerm AS string) IS NULL OR " +
            "LOWER(COALESCE(c.nombre, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) OR " +
            "COALESCE(c.numeroDocumento, '') LIKE CONCAT('%', CAST(:searchTerm AS string), '%'))")
    Page<Cliente> searchByNombreOrDocumento(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Búsqueda por tipo de documento
     * Útil para filtrar clientes por tipo (DNI, RUC, etc.)
     */
    Page<Cliente> findByTipoDocumento(String tipoDocumento, Pageable pageable);

    /**
     * Obtiene clientes con número de documento específico (ignorando mayúsculas/minúsculas)
     * Para búsquedas más flexibles
     */
    @Query("SELECT c FROM Cliente c WHERE UPPER(c.numeroDocumento) = UPPER(:numeroDocumento)")
    Optional<Cliente> findByNumeroDocumentoIgnoreCase(@Param("numeroDocumento") String numeroDocumento);

    // ============ CONSULTAS PARA REPORTES Y ESTADÍSTICAS ============

    /**
     * Obtiene los 10 clientes más recientes
     * Para mostrar en dashboard de CRM
     */
    @Query("SELECT c FROM Cliente c ORDER BY c.createdAt DESC")
    List<Cliente> findTop10ByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Cuenta clientes registrados en un período específico
     * Para métricas de crecimiento
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Cuenta clientes por tipo de documento
     * Para análisis de segmentación
     */
    @Query("SELECT c.tipoDocumento, COUNT(c) FROM Cliente c GROUP BY c.tipoDocumento")
    List<Object[]> countGroupByTipoDocumento();

    // ============ CONSULTAS PARA MÓDULO DE VENTAS Y COTIZACIONES ============





    // ============ CONSULTAS PARA MÓDULO DE SOPORTE Y CRM ============

    /**
     * Obtiene clientes con datos incompletos (para campañas de completado de perfil)
     */
    @Query("SELECT c FROM Cliente c WHERE " +
            "c.email IS NULL OR c.celular IS NULL OR c.direccion IS NULL")
    List<Cliente> findClientesConDatosIncompletos();

    /**
     * Busca clientes por coincidencia parcial en dirección
     * Útil para rutas de entrega o zonificación
     */
    @Query("SELECT c FROM Cliente c WHERE CAST(:direccion AS string) IS NULL OR " +
            "LOWER(COALESCE(c.direccion, '')) LIKE LOWER(CONCAT('%', CAST(:direccion AS string), '%'))")
    List<Cliente> findByDireccionContainingIgnoreCase(@Param("direccion") String direccion);

    // ============ CONSULTAS PARA MÓDULO DE PRODUCTOS (Policarbonato) ============

    /**
     * Busca clientes por tipo de documento y nombre (búsqueda combinada)
     * Para búsquedas más precisas
     */
    @Query("SELECT c FROM Cliente c WHERE " +
            "(CAST(:tipoDocumento AS string) IS NULL OR c.tipoDocumento = :tipoDocumento) AND " +
            "(CAST(:nombre AS string) IS NULL OR LOWER(COALESCE(c.nombre, '')) LIKE LOWER(CONCAT('%', CAST(:nombre AS string), '%')))")
    Page<Cliente> findClientesByTipoAndNombre(
            @Param("tipoDocumento") String tipoDocumento,
            @Param("nombre") String nombre,
            Pageable pageable);

    // ============ CONSULTAS PARA SEGURIDAD Y AUDITORÍA ============

    /**
     * Obtiene cliente por ID con sus relaciones (si es necesario)
     * Para evitar problemas de Lazy Loading
     */
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.ventas WHERE c.id = :id")
    Optional<Cliente> findByIdWithVentas(@Param("id") UUID id);



    /**
     * Busca clientes que NO han realizado compras en un período
     * Para campañas de reactivación
     */

    @Query("""
    SELECT c
    FROM Cliente c
    WHERE c.id NOT IN (
        SELECT DISTINCT c2.id
        FROM Cliente c2
        JOIN c2.ventas v
        WHERE v.createdAt >= CAST(:fechaDesde AS timestamp)
    )
""")
    List<Cliente> findClientesSinComprasDesde(
            @Param("fechaDesde") LocalDateTime fechaDesde
    );

    /**
     * Busca clientes que han realizado compras (tienen ventas asociadas)
     * Para campañas de fidelización y análisis de clientes activos
     * NOTA: Asumiendo que tienes relación con Ventas
     */
    @Query("""
    SELECT DISTINCT c
    FROM Cliente c
    JOIN c.ventas v
    WHERE v.createdAt >= CAST(:fechaDesde AS timestamp)
""")
    List<Cliente> findClientesConComprasDesde(
            @Param("fechaDesde") LocalDateTime fechaDesde
    );
}