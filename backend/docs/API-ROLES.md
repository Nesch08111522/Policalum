# API-ROLES — Permisos por endpoint

> Generado el 2026-10-09 a partir del código (`@PreAuthorize` de cada controller + validaciones de rol en los servicios) y cruzado
> con `/v3/api-docs`. **273 endpoints.** Fuente de verdad para mostrar u ocultar botones en el frontend.

## Cómo usar esta tabla en el frontend

1. Tras `POST /api/v1/auth/login` guarde `rol` (`ROLE_ADMIN` o `ROLE_MULTIFUNCIONAL`) junto al token.
2. Muestre una acción solo si la columna del rol tiene ✅. Aun así, **maneje el 403**: el backend es quien decide.
3. **Notas con "MULTIFUNCIONAL: …"**: el endpoint responde a ambos roles, pero con datos recortados (su sede, sus registros o sin costos).
   No muestre columnas de costo/margen a MULTIFUNCIONAL aunque el campo venga en el JSON (llega `null`).
4. **⚠️ Sin @PreAuthorize**: hoy responde a cualquier usuario autenticado. Para el frontend trátelos como indica la columna,
   pero la protección real está pendiente (ver docs/DEUDA-TECNICA.md). 🔴 marca los que son un riesgo de seguridad.

## Leyenda

| Símbolo | Significado |
|---|---|
| ✅ | El rol puede usar el endpoint |
| ❌ | El backend responde 403 a ese rol |
| ⚠️ | Sin `@PreAuthorize`: hoy cualquier usuario autenticado puede llamarlo |
| 🔴 | Riesgo de seguridad: debería estar restringido y no lo está |

## Resumen

| Tipo | Endpoints |
|---|---|
| Públicos (sin token) | 5 |
| ADMIN y MULTIFUNCIONAL | 67 |
| Solo ADMIN | 71 |
| Sin `@PreAuthorize` (cualquier autenticado) | 130 |
| **Total** | **273** |

Respuestas de error comunes: **401** usuario inactivo · **403** sin token, token inválido o rol sin permiso · **404** no existe ·
**409** estado no permite la acción · **400** validación (cuerpo `ErrorRespuesta`, ver Swagger).

## Endpoints por módulo

### Autenticación

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Autenticación | POST | `/api/v1/auth/login` | ✅ | ✅ | Público (sin token). Devuelve token, usuarioId, nombre, email y rol: el frontend decide la UI con `rol` |

### Ventas

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Ventas | GET | `/api/v1/ventas` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Ventas | POST | `/api/v1/ventas` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado. Requiere turno de caja ABIERTO del propio usuario en la sede |
| Ventas | GET | `/api/v1/ventas/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Ventas | POST | `/api/v1/ventas/{id}/anulacion` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado. Solo el vendedor de la venta o un ADMIN (se valida en el servicio) |

### Caja

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Caja | POST | `/api/v1/turnos-caja` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado. Abre turno propio; uno abierto por usuario y sede |
| Caja | POST | `/api/v1/turnos-caja/{id}/cierre` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado. Solo el dueño del turno |
| Caja | GET | `/api/v1/turnos-caja/{id}/resumen` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado. Esperado vs. declarado del turno |

### Cotizaciones

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Cotizaciones | GET | `/api/v1/cotizaciones` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Cotizaciones | POST | `/api/v1/cotizaciones` | ✅ | ✅ |  |
| Cotizaciones | GET | `/api/v1/cotizaciones/dashboard` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Cotizaciones | GET | `/api/v1/cotizaciones/vencidas` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado. Además marca como VENCIDAS las EMITIDAS cuya vigencia pasó |
| Cotizaciones | GET | `/api/v1/cotizaciones/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Cotizaciones | PUT | `/api/v1/cotizaciones/{id}` | ✅ | ✅ |  |
| Cotizaciones | DELETE | `/api/v1/cotizaciones/{id}` | ✅ | ✅ |  |
| Cotizaciones | POST | `/api/v1/cotizaciones/{id}/aceptar` | ✅ | ✅ |  |
| Cotizaciones | POST | `/api/v1/cotizaciones/{id}/convertir` | ✅ | ✅ | Requiere turno de caja ABIERTO |
| Cotizaciones | POST | `/api/v1/cotizaciones/{id}/rechazar` | ✅ | ✅ |  |

### Preformas

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Preformas | POST | `/api/v1/preformas` | ✅ | ✅ | No requiere turno |
| Preformas | GET | `/api/v1/preformas/abiertas` | ✅ | ✅ | Solo las del usuario actual |
| Preformas | GET | `/api/v1/preformas/dashboard` | ✅ | ❌ | Solo ADMIN |
| Preformas | GET | `/api/v1/preformas/vencidas` | ✅ | ✅ | MULTIFUNCIONAL: solo las propias |
| Preformas | GET | `/api/v1/preformas/{id}` | ✅ | ✅ |  |
| Preformas | PUT | `/api/v1/preformas/{id}` | ✅ | ✅ |  |
| Preformas | DELETE | `/api/v1/preformas/{id}` | ✅ | ✅ |  |
| Preformas | POST | `/api/v1/preformas/{id}/cancelar` | ✅ | ✅ |  |
| Preformas | POST | `/api/v1/preformas/{id}/confirmar` | ✅ | ✅ | Requiere turno de caja ABIERTO de quien confirma |

### Clientes

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Clientes | GET | `/api/v1/clientes` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | POST | `/api/v1/clientes` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/compras/activos` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/compras/inactivos` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/documento/{numeroDocumento}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/estadisticas/count` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/estadisticas/dashboard` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/estadisticas/hoy` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/estadisticas/tipo-documento` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/incompletos` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/recientes` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/validar/documento` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/validar/email` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | GET | `/api/v1/clientes/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | PUT | `/api/v1/clientes/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Clientes | DELETE | `/api/v1/clientes/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |

### Despachos

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Despachos | GET | `/api/v1/despachos` | ✅ | ✅ |  |
| Despachos | POST | `/api/v1/despachos` | ✅ | ✅ |  |
| Despachos | GET | `/api/v1/despachos/dashboard` | ✅ | ✅ |  |
| Despachos | GET | `/api/v1/despachos/repartidor/{userId}` | ✅ | ✅ |  |
| Despachos | GET | `/api/v1/despachos/seguimiento/{codigo}` | ✅ | ✅ |  |
| Despachos | GET | `/api/v1/despachos/{id}` | ✅ | ✅ |  |
| Despachos | POST | `/api/v1/despachos/{id}/asignar-repartidor` | ✅ | ✅ |  |
| Despachos | PATCH | `/api/v1/despachos/{id}/estado` | ✅ | ✅ |  |
| Despachos | GET | `/api/v1/despachos/{id}/evidencia` | ✅ | ✅ |  |
| Despachos | POST | `/api/v1/despachos/{id}/evidencia` | ✅ | ✅ |  |

### Movilidad

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Movilidad | GET | `/api/v1/movilidades` | ✅ | ✅ |  |
| Movilidad | POST | `/api/v1/movilidades` | ✅ | ❌ | Solo ADMIN |
| Movilidad | GET | `/api/v1/movilidades/dashboard` | ✅ | ✅ |  |
| Movilidad | GET | `/api/v1/movilidades/reportes/uso` | ✅ | ✅ |  |
| Movilidad | GET | `/api/v1/movilidades/{id}` | ✅ | ✅ |  |
| Movilidad | PUT | `/api/v1/movilidades/{id}` | ✅ | ❌ | Solo ADMIN |
| Movilidad | DELETE | `/api/v1/movilidades/{id}` | ✅ | ❌ | Solo ADMIN |
| Movilidad | POST | `/api/v1/movilidades/{id}/asignar-conductor` | ✅ | ❌ | Solo ADMIN. Solo ADMIN; el conductor debe ser MULTIFUNCIONAL |
| Movilidad | GET | `/api/v1/movilidades/{id}/historial` | ✅ | ✅ |  |

### Devoluciones

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Devoluciones | GET | `/api/v1/devoluciones` | ✅ | ✅ |  |
| Devoluciones | POST | `/api/v1/devoluciones` | ✅ | ✅ |  |
| Devoluciones | GET | `/api/v1/devoluciones/dashboard` | ✅ | ❌ | Solo ADMIN |
| Devoluciones | GET | `/api/v1/devoluciones/venta/{ventaId}` | ✅ | ✅ |  |
| Devoluciones | GET | `/api/v1/devoluciones/{id}` | ✅ | ✅ |  |
| Devoluciones | POST | `/api/v1/devoluciones/{id}/aprobar` | ✅ | ❌ | Solo ADMIN |
| Devoluciones | POST | `/api/v1/devoluciones/{id}/rechazar` | ✅ | ❌ | Solo ADMIN |
| Devoluciones | GET | `/api/v1/reembolsos` | ✅ | ✅ |  |
| Devoluciones | POST | `/api/v1/reembolsos` | ✅ | ✅ |  |
| Devoluciones | GET | `/api/v1/reembolsos/{id}` | ✅ | ✅ |  |
| Devoluciones | POST | `/api/v1/reembolsos/{id}/procesar` | ✅ | ❌ | Solo ADMIN. Solo ADMIN; en EFECTIVO requiere turno ABIERTO |

### Compras

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Compras | GET | `/api/v1/compras` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Compras | POST | `/api/v1/compras` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Compras | GET | `/api/v1/compras/dashboard` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Compras | GET | `/api/v1/compras/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Compras | PUT | `/api/v1/compras/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Compras | DELETE | `/api/v1/compras/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Compras | GET | `/api/v1/compras/{id}/adjuntos` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Compras | POST | `/api/v1/compras/{id}/adjuntos` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Compras | GET | `/api/v1/compras/{id}/adjuntos/{adjuntoId}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Compras | DELETE | `/api/v1/compras/{id}/adjuntos/{adjuntoId}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Compras | POST | `/api/v1/compras/{id}/anulacion` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado. Solo compras en BORRADOR |
| Compras | POST | `/api/v1/compras/{id}/recepcion` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado. Mueve Kardex, costo promedio y crea CxP |

### Proveedores

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Proveedores | GET | `/api/v1/proveedores` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Proveedores | POST | `/api/v1/proveedores` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Proveedores | GET | `/api/v1/proveedores/documento` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Proveedores | GET | `/api/v1/proveedores/estadisticas/dashboard` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Proveedores | GET | `/api/v1/proveedores/estadisticas/tipo-documento` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Proveedores | GET | `/api/v1/proveedores/incompletos` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Proveedores | GET | `/api/v1/proveedores/validar/documento` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Proveedores | GET | `/api/v1/proveedores/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Proveedores | PUT | `/api/v1/proveedores/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Proveedores | DELETE | `/api/v1/proveedores/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |

### Cuentas por pagar

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Cuentas por pagar | GET | `/api/v1/cuentas-por-pagar` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Cuentas por pagar | GET | `/api/v1/cuentas-por-pagar/compra/{compraId}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Cuentas por pagar | GET | `/api/v1/cuentas-por-pagar/dashboard` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Cuentas por pagar | GET | `/api/v1/cuentas-por-pagar/proximas-vencer` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Cuentas por pagar | GET | `/api/v1/cuentas-por-pagar/vencidas` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Cuentas por pagar | GET | `/api/v1/cuentas-por-pagar/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Cuentas por pagar | POST | `/api/v1/cuentas-por-pagar/{id}/pago` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |

### Catálogo

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Catálogo | GET | `/api/v1/catalogo-fracciones` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | POST | `/api/v1/catalogo-fracciones` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/catalogo-fracciones/estadisticas/dashboard` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/catalogo-fracciones/producto/{productoId}/disponibles` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/catalogo-fracciones/validar-factor` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/catalogo-fracciones/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | PUT | `/api/v1/catalogo-fracciones/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | DELETE | `/api/v1/catalogo-fracciones/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/categorias` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | POST | `/api/v1/categorias` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/categorias/estadisticas/dashboard` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/categorias/raices` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/categorias/validar-slug` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/categorias/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | PUT | `/api/v1/categorias/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | DELETE | `/api/v1/categorias/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/categorias/{parentId}/hijas` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/productos` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | POST | `/api/v1/productos` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/productos/check-sku` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/productos/dashboard/statistics` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/productos/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | PUT | `/api/v1/productos/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | DELETE | `/api/v1/productos/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/servicios` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | POST | `/api/v1/servicios` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/servicios/check-nombre` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/servicios/dashboard/statistics` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | GET | `/api/v1/servicios/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | PUT | `/api/v1/servicios/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Catálogo | DELETE | `/api/v1/servicios/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |

### Inventario

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Inventario | GET | `/api/v1/almacenes` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | POST | `/api/v1/almacenes` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | GET | `/api/v1/almacenes/estadisticas/dashboard` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | GET | `/api/v1/almacenes/sede/{sedeId}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | GET | `/api/v1/almacenes/validar-nombre` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | GET | `/api/v1/almacenes/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | PUT | `/api/v1/almacenes/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | DELETE | `/api/v1/almacenes/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | GET | `/api/v1/kardex` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | GET | `/api/v1/kardex/exportar` | ✅ | ❌ | Solo ADMIN. Solo ADMIN; CSV con tope de filas |
| Inventario | GET | `/api/v1/kardex/mermas` | ✅ | ❌ | Solo ADMIN. Solo ADMIN (costos) |
| Inventario | GET | `/api/v1/kardex/movimientos` | ✅ | ✅ |  |
| Inventario | POST | `/api/v1/kardex/movimientos` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | GET | `/api/v1/kardex/movimientos/{id}` | ✅ | ✅ |  |
| Inventario | GET | `/api/v1/kardex/producto/{productoId}/historial` | ✅ | ✅ |  |
| Inventario | GET | `/api/v1/kardex/saldos` | ✅ | ✅ |  |
| Inventario | GET | `/api/v1/kardex/saldos/almacen/{almacenId}` | ✅ | ✅ |  |
| Inventario | GET | `/api/v1/kardex/saldos/producto/{productoId}` | ✅ | ✅ |  |
| Inventario | POST | `/api/v1/kardex/transferencias` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | GET | `/api/v1/kardex/valorizacion` | ✅ | ❌ | Solo ADMIN. Solo ADMIN (costos) |
| Inventario | GET | `/api/v1/kardex/valorizacion/almacen/{almacenId}` | ✅ | ❌ | Solo ADMIN. Solo ADMIN (costos) |
| Inventario | POST | `/api/v1/stock` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | POST | `/api/v1/stock/ajuste` | ✅ | ✅ | MULTIFUNCIONAL: solo almacenes de su sede; > S/ 500 exige justificación (configurable) |
| Inventario | GET | `/api/v1/stock/ajustes` | ✅ | ✅ | MULTIFUNCIONAL: solo su sede, sin costos |
| Inventario | GET | `/api/v1/stock/almacen/{almacenId}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | GET | `/api/v1/stock/almacen/{almacenId}/producto/{productoId}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | GET | `/api/v1/stock/bajo-stock` | ✅ | ✅ | MULTIFUNCIONAL: solo su sede |
| Inventario | GET | `/api/v1/stock/consolidado` | ✅ | ✅ | MULTIFUNCIONAL: solo su sede |
| Inventario | GET | `/api/v1/stock/dashboard` | ✅ | ✅ | MULTIFUNCIONAL: solo su sede, sin valorización |
| Inventario | GET | `/api/v1/stock/producto/{productoId}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Inventario | POST | `/api/v1/stock/transferencia` | ✅ | ✅ | MULTIFUNCIONAL: origen y destino en su sede |
| Inventario | GET | `/api/v1/stock/transferencias` | ✅ | ✅ | MULTIFUNCIONAL: solo su sede |
| Inventario | GET | `/api/v1/stock/transferencias/{id}` | ✅ | ✅ | MULTIFUNCIONAL: sin costos |

### Finanzas

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Finanzas | GET | `/api/v1/finanzas/cuentas` | ✅ | ❌ | Solo ADMIN |
| Finanzas | POST | `/api/v1/finanzas/cuentas` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/cuentas/{id}` | ✅ | ❌ | Solo ADMIN |
| Finanzas | PUT | `/api/v1/finanzas/cuentas/{id}` | ✅ | ❌ | Solo ADMIN |
| Finanzas | DELETE | `/api/v1/finanzas/cuentas/{id}` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/cuentas/{id}/saldos` | ✅ | ❌ | Solo ADMIN |
| Finanzas | POST | `/api/v1/finanzas/cuentas/{id}/saldos` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/dashboard/evolucion-mensual` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/dashboard/kpis` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/dashboard/patrimonio` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/flujo-caja` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/flujo-caja/devengado` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/flujo-caja/proyeccion` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/retiros-socios` | ✅ | ❌ | Solo ADMIN |
| Finanzas | POST | `/api/v1/finanzas/retiros-socios` | ✅ | ❌ | Solo ADMIN. Requiere turno de caja ABIERTO |
| Finanzas | GET | `/api/v1/finanzas/retiros-socios/dashboard` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/retiros-socios/{id}` | ✅ | ❌ | Solo ADMIN |
| Finanzas | POST | `/api/v1/finanzas/retiros-socios/{id}/anular` | ✅ | ❌ | Solo ADMIN. Requiere turno de caja ABIERTO |
| Finanzas | GET | `/api/v1/finanzas/utilidad-neta` | ✅ | ❌ | Solo ADMIN |
| Finanzas | GET | `/api/v1/finanzas/utilidad-neta/periodo` | ✅ | ❌ | Solo ADMIN |

### RRHH

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| RRHH | POST | `/api/v1/rrhh/asistencias/configurar-totp/{empleadoId}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | GET | `/api/v1/rrhh/asistencias/dashboard` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | GET | `/api/v1/rrhh/asistencias/empleados` | ✅ | ✅ | Público (sin token). Público (combo del kiosco); expone la lista de empleados |
| RRHH | GET | `/api/v1/rrhh/asistencias/estado-dia` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | GET | `/api/v1/rrhh/asistencias/listar` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | POST | `/api/v1/rrhh/asistencias/manual` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | POST | `/api/v1/rrhh/asistencias/marcar-qr` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | POST | `/api/v1/rrhh/asistencias/marcar-qr-real` | ✅ | ✅ | Público (sin token). Público (kiosco de marcación con QR/TOTP) |
| RRHH | GET | `/api/v1/rrhh/asistencias/mis-asistencias` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | GET | `/api/v1/rrhh/asistencias/qr-config/{empleadoId}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | GET | `/api/v1/rrhh/asistencias/qr-publico` | ✅ | ✅ | Público (sin token). Público (kiosco de marcación) |
| RRHH | GET | `/api/v1/rrhh/asistencias/ultima-marcacion` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | GET | `/api/v1/rrhh/asistencias/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | PUT | `/api/v1/rrhh/asistencias/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | DELETE | `/api/v1/rrhh/asistencias/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| RRHH | GET | `/api/v1/rrhh/comisiones` | ✅ | ❌ | Solo ADMIN |
| RRHH | POST | `/api/v1/rrhh/comisiones/calcular` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/comisiones/empleado/{userId}` | ✅ | ✅ | MULTIFUNCIONAL: solo las propias |
| RRHH | GET | `/api/v1/rrhh/comisiones/periodo` | ✅ | ❌ | Solo ADMIN |
| RRHH | PATCH | `/api/v1/rrhh/comisiones/{id}/aprobar` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/legajos` | ✅ | ❌ | Solo ADMIN |
| RRHH | POST | `/api/v1/rrhh/legajos` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/legajos/dashboard` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/legajos/me` | ✅ | ✅ | Legajo propio |
| RRHH | GET | `/api/v1/rrhh/legajos/{empleadoId}` | ✅ | ❌ | Solo ADMIN |
| RRHH | PUT | `/api/v1/rrhh/legajos/{empleadoId}` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/legajos/{id}/documentos` | ✅ | ✅ | MULTIFUNCIONAL: solo los de su propio legajo |
| RRHH | POST | `/api/v1/rrhh/legajos/{id}/documentos` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/legajos/{id}/documentos/{docId}` | ✅ | ✅ | MULTIFUNCIONAL: solo los de su propio legajo |
| RRHH | DELETE | `/api/v1/rrhh/legajos/{id}/documentos/{docId}` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/nominas` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/nominas/dashboard` | ✅ | ❌ | Solo ADMIN |
| RRHH | POST | `/api/v1/rrhh/nominas/generar` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/nominas/{id}` | ✅ | ❌ | Solo ADMIN |
| RRHH | DELETE | `/api/v1/rrhh/nominas/{id}` | ✅ | ❌ | Solo ADMIN |
| RRHH | POST | `/api/v1/rrhh/nominas/{id}/aprobar` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/nominas/{id}/boleta` | ✅ | ✅ | MULTIFUNCIONAL: solo su propia boleta |
| RRHH | POST | `/api/v1/rrhh/nominas/{id}/pagar` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/turnos` | ✅ | ❌ | Solo ADMIN |
| RRHH | POST | `/api/v1/rrhh/turnos` | ✅ | ❌ | Solo ADMIN |
| RRHH | POST | `/api/v1/rrhh/turnos/asignar` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/turnos/dashboard` | ✅ | ❌ | Solo ADMIN |
| RRHH | GET | `/api/v1/rrhh/turnos/empleado/{userId}` | ✅ | ✅ | MULTIFUNCIONAL: solo los propios |
| RRHH | GET | `/api/v1/rrhh/turnos/{id}` | ✅ | ✅ |  |
| RRHH | PUT | `/api/v1/rrhh/turnos/{id}` | ✅ | ❌ | Solo ADMIN |
| RRHH | DELETE | `/api/v1/rrhh/turnos/{id}` | ✅ | ❌ | Solo ADMIN |

### Reportes

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Reportes | GET | `/api/v1/reportes/caja/arqueos` | ✅ | ✅ | MULTIFUNCIONAL: solo sus propios turnos |
| Reportes | GET | `/api/v1/reportes/caja/arqueos/exportar` | ✅ | ❌ | Solo ADMIN |
| Reportes | GET | `/api/v1/reportes/clientes/frecuentes` | ✅ | ✅ | MULTIFUNCIONAL: solo su sede |
| Reportes | GET | `/api/v1/reportes/clientes/frecuentes/exportar` | ✅ | ❌ | Solo ADMIN |
| Reportes | GET | `/api/v1/reportes/productos/estrella` | ✅ | ✅ | MULTIFUNCIONAL: su sede; criterio=MARGEN → 403 |
| Reportes | GET | `/api/v1/reportes/productos/rentabilidad` | ✅ | ✅ | MULTIFUNCIONAL: solo su sede, sin costo ni margen |
| Reportes | GET | `/api/v1/reportes/productos/rentabilidad/exportar` | ✅ | ❌ | Solo ADMIN |
| Reportes | GET | `/api/v1/reportes/proveedores/ranking` | ✅ | ✅ | MULTIFUNCIONAL: su sede, sin montos; criterio=VOLUMEN → 403 |
| Reportes | GET | `/api/v1/reportes/proveedores/ranking/exportar` | ✅ | ❌ | Solo ADMIN |
| Reportes | GET | `/api/v1/reportes/ventas` | ✅ | ✅ | MULTIFUNCIONAL: solo su sede |
| Reportes | GET | `/api/v1/reportes/ventas/exportar` | ✅ | ❌ | Solo ADMIN |
| Reportes | GET | `/api/v1/reportes/ventas/resumen` | ✅ | ✅ | MULTIFUNCIONAL: solo su sede |

### Configuración

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Configuración | GET | `/api/v1/configuracion/empresa` | ✅ | ✅ | MULTIFUNCIONAL: solo lectura |
| Configuración | PUT | `/api/v1/configuracion/empresa` | ✅ | ❌ | Solo ADMIN |
| Configuración | GET | `/api/v1/configuracion/parametros` | ✅ | ✅ | MULTIFUNCIONAL: solo lectura |
| Configuración | PUT | `/api/v1/configuracion/parametros` | ✅ | ❌ | Solo ADMIN |
| Configuración | GET | `/api/v1/configuracion/publica` | ✅ | ✅ | Público (sin token). Público (pantalla de login): nombre comercial y logo |
| Configuración | GET | `/api/v1/configuracion/series` | ✅ | ✅ | MULTIFUNCIONAL: solo lectura |
| Configuración | PUT | `/api/v1/configuracion/series` | ✅ | ❌ | Solo ADMIN |

### Auditoría

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Auditoría | GET | `/api/v1/auditoria/dashboard` | ✅ | ❌ | Solo ADMIN |
| Auditoría | GET | `/api/v1/auditoria/logs` | ✅ | ❌ | Solo ADMIN |
| Auditoría | GET | `/api/v1/auditoria/logs/entidad/{tabla}/{registroId}` | ✅ | ❌ | Solo ADMIN |
| Auditoría | GET | `/api/v1/auditoria/logs/exportar` | ✅ | ❌ | Solo ADMIN |
| Auditoría | GET | `/api/v1/auditoria/logs/usuario/{userId}` | ✅ | ❌ | Solo ADMIN |
| Auditoría | GET | `/api/v1/auditoria/logs/{id}` | ✅ | ❌ | Solo ADMIN |

### Administración

| Módulo | Método | Endpoint | ADMIN | MULTIFUNCIONAL | Notas |
|--------|--------|----------|-------|----------------|-------|
| Administración | GET | `/api/v1/roles` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Administración | POST | `/api/v1/roles` | ✅ | ✅ | 🔴 Sin @PreAuthorize: MULTIFUNCIONAL puede ejecutarlo (escalamiento de privilegios). Debe ser solo ADMIN |
| Administración | GET | `/api/v1/roles/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Administración | PUT | `/api/v1/roles/{id}` | ✅ | ✅ | 🔴 Sin @PreAuthorize: MULTIFUNCIONAL puede ejecutarlo (escalamiento de privilegios). Debe ser solo ADMIN |
| Administración | DELETE | `/api/v1/roles/{id}` | ✅ | ✅ | 🔴 Sin @PreAuthorize: MULTIFUNCIONAL puede ejecutarlo (escalamiento de privilegios). Debe ser solo ADMIN |
| Administración | GET | `/api/v1/sedes` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Administración | POST | `/api/v1/sedes` | ✅ | ✅ | 🔴 Sin @PreAuthorize: MULTIFUNCIONAL puede ejecutarlo (escalamiento de privilegios). Debe ser solo ADMIN |
| Administración | GET | `/api/v1/sedes/estado` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Administración | GET | `/api/v1/sedes/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Administración | PUT | `/api/v1/sedes/{id}` | ✅ | ✅ | 🔴 Sin @PreAuthorize: MULTIFUNCIONAL puede ejecutarlo (escalamiento de privilegios). Debe ser solo ADMIN |
| Administración | DELETE | `/api/v1/sedes/{id}` | ✅ | ✅ | 🔴 Sin @PreAuthorize: MULTIFUNCIONAL puede ejecutarlo (escalamiento de privilegios). Debe ser solo ADMIN |
| Administración | GET | `/api/v1/users` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Administración | POST | `/api/v1/users` | ✅ | ✅ | 🔴 Sin @PreAuthorize: MULTIFUNCIONAL puede ejecutarlo (escalamiento de privilegios). Debe ser solo ADMIN |
| Administración | GET | `/api/v1/users/{id}` | ✅ | ✅ | ⚠️ Sin @PreAuthorize: cualquier usuario autenticado |
| Administración | PUT | `/api/v1/users/{id}` | ✅ | ✅ | 🔴 Sin @PreAuthorize: MULTIFUNCIONAL puede ejecutarlo (escalamiento de privilegios). Debe ser solo ADMIN |
| Administración | DELETE | `/api/v1/users/{id}` | ✅ | ✅ | 🔴 Sin @PreAuthorize: MULTIFUNCIONAL puede ejecutarlo (escalamiento de privilegios). Debe ser solo ADMIN |
