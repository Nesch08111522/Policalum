package com.policalum.erp.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI 3 (Swagger UI en /swagger-ui.html, JSON en /v3/api-docs).
 *
 * <ul>
 *   <li>Seguridad: Bearer JWT global; el botón "Authorize" pide el token de POST /api/v1/auth/login.</li>
 *   <li>Tags: un tag por dominio (los controllers declaran el suyo con {@code @Tag}); aquí se fija el orden
 *       y la descripción.</li>
 *   <li>Errores: todas las respuestas 4xx/5xx usan el esquema {@code ErrorRespuesta}, que es el cuerpo que
 *       devuelve GlobalExceptionHandler.</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";
    static final String ESQUEMA_ERROR = "ErrorRespuesta";

    /** Rutas que no requieren token (deben coincidir con los permitAll de SecurityConfig). */
    static final List<String> RUTAS_PUBLICAS = List.of(
            "/api/v1/auth/",
            "/api/v1/configuracion/publica",
            "/api/v1/configuracion/logo",
            "/api/v1/rrhh/asistencias/qr-publico",
            "/api/v1/rrhh/asistencias/marcar-qr-real",
            "/api/v1/rrhh/asistencias/empleados");

    /** Orden y descripción de los tags (dominios) en Swagger UI. */
    static final Map<String, String> TAGS = tags();

    @Bean
    public OpenAPI policalumOpenApi(@Value("${policalum.api.version:1.0.0}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("POLICALUM ERP API")
                        .version(version)
                        .description("API REST del ERP de POLICALUM (Trujillo, Perú): ventas de planchas de policarbonato "
                                + "por fracciones, servicios, compras, inventario y Kardex, despachos, RRHH, finanzas, reportes "
                                + "y configuración.\n\n"
                                + "**Autenticación:** `POST /api/v1/auth/login` devuelve un JWT; envíelo como "
                                + "`Authorization: Bearer <token>` (botón *Authorize*).\n\n"
                                + "**Roles:** ADMIN y MULTIFUNCIONAL. MULTIFUNCIONAL no ve costos y en varios módulos solo su "
                                + "sede; el detalle por endpoint está en docs/API-ROLES.md.\n\n"
                                + "**Convenciones:** fechas ISO-8601 (`yyyy-MM-dd`), montos en PEN con IGV salvo que el campo "
                                + "diga *neto*, listados paginados con `page`, `size` (20 por defecto).")
                        .contact(new Contact().name("POLICALUM")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token de POST /api/v1/auth/login (sin el prefijo 'Bearer')."))
                        .addSchemas(ESQUEMA_ERROR, esquemaError()))
                .tags(TAGS.entrySet().stream().map(e -> new Tag().name(e.getKey()).description(e.getValue())).toList());
    }

    /**
     * Ajustes sobre el documento generado: rutas públicas sin candado, respuestas de error comunes y el
     * esquema ErrorRespuesta en todas las respuestas 4xx/5xx.
     */
    @Bean
    public OpenApiCustomizer respuestasComunesCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) return;
            openApi.getPaths().forEach((ruta, item) -> item.readOperations().forEach(op -> ajustar(ruta, op)));
        };
    }

    static void ajustar(String ruta, Operation op) {
        boolean publica = RUTAS_PUBLICAS.stream().anyMatch(ruta::startsWith);
        if (publica) op.setSecurity(List.of());
        ApiResponses r = op.getResponses() != null ? op.getResponses() : new ApiResponses();
        r.putIfAbsent("400", new ApiResponse().description("Datos inválidos (validación de campos o regla de negocio)"));
        if (!publica) {
            r.putIfAbsent("401", new ApiResponse().description("Token de un usuario inactivo o eliminado"));
            r.putIfAbsent("403", new ApiResponse().description("Sin token, token inválido o el rol no tiene permiso"));
        }
        r.putIfAbsent("500", new ApiResponse().description("Error inesperado del servidor"));
        Content error = new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ESQUEMA_ERROR)));
        r.forEach((codigo, respuesta) -> {
            if (codigo.startsWith("4") || codigo.startsWith("5")) respuesta.setContent(error);
        });
        op.setResponses(r);
    }

    /** Cuerpo de error de GlobalExceptionHandler. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Schema<?> esquemaError() {
        ObjectSchema s = new ObjectSchema();
        s.description("Respuesta de error estándar");
        s.addProperty("timestamp", new DateTimeSchema().example("2026-10-09T10:15:30"));
        s.addProperty("status", new IntegerSchema().example(409));
        s.addProperty("error", new StringSchema().example("Conflict"));
        s.addProperty("message", new StringSchema().example("La venta ya está anulada"));
        s.addProperty("errors", new MapSchema().additionalProperties(new StringSchema())
                .description("Solo en errores de validación: campo → mensaje")
                .example(Map.of("ruc", "El RUC debe tener 11 dígitos")));
        return s;
    }

    private static Map<String, String> tags() {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("Autenticación", "Login y token JWT");
        t.put("Ventas", "Punto de venta: registro y anulación de ventas");
        t.put("Caja", "Turnos de caja: apertura, resumen y cierre");
        t.put("Cotizaciones", "Cotizaciones: emisión, aceptación, rechazo y conversión a venta");
        t.put("Preformas", "Preformas de venta (carrito de mostrador) y su confirmación");
        t.put("Clientes", "Clientes");
        t.put("Despachos", "Órdenes de despacho, entregas y evidencias");
        t.put("Movilidad", "Vehículos de reparto y conductores");
        t.put("Devoluciones", "Devoluciones de ventas y reembolsos");
        t.put("Compras", "Compras a proveedores, recepción y adjuntos");
        t.put("Proveedores", "Proveedores");
        t.put("Cuentas por pagar", "Cuentas por pagar y sus pagos");
        t.put("Catálogo", "Productos, categorías, fracciones y servicios");
        t.put("Inventario", "Almacenes, stock, ajustes, transferencias y Kardex");
        t.put("Finanzas", "Flujo de caja, dashboards, cuentas financieras, retiros de socios y utilidad neta");
        t.put("RRHH", "Legajos, turnos de trabajo, asistencia, nóminas y comisiones");
        t.put("Reportes", "Reportes consolidados y exportaciones CSV");
        t.put("Configuración", "Empresa, parámetros de negocio y series de comprobante");
        t.put("Auditoría", "Registro de auditoría");
        t.put("Administración", "Usuarios, roles y sedes");
        return t;
    }
}
