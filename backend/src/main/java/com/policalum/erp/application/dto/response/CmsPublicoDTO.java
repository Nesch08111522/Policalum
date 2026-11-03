package com.policalum.erp.application.dto.response;

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
        String textoFaq
) {}
