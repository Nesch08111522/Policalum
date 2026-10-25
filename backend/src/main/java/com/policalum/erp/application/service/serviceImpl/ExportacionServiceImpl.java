package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.domain.model.Cliente;
import com.policalum.erp.domain.model.Compra;
import com.policalum.erp.domain.model.ConfiguracionEmpresa;
import com.policalum.erp.domain.model.GastoEmpresarial;
import com.policalum.erp.domain.model.KardexMovimiento;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.domain.model.RrhhNomina;
import com.policalum.erp.domain.model.Stock;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.domain.model.Venta;
import com.policalum.erp.infrastructure.export.ExcelEstilos;
import com.policalum.erp.infrastructure.export.PdfCorporativo;
import com.policalum.erp.infrastructure.export.ReporteSolicitud;
import com.policalum.erp.infrastructure.repository.ClienteRepository;
import com.policalum.erp.infrastructure.repository.CompraRepository;
import com.policalum.erp.infrastructure.repository.ConfiguracionEmpresaRepository;
import com.policalum.erp.infrastructure.repository.GastoEmpresarialRepository;
import com.policalum.erp.infrastructure.repository.KardexRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import com.policalum.erp.infrastructure.repository.RrhhNominaRepository;
import com.policalum.erp.infrastructure.repository.StockRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.VentaRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportacionServiceImpl {

    private static final int TOPE = 2000;

    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;
    private final KardexRepository kardexRepository;
    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final GastoEmpresarialRepository gastoRepository;
    private final RrhhNominaRepository nominaRepository;
    private final ConfiguracionEmpresaRepository empresaRepository;
    private final UserRepository userRepository;
    private final @Qualifier("reportes") Executor reportes;
    private final @Lazy ExportacionServiceImpl self;

    public byte[] generar(String modulo, String formato, String filtros) {
        String usuario = usuarioActual();
        try {
            return CompletableFuture.supplyAsync(() -> self.materializar(modulo, formato, filtros, usuario), reportes)
                    .get(45, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el reporte", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] materializar(String modulo, String formato, String filtros, String usuario) {
        ReporteSolicitud solicitud = armar(modulo, filtros, usuario);
        if ("pdf".equalsIgnoreCase(formato)) {
            return PdfCorporativo.generar(solicitud, empresa());
        }
        return ExcelEstilos.generar(solicitud);
    }

    private ReporteSolicitud armar(String modulo, String filtros, String usuario) {
        List<String> notas = new ArrayList<>();
        if (filtros != null && !filtros.isBlank()) notas.add(filtros);
        notas.add("Máximo " + TOPE + " filas, las más recientes.");
        return switch (modulo == null ? "" : modulo) {
            case "clientes" -> clientes(usuario, notas);
            case "productos" -> productos(usuario, notas);
            case "stock" -> stock(usuario, notas);
            case "kardex" -> kardex(usuario, notas);
            case "ventas" -> ventas(usuario, notas);
            case "compras" -> compras(usuario, notas);
            case "gastos" -> gastos(usuario, notas);
            case "rrhh" -> nominas(usuario, notas);
            default -> throw new IllegalArgumentException("Módulo de exportación no reconocido");
        };
    }

    private ReporteSolicitud clientes(String usuario, List<String> notas) {
        List<List<Object>> filas = new ArrayList<>();
        for (Cliente cliente : clienteRepository.findAll(PageRequest.of(0, TOPE, Sort.by(Sort.Direction.DESC, "createdAt")))) {
            filas.add(List.of(ReporteSolicitud.texto(cliente.getNombre()), ReporteSolicitud.texto(cliente.getTipoDocumento()),
                    ReporteSolicitud.texto(cliente.getNumeroDocumento()), ReporteSolicitud.texto(cliente.getCelular()),
                    ReporteSolicitud.texto(cliente.getEmail()), Boolean.TRUE.equals(cliente.getActivo()) ? "Sí" : "No",
                    ReporteSolicitud.fecha(cliente.getCreatedAt())));
        }
        return new ReporteSolicitud("Clientes", "Clientes", usuario, notas, List.of(
                ReporteSolicitud.Columna.texto("Nombre"), ReporteSolicitud.Columna.texto("Documento"),
                ReporteSolicitud.Columna.texto("Número"), ReporteSolicitud.Columna.texto("Celular"),
                ReporteSolicitud.Columna.texto("Correo"), ReporteSolicitud.Columna.texto("Activo"),
                ReporteSolicitud.Columna.fecha("Registro")), filas);
    }

    private ReporteSolicitud productos(String usuario, List<String> notas) {
        List<List<Object>> filas = new ArrayList<>();
        for (Producto producto : productoRepository.findAll(PageRequest.of(0, TOPE, Sort.by("nombre")))) {
            filas.add(List.of(ReporteSolicitud.texto(producto.getCodigoSku()), ReporteSolicitud.texto(producto.getNombre()),
                    ReporteSolicitud.texto(producto.getUnidad()), ReporteSolicitud.dinero(producto.getPrecioCompra()),
                    ReporteSolicitud.dinero(producto.getPrecioVentaBase()), Boolean.TRUE.equals(producto.getActivo()) ? "Sí" : "No"));
        }
        return new ReporteSolicitud("Catálogo de productos", "Productos", usuario, notas, List.of(
                ReporteSolicitud.Columna.texto("SKU"), ReporteSolicitud.Columna.texto("Producto"),
                ReporteSolicitud.Columna.texto("Unidad"), ReporteSolicitud.Columna.moneda("Precio compra"),
                ReporteSolicitud.Columna.moneda("Precio venta"), ReporteSolicitud.Columna.texto("Activo")), filas);
    }

    private ReporteSolicitud stock(String usuario, List<String> notas) {
        List<List<Object>> filas = new ArrayList<>();
        for (Stock stock : stockRepository.alertasDeStock()) {
            filas.add(List.of(ReporteSolicitud.texto(stock.getProducto().getNombre()), ReporteSolicitud.texto(stock.getAlmacen().getNombre()),
                    stock.getCantidad(), stock.getStockMinimo(), ReporteSolicitud.dinero(stock.getProducto().getPrecioCompra()),
                    ReporteSolicitud.dinero(stock.getProducto().getPrecioVentaBase())));
        }
        notas.add("Solo artículos en mínimo o crítico.");
        return new ReporteSolicitud("Alertas de stock", "Stock", usuario, notas, List.of(
                ReporteSolicitud.Columna.texto("Producto"), ReporteSolicitud.Columna.texto("Almacén"),
                ReporteSolicitud.Columna.texto("Cantidad"), ReporteSolicitud.Columna.texto("Mínimo"),
                ReporteSolicitud.Columna.moneda("Precio compra"), ReporteSolicitud.Columna.moneda("Precio venta")), filas);
    }

    private ReporteSolicitud kardex(String usuario, List<String> notas) {
        List<List<Object>> filas = new ArrayList<>();
        for (KardexMovimiento mov : kardexRepository.findAll(PageRequest.of(0, TOPE, Sort.by(Sort.Direction.DESC, "createdAt")))) {
            filas.add(List.of(ReporteSolicitud.fecha(mov.getCreatedAt()), ReporteSolicitud.texto(mov.getProducto().getNombre()),
                    ReporteSolicitud.texto(mov.getAlmacen().getNombre()), String.valueOf(mov.getTipoMovimiento()),
                    mov.getCantidadMovida(), mov.getStockResultante(),
                    ReporteSolicitud.dinero(mov.getCostoUnitario() != null ? mov.getCostoUnitario() : mov.getProducto().getPrecioCompra()),
                    ReporteSolicitud.dinero(mov.getPrecioVenta() != null ? mov.getPrecioVenta() : mov.getProducto().getPrecioVentaBase())));
        }
        return new ReporteSolicitud("Kardex valorizado", "Kardex", usuario, notas, List.of(
                ReporteSolicitud.Columna.fecha("Fecha"), ReporteSolicitud.Columna.texto("Producto"),
                ReporteSolicitud.Columna.texto("Almacén"), ReporteSolicitud.Columna.texto("Movimiento"),
                ReporteSolicitud.Columna.texto("Cantidad"), ReporteSolicitud.Columna.texto("Saldo"),
                ReporteSolicitud.Columna.moneda("Costo"), ReporteSolicitud.Columna.moneda("Precio venta")), filas);
    }

    private ReporteSolicitud ventas(String usuario, List<String> notas) {
        List<List<Object>> filas = new ArrayList<>();
        for (Venta venta : ventaRepository.findAll(PageRequest.of(0, TOPE, Sort.by(Sort.Direction.DESC, "createdAt")))) {
            filas.add(List.of(ReporteSolicitud.fecha(venta.getCreatedAt()), ReporteSolicitud.texto(venta.getSerieCorrelativo()),
                    venta.getCliente() == null ? "Público" : venta.getCliente().getNombre(),
                    String.valueOf(venta.getTipoComprobante()), String.valueOf(venta.getEstado()),
                    ReporteSolicitud.dinero(venta.getTotal())));
        }
        return new ReporteSolicitud("Ventas", "Ventas", usuario, notas, List.of(
                ReporteSolicitud.Columna.fecha("Fecha"), ReporteSolicitud.Columna.texto("Comprobante"),
                ReporteSolicitud.Columna.texto("Cliente"), ReporteSolicitud.Columna.texto("Tipo"),
                ReporteSolicitud.Columna.texto("Estado"), ReporteSolicitud.Columna.moneda("Total")), filas);
    }

    private ReporteSolicitud compras(String usuario, List<String> notas) {
        List<List<Object>> filas = new ArrayList<>();
        for (Compra compra : compraRepository.findAll(PageRequest.of(0, TOPE, Sort.by(Sort.Direction.DESC, "createdAt")))) {
            filas.add(List.of(ReporteSolicitud.fecha(compra.getCreatedAt()), ReporteSolicitud.texto(compra.getNumeroOrdenCompra()),
                    compra.getProveedor() == null ? "" : compra.getProveedor().getNombreRazon(),
                    String.valueOf(compra.getEstadoCompra()), ReporteSolicitud.dinero(compra.getTotal())));
        }
        return new ReporteSolicitud("Compras", "Compras", usuario, notas, List.of(
                ReporteSolicitud.Columna.fecha("Fecha"), ReporteSolicitud.Columna.texto("Orden"),
                ReporteSolicitud.Columna.texto("Proveedor"), ReporteSolicitud.Columna.texto("Estado"),
                ReporteSolicitud.Columna.moneda("Total")), filas);
    }

    private ReporteSolicitud gastos(String usuario, List<String> notas) {
        List<List<Object>> filas = new ArrayList<>();
        for (GastoEmpresarial gasto : gastoRepository.buscar(null, LocalDate.now().minusYears(2), LocalDate.now(), PageRequest.of(0, TOPE, Sort.by(Sort.Direction.DESC, "fecha")))) {
            filas.add(List.of(gasto.getFecha() == null ? null : gasto.getFecha().atStartOfDay(), String.valueOf(gasto.getTipo()),
                    gasto.getSede() == null ? "Todas" : gasto.getSede().getNombre(),
                    ReporteSolicitud.texto(gasto.getDescripcion()), ReporteSolicitud.dinero(gasto.getMonto())));
        }
        return new ReporteSolicitud("Gastos empresariales", "Finanzas", usuario, notas, List.of(
                ReporteSolicitud.Columna.fecha("Fecha"), ReporteSolicitud.Columna.texto("Tipo"),
                ReporteSolicitud.Columna.texto("Sede"), ReporteSolicitud.Columna.texto("Detalle"),
                ReporteSolicitud.Columna.moneda("Monto")), filas);
    }

    private ReporteSolicitud nominas(String usuario, List<String> notas) {
        List<List<Object>> filas = new ArrayList<>();
        for (RrhhNomina nomina : nominaRepository.findAll(PageRequest.of(0, TOPE, Sort.by(Sort.Direction.DESC, "createdAt")))) {
            User persona = nomina.getUser();
            String nombre = persona == null ? "" : ((persona.getNombre() == null ? "" : persona.getNombre()) + " " + (persona.getApellido() == null ? "" : persona.getApellido())).trim();
            filas.add(List.of(ReporteSolicitud.texto(nomina.getPeriodo()), nombre, String.valueOf(nomina.getEstado()),
                    ReporteSolicitud.dinero(nomina.getTotalNeto())));
        }
        return new ReporteSolicitud("Nóminas", "RRHH", usuario, notas, List.of(
                ReporteSolicitud.Columna.texto("Periodo"), ReporteSolicitud.Columna.texto("Persona"),
                ReporteSolicitud.Columna.texto("Estado"), ReporteSolicitud.Columna.moneda("Neto")), filas);
    }

    private PdfCorporativo.EmpresaPdf empresa() {
        ConfiguracionEmpresa empresa = empresaRepository.findById(ConfiguracionEmpresa.ID_UNICO).orElse(null);
        String nombre = empresa == null || empresa.getNombreComercial() == null ? "POLICALUM" : empresa.getNombreComercial();
        String razon = empresa == null || empresa.getRazonSocial() == null || empresa.getRazonSocial().isBlank() ? nombre : empresa.getRazonSocial();
        String ruc = empresa == null || empresa.getRuc() == null || empresa.getRuc().isBlank() ? "pendiente" : empresa.getRuc();
        String direccion = empresa == null || empresa.getDireccionFiscal() == null ? "" : empresa.getDireccionFiscal();
        return new PdfCorporativo.EmpresaPdf(nombre, razon, ruc, direccion, logo());
    }

    private byte[] logo() {
        for (String ruta : List.of("uploads/logo.png", "frontend/public/POLICALUM.png", "../frontend/public/POLICALUM.png")) {
            try {
                Path path = Path.of(ruta);
                if (Files.exists(path)) return Files.readAllBytes(path);
            } catch (Exception ignored) {
                // El PDF se emite igual sin imagen.
            }
        }
        return new byte[0];
    }

    private String usuarioActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return "";
        return userRepository.findByEmailAndActivoTrue(auth.getName())
                .map(user -> ((user.getNombre() == null ? "" : user.getNombre()) + " " + (user.getApellido() == null ? "" : user.getApellido())).trim())
                .orElse(auth.getName());
    }
}
