package com.policalum.erp.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Alta/edición en bloque de series (upsert por sede + tipo). Las no enviadas no cambian. */
public record SeriesRequestDTO(
        @NotEmpty @Size(max = 100) List<@Valid SerieComprobanteRequestDTO> series) { }
