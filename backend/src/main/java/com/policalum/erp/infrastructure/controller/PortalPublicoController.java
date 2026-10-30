package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.response.CategoriaPublicaDTO;
import com.policalum.erp.application.dto.response.EmpresaPublicaDTO;
import com.policalum.erp.application.dto.response.PaginaPublicaDTO;
import com.policalum.erp.application.dto.response.ProductoPublicoDTO;
import com.policalum.erp.application.dto.response.ProyectoPortafolioResponseDTO;
import com.policalum.erp.application.service.serviceImpl.PortalPublicoService;
import com.policalum.erp.application.service.serviceImpl.ProductoImagenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lectura pública del catálogo, la marca y el portafolio. Sin costos ni proveedores. */
@Tag(name = "Portal público")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PortalPublicoController {

    private final PortalPublicoService portal;
    private final ProductoImagenService imagenes;

    @GetMapping("/empresa")
    public ResponseEntity<EmpresaPublicaDTO> empresa() {
        return ResponseEntity.ok(portal.empresa());
    }

    @GetMapping("/catalogo/categorias")
    public ResponseEntity<List<CategoriaPublicaDTO>> categorias() {
        return ResponseEntity.ok(portal.categorias());
    }

    @GetMapping("/catalogo/destacados")
    public ResponseEntity<List<ProductoPublicoDTO>> destacados() {
        return fresco(portal.destacados());
    }

    @GetMapping("/catalogo/productos")
    public ResponseEntity<PaginaPublicaDTO<ProductoPublicoDTO>> productos(
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean destacados,
            @PageableDefault(size = 12, sort = "nombre") Pageable pageable) {
        return fresco(portal.catalogo(categoriaId, q, destacados, pageable));
    }

    @GetMapping("/catalogo/productos/{id}")
    public ResponseEntity<ProductoPublicoDTO> producto(@PathVariable UUID id) {
        return fresco(portal.producto(id));
    }

    @GetMapping("/catalogo/imagenes")
    public ResponseEntity<Resource> imagen(@RequestParam String ruta) {
        Resource resource = imagenes.cargar(ruta);
        String nombre = resource.getFilename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (nombre != null ? nombre : "imagen") + "\"")
                .contentType(tipo(nombre))
                .body(resource);
    }

    @GetMapping("/portafolio")
    public ResponseEntity<List<ProyectoPortafolioResponseDTO>> portafolio() {
        return ResponseEntity.ok(portal.portafolio());
    }

    private static <T> ResponseEntity<T> fresco(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }

    private static MediaType tipo(String nombre) {
        String lower = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
