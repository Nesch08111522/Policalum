package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.CalcularComisionesRequestDTO;
import com.policalum.erp.application.dto.response.CalculoComisionesResponseDTO;
import com.policalum.erp.application.dto.response.ComisionResponseDTO;
import com.policalum.erp.domain.enums.EstadoComision;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Comisiones de vendedores: % del legajo × ventas COMPLETADAS sin IGV del periodo. */
public interface ComisionService {

    /**
     * Calcula (o recalcula si siguen CALCULADAS) las comisiones del periodo.
     * Las APROBADAS o LIQUIDADAS no se modifican. Solo ADMIN.
     */
    CalculoComisionesResponseDTO calcular(CalcularComisionesRequestDTO request);

    /** CALCULADA → APROBADA. Solo ADMIN. */
    ComisionResponseDTO aprobar(UUID id);

    /** Listado paginado con filtros opcionales. Solo ADMIN. */
    Page<ComisionResponseDTO> buscar(String periodo, UUID sedeId, EstadoComision estado, Pageable pageable);

    /** Comisiones de un empleado. El ADMIN ve cualquiera; un empleado solo las suyas. */
    Page<ComisionResponseDTO> porEmpleado(UUID userId, Pageable pageable);

    /** Todas las comisiones de un periodo "YYYY-MM". Solo ADMIN. */
    List<ComisionResponseDTO> porPeriodo(String periodo, UUID sedeId);
}
