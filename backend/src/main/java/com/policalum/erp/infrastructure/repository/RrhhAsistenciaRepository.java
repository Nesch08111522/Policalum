// src/main/java/com/policalum/erp/infraestructure/repository/RrhhAsistenciaRepository.java

package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.RrhhAsistencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RrhhAsistenciaRepository extends JpaRepository<RrhhAsistencia, UUID> {

    // ============================================
    // ✅ MÉTODOS EXISTENTES
    // ============================================

    @Query("SELECT COUNT(a) FROM RrhhAsistencia a " +
            "JOIN a.empleado u " +
            "WHERE u.id = :empleadoId " +
            "AND u.activo = true " +
            "AND a.fechaHora >= :inicioDia")
    long countByEmpleadoIdAndFechaHoraAfter(@Param("empleadoId") UUID empleadoId,
                                            @Param("inicioDia") LocalDateTime inicioDia);

    @Query(value = "SELECT a FROM RrhhAsistencia a JOIN FETCH a.empleado u WHERE u.id = :empleadoId",
            countQuery = "SELECT COUNT(a) FROM RrhhAsistencia a WHERE a.empleado.id = :empleadoId")
    Page<RrhhAsistencia> findByEmpleadoId(@Param("empleadoId") UUID empleadoId, Pageable pageable);

    @Query(value = "SELECT a FROM RrhhAsistencia a JOIN FETCH a.empleado u",
            countQuery = "SELECT COUNT(a) FROM RrhhAsistencia a")
    Page<RrhhAsistencia> findAllConEmpleados(Pageable pageable);

    @Query(value = "SELECT a FROM RrhhAsistencia a JOIN FETCH a.empleado u " +
            "WHERE a.fechaHora BETWEEN :inicio AND :fin",
            countQuery = "SELECT COUNT(a) FROM RrhhAsistencia a WHERE a.fechaHora BETWEEN :inicio AND :fin")
    Page<RrhhAsistencia> findByFechaHoraBetween(@Param("inicio") LocalDateTime inicio,
                                                @Param("fin") LocalDateTime fin,
                                                Pageable pageable);

    @Query(value = "SELECT a FROM RrhhAsistencia a JOIN FETCH a.empleado u " +
            "WHERE u.id = :empleadoId AND a.fechaHora BETWEEN :inicio AND :fin",
            countQuery = "SELECT COUNT(a) FROM RrhhAsistencia a WHERE a.empleado.id = :empleadoId " +
                    "AND a.fechaHora BETWEEN :inicio AND :fin")
    Page<RrhhAsistencia> findByEmpleadoIdAndFechaHoraBetween(@Param("empleadoId") UUID empleadoId,
                                                             @Param("inicio") LocalDateTime inicio,
                                                             @Param("fin") LocalDateTime fin,
                                                             Pageable pageable);

    // ============================================
    // ✅ MÉTODOS CORREGIDOS (USANDO RANGOS DE FECHA)
    // ============================================

    // ✅ Última marcación de un empleado
    @Query("SELECT a FROM RrhhAsistencia a WHERE a.empleado.id = :empleadoId " +
            "ORDER BY a.fechaHora DESC LIMIT 1")
    RrhhAsistencia findUltimaMarcacion(@Param("empleadoId") UUID empleadoId);

    // ✅ Última entrada del día
    @Query("SELECT a FROM RrhhAsistencia a WHERE a.empleado.id = :empleadoId " +
            "AND a.fechaHora >= :inicioDia AND a.fechaHora < :finDia " +
            "AND a.tipoMarcacion = 'ENTRADA' " +
            "ORDER BY a.fechaHora DESC LIMIT 1")
    RrhhAsistencia findUltimaEntradaDelDia(@Param("empleadoId") UUID empleadoId,
                                           @Param("inicioDia") LocalDateTime inicioDia,
                                           @Param("finDia") LocalDateTime finDia);

    // ✅ Última salida del día
    @Query("SELECT a FROM RrhhAsistencia a WHERE a.empleado.id = :empleadoId " +
            "AND a.fechaHora >= :inicioDia AND a.fechaHora < :finDia " +
            "AND a.tipoMarcacion = 'SALIDA' " +
            "ORDER BY a.fechaHora DESC LIMIT 1")
    RrhhAsistencia findUltimaSalidaDelDia(@Param("empleadoId") UUID empleadoId,
                                          @Param("inicioDia") LocalDateTime inicioDia,
                                          @Param("finDia") LocalDateTime finDia);

    // ✅ Verificar si marcó entrada hoy
    @Query("SELECT COUNT(a) > 0 FROM RrhhAsistencia a " +
            "WHERE a.empleado.id = :empleadoId " +
            "AND a.fechaHora >= :inicioDia AND a.fechaHora < :finDia " +
            "AND a.tipoMarcacion = 'ENTRADA'")
    boolean yaMarcoEntradaHoy(@Param("empleadoId") UUID empleadoId,
                              @Param("inicioDia") LocalDateTime inicioDia,
                              @Param("finDia") LocalDateTime finDia);

    // ✅ Verificar si marcó salida hoy
    @Query("SELECT COUNT(a) > 0 FROM RrhhAsistencia a " +
            "WHERE a.empleado.id = :empleadoId " +
            "AND a.fechaHora >= :inicioDia AND a.fechaHora < :finDia " +
            "AND a.tipoMarcacion = 'SALIDA'")
    boolean yaMarcoSalidaHoy(@Param("empleadoId") UUID empleadoId,
                             @Param("inicioDia") LocalDateTime inicioDia,
                             @Param("finDia") LocalDateTime finDia);

    // ============================================
    // ✅ MÉTODOS PARA DASHBOARD
    // ============================================

    // ✅ Dashboard: Contar presentes hoy
    @Query("SELECT COUNT(DISTINCT a.empleado.id) FROM RrhhAsistencia a " +
            "WHERE a.fechaHora >= :inicioDia AND a.fechaHora < :finDia " +
            "AND a.tipoMarcacion = 'ENTRADA'")
    long countPresentesHoy(@Param("inicioDia") LocalDateTime inicioDia,
                           @Param("finDia") LocalDateTime finDia);

    // ✅ Dashboard: Contar tardanzas hoy
    @Query("SELECT COUNT(DISTINCT a.empleado.id) FROM RrhhAsistencia a " +
            "WHERE a.fechaHora >= :inicioDia AND a.fechaHora < :finDia " +
            "AND a.tipoMarcacion = 'ENTRADA' " +
            "AND EXTRACT(HOUR FROM a.fechaHora) > 8")
    long countTardanzasHoy(@Param("inicioDia") LocalDateTime inicioDia,
                           @Param("finDia") LocalDateTime finDia);

    // ✅ Dashboard: Últimas 10 asistencias
    @Query("SELECT a FROM RrhhAsistencia a JOIN FETCH a.empleado u " +
            "ORDER BY a.fechaHora DESC LIMIT 10")
    List<RrhhAsistencia> findUltimasAsistencias();

    // ✅ Contar empleados activos
    @Query("SELECT COUNT(u) FROM User u WHERE u.activo = true")
    long countEmpleadosActivos();


}