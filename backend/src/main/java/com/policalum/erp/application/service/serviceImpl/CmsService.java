package com.policalum.erp.application.service.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policalum.erp.application.dto.request.CmsAjusteRequestDTO;
import com.policalum.erp.application.dto.request.DiaAtencionDTO;
import com.policalum.erp.application.dto.request.HorarioOperativoRequestDTO;
import com.policalum.erp.application.dto.request.OfertaManualRequestDTO;
import com.policalum.erp.application.dto.response.AtencionPublicaDTO;
import com.policalum.erp.application.dto.response.CmsPublicoDTO;
import com.policalum.erp.application.service.AtencionHorario;
import com.policalum.erp.application.service.PrecioComercial;
import com.policalum.erp.domain.model.CmsAjuste;
import com.policalum.erp.domain.model.HorarioOperativo;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CmsAjusteRepository;
import com.policalum.erp.infrastructure.repository.HorarioOperativoRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CmsService {

    private static final List<String> SEMANA = List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO");
    private static final TypeReference<List<DiaAtencionDTO>> DIAS = new TypeReference<>() {};

    private final CmsAjusteRepository ajustes;
    private final HorarioOperativoRepository horarios;
    private final ProductoRepository productos;
    private final CmsPublicador publicador;
    private final ObjectMapper json = new ObjectMapper();

    @Transactional
    public CmsPublicoDTO guardarAjustes(CmsAjusteRequestDTO request) {
        CmsAjuste fila = asegurarAjuste();
        fila.setColorPrimario(request.primaryColor().toUpperCase());
        fila.setColorSecundario(request.secondaryColor().toUpperCase());
        fila.setTamanoBase(request.baseFontSize());
        fila.setModo(request.themeMode());
        fila.setBanner(limpio(request.banner(), 180));
        fila.setResumen(limpio(request.resumen(), 500));
        fila.setHorarioSemana(limpio(request.horarioSemana(), 80));
        fila.setHorarioSabado(limpio(request.horarioSabado(), 80));
        fila.setFacebook(limpio(request.facebook(), 200));
        fila.setInstagram(limpio(request.instagram(), 200));
        fila.setLinkedin(limpio(request.linkedin(), 200));
        fila.setYoutube(limpio(request.youtube(), 200));
        fila.setCorreoSoporte(limpio(request.correoSoporte(), 150));
        fila.setTextoPrivacidad(limpio(request.textoPrivacidad(), 4000));
        fila.setTextoTerminos(limpio(request.textoTerminos(), 4000));
        fila.setTextoGarantia(limpio(request.textoGarantia(), 4000));
        fila.setTextoFaq(limpio(request.textoFaq(), 4000));
        fila.setHeroKicker(limpio(request.heroKicker(), 80));
        fila.setHeroTitulo(limpio(request.heroTitulo(), 180));
        fila.setHeroTexto(limpio(request.heroTexto(), 400));
        ajustes.save(fila);
        publicador.publicar("ajustes");
        return publico();
    }

    @Transactional
    public CmsPublicoDTO guardarHorario(HorarioOperativoRequestDTO request) {
        HorarioOperativo fila = asegurarHorario();
        fila.setMensaje(request.mensaje() == null ? "" : request.mensaje().trim());
        fila.setActivo(Boolean.TRUE.equals(request.activo()) && !fila.getMensaje().isBlank());
        if (request.dias() != null) {
            validarDias(request.dias());
            try {
                fila.setHorarioJson(json.writeValueAsString(request.dias()));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("No se pudo guardar el horario");
            }
            reflejarTextos(request.dias());
        }
        horarios.save(fila);
        publicador.publicar("horario");
        return leer();
    }

    @Transactional
    public int sincronizarOfertas() {
        LocalDateTime desde = LocalDateTime.now().minusDays(90);
        int altas = productos.marcarOfertasEstancadas(desde);
        productos.quitarOfertasConRotacion(desde);
        publicador.publicar("catalogo");
        return altas;
    }

    @Transactional
    public void marcarOferta(UUID productoId, OfertaManualRequestDTO request) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        if (!Boolean.TRUE.equals(request.oferta())) {
            producto.setOferta(false);
            producto.setOfertaManual(false);
            producto.setPrecioOferta(null);
            producto.setOfertaPorcentaje(null);
            producto.setOfertaDesde(null);
            producto.setOfertaHasta(null);
            productos.save(producto);
            publicador.publicar("catalogo");
            return;
        }
        BigDecimal base = producto.getPrecioVentaBase();
        if (base == null || base.signum() <= 0) {
            throw new IllegalArgumentException("El producto no tiene precio de venta");
        }
        BigDecimal oferta;
        BigDecimal porcentaje;
        if (request.precioOferta() != null) {
            oferta = request.precioOferta().setScale(2, RoundingMode.HALF_UP);
            if (oferta.signum() <= 0 || oferta.compareTo(base) >= 0) {
                throw new IllegalArgumentException("El precio de oferta tiene que ser menor que el precio de venta");
            }
            porcentaje = base.subtract(oferta).multiply(new BigDecimal("100")).divide(base, 2, RoundingMode.HALF_UP);
        } else if (request.porcentaje() != null) {
            porcentaje = request.porcentaje().setScale(2, RoundingMode.HALF_UP);
            oferta = base.multiply(BigDecimal.ONE.subtract(porcentaje.movePointLeft(2))).setScale(2, RoundingMode.HALF_UP);
            if (oferta.signum() <= 0) {
                throw new IllegalArgumentException("El descuento deja el precio en cero");
            }
        } else {
            throw new IllegalArgumentException("Indica el porcentaje o el precio de oferta");
        }
        if (request.dias() == null) {
            throw new IllegalArgumentException("Indica por cuántos días corre la oferta");
        }
        LocalDate hoy = LocalDate.now(PrecioComercial.LIMA);
        producto.setPrecioOferta(oferta);
        producto.setOfertaPorcentaje(porcentaje);
        producto.setOfertaDesde(hoy);
        producto.setOfertaHasta(hoy.plusDays(request.dias() - 1L));
        producto.setOferta(true);
        producto.setOfertaManual(true);
        productos.save(producto);
        publicador.publicar("catalogo");
    }

    @Cacheable(cacheNames = "cms-publico", key = "'actual'")
    @Transactional(readOnly = true)
    public CmsPublicoDTO publico() {
        return leer();
    }

    @Transactional(readOnly = true)
    public CmsPublicoDTO actual() {
        return leer();
    }

    private CmsPublicoDTO leer() {
        CmsAjuste ajuste = ajustes.findById(CmsAjuste.ID).orElse(null);
        HorarioOperativo horario = horarios.findById(HorarioOperativo.ID).orElse(null);
        String mensaje = horario == null || horario.getMensaje() == null ? "" : horario.getMensaje();
        boolean activo = horario != null && Boolean.TRUE.equals(horario.getActivo()) && !mensaje.isBlank();
        List<DiaAtencionDTO> dias = leerDias(horario);
        AtencionPublicaDTO atencion = AtencionHorario.evaluar(dias, LocalDateTime.now(PrecioComercial.LIMA));
        return new CmsPublicoDTO(
                ajuste == null || ajuste.getColorPrimario() == null ? "#1FA2D6" : ajuste.getColorPrimario(),
                ajuste == null || ajuste.getColorSecundario() == null ? "#FFFFFF" : ajuste.getColorSecundario(),
                ajuste == null || ajuste.getTamanoBase() == null ? 16 : ajuste.getTamanoBase(),
                ajuste == null || ajuste.getModo() == null ? "aether" : ajuste.getModo(),
                mensaje,
                activo,
                ajuste == null ? null : ajuste.getBanner(),
                ajuste == null ? null : ajuste.getResumen(),
                ajuste == null ? null : ajuste.getHorarioSemana(),
                ajuste == null ? null : ajuste.getHorarioSabado(),
                ajuste == null ? null : ajuste.getFacebook(),
                ajuste == null ? null : ajuste.getInstagram(),
                ajuste == null ? null : ajuste.getLinkedin(),
                ajuste == null ? null : ajuste.getYoutube(),
                ajuste == null ? null : ajuste.getCorreoSoporte(),
                ajuste == null ? null : ajuste.getTextoPrivacidad(),
                ajuste == null ? null : ajuste.getTextoTerminos(),
                ajuste == null ? null : ajuste.getTextoGarantia(),
                ajuste == null ? null : ajuste.getTextoFaq(),
                ajuste == null ? null : ajuste.getHeroKicker(),
                ajuste == null ? null : ajuste.getHeroTitulo(),
                ajuste == null ? null : ajuste.getHeroTexto(),
                atencion,
                dias);
    }

    private List<DiaAtencionDTO> leerDias(HorarioOperativo horario) {
        if (horario == null || horario.getHorarioJson() == null || horario.getHorarioJson().isBlank()) return List.of();
        try {
            List<DiaAtencionDTO> dias = json.readValue(horario.getHorarioJson(), DIAS);
            return dias == null ? List.of() : dias;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private void validarDias(List<DiaAtencionDTO> dias) {
        if (dias.size() != 7) throw new IllegalArgumentException("Indica los siete días de la semana");
        Set<String> vistos = new LinkedHashSet<>();
        for (DiaAtencionDTO dia : dias) {
            if (!SEMANA.contains(dia.dia()) || !vistos.add(dia.dia())) {
                throw new IllegalArgumentException("Cada día de la semana se indica una sola vez");
            }
            if (Boolean.TRUE.equals(dia.abierto())) {
                if (dia.desde() == null || dia.desde().isBlank() || dia.hasta() == null || dia.hasta().isBlank()) {
                    throw new IllegalArgumentException("Un día abierto necesita hora de inicio y de cierre");
                }
                if (!LocalTime.parse(dia.hasta()).isAfter(LocalTime.parse(dia.desde()))) {
                    throw new IllegalArgumentException("La hora de cierre tiene que ser posterior a la de apertura");
                }
            }
        }
    }

    private void reflejarTextos(List<DiaAtencionDTO> dias) {
        CmsAjuste ajuste = asegurarAjuste();
        ajuste.setHorarioSemana(textoDe(dias, List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES")));
        ajuste.setHorarioSabado(textoDe(dias, List.of("SABADO")));
        ajustes.save(ajuste);
    }

    private String textoDe(List<DiaAtencionDTO> dias, List<String> claves) {
        List<String> partes = new ArrayList<>();
        for (DiaAtencionDTO dia : dias) {
            if (!claves.contains(dia.dia())) continue;
            if (!Boolean.TRUE.equals(dia.abierto())) {
                partes.add("Cerrado");
                continue;
            }
            partes.add(dia.desde() + " a " + dia.hasta());
        }
        if (partes.isEmpty()) return null;
        if (partes.stream().distinct().count() == 1) return cortar(partes.get(0));
        return cortar(String.join(" · ", partes));
    }

    private String cortar(String texto) {
        return texto.length() <= 80 ? texto : texto.substring(0, 80);
    }

    private String limpio(String valor, int maximo) {
        if (valor == null) return null;
        String texto = valor.trim();
        if (texto.isEmpty()) return null;
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    private CmsAjuste asegurarAjuste() {
        return ajustes.findById(CmsAjuste.ID).orElseGet(() -> {
            CmsAjuste nuevo = new CmsAjuste();
            nuevo.setId(CmsAjuste.ID);
            return ajustes.save(nuevo);
        });
    }

    private HorarioOperativo asegurarHorario() {
        return horarios.findById(HorarioOperativo.ID).orElseGet(() -> {
            HorarioOperativo nuevo = new HorarioOperativo();
            nuevo.setId(HorarioOperativo.ID);
            return horarios.save(nuevo);
        });
    }
}
