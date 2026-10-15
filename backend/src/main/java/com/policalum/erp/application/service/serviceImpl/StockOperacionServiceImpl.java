package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.AjusteStockRequestDTO;
import com.policalum.erp.application.dto.request.KardexMovimientoRequestDTO;
import com.policalum.erp.application.dto.request.TransferenciaRequestDTO;
import com.policalum.erp.application.dto.request.TransferenciaStockRequestDTO;
import com.policalum.erp.application.dto.response.AjusteStockResponseDTO;
import com.policalum.erp.application.dto.response.KardexMovimientoResponseDTO;
import com.policalum.erp.application.dto.response.ParametrosInventarioDTO;
import com.policalum.erp.application.dto.response.SaldoKardexDTO;
import com.policalum.erp.application.dto.response.StockConsolidadoDTO;
import com.policalum.erp.application.dto.response.StockDashboardDTO;
import com.policalum.erp.application.dto.response.TransferenciaStockResponseDTO;
import com.policalum.erp.application.mapper.KardexConsultaMapper;
import com.policalum.erp.application.mapper.StockOperacionMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.application.service.CorrelativoService;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.application.service.StockOperacionService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.enums.TipoReferencia;
import com.policalum.erp.domain.model.AjusteStock;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.domain.model.Stock;
import com.policalum.erp.domain.model.TransferenciaStock;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.AjusteStockRepository;
import com.policalum.erp.infrastructure.repository.AlmacenRepository;
import com.policalum.erp.infrastructure.repository.KardexRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import com.policalum.erp.infrastructure.repository.StockRepository;
import com.policalum.erp.infrastructure.repository.TransferenciaStockRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.projection.MontoCantidadProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeProjection;
import com.policalum.erp.infrastructure.repository.projection.TotalPorTipoMovimientoProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StockOperacionServiceImpl implements StockOperacionService {

    static final int ALERTAS_DASHBOARD = 5;
    static final LocalDate FECHA_MIN = LocalDate.of(2000, 1, 1);
    static final LocalDate FECHA_MAX = LocalDate.of(2100, 1, 1);

    private final KardexService kardexService;
    private final ConfiguracionService configuracionService;
    private final StockRepository stockRepository;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final AjusteStockRepository ajusteRepository;
    private final TransferenciaStockRepository transferenciaRepository;
    private final KardexRepository kardexRepository;
    private final UserRepository userRepository;
    private final CorrelativoService correlativoService;
    private final StockOperacionMapper mapper;
    private final KardexConsultaMapper consultaMapper;
    private final AuditService auditService;

    // ===================== AJUSTES =====================

    @Override
    public AjusteStockResponseDTO registrarAjuste(AjusteStockRequestDTO r) {
        User actual = usuarioActual();
        if (r.cantidad().signum() == 0) throw new IllegalArgumentException("La cantidad del ajuste no puede ser cero");
        String motivo = texto(r.motivo());
        if (motivo == null) throw new IllegalArgumentException("El motivo del ajuste es obligatorio");

        Producto producto = producto(r.productoId());
        Almacen almacen = almacenActivo(r.almacenId());
        exigirAccesoSede(actual, almacen);

        // FOR UPDATE: nadie más mueve este stock mientras se valida y registra el ajuste
        Stock stock = stockRepository.findParaActualizar(almacen.getId(), producto.getId()).orElse(null);
        BigDecimal anterior = stock != null && stock.getCantidad() != null ? stock.getCantidad() : BigDecimal.ZERO;
        TipoMovimiento tipo = r.cantidad().signum() > 0 ? TipoMovimiento.ENTRADA : TipoMovimiento.SALIDA;
        BigDecimal cantidadAbs = r.cantidad().abs();
        if (tipo == TipoMovimiento.SALIDA && anterior.compareTo(cantidadAbs) < 0) {
            throw new IllegalArgumentException("El ajuste dejaría stock negativo: hay " + anterior.stripTrailingZeros().toPlainString()
                    + " " + nvl(producto.getUnidad()) + " en " + almacen.getNombre() + " y se quiere descontar "
                    + cantidadAbs.stripTrailingZeros().toPlainString());
        }

        BigDecimal costo = producto.getPrecioCompra() != null ? producto.getPrecioCompra() : BigDecimal.ZERO;
        BigDecimal valor = cantidadAbs.multiply(costo).setScale(2, RoundingMode.HALF_UP);
        String justificacion = texto(r.justificacion());
        // Ajustes por encima del umbral (S/) exigen justificación ampliada (Configuración > inventario).
        ParametrosInventarioDTO reglas = configuracionService.inventario();
        if (valor.compareTo(reglas.umbralJustificacionAjuste()) > 0
                && (justificacion == null || justificacion.length() < reglas.minCaracteresJustificacion())) {
            throw new IllegalArgumentException("El ajuste vale S/ " + valor.toPlainString() + " (más de S/ "
                    + reglas.umbralJustificacionAjuste().toPlainString() + "): escriba una justificación de al menos "
                    + reglas.minCaracteresJustificacion() + " caracteres");
        }

        AjusteStock ajuste = ajusteRepository.save(AjusteStock.builder()
                .codigo(codigo("AJ"))
                .producto(producto).almacen(almacen).tipoMovimiento(tipo).cantidad(r.cantidad())
                .stockAnterior(anterior).stockResultante(anterior.add(r.cantidad()))
                .costoUnitario(costo).valor(valor).motivo(motivo).justificacion(justificacion)
                .user(actual).build());

        KardexMovimientoResponseDTO mov = kardexService.registrarMovimiento(new KardexMovimientoRequestDTO(
                producto, almacen, ajuste.getId(), TipoReferencia.AJUSTE, tipo, cantidadAbs, BigDecimal.ZERO, actual,
                ajuste.getCodigo() + " - " + motivo));
        ajuste.setStockResultante(mov.stockResultante());
        ajuste.setKardexMovimiento(kardexRepository.getReferenceById(mov.id()));
        AjusteStock guardado = ajusteRepository.save(ajuste);

        Map<String, Object> d = new LinkedHashMap<>();
        d.put("codigo", guardado.getCodigo());
        d.put("productoId", producto.getId().toString());
        d.put("almacenId", almacen.getId().toString());
        d.put("cantidad", guardado.getCantidad());
        d.put("stockAnterior", guardado.getStockAnterior());
        d.put("stockResultante", guardado.getStockResultante());
        d.put("valor", guardado.getValor());
        d.put("motivo", motivo);
        if (justificacion != null) d.put("justificacion", justificacion);
        auditService.registrar("stock_ajustes", guardado.getId(), AccionAudit.INSERT, d);
        log.info("Ajuste {}: {} {} de {} en {} (S/ {})", guardado.getCodigo(), tipo, cantidadAbs, producto.getNombre(),
                almacen.getNombre(), valor);
        return visible(mapper.toAjusteDto(guardado), actual);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AjusteStockResponseDTO> buscarAjustes(UUID productoId, UUID almacenId, UUID sedeId, UUID userId, TipoMovimiento tipo,
                                                      LocalDate desde, LocalDate hasta, Pageable pageable) {
        User actual = usuarioActual();
        UUID sede = sedePermitida(actual, sedeId);
        LocalDate[] r = rango(desde, hasta);
        return ajusteRepository.buscar(productoId, almacenId, sede, userId, tipo, r[0].atStartOfDay(), r[1].plusDays(1).atStartOfDay(),
                sinOrden(pageable)).map(a -> visible(mapper.toAjusteDto(a), actual));
    }

    // ===================== TRANSFERENCIAS =====================

    @Override
    public TransferenciaStockResponseDTO transferir(TransferenciaStockRequestDTO r) {
        User actual = usuarioActual();
        if (r.almacenOrigenId().equals(r.almacenDestinoId())) {
            throw new IllegalArgumentException("El almacén de origen y destino no pueden ser el mismo");
        }
        Producto producto = producto(r.productoId());
        Almacen origen = almacenActivo(r.almacenOrigenId());
        Almacen destino = almacenActivo(r.almacenDestinoId());
        exigirAccesoSede(actual, origen);
        exigirAccesoSede(actual, destino);

        // Bloqueo en orden fijo (por id) para que dos transferencias cruzadas A→B y B→A no se bloqueen mutuamente
        Stock stockOrigen = null;
        for (UUID almacenId : ordenados(origen.getId(), destino.getId())) {
            Stock s = stockRepository.findParaActualizar(almacenId, producto.getId()).orElse(null);
            if (almacenId.equals(origen.getId())) stockOrigen = s;
        }
        BigDecimal disponible = stockOrigen != null && stockOrigen.getCantidad() != null ? stockOrigen.getCantidad() : BigDecimal.ZERO;
        if (disponible.compareTo(r.cantidad()) < 0) {
            throw new IllegalArgumentException("Stock insuficiente en " + origen.getNombre() + ": hay "
                    + disponible.stripTrailingZeros().toPlainString() + " " + nvl(producto.getUnidad()) + " y se quieren transferir "
                    + r.cantidad().stripTrailingZeros().toPlainString());
        }

        boolean entreSedes = !Objects.equals(sedeId(origen), sedeId(destino));
        String motivo = texto(r.motivo());
        TransferenciaStock t = transferenciaRepository.save(TransferenciaStock.builder()
                .codigo(codigo("TRF")).producto(producto).almacenOrigen(origen).almacenDestino(destino)
                .cantidad(r.cantidad()).entreSedes(entreSedes).motivo(motivo).referenciaDocumento(texto(r.referenciaDocumento()))
                .user(actual).build());

        String detalle = t.getCodigo() + (motivo != null ? " - " + motivo : "");
        List<KardexMovimientoResponseDTO> movs = kardexService.registrarTransferencia(new TransferenciaRequestDTO(
                producto.getId(), origen.getId(), destino.getId(), r.cantidad(), detalle, t.getReferenciaDocumento()), t.getId());
        t.setMovimientoSalida(kardexRepository.getReferenceById(movs.get(0).id()));
        t.setMovimientoEntrada(kardexRepository.getReferenceById(movs.get(1).id()));
        transferenciaRepository.save(t);

        Map<String, Object> d = new LinkedHashMap<>();
        d.put("codigo", t.getCodigo());
        d.put("productoId", producto.getId().toString());
        d.put("origenId", origen.getId().toString());
        d.put("destinoId", destino.getId().toString());
        d.put("cantidad", t.getCantidad());
        d.put("entreSedes", entreSedes);
        if (motivo != null) d.put("motivo", motivo);
        auditService.registrar("stock_transferencias", t.getId(), AccionAudit.INSERT, d);
        log.info("Transferencia {}: {} {} de {} a {}", t.getCodigo(), r.cantidad(), producto.getNombre(), origen.getNombre(), destino.getNombre());

        return mapper.toTransferenciaDto(transferenciaRepository.findDetalleById(t.getId())
                .orElseThrow(() -> new IllegalStateException("Transferencia recién creada no encontrada: " + t.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransferenciaStockResponseDTO> buscarTransferencias(UUID productoId, UUID almacenId, UUID sedeId, boolean soloEntreSedes,
                                                                    LocalDate desde, LocalDate hasta, Pageable pageable) {
        User actual = usuarioActual();
        UUID sede = sedePermitida(actual, sedeId);
        LocalDate[] r = rango(desde, hasta);
        return transferenciaRepository.buscar(productoId, almacenId, sede, soloEntreSedes, r[0].atStartOfDay(),
                r[1].plusDays(1).atStartOfDay(), sinOrden(pageable)).map(mapper::toTransferenciaDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferenciaStockResponseDTO obtenerTransferencia(UUID id) {
        User actual = usuarioActual();
        TransferenciaStock t = transferenciaRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transferencia no encontrada: " + id));
        if (!esAdmin(actual)) {
            UUID propia = sedeId(actual);
            if (!Objects.equals(propia, sedeId(t.getAlmacenOrigen())) && !Objects.equals(propia, sedeId(t.getAlmacenDestino()))) {
                throw new AccessDeniedException("La transferencia no involucra a su sede");
            }
        }
        return mapper.toTransferenciaDto(t);
    }

    // ===================== CONSULTAS =====================

    @Override
    @Transactional(readOnly = true)
    public Page<SaldoKardexDTO> bajoStock(UUID sedeId, UUID almacenId, Pageable pageable) {
        User actual = usuarioActual();
        UUID sede = sedePermitida(actual, sedeId);
        if (almacenId != null) {
            Almacen a = almacenRepository.findById(almacenId).orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado: " + almacenId));
            exigirAccesoSede(actual, a);
        }
        return stockRepository.buscarSaldos(null, almacenId, sede, null, true, sinOrden(pageable)).map(consultaMapper::toSaldoDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockConsolidadoDTO> consolidado(UUID sedeId, String busqueda, Pageable pageable) {
        User actual = usuarioActual();
        UUID sede = sedePermitida(actual, sedeId);
        String patron = busqueda == null || busqueda.isBlank() ? null : "%" + busqueda.trim().toLowerCase(Locale.ROOT) + "%";
        return stockRepository.consolidado(sede, patron, sinOrden(pageable)).map(mapper::toConsolidadoDto);
    }

    @Override
    @Transactional(readOnly = true)
    public StockDashboardDTO dashboard(UUID sedeId) {
        User actual = usuarioActual();
        UUID sede = sedePermitida(actual, sedeId);
        LocalDate hoy = LocalDate.now();
        LocalDateTime ini = hoy.atStartOfDay(), fin = hoy.plusDays(1).atStartOfDay();
        boolean admin = esAdmin(actual);

        long entradas = 0, salidas = 0;
        for (TotalPorTipoMovimientoProjection t : kardexRepository.conteoPorTipo(sede, ini, fin)) {
            long n = t.getMovimientos() != null ? t.getMovimientos() : 0L;
            if (t.getTipo() == TipoMovimiento.ENTRADA) entradas += n;
            else if (t.getTipo() == TipoMovimiento.SALIDA) salidas += n;
        }
        MontoCantidadProjection ajustes = ajusteRepository.resumen(sede, ini, fin);
        long cantAjustes = ajustes != null && ajustes.getCantidad() != null ? ajustes.getCantidad() : 0L;
        BigDecimal valorAjustes = ajustes != null && ajustes.getTotal() != null ? ajustes.getTotal().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        BigDecimal valorInventario = null;
        if (admin) {
            valorInventario = stockRepository.valorizadoPorSede(sede).stream().map(MontoPorSedeProjection::getTotal)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        }
        List<SaldoKardexDTO> alertas = stockRepository.buscarSaldos(null, null, sede, null, true, PageRequest.of(0, ALERTAS_DASHBOARD))
                .map(consultaMapper::toSaldoDto).getContent();

        return new StockDashboardDTO(sede, hoy, stockRepository.contarProductosConStock(sede), stockRepository.contarBajoMinimo(sede),
                stockRepository.contarAgotados(sede), valorInventario, entradas + salidas, entradas, salidas, cantAjustes,
                admin ? valorAjustes : null, transferenciaRepository.contar(sede, ini, fin), alertas);
    }

    // ===================== HELPERS =====================

    /** ADMIN: la sede pedida (o todas). MULTIFUNCIONAL: siempre la suya; pedir otra es 403. */
    UUID sedePermitida(User u, UUID sedeId) {
        if (esAdmin(u)) return sedeId;
        UUID propia = sedeId(u);
        if (sedeId != null && !sedeId.equals(propia)) throw new AccessDeniedException("Solo puede consultar su propia sede");
        return propia;
    }

    private void exigirAccesoSede(User u, Almacen a) {
        if (esAdmin(u)) return;
        if (!Objects.equals(sedeId(u), sedeId(a))) {
            throw new AccessDeniedException("El almacén " + a.getNombre() + " no pertenece a su sede");
        }
    }

    /** Oculta costos a quien no es ADMIN. */
    private AjusteStockResponseDTO visible(AjusteStockResponseDTO d, User u) {
        if (esAdmin(u)) return d;
        return new AjusteStockResponseDTO(d.id(), d.codigo(), d.productoId(), d.productoNombre(), d.codigoSku(), d.unidad(),
                d.almacenId(), d.almacenNombre(), d.sedeId(), d.sedeNombre(), d.tipoMovimiento(), d.cantidad(), d.stockAnterior(),
                d.stockResultante(), null, null, d.motivo(), d.justificacion(), d.kardexMovimientoId(), d.userId(), d.userNombre(),
                d.createdAt());
    }

    private Producto producto(UUID id) {
        return productoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private Almacen almacenActivo(UUID id) {
        Almacen a = almacenRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado: " + id));
        if (Boolean.FALSE.equals(a.getActivo())) throw new IllegalStateException("El almacén " + a.getNombre() + " está inactivo");
        return a;
    }

    private String codigo(String prefijo) {
        int anio = LocalDate.now().getYear();
        return String.format("%s-%d-%06d", prefijo, anio, correlativoService.siguiente(prefijo + "-" + anio));
    }

    static List<UUID> ordenados(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? List.of(a, b) : List.of(b, a);
    }

    private static LocalDate[] rango(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : FECHA_MIN;
        LocalDate h = hasta != null ? hasta : FECHA_MAX;
        if (d.isAfter(h)) throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        return new LocalDate[] {d, h};
    }

    private static Pageable sinOrden(Pageable p) {
        return p.isPaged() ? PageRequest.of(p.getPageNumber(), p.getPageSize()) : p;
    }

    private static UUID sedeId(Almacen a) {
        return a != null && a.getSede() != null ? a.getSede().getId() : null;
    }

    private static UUID sedeId(User u) {
        return u.getSede() != null ? u.getSede().getId() : null;
    }

    private static String texto(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static boolean esAdmin(User u) {
        return u.getRol() != null && u.getRol().getNombre() == RolNombre.ROLE_ADMIN;
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
