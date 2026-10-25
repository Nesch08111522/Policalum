package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.response.VendedorRendimientoDTO;
import com.policalum.erp.infrastructure.repository.VentaRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rendimiento")
@RestController
@RequestMapping("/api/v1/reportes/ventas/vendedores")
@RequiredArgsConstructor
public class RendimientoController {

    private final VentaRepository ventaRepository;

    @GetMapping
    public ResponseEntity<List<VendedorRendimientoDTO>> vendedores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "5") int top) {
        LocalDate inicio = desde == null ? LocalDate.now().withDayOfMonth(1) : desde;
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        int limite = Math.min(Math.max(top, 1), 20);
        List<VendedorRendimientoDTO> filas = new ArrayList<>();
        for (Object[] fila : ventaRepository.rendimientoVendedores(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(), PageRequest.of(0, limite))) {
            String nombre = ((fila[0] == null ? "" : fila[0]) + " " + (fila[1] == null ? "" : fila[1])).trim();
            long cantidad = fila[2] instanceof Number numero ? numero.longValue() : 0L;
            BigDecimal total = fila[3] instanceof BigDecimal monto ? monto : BigDecimal.ZERO;
            filas.add(new VendedorRendimientoDTO(nombre.isBlank() ? "Sin nombre" : nombre, cantidad, total));
        }
        return ResponseEntity.ok(filas);
    }
}
