package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.CotizacionResponseDTO;
import com.policalum.erp.application.dto.response.DetalleCotizacionResponseDTO;
import com.policalum.erp.application.service.CotizacionService;
import com.policalum.erp.domain.model.ConfiguracionEmpresa;
import com.policalum.erp.infrastructure.export.ExcelEstilos;
import com.policalum.erp.infrastructure.export.PdfCorporativo;
import com.policalum.erp.infrastructure.export.ReporteSolicitud;
import com.policalum.erp.infrastructure.repository.ConfiguracionEmpresaRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CotizacionDocumentoService {

    private final CotizacionService cotizacionService;
    private final ConfiguracionEmpresaRepository empresaRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargar(UUID id, String formato) {
        CotizacionResponseDTO cotizacion = cotizacionService.findById(id);
        boolean pdf = "pdf".equalsIgnoreCase(formato);
        ReporteSolicitud solicitud = armar(cotizacion);
        byte[] cuerpo = pdf ? PdfCorporativo.generar(solicitud, empresa()) : ExcelEstilos.generar(solicitud);
        String nombre = "cotizacion-" + id.toString().substring(0, 8) + (pdf ? ".pdf" : ".xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.parseMediaType(pdf ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    private ReporteSolicitud armar(CotizacionResponseDTO cotizacion) {
        List<String> filtros = List.of(
                "Cliente: " + (cotizacion.clienteNombre() == null ? "Público" : cotizacion.clienteNombre()),
                "Estado: " + cotizacion.estado(),
                "Vigencia: " + cotizacion.fechaVigencia(),
                "Sede: " + (cotizacion.sedeNombre() == null ? "" : cotizacion.sedeNombre()));
        List<List<Object>> filas = new ArrayList<>();
        if (cotizacion.lineas() != null) {
            for (DetalleCotizacionResponseDTO linea : cotizacion.lineas()) {
                String concepto = linea.productoNombre() != null ? linea.productoNombre()
                        : linea.servicioNombre() != null ? linea.servicioNombre()
                        : linea.fraccionNombre() != null ? linea.fraccionNombre() : "Ítem";
                filas.add(List.of(concepto, linea.cantidad(), ReporteSolicitud.dinero(linea.precioUnitarioConIgv()), ReporteSolicitud.dinero(linea.subtotal())));
            }
        }
        filas.add(List.of("Total", null, null, ReporteSolicitud.dinero(cotizacion.total())));
        return new ReporteSolicitud("Cotización", "Cotizaciones", usuario(), filtros, List.of(
                ReporteSolicitud.Columna.texto("Concepto"),
                ReporteSolicitud.Columna.texto("Cantidad"),
                ReporteSolicitud.Columna.moneda("Precio"),
                ReporteSolicitud.Columna.moneda("Subtotal")), filas);
    }

    private PdfCorporativo.EmpresaPdf empresa() {
        ConfiguracionEmpresa empresa = empresaRepository.findById(ConfiguracionEmpresa.ID_UNICO).orElse(null);
        String nombre = empresa == null || empresa.getNombreComercial() == null ? "POLICALUM" : empresa.getNombreComercial();
        String razon = empresa == null || empresa.getRazonSocial() == null || empresa.getRazonSocial().isBlank() ? nombre : empresa.getRazonSocial();
        String ruc = empresa == null || empresa.getRuc() == null || empresa.getRuc().isBlank() ? "pendiente" : empresa.getRuc();
        byte[] logo = new byte[0];
        for (String ruta : List.of("uploads/logo.png", "frontend/public/POLICALUM.png")) {
            try {
                Path path = Path.of(ruta);
                if (Files.exists(path)) { logo = Files.readAllBytes(path); break; }
            } catch (Exception ignored) {
                logo = new byte[0];
            }
        }
        return new PdfCorporativo.EmpresaPdf(nombre, razon, ruc, empresa == null ? "" : empresa.getDireccionFiscal(), logo);
    }

    private String usuario() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "";
        return userRepository.findByEmailAndActivoTrue(auth.getName())
                .map(user -> ((user.getNombre() == null ? "" : user.getNombre()) + " " + (user.getApellido() == null ? "" : user.getApellido())).trim())
                .orElse(auth.getName());
    }
}
