package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.enums.TipoComprobante;
import com.policalum.erp.domain.model.SerieComprobante;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SerieComprobanteRepository extends JpaRepository<SerieComprobante, UUID> {

    /** Series con su sede (y quien actualizó) cargadas; sedeId null = todas. Orden: sede, tipo. */
    @Query("SELECT s FROM SerieComprobante s JOIN FETCH s.sede se LEFT JOIN FETCH s.actualizadoPor " +
            "WHERE (CAST(:sedeId AS string) IS NULL OR se.id = :sedeId) ORDER BY se.nombre, s.tipoComprobante")
    List<SerieComprobante> listar(@Param("sedeId") UUID sedeId);

    /** Serie de una sede y tipo, bloqueada para edición. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SerieComprobante s WHERE s.sede.id = :sedeId AND s.tipoComprobante = :tipo")
    Optional<SerieComprobante> findParaActualizar(@Param("sedeId") UUID sedeId, @Param("tipo") TipoComprobante tipo);

    /** Serie por código (único en la empresa). */
    Optional<SerieComprobante> findBySerie(String serie);
}
