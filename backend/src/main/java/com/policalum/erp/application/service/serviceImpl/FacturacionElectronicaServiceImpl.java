package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.EmpresaResponseDTO;
import com.policalum.erp.application.dto.response.FacturacionElectronicaPanelDTO;
import com.policalum.erp.application.dto.response.FacturacionElectronicaPanelDTO.ComprobanteSunatDTO;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.application.service.FacturacionElectronicaService;
import com.policalum.erp.application.service.SunatFacturacionClient;
import com.policalum.erp.domain.enums.EstadoSunat;
import com.policalum.erp.domain.enums.TipoComprobante;
import com.policalum.erp.domain.model.Cliente;
import com.policalum.erp.domain.model.Venta;
import com.policalum.erp.infrastructure.repository.VentaRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacturacionElectronicaServiceImpl implements FacturacionElectronicaService {

    private final VentaRepository ventaRepository;
    private final ConfiguracionService configuracionService;
    private final SunatFacturacionClient sunat;

    @Override
    public FacturacionElectronicaPanelDTO panel(UUID sedeId) {
        EmpresaResponseDTO empresa = configuracionService.obtenerEmpresa();
        String ruc = empresa.ruc() == null ? "" : empresa.ruc().trim();
        SunatFacturacionClient.EnlaceSunat enlace = sunat.enlace();
        Map<TipoComprobante, Long> porTipo = contarTipos(sedeId);
        Map<EstadoSunat, Long> porEstado = contarEstados(sedeId);
        List<ComprobanteSunatDTO> historial = ventaRepository.recientesParaSunat(sedeId, PageRequest.of(0, 20)).stream()
                .map(this::comprobante)
                .toList();
        return new FacturacionElectronicaPanelDTO(
                ruc,
                ruc.startsWith("20") && ruc.length() == 11,
                empresa.razonSocial(),
                enlace.codigo(),
                enlace.mensaje(),
                porTipo.getOrDefault(TipoComprobante.BOLETA, 0L),
                porTipo.getOrDefault(TipoComprobante.FACTURA, 0L),
                0L,
                porEstado.getOrDefault(EstadoSunat.ACEPTADO, 0L),
                0L,
                porEstado.getOrDefault(EstadoSunat.RECHAZADO, 0L),
                historial);
    }

    private Map<TipoComprobante, Long> contarTipos(UUID sedeId) {
        Map<TipoComprobante, Long> totales = new EnumMap<>(TipoComprobante.class);
        for (Object[] fila : ventaRepository.contarPorTipoComprobante(sedeId)) {
            totales.put((TipoComprobante) fila[0], (Long) fila[1]);
        }
        return totales;
    }

    private Map<EstadoSunat, Long> contarEstados(UUID sedeId) {
        Map<EstadoSunat, Long> totales = new EnumMap<>(EstadoSunat.class);
        for (Object[] fila : ventaRepository.contarPorEstadoSunat(sedeId)) {
            if (fila[0] != null) totales.put((EstadoSunat) fila[0], (Long) fila[1]);
        }
        return totales;
    }

    private ComprobanteSunatDTO comprobante(Venta venta) {
        Cliente cliente = venta.getCliente();
        String documento = cliente == null || cliente.getNumeroDocumento() == null ? "—" : cliente.getNumeroDocumento();
        EstadoSunat estado = venta.getEstadoSunat() == null ? EstadoSunat.PENDIENTE : venta.getEstadoSunat();
        return new ComprobanteSunatDTO(venta.getSerieCorrelativo(), documento, estado.name(), venta.getTotal());
    }
}
