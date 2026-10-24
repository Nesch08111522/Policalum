package com.policalum.erp.application.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Guarda un archivo en la ruta configurada y retorna la ruta relativa.
     * Ej: "compras/2026/05/uuid.pdf"
     */
    String guardar(MultipartFile archivo, String subcarpeta);

    /**
     * Escribe en una ruta fija, sin fecha ni UUID. La extensión sale del archivo.
     * {@code empresa/logo} queda como {@code empresa/logo.png} y reemplaza ese mismo archivo.
     */
    String guardarFijo(MultipartFile archivo, String rutaSinExtension);

    /** Borra otros archivos del mismo nombre y distinta extensión. Conserva {@code rutaConservada}. */
    void descartarVariantes(String rutaConservada);

    /** Carga un archivo como Resource para descarga. */
    Resource cargar(String rutaRelativa);

    /** Elimina un archivo del disco. No falla si no existe. */
    void eliminar(String rutaRelativa);

    /** Retorna true si el archivo existe. */
    boolean existe(String rutaRelativa);
}