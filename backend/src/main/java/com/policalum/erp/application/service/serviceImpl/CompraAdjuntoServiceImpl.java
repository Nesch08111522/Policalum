package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.CompraAdjuntoResponseDTO;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.CompraAdjuntoService;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.model.Compra;
import com.policalum.erp.domain.model.CompraAdjunto;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.FileStorageException;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.config.FileStorageProperties;
import com.policalum.erp.infrastructure.repository.CompraAdjuntoRepository;
import com.policalum.erp.infrastructure.repository.CompraRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompraAdjuntoServiceImpl implements CompraAdjuntoService {

    private static final String SUB_CARPETA = "compras";

    private final CompraAdjuntoRepository adjuntoRepository;
    private final CompraRepository compraRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FileStorageProperties props;
    private final AuditService auditService;

    @Override
    public CompraAdjuntoResponseDTO subir(UUID compraId, MultipartFile archivo, String tipo) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada: " + compraId));

        String tipoNormalizado = tipo != null ? tipo.trim().toUpperCase() : "OTRO";
        if (!props.getAllowedTypes().contains(tipoNormalizado)) {
            throw new ValidationException("Tipo de adjunto no permitido: " + tipoNormalizado);
        }

        String rutaRelativa = fileStorageService.guardar(archivo, SUB_CARPETA);
        User user = usuarioActual();

        CompraAdjunto adjunto = CompraAdjunto.builder()
                .compra(compra)
                .tipo(tipoNormalizado)
                .nombreOriginal(archivo.getOriginalFilename())
                .urlArchivo(rutaRelativa)
                .contentType(archivo.getContentType())
                .tamanoBytes(archivo.getSize())
                .subidoPor(user)
                .build();

        CompraAdjunto guardado = adjuntoRepository.save(adjunto);

        auditService.registrar("compra_adjuntos", guardado.getId(), AccionAudit.INSERT,
                Map.of("compraId", compraId.toString(),
                        "tipo", tipoNormalizado,
                        "nombreOriginal", guardado.getNombreOriginal()));

        return toDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraAdjuntoResponseDTO> listar(UUID compraId) {
        return adjuntoRepository.findByCompra_IdOrderByCreatedAtDesc(compraId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargar(UUID compraId, UUID adjuntoId) {
        CompraAdjunto adjunto = findAdjunto(compraId, adjuntoId);
        return fileStorageService.cargar(adjunto.getUrlArchivo());
    }

    @Override
    public void eliminar(UUID compraId, UUID adjuntoId) {
        CompraAdjunto adjunto = findAdjunto(compraId, adjuntoId);

        try {
            fileStorageService.eliminar(adjunto.getUrlArchivo());
        } catch (Exception e) {
            log.warn("Error eliminando archivo físico: {}", e.getMessage());
        }

        adjuntoRepository.delete(adjunto);

        auditService.registrar("compra_adjuntos", adjuntoId, AccionAudit.DELETE,
                Map.of("compraId", compraId.toString(),
                        "nombreOriginal", adjunto.getNombreOriginal()));
    }

    // ============ HELPERS ============

    private CompraAdjunto findAdjunto(UUID compraId, UUID adjuntoId) {
        CompraAdjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjunto no encontrado: " + adjuntoId));
        if (!adjunto.getCompra().getId().equals(compraId)) {
            throw new FileStorageException("El adjunto no pertenece a la compra indicada");
        }
        return adjunto;
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }

    private CompraAdjuntoResponseDTO toDto(CompraAdjunto a) {
        return new CompraAdjuntoResponseDTO(
                a.getId(),
                a.getTipo(),
                a.getNombreOriginal(),
                a.getUrlArchivo(),
                a.getContentType(),
                a.getTamanoBytes(),
                a.getSubidoPor() != null ? a.getSubidoPor().getId() : null,
                a.getSubidoPor() != null
                        ? a.getSubidoPor().getNombre() + " " + a.getSubidoPor().getApellido()
                        : null,
                a.getCreatedAt()
        );
    }
}