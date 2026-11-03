package com.policalum.erp.application.dto.response;

public record CmsPublicoDTO(
        String primaryColor,
        String secondaryColor,
        int baseFontSize,
        String themeMode,
        String aviso,
        boolean avisoActivo
) {}
