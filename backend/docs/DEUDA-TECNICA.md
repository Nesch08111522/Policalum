# DEUDA TÉCNICA — Backend POLICALUM ERP

> Actualizado el 2026-10-09, al cerrar el backend para el frontend en Angular (18 módulos, 348 tests unitarios en verde).
> M15 (Webhooks) queda pausado hasta que exista la página web; M2 (SUNAT) hasta tener certificado y proveedor.
> Hallazgos verificados en ejecución real: ver el detalle y las líneas exactas en `docs/REPORTE-LIMPIEZA.md`.
> Sprints de 1 semana; esfuerzo en días-desarrollador.

## Resumen

| Prioridad | Ítems | Esfuerzo total | Cuándo |
|---|---|---|---|
| 🔴 Crítico — Sprint 0 (antes de que el frontend consuma la API) | 9 | ~3,5 días | Inmediato |
| 🔴 Crítico — Sprint 1 | 3 | ~8 días | Sprint 1 |
| 🟠 Alto | 6 | ~9 días | Sprints 2-3 |
| 🟡 Medio | 9 | ~5,5 días + M2 (10-15) + M15 (5-6) | Sprints 4-5 |
| ⚪ Bajo | 10 | ~6 días | Backlog |

---

## 🔴 Crítico — Sprint 0 (antes del frontend)

Son bugs y agujeros **verificados ejecutando la aplicación**. Rompen pantallas que el frontend usará el primer día o permiten
que un MULTIFUNCIONAL se convierta en ADMIN.

- **S0-1. Escalamiento de privilegios en usuarios, roles y sedes** — `UserController`, `RolController` y `SedeController` no
  tienen `@PreAuthorize` y sus servicios no validan el rol: un MULTIFUNCIONAL creó un usuario ADMIN e inició sesión con él.
  Corrección: `@PreAuthorize("hasRole('ADMIN')")` a nivel de clase en los tres.
  Impacto: **Crítico** (control total del sistema). Esfuerzo: 0,25 días. Sprint: 0.
- **S0-2. Escrituras antiguas de stock sin control** — `POST /kardex/movimientos` acepta el `user` del body (falsifica la
  autoría del Kardex) y `POST /stock` sobrescribe la cantidad sin movimiento de Kardex; ambos responden a cualquier usuario
  autenticado. Corrección: retirarlos (los reemplazan `/stock/ajuste` y `/stock/transferencia` de M11) o dejarlos solo para
  ADMIN e ignorar `user`. Impacto: Alto (inventario y auditoría alterables). Esfuerzo: 0,5 días. Sprint: 0.
- **S0-3. Listados que dan 500 sin filtros** — `GET /ventas`, `/cotizaciones` y `/kardex` (fechas null en
  `(:desde IS NULL OR …)`) y `GET /productos`, `/categorias`, `/almacenes`, `/catalogo-fracciones`, `/servicios` y
  `/stock/almacen/{id}` (`TRIM`/`LOWER` sobre texto null → `bytea`). Corrección: patrón de M10–M16 (`FECHA_MIN`/`FECHA_MAX` y
  patrón de búsqueda armado en Java). Impacto: Alto (las pantallas principales no cargan). Esfuerzo: 1 día. Sprint: 0.
- **S0-4. Flujo de compras roto** — `POST /compras` da 500 (`Compra.detalles` null: `@Builder` sin `@Builder.Default`) y el
  detalle, la edición, la recepción y la anulación dan 500 (`JOIN FETCH` de dos listas en `findWithDetailsById`). Corrección:
  `@Builder.Default` en `detalles`/`adjuntos`, quitar el fetch de `adjuntos` e incluir colecciones en
  `EntidadesBuilderDefaultsTest`. Impacto: Alto (M1 inutilizable). Esfuerzo: 0,5 días. Sprint: 0.
- **S0-5. No se pueden crear ni editar clientes** — `@Size` sobre el enum `tipoDocumento` en `ClienteRequestDTO` → 400
  siempre. Corrección: quitar la anotación. Impacto: Alto. Esfuerzo: 0,1 días. Sprint: 0.
- **S0-6. CORS no permite `PATCH`** — el preflight desde `http://localhost:4200` responde 403 para `PATCH`; afecta
  `PATCH /despachos/{id}/estado` y `PATCH /rrhh/comisiones/{id}/aprobar`. Corrección: agregar `PATCH` a `setAllowedMethods`
  en `SecurityConfig`. Impacto: Alto para el frontend. Esfuerzo: 0,1 días. Sprint: 0.
- **S0-7. Errores del cliente devuelven 500** — `GlobalExceptionHandler` no maneja parámetro faltante, UUID inválido, JSON
  mal formado, método no soportado ni ruta inexistente (12 GET de validación dan 500), y roles, sedes y usuarios
  inexistentes lanzan `RuntimeException` (500 en vez de 404). Corrección: 5 handlers nuevos y `ResourceNotFoundException`.
  Impacto: Medio-alto (el frontend no puede distinguir error suyo de caída del servidor). Esfuerzo: 0,5 días. Sprint: 0.
- **S0-8. Contrato JSON de las páginas** — Spring advierte *"Serializing PageImpl instances as-is is not supported"*: el JSON
  de `Page<…>` puede cambiar entre versiones. Decidir **antes** de que Angular dependa de él: activar
  `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` (formato estable `{content, page:{size, number,
  totalElements, totalPages}}`) y documentarlo. Impacto: Medio (romper el frontend en una actualización). Esfuerzo: 0,25
  días. Sprint: 0.
- **S0-9. Detalles menores que el frontend notará** — `GET /preformas/vencidas` da 500 (lazy loading tras
  `clearAutomatically`, bug de M18); `POST /categorias` no devuelve el `id` creado. Impacto: Medio. Esfuerzo: 0,25 días.
  Sprint: 0.

## 🔴 Crítico — Sprint 1

- **ddl-auto=update en producción** — Hibernate altera el esquema al arrancar: puede borrar o reinterpretar columnas sin
  aviso y no hay forma de revertir. Ya obligó a SQL de reparación manual (`sql/reparacion_builder_defaults.sql`).
  Corrección: `validate` en producción una vez exista Liquibase. Impacto: Alto (pérdida de datos). Esfuerzo: 2 días.
  Sprint: 1.
- **Sin Liquibase** — Migraciones no auditables ni reproducibles; los ambientes divergen. Corrección: Liquibase con un
  changelog base generado desde la BD actual (`generateChangeLog`) + un changeset por cambio futuro. Impacto: Alto.
  Esfuerzo: 3 días. Sprint: 1.
- **Sin tests de integración contra PostgreSQL** — Los 348 tests son unitarios con mocks; ninguno ejecuta las consultas
  reales. Por eso S0-3, S0-4 y S0-5 llegaron hasta hoy (el validador de HQL valida sintaxis, no tipos de PostgreSQL ni
  fetch múltiples). Corrección: Testcontainers (PostgreSQL) con un smoke test de todos los GET y los flujos de alta, en CI.
  Impacto: Alto. Esfuerzo: 3 días. Sprint: 1.

## 🟠 Alto (sprints 2-3)

- **JWT secret hardcodeado** — `jwt.secret` está en `application.properties` (junto con la contraseña de la BD y
  `totp.secret-key-publico`): quien tenga el repositorio puede firmar tokens válidos. Corrección: variables de entorno
  (`${JWT_SECRET}`), rotar el secreto actual y subir jjwt 0.11.5 → 0.12.x. Impacto: Alto. Esfuerzo: 1 día. Sprint: 2.
- **IGV hardcodeado en 5 servicios** — `0.18`/`18.00` en `VentaServiceImpl`, `CotizacionServiceImpl`,
  `PreformaServiceImpl`, `CompraServiceImpl` y `UtilidadNetaServiceImpl`, más el default `porcentajeIgv` de 4 entidades
  de detalle. M14 dejó la infraestructura (`ConfiguracionService`); falta un componente `Igv` único y migrar.
  Impacto: Alto si cambia la tasa o hay productos exonerados. Esfuerzo: 2 días. Sprint: 2.
- **Valores RRHH hardcodeados** — ONP 13% (`NominaServiceImpl.java:60`), tolerancia de 10 minutos
  (`RrhhAsistenciaImpl.java:50`) y tope de 48 h semanales (`TurnoTrabajoServiceImpl.java:59`). Corrección: dominio
  `configuracion_rrhh` en M14 con los mismos valores por defecto. Impacto: Medio-alto (cambios legales requieren redeploy).
  Esfuerzo: 1,5 días. Sprint: 2.
- **Sin perfiles dev/prod** — Un solo `application.properties`: en producción quedarían `show-sql=true`, Swagger abierto,
  los usuarios del `DataLoader` con contraseña fija (`admin123`) y el **secreto TOTP escrito en el log**
  (`DataLoader.java:183`). Corrección: `application-dev`/`application-prod`, `DataLoader` solo en dev, sin secretos en logs.
  Impacto: Alto. Esfuerzo: 1 día. Sprint: 2.
- **114 endpoints sin `@PreAuthorize` restantes** — Tras S0-1 (16 de usuarios, roles y sedes) quedan maestros y operaciones que responden a
  cualquier autenticado (precios de productos/fracciones/servicios, recepción de compras, pagos de CxP, almacenes…).
  Corrección: anotar según `docs/API-ROLES.md`. Impacto: Medio-alto. Esfuerzo: 2 días. Sprint: 3.
- **DTOs de request con entidades JPA** — `AlmacenRequestDTO.sede`, `CategoriaRequestDTO.parent`,
  `KardexMovimientoRequestDTO`, `StockRequestDTO` y `UserRequestDTO` (`rol`, `sede` y un campo `passwordHash` que recibe la
  contraseña en claro) permiten asignación masiva. Corrección: ids en lugar de entidades, como en los módulos nuevos.
  Impacto: Medio. Esfuerzo: 1,5 días. Sprint: 3.

## 🟡 Medio (sprints 4-5)

- **isDevelopmentEnvironment() en ExceptionHandler siempre true** — `GlobalExceptionHandler.java:159` devuelve `true`, así
  que todo 500 incluye `details` con el mensaje interno (SQL, clases); se vio en la prueba. Corrección: depender del perfil
  activo. Impacto: Medio (filtra información). Esfuerzo: 0,25 días. Sprint: 4.
- **Migración a SUNAT (M2)** — Comprobantes electrónicos: hoy las ventas quedan `estadoSunat = PENDIENTE` y
  `serieCorrelativo = INT-…`. Ya existen las series por sede (M14) y los datos de la empresa con RUC validado. Espera
  certificado digital y proveedor (OSE/PSE). Impacto: Alto en lo legal cuando entre en vigencia. Esfuerzo: 10-15 días.
  Sprint: 4-5 (sujeto al certificado).
- **Migración a Webhooks (M15)** — Ventas online (MercadoPago; Niubiz por definir). Diseño aprobado en análisis:
  guardar el evento → procesar async con reintentos e idempotencia en 3 capas. Pendiente: plataforma de la tienda, cómo
  registrar ventas sin turno (`registrarOnline`) y el método de pago `MERCADOPAGO`. Espera la página web. Impacto: Medio.
  Esfuerzo: 5-6 días. Sprint: cuando exista la web.
- **El cierre de caja cuenta el efectivo de ventas anuladas** — `PagoVentaRepository.sumMontoByTurnoAndMetodo` no filtra
  por estado de la venta, así que el esperado del cierre incluye ventas anuladas. El reporte de arqueos (M12) lo muestra en
  `efectivoVentasAnuladas`. Corrección: filtrar `COMPLETADA` (y registrar la devolución de efectivo al anular).
  Impacto: Medio (descuadres de caja). Esfuerzo: 1 día. Sprint: 4.
- **Costo histórico en ventas** — `DetalleVenta` no guarda el costo del momento: la rentabilidad (M12) usa el costo
  promedio actual. Corrección: columna `costoUnitario` en `DetalleVenta`/`DetallePreforma` llenada al vender. Impacto:
  Medio (márgenes aproximados). Esfuerzo: 1,5 días. Sprint: 4.
- **CORS demasiado amplio** — Permite `https://*.ngrok-free.dev`, `http://192.168.*:*` e IPs de desarrollo con
  `allowCredentials=true`. Corrección: orígenes por perfil (`CORS_ORIGINS` en prod). Impacto: Medio. Esfuerzo: 0,25 días.
  Sprint: 4.
- **Swagger abierto en producción** — `/swagger-ui.html` y `/v3/api-docs` son públicos. Corrección: en el perfil prod,
  `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false`, o protegerlos para ADMIN. Impacto: Bajo-medio.
  Esfuerzo: 0,1 días. Sprint: 4.
- **Nombre engañoso `valorUnitarioNeto`** — En `DetalleVenta` y `DetallePreforma` guarda el neto de **toda la línea**, no el
  unitario. Corrección: renombrar la columna con migración Liquibase. Impacto: Bajo-medio (errores en futuros reportes).
  Esfuerzo: 0,5 días. Sprint: 5.
- **Documentación OpenAPI parcial** — `@Operation`/`@ApiResponses` solo en Ventas, Compras, Cotizaciones, Preformas y
  Nóminas (43 endpoints); los otros 230 tienen tag y esquemas pero sin descripción. Impacto: Bajo-medio. Esfuerzo: 2 días.
  Sprint: 5.

## ⚪ Bajo (backlog)

- **TODO(M12.1): puntualidad de proveedores requiere `fechaEntregaPrometida`** — Hoy el ranking mide días de entrega
  (emisión → entrada en Kardex) porque las compras no guardan fecha prometida y la recepción es siempre total.
  Impacto: Bajo. Esfuerzo: 1 día. Sprint: backlog.
- **TODO(M12.2): devoluciones con filtro de vendedor** — El resumen de ventas no resta devoluciones si se filtra por
  vendedor, cliente, canal o tipo (se advierte en la respuesta). Requiere una consulta de devoluciones con esos filtros.
  Impacto: Bajo. Esfuerzo: 0,5 días. Sprint: backlog.
- **Endpoint `/resumen-global` en reportes** — Un solo GET con los KPIs de los 6 reportes para el dashboard de inicio.
  Impacto: Bajo. Esfuerzo: 1 día. Sprint: backlog.
- **Purga de payloads de webhooks antiguos (cuando M15 se active)** — Borrar el payload de eventos `PROCESADO` con más de
  90 días y conservar el resumen. Impacto: Bajo. Esfuerzo: 0,5 días. Sprint: con M15.
- **Conciliación con MercadoPago (M15 fase 2)** — Job que consulta los pagos aprobados de las últimas horas y crea los
  eventos que se hayan perdido por caídas largas. Impacto: Bajo. Esfuerzo: 1,5 días. Sprint: después de M15.
- **TODO(M5.1): retiros con múltiples socios** — `RetiroSocioServiceImpl.java:136`: forzar `adminId = usuarioActual`
  cuando haya más de un socio. Impacto: Bajo. Esfuerzo: 0,25 días. Sprint: backlog.
- **Código muerto** — `TotpConfigService` (interfaz sin implementación), 3 DTOs huérfanos y 25 métodos públicos sin uso
  (lista en `docs/REPORTE-LIMPIEZA.md`). Impacto: Bajo. Esfuerzo: 0,5 días. Sprint: backlog.
- **Patrón `@Builder` + colección en `Cliente.ventas`** — Mismo origen que S0-4; hoy no falla porque nadie agrega ventas a
  esa lista. Corrección: `@Builder.Default`. Impacto: Bajo. Esfuerzo: 0,1 días. Sprint: backlog.
- **Actualizar springdoc 3.0.2 → 3.0.3** — 3.0.2 se eligió por estar ya en el repositorio local; 3.0.3 es la versión más
  reciente de la línea 3.0.x (Spring Boot 4.0). Impacto: Bajo. Esfuerzo: 0,1 días. Sprint: backlog.
- **Endpoint público de empleados del kiosco** — `GET /rrhh/asistencias/empleados` es `permitAll` para el combo del kiosco
  de marcación; revisar qué datos expone cuando se use en producción. Impacto: Bajo. Esfuerzo: 0,25 días. Sprint: backlog.

---

## Orden sugerido

1. **Sprint 0** (≈3,5 días): S0-1 a S0-9. Con esto el frontend puede consumir todos los listados y altas sin errores 500 y
   sin el agujero de privilegios.
2. **Sprint 1**: Liquibase → `ddl-auto=validate` → Testcontainers en CI.
3. **Sprints 2-3**: secretos y perfiles, IGV y RRHH configurables, permisos restantes, DTOs sin entidades.
4. **Sprints 4-5**: M2 SUNAT (cuando llegue el certificado) y deuda media; M15 cuando exista la web.
