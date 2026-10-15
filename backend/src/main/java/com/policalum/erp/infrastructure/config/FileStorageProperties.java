package com.policalum.erp.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "policalum.uploads")
@Getter @Setter
public class FileStorageProperties {

    private String basePath = "./uploads";
    private long maxFileSizeBytes = 10 * 1024 * 1024; // 10 MB
    private List<String> allowedExtensions = List.of("pdf", "jpg", "jpeg", "png", "webp");
    private List<String> allowedTypes = List.of("FACTURA", "GUIA_REMISION", "ORDEN_COMPRA", "OTRO");
}