package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.PagoCxPRequestDTO;
import com.policalum.erp.application.dto.response.CuentaPorPagarResponseDTO;
import com.policalum.erp.application.dto.response.ResumenCxPDashboardDTO;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.CuentaPorPagarService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.EstadoCxP;
import com.policalum.erp.domain.enums.EstadoPago;
import com.policalum.erp.domain.model.Compra;
import com.policalum.erp.domain.model.FinanzasCuentaPorPagar;
import com.policalum.erp.domain.model.PagoCuentaPorPagar;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CompraRepository;
import com.policalum.erp.infrastructure.repository.FinanzasCuentaPorPagarRepository;
import com.policalum.erp.infrastructure.repository.PagoCuentaPorPagarRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CuentaPorPagarServiceImpl implements CuentaPorPagarService {

    private final FinanzasCuentaPorPagarRepository cuentaRepository;
    private final CompraRepository compraRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PagoCuentaPorPagarRepository pagoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CuentaPorPagarResponseDTO> buscar(UUID sedeId, UUID proveedorId, EstadoCxP estado,
                                                  Boolean soloVencidas, Pageable pageable) {
        return cuentaRepository.search(sedeId, proveedorId, estado, soloVencidas, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaPorPagarResponseDTO obtener(UUID id) {
        return toDto(findById(id));
    }

    @Override
    public CuentaPorPagarResponseDTO registrarPago(UUID id, PagoCxPRequestDTO dto) {
        // Módulo 6: FOR UPDATE para que dos pagos simultáneos no superen el saldo
        FinanzasCuentaPorPagar cxp = cuentaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por pagar no encontrada: " + id));

        if (cxp.getEstado() == EstadoCxP.PAGADO) {
            throw new IllegalStateException("La cuenta por pagar ya está pagada en su totalidad");
        }

        BigDecimal montoPagado = cxp.getMontoPagado() != null ? cxp.getMontoPagado() : BigDecimal.ZERO;
        BigDecimal saldo = cxp.getMontoTotal().subtract(montoPagado);

        if (dto.monto().compareTo(saldo) > 0) {
            throw new ValidationException("El monto a pagar (S/ " + dto.monto() +
                    ") excede el saldo pendiente (S/ " + saldo + ")");
        }

        BigDecimal nuevoPagado = montoPagado.add(dto.monto());
        cxp.setMontoPagado(nuevoPagado);

        if (nuevoPagado.compareTo(cxp.getMontoTotal()) >= 0) {
            cxp.setEstado(EstadoCxP.PAGADO);
            cxp.setFechaPago(LocalDateTime.now());  // fecha del pago final (los abonos están en finanzas_pagos_cxp)
        } else {
            cxp.setEstado(EstadoCxP.PAGADO_PARCIAL);
        }

        User pagador = usuarioActual();
        LocalDateTime ahora = LocalDateTime.now();
        cxp.setMetodoPago(dto.metodoPago().name());
        cxp.setPagadoPor(pagador);

        FinanzasCuentaPorPagar guardada = cuentaRepository.save(cxp);

        // Módulo 6: cada abono queda con su fecha y método para el flujo de caja
        pagoRepository.save(PagoCuentaPorPagar.builder()
                .cuentaPorPagar(guardada)
                .monto(dto.monto())
                .metodoPago(dto.metodoPago())
                .referencia(dto.referencia())
                .observaciones(dto.observaciones())
                .fechaPago(ahora)
                .registradoPor(pagador)
                .build());

        // Sincronizar con la compra asociada
        if (cxp.getCompra() != null) {
            Compra compra = cxp.getCompra();
            compra.setEstadoPago(guardada.getEstado() == EstadoCxP.PAGADO
                    ? EstadoPago.PAGADO
                    : EstadoPago.PAGADO_PARCIAL);
            compraRepository.save(compra);
        }

        auditService.registrar("finanzas_cuentas_por_pagar", guardada.getId(),
                AccionAudit.UPDATE,
                Map.of("accion", "PAGO",
                        "monto", dto.monto(),
                        "metodoPago", dto.metodoPago().name(),
                        "nuevoEstado", guardada.getEstado().name(),
                        "referencia", dto.referencia() != null ? dto.referencia() : ""));

        log.info("Pago registrado en CxP {}: S/ {} - Estado: {}",
                guardada.getId(), dto.monto(), guardada.getEstado());

        return toDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaPorPagarResponseDTO obtenerPorCompra(UUID compraId) {
        FinanzasCuentaPorPagar cxp = cuentaRepository.findByCompra_Id(compraId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe cuenta por pagar para la compra: " + compraId));
        return toDto(cxp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagarResponseDTO> vencidas(UUID sedeId) {
        Pageable unpaged = Pageable.unpaged();
        return cuentaRepository.search(sedeId, null, null, true, unpaged)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagarResponseDTO> proximasAVencer(UUID sedeId, int dias) {
        LocalDate limite = LocalDate.now().plusDays(dias);
        Pageable unpaged = Pageable.unpaged();
        return cuentaRepository.search(sedeId, null, EstadoCxP.PENDIENTE, false, unpaged)
                .stream()
                .filter(c -> c.getFechaVencimiento() != null
                        && !c.getFechaVencimiento().isAfter(limite))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenCxPDashboardDTO dashboard(UUID sedeId) {
        Pageable unpaged = Pageable.unpaged();
        List<FinanzasCuentaPorPagar> todas = cuentaRepository
                .search(sedeId, null, null, null, unpaged).getContent();

        long pendientes = todas.stream().filter(c -> c.getEstado() == EstadoCxP.PENDIENTE).count();
        long parciales = todas.stream().filter(c -> c.getEstado() == EstadoCxP.PAGADO_PARCIAL).count();
        long pagadas = todas.stream().filter(c -> c.getEstado() == EstadoCxP.PAGADO).count();
        long vencidas = todas.stream()
                .filter(c -> c.getFechaVencimiento() != null
                        && c.getFechaVencimiento().isBefore(LocalDate.now())
                        && c.getEstado() != EstadoCxP.PAGADO)
                .count();

        BigDecimal totalPorPagar = todas.stream()
                .filter(c -> c.getEstado() != EstadoCxP.PAGADO)
                .map(c -> c.getMontoTotal().subtract(
                        c.getMontoPagado() != null ? c.getMontoPagado() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPagado = todas.stream()
                .map(c -> c.getMontoPagado() != null ? c.getMontoPagado() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVencido = todas.stream()
                .filter(c -> c.getFechaVencimiento() != null
                        && c.getFechaVencimiento().isBefore(LocalDate.now())
                        && c.getEstado() != EstadoCxP.PAGADO)
                .map(c -> c.getMontoTotal().subtract(
                        c.getMontoPagado() != null ? c.getMontoPagado() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumenCxPDashboardDTO(
                todas.size(), pendientes, parciales, pagadas, vencidas,
                totalPorPagar, totalPagado, totalVencido);
    }

    // ============ HELPERS ============

    private FinanzasCuentaPorPagar findById(UUID id) {
        return cuentaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cuenta por pagar no encontrada: " + id));
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }

    private CuentaPorPagarResponseDTO toDto(FinanzasCuentaPorPagar c) {
        BigDecimal pagado = c.getMontoPagado() != null ? c.getMontoPagado() : BigDecimal.ZERO;
        BigDecimal saldo = c.getMontoTotal().subtract(pagado);

        boolean vencida = c.getFechaVencimiento() != null
                && c.getFechaVencimiento().isBefore(LocalDate.now())
                && c.getEstado() != EstadoCxP.PAGADO;

        return new CuentaPorPagarResponseDTO(
                c.getId(),
                c.getSede() != null ? c.getSede().getId() : null,
                c.getSede() != null ? c.getSede().getNombre() : null,
                c.getProveedor() != null ? c.getProveedor().getId() : null,
                c.getProveedor() != null ? c.getProveedor().getNombreRazon() : null,
                c.getProveedor() != null ? c.getProveedor().getNumeroDocumento() : null,
                c.getCompra() != null ? c.getCompra().getId() : null,
                c.getCompra() != null ? c.getCompra().getNumeroOrdenCompra() : null,
                c.getTipoReferencia(),
                c.getDescripcion(),
                c.getMontoTotal(),
                pagado,
                saldo,
                c.getEstado(),
                c.getFechaVencimiento(),
                vencida,
                c.getFechaPago(),
                c.getMetodoPago(),
                c.getPagadoPor() != null ? c.getPagadoPor().getId() : null,
                c.getPagadoPor() != null
                        ? c.getPagadoPor().getNombre() + " " + c.getPagadoPor().getApellido()
                        : null,
                c.getCreatedAt()
        );
    }
}