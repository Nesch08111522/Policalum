package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.EmpresaRequestDTO;
import com.policalum.erp.application.dto.request.ParametrosRequestDTO;
import com.policalum.erp.application.dto.request.SeriesRequestDTO;
import com.policalum.erp.application.dto.response.ConfiguracionPublicaDTO;
import com.policalum.erp.application.dto.response.EmpresaResponseDTO;
import com.policalum.erp.application.dto.response.ParametrosFinanzasDTO;
import com.policalum.erp.application.dto.response.ParametrosInventarioDTO;
import com.policalum.erp.application.dto.response.ParametrosReportesDTO;
import com.policalum.erp.application.dto.response.ParametrosResponseDTO;
import com.policalum.erp.application.dto.response.ParametrosVentasDTO;
import com.policalum.erp.application.dto.response.SerieComprobanteResponseDTO;
import java.util.List;
import java.util.UUID;

/**
 * Configuración global (Módulo 14): empresa, parámetros de negocio por dominio y series de comprobante.
 * Mono-empresa, sin caché (cada lectura va a la BD: son filas únicas por PK). Solo ADMIN modifica.
 */
public interface ConfiguracionService {

    /** Datos de la empresa. */
    EmpresaResponseDTO obtenerEmpresa();

    /** Reemplaza los datos de la empresa (valida dígito verificador del RUC). Solo ADMIN. Auditado. */
    EmpresaResponseDTO actualizarEmpresa(EmpresaRequestDTO request);

    /** Parámetros de todos los dominios. */
    ParametrosResponseDTO obtenerParametros();

    /** Actualiza los dominios enviados (los null no cambian). Solo ADMIN. Auditado por tabla. */
    ParametrosResponseDTO actualizarParametros(ParametrosRequestDTO request);

    /** Series de comprobante; sedeId null = todas las sedes. */
    List<SerieComprobanteResponseDTO> listarSeries(UUID sedeId);

    /** Upsert de series por sede + tipo; devuelve todas las series. Solo ADMIN. Auditado. */
    List<SerieComprobanteResponseDTO> actualizarSeries(SeriesRequestDTO request);

    /** Nombre comercial y logo, sin autenticación. */
    ConfiguracionPublicaDTO publica();

    /** Snapshot de ventas para otros servicios; si la fila no existe devuelve los valores de fábrica. */
    ParametrosVentasDTO ventas();

    /** Snapshot de inventario para otros servicios; si la fila no existe devuelve los valores de fábrica. */
    ParametrosInventarioDTO inventario();

    /** Snapshot de finanzas para otros servicios; si la fila no existe devuelve los valores de fábrica. */
    ParametrosFinanzasDTO finanzas();

    /** Snapshot de reportes para otros servicios; si la fila no existe devuelve los valores de fábrica. */
    ParametrosReportesDTO reportes();

    /** Crea con valores de fábrica las filas de configuración que falten. Idempotente; se ejecuta al iniciar. */
    void inicializarSiFalta();
}
