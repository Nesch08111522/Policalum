package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.CancelarPreformaRequestDTO;
import com.policalum.erp.application.dto.request.ConfirmarPreformaRequestDTO;
import com.policalum.erp.application.dto.request.PreformaVentaRequestDTO;
import com.policalum.erp.application.dto.response.ConfirmacionPreformaDTO;
import com.policalum.erp.application.dto.response.PreformaDashboardDTO;
import com.policalum.erp.application.dto.response.PreformaListadoDTO;
import com.policalum.erp.application.dto.response.PreformaVentaResponseDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Preformas de venta: ABIERTA → CONFIRMADA (crea la Venta) | CANCELADA | EXPIRADA.
 * Una ABIERTA vencida se marca EXPIRADA al consultarla y, además, cada 5 minutos.
 */
public interface PreformaService {

    /** Crea una preforma ABIERTA (código PRE-AAAA-NNNNNN, vigencia por defecto 2 h). No requiere turno. */
    PreformaVentaResponseDTO crear(PreformaVentaRequestDTO request);

    /** Detalle; si venció, la marca EXPIRADA antes de devolverla. */
    PreformaVentaResponseDTO obtener(UUID id);

    /** Edita y recalcula precios. Solo ABIERTA no vencida; creador o ADMIN. */
    PreformaVentaResponseDTO actualizar(UUID id, PreformaVentaRequestDTO request);

    /** Elimina una preforma ABIERTA. Creador o ADMIN. */
    void eliminar(UUID id);

    /**
     * Convierte en venta con ventaService.registrar(), usando el turno ABIERTO de quien confirma
     * en la sede de la preforma (puede ser el cajero, no solo el creador).
     *
     * @throws IllegalStateException si expiró, no hay turno o los precios del catálogo cambiaron (409)
     */
    ConfirmacionPreformaDTO confirmar(UUID id, ConfirmarPreformaRequestDTO request);

    /** Cancela (motivo opcional). No toca stock ni caja. Creador o ADMIN. */
    PreformaVentaResponseDTO cancelar(UUID id, CancelarPreformaRequestDTO request);

    /** Preformas ABIERTAS vigentes del usuario actual, de la que vence antes a la que vence después. */
    List<PreformaListadoDTO> abiertasDelUsuario();

    /** Bandeja de EXPIRADAS: el ADMIN ve todas (filtro por sede); el resto, solo las suyas. */
    Page<PreformaListadoDTO> vencidas(UUID sedeId, Pageable pageable);

    /** KPIs del periodo (por defecto, mes en curso). Solo ADMIN. */
    PreformaDashboardDTO dashboard(UUID sedeId, LocalDate desde, LocalDate hasta);

    /** Uso interno (tarea programada): marca EXPIRADAS todas las ABIERTAS vencidas. */
    int expirarVencidas();
}
