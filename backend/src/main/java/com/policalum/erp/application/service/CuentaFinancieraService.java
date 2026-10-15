package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.CuentaFinancieraRequestDTO;
import com.policalum.erp.application.dto.request.RegistrarSaldoCuentaRequestDTO;
import com.policalum.erp.application.dto.response.CuentaFinancieraResponseDTO;
import com.policalum.erp.application.dto.response.SaldoCuentaResponseDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Cuentas de la empresa (cajas, bancos, billeteras) y sus saldos de corte. Solo ADMIN. */
public interface CuentaFinancieraService {

    /**
     * Crea una cuenta. 409 si alguno de sus métodos de pago ya está asignado a otra cuenta
     * activa que cubra la misma sede.
     */
    CuentaFinancieraResponseDTO crear(CuentaFinancieraRequestDTO request);

    /** Actualiza datos y métodos de pago (misma validación de conflictos que crear). */
    CuentaFinancieraResponseDTO actualizar(UUID id, CuentaFinancieraRequestDTO request);

    /** Detalle con su último saldo registrado. */
    CuentaFinancieraResponseDTO obtener(UUID id);

    /**
     * Lista cuentas. sedeId filtra las de esa sede (las corporativas no se incluyen);
     * incluirInactivas=false devuelve solo activas.
     */
    List<CuentaFinancieraResponseDTO> listar(UUID sedeId, boolean incluirInactivas);

    /** Desactiva la cuenta (se conserva el historial de saldos). Libera sus métodos de pago. */
    void desactivar(UUID id);

    /** Registra el saldo real de la cuenta a una fecha (corte). Solo cuentas activas. */
    SaldoCuentaResponseDTO registrarSaldo(UUID cuentaId, RegistrarSaldoCuentaRequestDTO request);

    /** Historial de cortes, del más reciente al más antiguo. */
    Page<SaldoCuentaResponseDTO> historialSaldos(UUID cuentaId, Pageable pageable);
}
