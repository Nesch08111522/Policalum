package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.FormatoCotizacionRequestDTO;
import com.policalum.erp.application.dto.response.CargosCotizacionDTO;
import com.policalum.erp.application.dto.response.FormatoCotizacionDTO;
import com.policalum.erp.application.service.support.CalculoCotizacion;
import com.policalum.erp.domain.enums.TipoCargoCotizacion;
import com.policalum.erp.domain.model.ConceptoCotizacion;
import com.policalum.erp.domain.model.ParametroCotizacion;
import com.policalum.erp.domain.model.TarifaMovilidad;
import com.policalum.erp.exception.BusinessException;
import com.policalum.erp.infrastructure.repository.ConceptoCotizacionRepository;
import com.policalum.erp.infrastructure.repository.ParametroCotizacionRepository;
import com.policalum.erp.infrastructure.repository.TarifaMovilidadRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FormatoCotizacionService {

    /** Valores de arranque. En Configuración se reemplazan sin tocar el código. */
    private static final List<TarifaSemilla> DISTRITOS = List.of(
            new TarifaSemilla("Trujillo", "20.00", 20, "1.00"),
            new TarifaSemilla("Víctor Larco Herrera", "25.00", 15, "1.20"),
            new TarifaSemilla("La Esperanza", "25.00", 15, "1.20"),
            new TarifaSemilla("El Porvenir", "25.00", 15, "1.20"),
            new TarifaSemilla("Florencia de Mora", "22.00", 15, "1.20"),
            new TarifaSemilla("Huanchaco", "40.00", 10, "2.00"),
            new TarifaSemilla("Moche", "35.00", 10, "1.80"),
            new TarifaSemilla("Laredo", "35.00", 10, "1.80"),
            new TarifaSemilla("Salaverry", "45.00", 10, "2.20"),
            new TarifaSemilla("Poroto", "55.00", 8, "2.50"),
            new TarifaSemilla("Simbal", "60.00", 8, "2.80")
    );

    private final ParametroCotizacionRepository parametros;
    private final TarifaMovilidadRepository tarifas;
    private final ConceptoCotizacionRepository conceptos;

    @Transactional
    public void asegurar() {
        if (parametros.findById(ParametroCotizacion.ID_UNICO).isEmpty()) {
            parametros.save(ParametroCotizacion.builder()
                    .id(ParametroCotizacion.ID_UNICO)
                    .utilidadPorcentaje(BigDecimal.ZERO.setScale(2))
                    .build());
        }
        for (TarifaSemilla semilla : DISTRITOS) {
            if (tarifas.findByDistritoIgnoreCase(semilla.distrito()).isEmpty()) {
                tarifas.save(TarifaMovilidad.builder()
                        .distrito(semilla.distrito())
                        .precioBase(new BigDecimal(semilla.base()))
                        .unidadesIncluidas(semilla.incluidas())
                        .precioUnidadExtra(new BigDecimal(semilla.extra()))
                        .activo(true)
                        .build());
            }
        }
    }

    @Transactional(readOnly = true)
    public FormatoCotizacionDTO actual(boolean soloActivos) {
        ParametroCotizacion parametro = parametro();
        List<FormatoCotizacionDTO.DistritoTarifaDTO> distritos = tarifas.findAll().stream()
                .filter(item -> !soloActivos || Boolean.TRUE.equals(item.getActivo()))
                .sorted(Comparator.comparing(TarifaMovilidad::getDistrito, String.CASE_INSENSITIVE_ORDER))
                .map(item -> new FormatoCotizacionDTO.DistritoTarifaDTO(
                        item.getId(), item.getDistrito(), item.getPrecioBase(), item.getUnidadesIncluidas(),
                        item.getPrecioUnidadExtra(), item.getActivo()))
                .toList();
        List<FormatoCotizacionDTO.ConceptoCargoDTO> cargos = conceptos.findAllByOrderByOrdenAscNombreAsc().stream()
                .filter(item -> !soloActivos || Boolean.TRUE.equals(item.getActivo()))
                .map(item -> new FormatoCotizacionDTO.ConceptoCargoDTO(
                        item.getId(), item.getNombre(), item.getTipo(), item.getValor(), item.getActivo()))
                .toList();
        return new FormatoCotizacionDTO(parametro.getUtilidadPorcentaje(), distritos, cargos);
    }

    @Transactional
    public FormatoCotizacionDTO guardar(FormatoCotizacionRequestDTO request) {
        ParametroCotizacion parametro = parametro();
        parametro.setUtilidadPorcentaje(CalculoCotizacion.dinero(request.utilidadPorcentaje()));
        parametros.save(parametro);
        for (FormatoCotizacionRequestDTO.Distrito pedido : request.distritos()) {
            String nombre = pedido.distrito().trim();
            TarifaMovilidad tarifa = tarifas.findByDistritoIgnoreCase(nombre).orElseGet(TarifaMovilidad::new);
            tarifa.setDistrito(nombre);
            tarifa.setPrecioBase(CalculoCotizacion.dinero(pedido.precioBase()));
            tarifa.setUnidadesIncluidas(pedido.unidadesIncluidas());
            tarifa.setPrecioUnidadExtra(CalculoCotizacion.dinero(pedido.precioUnidadExtra()));
            tarifa.setActivo(pedido.activo());
            tarifas.save(tarifa);
        }
        conceptos.deleteAll(conceptos.findAll());
        conceptos.flush();
        int orden = 0;
        if (request.conceptos() != null) {
            for (FormatoCotizacionRequestDTO.Concepto pedido : request.conceptos()) {
                if (pedido.tipo() == TipoCargoCotizacion.PORCENTAJE && pedido.valor().compareTo(new BigDecimal("100")) > 0) {
                    throw new BusinessException("Un concepto en porcentaje no puede pasar de 100");
                }
                conceptos.save(ConceptoCotizacion.builder()
                        .nombre(pedido.nombre().trim())
                        .tipo(pedido.tipo())
                        .valor(CalculoCotizacion.dinero(pedido.valor()))
                        .activo(pedido.activo())
                        .orden(orden++)
                        .build());
            }
        }
        return actual(false);
    }

    @Transactional(readOnly = true)
    public CargosCotizacionDTO calcular(BigDecimal subtotal, BigDecimal piezas, String distrito) {
        TarifaMovilidad tarifa = tarifas.findByDistritoIgnoreCase(distrito == null ? "" : distrito.trim())
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .orElseThrow(() -> new BusinessException("Elige un distrito de Trujillo con tarifa de movilidad"));
        BigDecimal materiales = CalculoCotizacion.dinero(subtotal);
        BigDecimal porcentaje = parametro().getUtilidadPorcentaje();
        BigDecimal utilidad = CalculoCotizacion.porcentaje(materiales, porcentaje);
        BigDecimal movilidad = CalculoCotizacion.movilidad(
                tarifa.getPrecioBase(), tarifa.getUnidadesIncluidas(), tarifa.getPrecioUnidadExtra(), piezas);
        List<String> lineas = new ArrayList<>();
        BigDecimal otros = BigDecimal.ZERO;
        for (ConceptoCotizacion concepto : conceptos.findAllByOrderByOrdenAscNombreAsc()) {
            if (!Boolean.TRUE.equals(concepto.getActivo())) continue;
            BigDecimal monto = concepto.getTipo() == TipoCargoCotizacion.PORCENTAJE
                    ? CalculoCotizacion.porcentaje(materiales, concepto.getValor())
                    : CalculoCotizacion.dinero(concepto.getValor());
            otros = otros.add(monto);
            lineas.add(concepto.getNombre() + ": " + monto.toPlainString());
        }
        return new CargosCotizacionDTO(
                tarifa.getDistrito(),
                piezas,
                materiales,
                CalculoCotizacion.dinero(porcentaje),
                utilidad,
                movilidad,
                otros.setScale(2),
                lineas.isEmpty() ? null : String.join("\n", lineas),
                false,
                null);
    }

    private ParametroCotizacion parametro() {
        return parametros.findById(ParametroCotizacion.ID_UNICO).orElseGet(() -> ParametroCotizacion.builder()
                .id(ParametroCotizacion.ID_UNICO)
                .utilidadPorcentaje(BigDecimal.ZERO.setScale(2))
                .build());
    }

    private record TarifaSemilla(String distrito, String base, int incluidas, String extra) {}
}
