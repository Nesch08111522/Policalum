package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.*;
import com.policalum.erp.application.dto.response.CompraListadoDTO;
import com.policalum.erp.application.dto.response.CompraResponseDTO;
import com.policalum.erp.application.mapper.CompraMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.CompraService;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.application.service.support.RangosConsulta;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompraServiceImpl implements CompraService {

    private static final BigDecimal IGV_DIVISOR = new BigDecimal("1.18");
    private static final BigDecimal IGV_PORCENTAJE = new BigDecimal("18.00");

    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final FinanzasCuentaPorPagarRepository cuentaPorPagarRepository;
    private final ProveedorRepository proveedorRepository;
    private final SedeRepository sedeRepository;
    private final AlmacenRepository almacenRepository;
    private final ProductoRepository productoRepository;
    private final CatalogoFraccionRepository catalogoFraccionRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final KardexService kardexService;
    private final AuditService auditService;
    private final CompraMapper compraMapper;

    // ==================================================================
    // CREAR
    // ==================================================================
    @Override
    public CompraResponseDTO crear(CompraRequestDTO dto) {
        User user = usuarioActual();

        Proveedor proveedor = proveedorRepository.findById(dto.proveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        if (!Boolean.TRUE.equals(proveedor.getActivo())) {
            throw new ValidationException("El proveedor está inactivo");
        }

        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        if (dto.fechaEmision() != null && dto.fechaEmision().isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de emisión no puede ser futura");
        }

        Compra compra = Compra.builder()
                .numeroOrdenCompra(generarNumeroOrdenCompra())
                .proveedor(proveedor)
                .sede(sede)
                .user(user)
                .numeroFacturaProveedor(dto.numeroFacturaProveedor())
                .fechaEmision(dto.fechaEmision())
                .moneda(dto.moneda() != null ? dto.moneda() : Moneda.PEN)
                .observaciones(dto.observaciones())
                .estadoCompra(EstadoCompra.BORRADOR)
                .estadoRecepcion(EstadoRecepcion.PENDIENTE)
                .estadoPago(EstadoPago.PENDIENTE)
                .subtotalNeto(BigDecimal.ZERO)
                .totalIgv(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();

        // Procesar líneas
        BigDecimal[] acumulados = procesarLineas(compra, dto.detalles(), sede);
        compra.setSubtotalNeto(acumulados[0]);
        compra.setTotalIgv(acumulados[1]);
        compra.setTotal(acumulados[2]);

        // Fecha de vencimiento según condiciones del proveedor
        if (proveedor.getDiasCredito() != null && proveedor.getDiasCredito() > 0) {
            compra.setFechaVencimientoPago(dto.fechaEmision().plusDays(proveedor.getDiasCredito()));
        }

        Compra guardada = compraRepository.save(compra);

        // Auditoría
        auditService.registrar("compras", guardada.getId(), AccionAudit.INSERT,
                Map.of("numeroOrdenCompra", guardada.getNumeroOrdenCompra(),
                        "total", guardada.getTotal(),
                        "proveedorId", proveedor.getId().toString()));

        log.info("Compra creada: {} - Total: {}", guardada.getNumeroOrdenCompra(), guardada.getTotal());
        return compraMapper.toResponseDto(guardada);
    }

    // ==================================================================
    // ACTUALIZAR (solo BORRADOR)
    // ==================================================================
    @Override
    public CompraResponseDTO actualizar(UUID id, CompraRequestDTO dto) {
        Compra compra = findCompra(id);

        if (compra.getEstadoCompra() != EstadoCompra.BORRADOR) {
            throw new IllegalStateException("Solo se pueden editar compras en estado BORRADOR");
        }

        Proveedor proveedor = proveedorRepository.findById(dto.proveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        if (!Boolean.TRUE.equals(proveedor.getActivo())) {
            throw new ValidationException("El proveedor está inactivo");
        }

        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        if (dto.fechaEmision() != null && dto.fechaEmision().isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de emisión no puede ser futura");
        }

        // Limpiar detalles viejos (orphanRemoval se encarga en flush)
        compra.getDetalles().clear();
        compraRepository.flush();

        compra.setProveedor(proveedor);
        compra.setSede(sede);
        compra.setNumeroFacturaProveedor(dto.numeroFacturaProveedor());
        compra.setFechaEmision(dto.fechaEmision());
        compra.setObservaciones(dto.observaciones());
        if (dto.moneda() != null) compra.setMoneda(dto.moneda());
        compra.setUpdatedAt(LocalDateTime.now());

        BigDecimal[] acumulados = procesarLineas(compra, dto.detalles(), sede);
        compra.setSubtotalNeto(acumulados[0]);
        compra.setTotalIgv(acumulados[1]);
        compra.setTotal(acumulados[2]);

        if (proveedor.getDiasCredito() != null && proveedor.getDiasCredito() > 0) {
            compra.setFechaVencimientoPago(dto.fechaEmision().plusDays(proveedor.getDiasCredito()));
        } else {
            compra.setFechaVencimientoPago(null);
        }

        Compra guardada = compraRepository.save(compra);

        auditService.registrar("compras", guardada.getId(), AccionAudit.UPDATE,
                Map.of("numeroOrdenCompra", guardada.getNumeroOrdenCompra(),
                        "total", guardada.getTotal()));

        return compraMapper.toResponseDto(guardada);
    }

    // ==================================================================
    // OBTENER
    // ==================================================================
    @Override
    @Transactional(readOnly = true)
    public CompraResponseDTO obtener(UUID id) {
        Compra compra = compraRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada"));
        return compraMapper.toResponseDto(compra);
    }

    // ==================================================================
    // BUSCAR
    // ==================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<CompraListadoDTO> buscar(UUID proveedorId, UUID sedeId, EstadoCompra estado,
                                         LocalDate desde, LocalDate hasta, Pageable pageable) {
        return compraRepository.search(proveedorId, sedeId, estado, RangosConsulta.desde(desde), RangosConsulta.hasta(hasta), pageable)
                .map(compraMapper::toListadoDto);
    }

    // ==================================================================
    // RECIBIR (dispara Kardex + Stock + Costo Promedio + CxP)
    // ==================================================================
    @Override
    public CompraResponseDTO recibir(UUID id, RecepcionCompraRequestDTO dto) {
        Compra compra = findCompra(id);

        if (compra.getEstadoCompra() != EstadoCompra.BORRADOR) {
            throw new IllegalStateException("Solo se pueden recibir compras en estado BORRADOR");
        }
        if (compra.getDetalles().isEmpty()) {
            throw new IllegalStateException("La compra no tiene detalles");
        }

        User user = usuarioActual();

        if (dto != null) {
            if (dto.numeroFacturaProveedor() != null) {
                compra.setNumeroFacturaProveedor(dto.numeroFacturaProveedor());
            }
            if (dto.observaciones() != null) {
                compra.setObservaciones(dto.observaciones());
            }
        }

        // Procesar cada detalle
        for (DetalleCompra detalle : compra.getDetalles()) {
            // 1. Cantidad real a mover en Kardex
            BigDecimal cantidadKardex = detalle.getCantidad();
            if (detalle.getFraccion() != null) {
                cantidadKardex = cantidadKardex.multiply(detalle.getFraccion().getFactorDescuentoKardex());
            }

            // 2. Movimiento de Kardex (ENTRADA)
            kardexService.registrarMovimiento(new KardexMovimientoRequestDTO(
                    detalle.getProducto(),
                    detalle.getAlmacen(),
                    compra.getId(),
                    TipoReferencia.COMPRA,
                    TipoMovimiento.ENTRADA,
                    cantidadKardex,
                    BigDecimal.ZERO,
                    user,
                    "Entrada por compra " + compra.getNumeroOrdenCompra()
            ));

            // 3. Marcar cantidad recibida
            detalle.setCantidadRecibida(detalle.getCantidad());

            // 4. Actualizar costo promedio ponderado del producto
            actualizarCostoPromedioPonderado(detalle, cantidadKardex);

            detalleCompraRepository.save(detalle);
        }

        // 5. Crear Cuenta por Pagar automática
        FinanzasCuentaPorPagar cxp = FinanzasCuentaPorPagar.builder()
                .sede(compra.getSede())
                .proveedor(compra.getProveedor())
                .compra(compra)
                .referenciaId(compra.getId())
                .tipoReferencia(TipoReferenciaCxP.COMPRA)
                .descripcion("Compra " + compra.getNumeroOrdenCompra()
                        + " - " + compra.getProveedor().getNombreRazon())
                .montoTotal(compra.getTotal())
                .montoPagado(BigDecimal.ZERO)
                .estado(EstadoCxP.PENDIENTE)
                .fechaVencimiento(compra.getFechaVencimientoPago())
                .build();
        cuentaPorPagarRepository.save(cxp);

        // 6. Actualizar estados
        compra.setEstadoCompra(EstadoCompra.RECIBIDA);
        compra.setEstadoRecepcion(EstadoRecepcion.RECIBIDO);
        compra.setEstadoPago(EstadoPago.PENDIENTE);
        compra.setUpdatedAt(LocalDateTime.now());

        Compra guardada = compraRepository.save(compra);

        auditService.registrar("compras", guardada.getId(), AccionAudit.UPDATE,
                Map.of("accion", "RECEPCION",
                        "numeroOrdenCompra", guardada.getNumeroOrdenCompra(),
                        "cxpId", cxp.getId().toString()));

        log.info("Compra recibida: {} - CxP generada: {}", guardada.getNumeroOrdenCompra(), cxp.getId());
        return compraMapper.toResponseDto(guardada);
    }

    // ==================================================================
    // ANULAR
    // ==================================================================
    @Override
    public CompraResponseDTO anular(UUID id, AnularCompraRequestDTO dto) {
        Compra compra = findCompra(id);

        if (compra.getEstadoCompra() == EstadoCompra.ANULADA) {
            throw new IllegalStateException("La compra ya está anulada");
        }
        if (compra.getEstadoCompra() == EstadoCompra.RECIBIDA) {
            throw new IllegalStateException(
                    "No se puede anular una compra ya recibida. Genere una devolución al proveedor.");
        }

        User user = usuarioActual();
        compra.setEstadoCompra(EstadoCompra.ANULADA);
        compra.setAnuladoPor(user);
        compra.setFechaAnulacion(LocalDateTime.now());
        compra.setMotivoAnulacion(dto.motivo());
        compra.setUpdatedAt(LocalDateTime.now());

        Compra guardada = compraRepository.save(compra);

        auditService.registrar("compras", guardada.getId(), AccionAudit.UPDATE,
                Map.of("accion", "ANULACION",
                        "numeroOrdenCompra", guardada.getNumeroOrdenCompra(),
                        "motivo", dto.motivo()));

        return compraMapper.toResponseDto(guardada);
    }

    // ==================================================================
    // ELIMINAR (solo BORRADOR)
    // ==================================================================
    @Override
    public void eliminar(UUID id) {
        Compra compra = findCompra(id);
        if (compra.getEstadoCompra() != EstadoCompra.BORRADOR) {
            throw new IllegalStateException("Solo se pueden eliminar compras en estado BORRADOR");
        }
        compraRepository.delete(compra);
        auditService.registrar("compras", id, AccionAudit.DELETE,
                Map.of("numeroOrdenCompra", compra.getNumeroOrdenCompra()));
    }

    // ==================================================================
    // DASHBOARD
    // ==================================================================
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCompras", compraRepository.count());
        stats.put("comprasBorrador", compraRepository.countByEstadoCompra(EstadoCompra.BORRADOR));
        stats.put("comprasRecibidas", compraRepository.countByEstadoCompra(EstadoCompra.RECIBIDA));
        stats.put("comprasAnuladas", compraRepository.countByEstadoCompra(EstadoCompra.ANULADA));
        stats.put("cxpPendientes", cuentaPorPagarRepository.countByEstado(EstadoCxP.PENDIENTE));
        stats.put("cxpPagadas", cuentaPorPagarRepository.countByEstado(EstadoCxP.PAGADO));
        return stats;
    }

    // ==================================================================
    // MÉTODOS PRIVADOS
    // ==================================================================

    private Compra findCompra(UUID id) {
        return compraRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con ID: " + id));
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }

    private String generarNumeroOrdenCompra() {
        int year = LocalDate.now().getYear();
        Integer max = compraRepository.findMaxCorrelativoByYear(year);
        int siguiente = (max == null ? 0 : max) + 1;
        return String.format("OC-%d-%04d", year, siguiente);
    }

    /**
     * Procesa las líneas del DTO, valida y calcula importes.
     * Retorna [subtotalNeto, totalIgv, total].
     */
    private BigDecimal[] procesarLineas(Compra compra, List<DetalleCompraCommandDTO> lineas, Sede sede) {
        BigDecimal subtotalNeto = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (DetalleCompraCommandDTO linea : lineas) {
            Producto producto = productoRepository.findById(linea.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado: " + linea.productoId()));
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new ValidationException("El producto " + producto.getNombre() + " está inactivo");
            }

            Almacen almacen = almacenRepository.findById(linea.almacenId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Almacén no encontrado: " + linea.almacenId()));
            if (!almacen.getSede().getId().equals(sede.getId())) {
                throw new ValidationException(
                        "El almacén " + almacen.getNombre() + " no pertenece a la sede indicada");
            }

            CatalogoFraccion fraccion = null;
            if (linea.fraccionId() != null) {
                fraccion = catalogoFraccionRepository.findWithProductoById(linea.fraccionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Fracción no encontrada"));
                if (!fraccion.getProducto().getId().equals(producto.getId())) {
                    throw new ValidationException(
                            "La fracción no pertenece al producto " + producto.getNombre());
                }
                if (!Boolean.TRUE.equals(fraccion.getActivo())) {
                    throw new ValidationException("La fracción está inactiva");
                }
            }

            BigDecimal precioConIgv = linea.precioUnitarioConIgv();
            BigDecimal cantidad = linea.cantidad();

            BigDecimal valorNetoUnitario = precioConIgv.divide(IGV_DIVISOR, 4, RoundingMode.HALF_UP);
            BigDecimal igvUnitario = precioConIgv.subtract(valorNetoUnitario)
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal igvLinea = igvUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = precioConIgv.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netoLinea = valorNetoUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);

            DetalleCompra detalle = DetalleCompra.builder()
                    .producto(producto)
                    .fraccion(fraccion)
                    .almacen(almacen)
                    .cantidad(cantidad)
                    .precioUnitarioConIgv(precioConIgv)
                    .valorUnitarioNeto(valorNetoUnitario)
                    .igvLinea(igvLinea)
                    .porcentajeIgv(IGV_PORCENTAJE)
                    .subtotal(subtotal)
                    .cantidadRecibida(BigDecimal.ZERO)
                    .build();

            compra.addDetalle(detalle);

            subtotalNeto = subtotalNeto.add(netoLinea);
            totalIgv = totalIgv.add(igvLinea);
            total = total.add(subtotal);
        }

        return new BigDecimal[]{
                subtotalNeto.setScale(2, RoundingMode.HALF_UP),
                totalIgv.setScale(2, RoundingMode.HALF_UP),
                total.setScale(2, RoundingMode.HALF_UP)
        };
    }

    /**
     * Actualiza el costo promedio ponderado del producto a nivel global
     * (sumando todos los stocks del producto).
     */
    private void actualizarCostoPromedioPonderado(DetalleCompra detalle, BigDecimal cantidadKardex) {
        Producto producto = detalle.getProducto();

        // Stock total actual del producto en todos los almacenes
        List<Stock> stocks = stockRepository.findByProducto_Id(producto.getId());
        BigDecimal stockTotal = stocks.stream()
                .map(s -> s.getCantidad() != null ? s.getCantidad() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // El costo actual del producto es "precioCompra" (representa el costo unitario actual)
        BigDecimal costoActual = producto.getPrecioCompra() != null
                ? producto.getPrecioCompra() : BigDecimal.ZERO;

        BigDecimal costoNuevo = detalle.getValorUnitarioNeto();

        // Si no hay stock previo, simplemente tomamos el nuevo costo
        BigDecimal costoPromedio;
        if (stockTotal.compareTo(BigDecimal.ZERO) <= 0) {
            costoPromedio = costoNuevo;
        } else {
            BigDecimal valorInventarioActual = stockTotal.multiply(costoActual);
            BigDecimal valorCompraNueva = cantidadKardex.multiply(costoNuevo);
            BigDecimal stockTotalNuevo = stockTotal.add(cantidadKardex);
            costoPromedio = valorInventarioActual.add(valorCompraNueva)
                    .divide(stockTotalNuevo, 2, RoundingMode.HALF_UP);
        }

        producto.setPrecioCompra(costoPromedio);
        productoRepository.save(producto);
    }
}