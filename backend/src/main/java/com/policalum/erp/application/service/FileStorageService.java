package com.policalum.erp.application.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Guarda un archivo en la ruta configurada y retorna la ruta relativa.
     * Ej: "compras/2026/05/uuid.pdf"
     */
    String guardar(MultipartFile archivo, String subcarpeta);

    /** Carga un archivo como Resource para descarga. */
    Resource cargar(String rutaRelativa);

    /** Elimina un archivo del disco. No falla si no existe. */
    void eliminar(String rutaRelativa);

    /** Retorna true si el archivo existe. */
    boolean existe(String rutaRelativa);
}