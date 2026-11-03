package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.CmsAjusteRequestDTO;
import com.policalum.erp.application.dto.request.HorarioOperativoRequestDTO;
import com.policalum.erp.application.dto.response.CmsPublicoDTO;
import com.policalum.erp.domain.model.CmsAjuste;
import com.policalum.erp.domain.model.HorarioOperativo;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CmsAjusteRepository;
import com.policalum.erp.infrastructure.repository.HorarioOperativoRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CmsService {

    private final CmsAjusteRepository ajustes;
    private final HorarioOperativoRepository horarios;
    private final ProductoRepository productos;
    private final CmsPublicador publicador;

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
        ajustes.save(fila);
        publicador.publicar("ajustes");
        return publico();
    }

    @Transactional
    public CmsPublicoDTO guardarHorario(HorarioOperativoRequestDTO request) {
        HorarioOperativo fila = asegurarHorario();
        fila.setMensaje(request.mensaje() == null ? "" : request.mensaje().trim());
        fila.setActivo(Boolean.TRUE.equals(request.activo()) && !fila.getMensaje().isBlank());
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
    public void marcarOferta(UUID productoId, boolean oferta) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        producto.setOferta(oferta);
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
                ajuste == null ? null : ajuste.getTextoFaq());
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
