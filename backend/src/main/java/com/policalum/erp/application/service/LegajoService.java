package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.LegajoRequestDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.LegajoDashboardDTO;
import com.policalum.erp.application.dto.response.LegajoDocumentoResponseDTO;
import com.policalum.erp.application.dto.response.LegajoListadoDTO;
import com.policalum.erp.application.dto.response.LegajoResponseDTO;
import com.policalum.erp.domain.enums.SistemaPension;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Legajo digital del empleado (uno por User) y sus documentos.
 * Escritura: solo ADMIN. Lectura: ADMIN, o el propio empleado sobre su legajo.
 */
public interface LegajoService {

    /**
     * Crea el legajo de un empleado activo.
     *
     * @throws IllegalStateException si el empleado está inactivo o ya tiene legajo (409)
     * @throws IllegalArgumentException si los datos son incoherentes (400)
     */
    LegajoResponseDTO crear(LegajoRequestDTO request);

    /** Legajo de un empleado por su id de usuario. Solo ADMIN. */
    LegajoResponseDTO obtenerPorEmpleado(UUID empleadoId);

    /** Legajo del usuario autenticado. */
    LegajoResponseDTO obtenerMio();

    /** Actualiza el legajo del empleado (body.empleadoId debe coincidir). Solo ADMIN. */
    LegajoResponseDTO actualizar(UUID empleadoId, LegajoRequestDTO request);

    /** Listado paginado con filtros opcionales; texto busca en nombre, apellido y email. Solo ADMIN. */
    Page<LegajoListadoDTO> buscar(UUID sedeId, String tipoContrato, SistemaPension sistemaPension,
                                  Boolean activo, String texto, Pageable pageable);

    /** Sube un documento al legajo (id del legajo). Solo ADMIN. */
    LegajoDocumentoResponseDTO subirDocumento(UUID legajoId, MultipartFile archivo, String tipo, String descripcion);

    /** Documentos del legajo. ADMIN o el dueño del legajo. */
    List<LegajoDocumentoResponseDTO> listarDocumentos(UUID legajoId);

    /** Descarga un documento. ADMIN o el dueño del legajo. Queda auditado. */
    ArchivoDescargaDTO descargarDocumento(UUID legajoId, UUID documentoId);

    /** Elimina el registro y el archivo (el archivo, tras el commit). Solo ADMIN. */
    void eliminarDocumento(UUID legajoId, UUID documentoId);

    /** KPIs de legajos (sedeId opcional). Solo ADMIN. */
    LegajoDashboardDTO dashboard(UUID sedeId);
}
