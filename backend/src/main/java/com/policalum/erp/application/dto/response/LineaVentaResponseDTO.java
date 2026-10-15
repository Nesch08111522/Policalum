package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaVentaResponseDTO(UUID id, UUID productoId, String productoNombre,
                                    UUID fraccionId, String fraccionNombre,
                                    UUID servicioId, String servicioNombre,
                                    BigDecimal cantidad, BigDecimal precioUnitarioConIgv,
                                    BigDecimal subtotal) { }
