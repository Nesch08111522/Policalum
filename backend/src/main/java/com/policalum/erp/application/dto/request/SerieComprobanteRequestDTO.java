package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.TipoComprobante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/**
 * Serie de una sede y tipo. BOLETA = B + 3, FACTURA = F + 3, TICKET = 4 caracteres que no empiecen
 * con B ni F (letras o números; se guarda en mayúsculas). activa null = true al crear / no cambia al editar.
 */
public record SerieComprobanteRequestDTO(
        @NotNull UUID sedeId,
        @NotNull TipoComprobante tipoComprobante,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{4}", message = "La serie debe tener 4 letras o números") String serie,
        Boolean activa) { }
