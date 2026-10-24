package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.exception.FileStorageException;
import com.policalum.erp.infrastructure.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties props;

    @Override
    public String guardar(MultipartFile archivo, String subcarpeta) {
        String extension = validar(archivo);
        LocalDate hoy = LocalDate.now();
        String fechaPath = String.format("%04d/%02d", hoy.getYear(), hoy.getMonthValue());
        String rutaRelativa = subcarpeta + "/" + fechaPath + "/" + UUID.randomUUID() + "." + extension;
        escribir(rutaRelativa, archivo);
        log.info("Archivo guardado: {}", rutaRelativa);
        return rutaRelativa;
    }

    @Override
    public String guardarFijo(MultipartFile archivo, String rutaSinExtension) {
        String extension = validar(archivo);
        if (rutaSinExtension == null || rutaSinExtension.isBlank()
                || rutaSinExtension.contains("..") || rutaSinExtension.startsWith("/") || rutaSinExtension.endsWith("/")) {
            throw new FileStorageException("Ruta de destino inválida");
        }
        String rutaRelativa = rutaSinExtension + "." + extension;
        escribir(rutaRelativa, archivo);
        log.info("Archivo reemplazado: {}", rutaRelativa);
        return rutaRelativa;
    }

    @Override
    public void descartarVariantes(String rutaConservada) {
        if (rutaConservada == null || rutaConservada.isBlank() || rutaConservada.contains("..")) return;
        try {
            Path basePath = baseAbsoluta();
            Path archivo = basePath.resolve(rutaConservada).normalize();
            if (!archivo.startsWith(basePath) || archivo.getParent() == null || archivo.getFileName() == null) return;
            if (!Files.isDirectory(archivo.getParent())) return;
            String nombre = archivo.getFileName().toString();
            int punto = nombre.lastIndexOf('.');
            if (punto <= 0) return;
            String raiz = nombre.substring(0, punto);
            try (DirectoryStream<Path> hermanos = Files.newDirectoryStream(archivo.getParent())) {
                for (Path candidato : hermanos) {
                    String actual = candidato.getFileName().toString();
                    if (actual.startsWith(raiz + ".") && !candidato.equals(archivo)) {
                        Files.deleteIfExists(candidato);
                    }
                }
            }
        } catch (IOException | InvalidPathException e) {
            log.warn("No se pudieron descartar variantes de {}: {}", rutaConservada, e.getMessage());
        }
    }

    @Override
    public Resource cargar(String rutaRelativa) {
        try {
            Path basePath = baseAbsoluta();
            Path archivo = basePath.resolve(rutaRelativa).normalize();

            if (!archivo.startsWith(basePath)) {
                throw new FileStorageException("Ruta inválida");
            }

            Resource resource = new UrlResource(archivo.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new FileStorageException("Archivo no encontrado o no legible: " + rutaRelativa);

        } catch (InvalidPathException e) {
            throw new FileStorageException("La ruta del archivo no es válida", e);
        } catch (IOException e) {
            throw new FileStorageException("Error al cargar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(String rutaRelativa) {
        try {
            Path basePath = baseAbsoluta();
            Path archivo = basePath.resolve(rutaRelativa).normalize();
            if (!archivo.startsWith(basePath)) return;
            Files.deleteIfExists(archivo);
            log.info("Archivo eliminado: {}", rutaRelativa);
        } catch (IOException | InvalidPathException e) {
            log.warn("No se pudo eliminar archivo {}: {}", rutaRelativa, e.getMessage());
        }
    }

    @Override
    public boolean existe(String rutaRelativa) {
        try {
            Path basePath = baseAbsoluta();
            Path archivo = basePath.resolve(rutaRelativa).normalize();
            return archivo.startsWith(basePath) && Files.exists(archivo);
        } catch (IOException | InvalidPathException e) {
            return false;
        }
    }

    /**
     * Una ruta relativa se ancla al directorio de trabajo del proceso.
     * Así {@code ./uploads} no depende de un path temporal del contenedor multipart.
     */
    private Path baseAbsoluta() throws IOException {
        String configurada = props.getBasePath() == null || props.getBasePath().isBlank()
                ? "uploads"
                : props.getBasePath();
        Path ruta = Paths.get(configurada);
        if (!ruta.isAbsolute()) {
            ruta = Paths.get(System.getProperty("user.dir")).resolve(ruta);
        }
        Path absoluta = ruta.toAbsolutePath().normalize();
        Files.createDirectories(absoluta);
        return absoluta;
    }

    private String validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new FileStorageException("El archivo está vacío");
        }
        if (archivo.getSize() > props.getMaxFileSizeBytes()) {
            throw new FileStorageException("El archivo excede el tamaño máximo permitido ("
                    + (props.getMaxFileSizeBytes() / 1024 / 1024) + " MB)");
        }
        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
        nombreOriginal = nombreOriginal.replace('\\', '/');
        int barra = nombreOriginal.lastIndexOf('/');
        if (barra >= 0) nombreOriginal = nombreOriginal.substring(barra + 1);
        String extension = extraerExtension(nombreOriginal).toLowerCase(Locale.ROOT);
        if (!props.getAllowedExtensions().contains(extension)) {
            throw new FileStorageException("Extensión no permitida: ." + extension
                    + ". Permitidas: " + props.getAllowedExtensions());
        }
        return extension;
    }

    /** Copia el flujo del multipart. No usa Part.write, que en Tomcat ignora la ruta absoluta. */
    private void escribir(String rutaRelativa, MultipartFile archivo) {
        try {
            Path basePath = baseAbsoluta();
            Path destino = basePath.resolve(rutaRelativa).normalize();
            if (!destino.startsWith(basePath) || destino.getParent() == null) {
                throw new FileStorageException("Ruta de destino inválida");
            }
            Files.createDirectories(destino.getParent());
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (InvalidPathException e) {
            throw new FileStorageException("La ruta del archivo no es válida", e);
        } catch (IllegalStateException e) {
            throw new FileStorageException("No se pudo escribir la imagen en el disco", e);
        } catch (IOException e) {
            throw new FileStorageException("Error al guardar el archivo: " + e.getMessage(), e);
        }
    }

    private String extraerExtension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return (punto > 0) ? nombre.substring(punto + 1) : "";
    }
}