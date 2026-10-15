package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.exception.FileStorageException;
import com.policalum.erp.infrastructure.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties props;

    @Override
    public String guardar(MultipartFile archivo, String subcarpeta) {
        if (archivo == null || archivo.isEmpty()) {
            throw new FileStorageException("El archivo está vacío");
        }

        if (archivo.getSize() > props.getMaxFileSizeBytes()) {
            throw new FileStorageException("El archivo excede el tamaño máximo permitido ("
                    + (props.getMaxFileSizeBytes() / 1024 / 1024) + " MB)");
        }

        String nombreOriginal = StringUtils.cleanPath(
                archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo");
        String extension = extraerExtension(nombreOriginal).toLowerCase();

        if (!props.getAllowedExtensions().contains(extension)) {
            throw new FileStorageException("Extensión no permitida: ." + extension
                    + ". Permitidas: " + props.getAllowedExtensions());
        }

        // Ruta: compras/2026/05/uuid.ext
        LocalDate hoy = LocalDate.now();
        String fechaPath = String.format("%04d/%02d", hoy.getYear(), hoy.getMonthValue());
        String nombreUnico = UUID.randomUUID() + "." + extension;
        String rutaRelativa = subcarpeta + "/" + fechaPath + "/" + nombreUnico;

        try {
            Path basePath = Paths.get(props.getBasePath()).toAbsolutePath().normalize();
            Path destino = basePath.resolve(rutaRelativa).normalize();

            if (!destino.startsWith(basePath)) {
                throw new FileStorageException("Ruta de destino inválida");
            }

            Files.createDirectories(destino.getParent());
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            log.info("Archivo guardado: {}", rutaRelativa);
            return rutaRelativa;

        } catch (IOException e) {
            throw new FileStorageException("Error al guardar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource cargar(String rutaRelativa) {
        try {
            Path basePath = Paths.get(props.getBasePath()).toAbsolutePath().normalize();
            Path archivo = basePath.resolve(rutaRelativa).normalize();

            if (!archivo.startsWith(basePath)) {
                throw new FileStorageException("Ruta inválida");
            }

            Resource resource = new UrlResource(archivo.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new FileStorageException("Archivo no encontrado o no legible: " + rutaRelativa);

        } catch (MalformedURLException e) {
            throw new FileStorageException("Error al cargar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(String rutaRelativa) {
        try {
            Path basePath = Paths.get(props.getBasePath()).toAbsolutePath().normalize();
            Path archivo = basePath.resolve(rutaRelativa).normalize();
            Files.deleteIfExists(archivo);
            log.info("Archivo eliminado: {}", rutaRelativa);
        } catch (IOException e) {
            log.warn("No se pudo eliminar archivo {}: {}", rutaRelativa, e.getMessage());
        }
    }

    @Override
    public boolean existe(String rutaRelativa) {
        Path basePath = Paths.get(props.getBasePath()).toAbsolutePath().normalize();
        Path archivo = basePath.resolve(rutaRelativa).normalize();
        return Files.exists(archivo);
    }

    private String extraerExtension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return (punto > 0) ? nombre.substring(punto + 1) : "";
    }
}