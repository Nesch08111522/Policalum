package com.policalum.erp.application.dto.response;

import com.policalum.erp.application.dto.request.DiaAtencionDTO;
import java.util.List;

public record CmsPublicoDTO(
        String primaryColor,
        String secondaryColor,
        int baseFontSize,
        String themeMode,
        String aviso,
        boolean avisoActivo,
        String banner,
        String resumen,
        String horarioSemana,
        String horarioSabado,
        String facebook,
        String instagram,
        String linkedin,
        String youtube,
        String correoSoporte,
        String textoPrivacidad,
        String textoTerminos,
        String textoGarantia,
        String textoFaq,
        String heroKicker,
        String heroTitulo,
        String heroTexto,
        AtencionPublicaDTO atencion,
        List<DiaAtencionDTO> dias
) {}
