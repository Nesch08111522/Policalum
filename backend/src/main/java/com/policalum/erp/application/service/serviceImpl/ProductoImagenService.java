package com.policalum.erp.application.service.serviceImpl;

import java.util.Locale;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.policalum.erp.application.dto.request.ProductoRequestDTO;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.exception.FileStorageException;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoImagenService {

    static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp");

    private final FileStorageService fileStorageService;

    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidationException("Selecciona un archivo de imagen");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ValidationException("La imagen no puede superar 2 MB");
        }
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        String extension = "";
        int punto = nombre.lastIndexOf('.');
        if (punto >= 0) {
            extension = nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        }
        if (!EXTENSIONES.contains(extension)) {
            throw new ValidationException("La imagen debe ser JPG, PNG o WEBP");
        }
        String tipo = archivo.getContentType() != null ? archivo.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!tipo.isBlank() && !tipo.startsWith("image/")) {
            throw new FileStorageException("El archivo no es una imagen");
        }
        return fileStorageService.guardar(archivo, "productos");
    }

    /** Si llega un archivo, guarda la ruta relativa. Si no, conserva la URL o la ruta que ya trae el DTO. */
    public ProductoRequestDTO aplicar(ProductoRequestDTO dto, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return dto;
        }
        String ruta = guardar(archivo);
        return new ProductoRequestDTO(
                dto.categoriaId(),
                dto.proveedorId(),
                dto.codigoSku(),
                dto.nombre(),
                dto.descripcion(),
                dto.tipo(),
                dto.precioCompra(),
                dto.precioVentaBase(),
                dto.unidad(),
                dto.esFraccionable(),
                dto.activo(),
                dto.incluyeIgv(),
                dto.tipoAfectacion(),
                dto.fracciones(),
                ruta);
    }

    public Resource cargar(String ruta) {
        if (ruta == null || ruta.isBlank() || ruta.contains("..") || !ruta.startsWith("productos/")) {
            throw new ValidationException("Ruta de imagen no válida");
        }
        return fileStorageService.cargar(ruta);
    }
}
