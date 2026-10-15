# REPORTE DE LIMPIEZA — Backend POLICALUM ERP

> Generado el 2026-10-09. **Solo reporte: no se eliminó ni corrigió nada.** Fuentes: análisis estático de `src/main/java` y
> una ejecución real de la aplicación (Spring Boot 4.0.6 + PostgreSQL 16, BD vacía, usuarios del DataLoader), en la que se
> llamaron los 172 GET con parámetros por defecto y los flujos principales de escritura con la colección Postman.

## Resumen

| Hallazgo | Cantidad | Prioridad |
|---|---|---|
| 🔴 Bugs que rompen pantallas del frontend (verificados en ejecución) | 9 grupos | Sprint 0, antes del frontend |
| 🔴 Agujeros de seguridad (verificados) | 4 | Sprint 0 |
| Endpoints sin `@PreAuthorize` | 130 en 18 controllers | Sprint 1 |
| Servicios sin uso | 1 | Backlog |
| DTOs huérfanos | 3 | Backlog |
| Entidades sin uso | 0 | — |
| Métodos públicos sin uso | 25 | Backlog |
| TODO / FIXME | 1 | Backlog |

## 1. 🔴 Bugs que rompen el frontend (verificados en ejecución)

La prueba de los 172 GET dio **101 × 200, 46 × 404 (ids de prueba inexistentes) y 25 × 500**. Los 25 errores 500 más los de
escritura se agrupan así:

| # | Endpoints afectados | Síntoma | Causa | Ubicación | Corrección propuesta |
|---|---|---|---|---|---|
| 1.1 | `GET /ventas`, `GET /cotizaciones`, `GET /kardex` (listados sin filtro de fecha) | 500 `could not determine data type of parameter` | Parámetros `LocalDateTime` en null con el patrón `(:desde IS NULL OR …)`: PostgreSQL no puede tipar el null | `VentaRepository.java:29` (`buscar`), `CotizacionRepository.java:20` (`buscar`), `KardexRepository.java:29` (`searchMovimientos`) | Mismo patrón que M10–M13: el servicio reemplaza null por `FECHA_MIN`/`FECHA_MAX` y la consulta usa rango semiabierto sin `IS NULL` |
| 1.2 | `GET /productos`, `/categorias`, `/almacenes`, `/catalogo-fracciones`, `/servicios`, `/stock/almacen/{id}` (sin texto de búsqueda) | 500 `function btrim(bytea)` / `lower(bytea)` does not exist | `TRIM(:searchTerm)` / `LOWER(CONCAT(…, :searchTerm, …))` con `searchTerm` null: PostgreSQL lo tipa como `bytea` | `ProductoRepository.java:22`, `CategoriaRepository.java:21`, `AlmacenRepository.java:22`, `CatalogoFraccionRepository.java:25`, `ServicioRepository.java:29`, `StockRepository.java:34` | Armar el patrón en Java (`"%" + texto.toLowerCase() + "%"` o null) y en la consulta `(:patron IS NULL OR LOWER(x) LIKE :patron)`, como en Movilidad (M16) |
| 1.3 | `GET /compras/{id}`, `PUT /compras/{id}`, `POST /compras/{id}/recepcion`, `/anulacion`, `DELETE` y adjuntos | 500 `MultipleBagFetchException` | `findWithDetailsById` hace `JOIN FETCH` de dos `List` (`detalles` y `adjuntos`) | `CompraRepository.java:37-49`; usado por `CompraServiceImpl.java:177` y `:348` (`findCompra`) | Quitar el fetch de `adjuntos` (se cargan aparte) o cambiar una de las colecciones a `Set` |
| 1.4 | `POST /compras` | 500 `this.detalles is null` | `@Builder` ignora `= new ArrayList<>()` en `detalles` y `adjuntos`; el guardián `EntidadesBuilderDefaultsTest` excluye colecciones | `Compra.java:77` y `:80`, usado en `CompraServiceImpl.java:75` | `@Builder.Default` en ambas listas (como `Producto.fracciones`) y quitar la exclusión de colecciones del test guardián |
| 1.5 | `POST /clientes` y `PUT /clientes/{id}` | 400 siempre: `HV000030 No validator … Size … ClienteTipoDocumentoEnum` | `@Size` sobre un enum | `ClienteRequestDTO.java:11` | Quitar `@Size` del campo `tipoDocumento` |
| 1.6 | `GET /preformas/vencidas` | 500 `LazyInitializationException` (Rol) | `expirarVencidas` usa `clearAutomatically = true`, desconecta al usuario y luego `esAdmin(actual)` toca el rol perezoso (bug de M18) | `PreformaServiceImpl.java:270-274`, `PreformaVentaRepository.java:49` | Calcular `esAdmin`/`userId` antes de `expirarVencidas` |
| 1.7 | `GET /roles/{id}`, `GET /sedes/{id}` (y PUT/DELETE de roles, sedes y usuarios inexistentes) | 500 en vez de 404 | `new RuntimeException("… no encontrado")` | `RolImpl.java:49,61,71`, `SedeServiceImpl.java:53,61,74`, `UserImpl.java:75` | Lanzar `ResourceNotFoundException` |
| 1.8 | 12 GET con parámetro obligatorio ausente (`/productos/check-sku`, `/categorias/validar-slug`, `/almacenes/validar-nombre`, `/clientes/validar/*`, `/proveedores/documento`, `/proveedores/validar/documento`, `/servicios/check-nombre`, `/catalogo-fracciones/validar-factor`, `/sedes/estado`, `/rrhh/comisiones/periodo`, `/finanzas/utilidad-neta/periodo`); además UUID inválido en la ruta, JSON mal formado, método HTTP no soportado y ruta inexistente | 500 en vez de 400 / 405 / 404 | `GlobalExceptionHandler` no maneja `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException`, `HttpRequestMethodNotSupportedException` ni `NoResourceFoundException`; caen en el genérico `Exception` → 500 | `GlobalExceptionHandler.java:138` | Agregar esos 5 handlers (400, 400, 400, 405, 404) |
| 1.9 | `POST /categorias`; `POST /productos` con `fracciones` | La respuesta de categoría **no trae `id`**; un producto no se puede crear con sus fracciones en el mismo body | `CategoriaResponseDTO` sin campo `id`; `CatalogoFraccionRequestDTO.productoId` es obligatorio aunque el producto aún no existe | `CategoriaResponseDTO.java:5`; `ProductoRequestDTO` → `fracciones[].productoId` | Agregar `id` a la respuesta; DTO de fracción sin `productoId` para el alta anidada (hoy el alta crea 4 fracciones automáticas, así que basta enviar `fracciones: []`) |

> El frontend tropezará con 1.1, 1.2, 1.3, 1.4 y 1.5 el primer día: son los listados principales y las altas de compra y cliente.

## 2. 🔴 Seguridad (verificado en ejecución)

| # | Hallazgo | Evidencia | Ubicación | Corrección |
|---|---|---|---|---|
| 2.1 | **Escalamiento de privilegios:** un MULTIFUNCIONAL crea un usuario ADMIN | `POST /users` con el token de `empleado@policalum.com` → 201; login del usuario creado → `ROLE_ADMIN` | `UserController.java:31`, `RolController.java:31`, `SedeController.java:23` (sin `@PreAuthorize`; los servicios tampoco validan) | `@PreAuthorize("hasRole('ADMIN')")` a nivel de clase en los tres |
| 2.2 | `POST /kardex/movimientos` acepta `user` en el body y registra el movimiento a nombre de ese usuario; cualquier autenticado mueve stock sin las reglas de M11 (umbral, justificación, sede) | `KardexServiceImpl.java:122`: `dto.user() != null ? dto.user() : usuarioActual` | `KardexController.java:54` y `:61` (`/transferencias` antiguo) | Solo ADMIN, ignorar `user` del body, o retirarlos a favor de `/stock/ajuste` y `/stock/transferencia` (M11) |
| 2.3 | `POST /stock` sobrescribe la cantidad de stock sin movimiento de Kardex | `StockServiceImpl.saveOrUpdate` hace `stock.setCantidad(dto.cantidad())` | `StockController.java:57` | Retirar el endpoint (lo reemplaza `POST /stock/ajuste`) o dejarlo solo para ADMIN y registrar el Kardex |
| 2.4 | DTOs de request que reciben **entidades JPA** completas (asignación masiva: el cliente puede mandar campos que no debería) | `AlmacenRequestDTO.sede`, `CategoriaRequestDTO.parent`, `KardexMovimientoRequestDTO.producto/almacen/user`, `StockRequestDTO.almacen/producto`, `UserRequestDTO.rol/sede` (+ el campo se llama `passwordHash` pero recibe la contraseña en claro) | `application/dto/request/` | Cambiar a ids (`sedeId`, `parentId`, `rolId`…) como en los módulos nuevos |

## 3. Endpoints sin `@PreAuthorize` (130)

Hoy responden a **cualquier usuario autenticado**. Los 5 públicos de `SecurityConfig` (login, `configuracion/publica` y 3 de
asistencia por QR) no se cuentan. Detalle de permisos esperados en `docs/API-ROLES.md`.

| Controller | Endpoints | Riesgo |
|---|---|---|
| `KardexController` | 3 | 🔴 Alto: POST /movimientos acepta `user` del cliente (falsifica autoría) y mueve stock sin reglas de M11 |
| `RolController` | 5 | 🔴 Crítico: MULTIFUNCIONAL puede crear/editar/borrar roles |
| `SedeController` | 6 | 🔴 Alto: MULTIFUNCIONAL puede crear/editar/borrar sedes |
| `StockController` | 4 | 🔴 Alto: POST /stock sobrescribe la cantidad sin pasar por Kardex |
| `UserController` | 5 | 🔴 Crítico: escalamiento de privilegios (MULTIFUNCIONAL crea ADMIN, verificado) |
| `AlmacenController` | 8 | 🟠 MULTIFUNCIONAL puede crear/borrar almacenes |
| `CatalogoFraccionController` | 8 | 🟠 MULTIFUNCIONAL puede cambiar precios de fracciones |
| `CompraController` | 12 | 🟠 MULTIFUNCIONAL puede recibir/anular compras y ver costos |
| `CuentaPorPagarController` | 7 | 🟠 MULTIFUNCIONAL puede registrar pagos de CxP y ver deudas |
| `ProductoController` | 7 | 🟠 MULTIFUNCIONAL puede cambiar precios y costo de compra |
| `ServicioController` | 7 | 🟠 MULTIFUNCIONAL puede cambiar precios de servicios |
| `CategoriaController` | 9 | 🟡 Maestro editable por cualquier autenticado |
| `ClienteController` | 16 | 🟡 Operativo: razonable para ambos roles; falta declararlo |
| `CotizacionController` | 4 | 🟡 Solo GETs sin anotación; las escrituras sí la tienen |
| `ProveedorController` | 10 | 🟡 Maestro de proveedores editable por cualquier autenticado |
| `RrhhAsistenciaController` | 12 | 🟡 Revisar: marcación y consulta de asistencia de otros empleados |
| `TurnoCajaController` | 3 | 🟡 Operativo: razonable para ambos roles; falta declararlo |
| `VentaController` | 4 | 🟡 Operativo: razonable para ambos roles; falta declararlo |

<details><summary><code>AlmacenController</code> — 8 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/almacenes` | `infrastructure/controller/AlmacenController.java:56` (`search`) |
| POST | `/api/v1/almacenes` | `infrastructure/controller/AlmacenController.java:40` (`create`) |
| GET | `/api/v1/almacenes/estadisticas/dashboard` | `infrastructure/controller/AlmacenController.java:85` (`dashboard`) |
| GET | `/api/v1/almacenes/sede/{sedeId}` | `infrastructure/controller/AlmacenController.java:65` (`findBySede`) |
| GET | `/api/v1/almacenes/validar-nombre` | `infrastructure/controller/AlmacenController.java:76` (`validateName`) |
| DELETE | `/api/v1/almacenes/{id}` | `infrastructure/controller/AlmacenController.java:70` (`delete`) |
| GET | `/api/v1/almacenes/{id}` | `infrastructure/controller/AlmacenController.java:51` (`findById`) |
| PUT | `/api/v1/almacenes/{id}` | `infrastructure/controller/AlmacenController.java:45` (`update`) |

</details>

<details><summary><code>CatalogoFraccionController</code> — 8 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/catalogo-fracciones` | `infrastructure/controller/CatalogoFraccionController.java:58` (`search`) |
| POST | `/api/v1/catalogo-fracciones` | `infrastructure/controller/CatalogoFraccionController.java:41` (`create`) |
| GET | `/api/v1/catalogo-fracciones/estadisticas/dashboard` | `infrastructure/controller/CatalogoFraccionController.java:88` (`dashboard`) |
| GET | `/api/v1/catalogo-fracciones/producto/{productoId}/disponibles` | `infrastructure/controller/CatalogoFraccionController.java:67` (`findAvailableByProducto`) |
| GET | `/api/v1/catalogo-fracciones/validar-factor` | `infrastructure/controller/CatalogoFraccionController.java:79` (`validateFactor`) |
| DELETE | `/api/v1/catalogo-fracciones/{id}` | `infrastructure/controller/CatalogoFraccionController.java:73` (`delete`) |
| GET | `/api/v1/catalogo-fracciones/{id}` | `infrastructure/controller/CatalogoFraccionController.java:53` (`findById`) |
| PUT | `/api/v1/catalogo-fracciones/{id}` | `infrastructure/controller/CatalogoFraccionController.java:47` (`update`) |

</details>

<details><summary><code>CategoriaController</code> — 9 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/categorias` | `infrastructure/controller/CategoriaController.java:56` (`search`) |
| POST | `/api/v1/categorias` | `infrastructure/controller/CategoriaController.java:40` (`create`) |
| GET | `/api/v1/categorias/estadisticas/dashboard` | `infrastructure/controller/CategoriaController.java:88` (`dashboard`) |
| GET | `/api/v1/categorias/raices` | `infrastructure/controller/CategoriaController.java:64` (`findRoots`) |
| GET | `/api/v1/categorias/validar-slug` | `infrastructure/controller/CategoriaController.java:80` (`validateSlug`) |
| DELETE | `/api/v1/categorias/{id}` | `infrastructure/controller/CategoriaController.java:74` (`delete`) |
| GET | `/api/v1/categorias/{id}` | `infrastructure/controller/CategoriaController.java:51` (`findById`) |
| PUT | `/api/v1/categorias/{id}` | `infrastructure/controller/CategoriaController.java:45` (`update`) |
| GET | `/api/v1/categorias/{parentId}/hijas` | `infrastructure/controller/CategoriaController.java:69` (`findChildren`) |

</details>

<details><summary><code>ClienteController</code> — 16 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/clientes` | `infrastructure/controller/ClienteController.java:108` (`searchClientes`) |
| POST | `/api/v1/clientes` | `infrastructure/controller/ClienteController.java:40` (`createCliente`) |
| GET | `/api/v1/clientes/compras/activos` | `infrastructure/controller/ClienteController.java:145` (`getClientesWithPurchases`) |
| GET | `/api/v1/clientes/compras/inactivos` | `infrastructure/controller/ClienteController.java:161` (`getClientesWithoutPurchases`) |
| GET | `/api/v1/clientes/documento/{numeroDocumento}` | `infrastructure/controller/ClienteController.java:81` (`getClienteByDocumento`) |
| GET | `/api/v1/clientes/estadisticas/count` | `infrastructure/controller/ClienteController.java:193` (`countTotalClientes`) |
| GET | `/api/v1/clientes/estadisticas/dashboard` | `infrastructure/controller/ClienteController.java:179` (`getDashboardStatistics`) |
| GET | `/api/v1/clientes/estadisticas/hoy` | `infrastructure/controller/ClienteController.java:200` (`countClientesRegistradosHoy`) |
| GET | `/api/v1/clientes/estadisticas/tipo-documento` | `infrastructure/controller/ClienteController.java:186` (`getDistribucionPorTipoDocumento`) |
| GET | `/api/v1/clientes/incompletos` | `infrastructure/controller/ClienteController.java:136` (`getClientesIncompletos`) |
| GET | `/api/v1/clientes/recientes` | `infrastructure/controller/ClienteController.java:129` (`getRecentClientes`) |
| GET | `/api/v1/clientes/validar/documento` | `infrastructure/controller/ClienteController.java:209` (`validateDocumento`) |
| GET | `/api/v1/clientes/validar/email` | `infrastructure/controller/ClienteController.java:224` (`validateEmail`) |
| DELETE | `/api/v1/clientes/{id}` | `infrastructure/controller/ClienteController.java:93` (`deleteCliente`) |
| GET | `/api/v1/clientes/{id}` | `infrastructure/controller/ClienteController.java:69` (`getClienteById`) |
| PUT | `/api/v1/clientes/{id}` | `infrastructure/controller/ClienteController.java:54` (`updateCliente`) |

</details>

<details><summary><code>CompraController</code> — 12 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/compras` | `infrastructure/controller/CompraController.java:88` (`buscar`) |
| POST | `/api/v1/compras` | `infrastructure/controller/CompraController.java:54` (`crear`) |
| GET | `/api/v1/compras/dashboard` | `infrastructure/controller/CompraController.java:140` (`dashboard`) |
| DELETE | `/api/v1/compras/{id}` | `infrastructure/controller/CompraController.java:130` (`eliminar`) |
| GET | `/api/v1/compras/{id}` | `infrastructure/controller/CompraController.java:79` (`obtener`) |
| PUT | `/api/v1/compras/{id}` | `infrastructure/controller/CompraController.java:68` (`actualizar`) |
| GET | `/api/v1/compras/{id}/adjuntos` | `infrastructure/controller/CompraController.java:165` (`listarAdjuntos`) |
| POST | `/api/v1/compras/{id}/adjuntos` | `infrastructure/controller/CompraController.java:152` (`subirAdjunto`) |
| DELETE | `/api/v1/compras/{id}/adjuntos/{adjuntoId}` | `infrastructure/controller/CompraController.java:191` (`eliminarAdjunto`) |
| GET | `/api/v1/compras/{id}/adjuntos/{adjuntoId}` | `infrastructure/controller/CompraController.java:175` (`descargarAdjunto`) |
| POST | `/api/v1/compras/{id}/anulacion` | `infrastructure/controller/CompraController.java:118` (`anular`) |
| POST | `/api/v1/compras/{id}/recepcion` | `infrastructure/controller/CompraController.java:105` (`recibir`) |

</details>

<details><summary><code>CotizacionController</code> — 4 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/cotizaciones` | `infrastructure/controller/CotizacionController.java:63` (`search`) |
| GET | `/api/v1/cotizaciones/dashboard` | `infrastructure/controller/CotizacionController.java:115` (`dashboard`) |
| GET | `/api/v1/cotizaciones/vencidas` | `infrastructure/controller/CotizacionController.java:109` (`vencidas`) |
| GET | `/api/v1/cotizaciones/{id}` | `infrastructure/controller/CotizacionController.java:57` (`findById`) |

</details>

<details><summary><code>CuentaPorPagarController</code> — 7 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/cuentas-por-pagar` | `infrastructure/controller/CuentaPorPagarController.java:30` (`buscar`) |
| GET | `/api/v1/cuentas-por-pagar/compra/{compraId}` | `infrastructure/controller/CuentaPorPagarController.java:51` (`obtenerPorCompra`) |
| GET | `/api/v1/cuentas-por-pagar/dashboard` | `infrastructure/controller/CuentaPorPagarController.java:69` (`dashboard`) |
| GET | `/api/v1/cuentas-por-pagar/proximas-vencer` | `infrastructure/controller/CuentaPorPagarController.java:62` (`proximasAVencer`) |
| GET | `/api/v1/cuentas-por-pagar/vencidas` | `infrastructure/controller/CuentaPorPagarController.java:56` (`vencidas`) |
| GET | `/api/v1/cuentas-por-pagar/{id}` | `infrastructure/controller/CuentaPorPagarController.java:40` (`obtener`) |
| POST | `/api/v1/cuentas-por-pagar/{id}/pago` | `infrastructure/controller/CuentaPorPagarController.java:45` (`registrarPago`) |

</details>

<details><summary><code>KardexController</code> — 3 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/kardex` | `infrastructure/controller/KardexController.java:69` (`searchMovimientos`) |
| POST | `/api/v1/kardex/movimientos` | `infrastructure/controller/KardexController.java:55` (`registrarMovimiento`) |
| POST | `/api/v1/kardex/transferencias` | `infrastructure/controller/KardexController.java:62` (`registrarTransferencia`) |

</details>

<details><summary><code>ProductoController</code> — 7 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/productos` | `infrastructure/controller/ProductoController.java:97` (`search`) |
| POST | `/api/v1/productos` | `infrastructure/controller/ProductoController.java:49` (`create`) |
| GET | `/api/v1/productos/check-sku` | `infrastructure/controller/ProductoController.java:130` (`isSkuDisponible`) |
| GET | `/api/v1/productos/dashboard/statistics` | `infrastructure/controller/ProductoController.java:144` (`getDashboardStatistics`) |
| DELETE | `/api/v1/productos/{id}` | `infrastructure/controller/ProductoController.java:117` (`delete`) |
| GET | `/api/v1/productos/{id}` | `infrastructure/controller/ProductoController.java:81` (`findById`) |
| PUT | `/api/v1/productos/{id}` | `infrastructure/controller/ProductoController.java:68` (`update`) |

</details>

<details><summary><code>ProveedorController</code> — 10 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/proveedores` | `infrastructure/controller/ProveedorController.java:72` (`search`) |
| POST | `/api/v1/proveedores` | `infrastructure/controller/ProveedorController.java:43` (`create`) |
| GET | `/api/v1/proveedores/documento` | `infrastructure/controller/ProveedorController.java:59` (`findByDocumento`) |
| GET | `/api/v1/proveedores/estadisticas/dashboard` | `infrastructure/controller/ProveedorController.java:88` (`dashboard`) |
| GET | `/api/v1/proveedores/estadisticas/tipo-documento` | `infrastructure/controller/ProveedorController.java:93` (`statisticsByDocumentType`) |
| GET | `/api/v1/proveedores/incompletos` | `infrastructure/controller/ProveedorController.java:83` (`findIncompleteContactData`) |
| GET | `/api/v1/proveedores/validar/documento` | `infrastructure/controller/ProveedorController.java:98` (`validateDocument`) |
| DELETE | `/api/v1/proveedores/{id}` | `infrastructure/controller/ProveedorController.java:66` (`delete`) |
| GET | `/api/v1/proveedores/{id}` | `infrastructure/controller/ProveedorController.java:54` (`findById`) |
| PUT | `/api/v1/proveedores/{id}` | `infrastructure/controller/ProveedorController.java:48` (`update`) |

</details>

<details><summary><code>RolController</code> — 5 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/roles` | `infrastructure/controller/RolController.java:36` (`getAll`) |
| POST | `/api/v1/roles` | `infrastructure/controller/RolController.java:42` (`createRol`) |
| DELETE | `/api/v1/roles/{id}` | `infrastructure/controller/RolController.java:59` (`deleteRol`) |
| GET | `/api/v1/roles/{id}` | `infrastructure/controller/RolController.java:48` (`getRol`) |
| PUT | `/api/v1/roles/{id}` | `infrastructure/controller/RolController.java:53` (`updateRol`) |

</details>

<details><summary><code>RrhhAsistenciaController</code> — 12 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| POST | `/api/v1/rrhh/asistencias/configurar-totp/{empleadoId}` | `infrastructure/controller/RrhhAsistenciaController.java:108` (`configurarTOTP`) |
| GET | `/api/v1/rrhh/asistencias/dashboard` | `infrastructure/controller/RrhhAsistenciaController.java:263` (`obtenerDashboard`) |
| GET | `/api/v1/rrhh/asistencias/estado-dia` | `infrastructure/controller/RrhhAsistenciaController.java:298` (`obtenerEstadoDelDia`) |
| GET | `/api/v1/rrhh/asistencias/listar` | `infrastructure/controller/RrhhAsistenciaController.java:163` (`listarAsistencias`) |
| POST | `/api/v1/rrhh/asistencias/manual` | `infrastructure/controller/RrhhAsistenciaController.java:143` (`registrarManual`) |
| POST | `/api/v1/rrhh/asistencias/marcar-qr` | `infrastructure/controller/RrhhAsistenciaController.java:134` (`marcarPorQr`) |
| GET | `/api/v1/rrhh/asistencias/mis-asistencias` | `infrastructure/controller/RrhhAsistenciaController.java:192` (`misAsistencias`) |
| GET | `/api/v1/rrhh/asistencias/qr-config/{empleadoId}` | `infrastructure/controller/RrhhAsistenciaController.java:78` (`generarQrConfiguracion`) |
| GET | `/api/v1/rrhh/asistencias/ultima-marcacion` | `infrastructure/controller/RrhhAsistenciaController.java:283` (`obtenerUltimaMarcacion`) |
| DELETE | `/api/v1/rrhh/asistencias/{id}` | `infrastructure/controller/RrhhAsistenciaController.java:244` (`eliminarMarcacion`) |
| GET | `/api/v1/rrhh/asistencias/{id}` | `infrastructure/controller/RrhhAsistenciaController.java:215` (`buscarPorId`) |
| PUT | `/api/v1/rrhh/asistencias/{id}` | `infrastructure/controller/RrhhAsistenciaController.java:223` (`actualizarManual`) |

</details>

<details><summary><code>SedeController</code> — 6 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/sedes` | `infrastructure/controller/SedeController.java:34` (`listarTodas`) |
| POST | `/api/v1/sedes` | `infrastructure/controller/SedeController.java:28` (`crearSede`) |
| GET | `/api/v1/sedes/estado` | `infrastructure/controller/SedeController.java:40` (`listarPorEstado`) |
| DELETE | `/api/v1/sedes/{id}` | `infrastructure/controller/SedeController.java:60` (`eliminarSede`) |
| GET | `/api/v1/sedes/{id}` | `infrastructure/controller/SedeController.java:47` (`buscarPorId`) |
| PUT | `/api/v1/sedes/{id}` | `infrastructure/controller/SedeController.java:52` (`actualizarSede`) |

</details>

<details><summary><code>ServicioController</code> — 7 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/servicios` | `infrastructure/controller/ServicioController.java:81` (`search`) |
| POST | `/api/v1/servicios` | `infrastructure/controller/ServicioController.java:34` (`create`) |
| GET | `/api/v1/servicios/check-nombre` | `infrastructure/controller/ServicioController.java:106` (`isNombreDisponible`) |
| GET | `/api/v1/servicios/dashboard/statistics` | `infrastructure/controller/ServicioController.java:118` (`getDashboardStatistics`) |
| DELETE | `/api/v1/servicios/{id}` | `infrastructure/controller/ServicioController.java:97` (`delete`) |
| GET | `/api/v1/servicios/{id}` | `infrastructure/controller/ServicioController.java:66` (`findById`) |
| PUT | `/api/v1/servicios/{id}` | `infrastructure/controller/ServicioController.java:53` (`update`) |

</details>

<details><summary><code>StockController</code> — 4 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| POST | `/api/v1/stock` | `infrastructure/controller/StockController.java:58` (`saveOrUpdate`) |
| GET | `/api/v1/stock/almacen/{almacenId}` | `infrastructure/controller/StockController.java:37` (`getStockByAlmacen`) |
| GET | `/api/v1/stock/almacen/{almacenId}/producto/{productoId}` | `infrastructure/controller/StockController.java:46` (`getStockByAlmacenAndProducto`) |
| GET | `/api/v1/stock/producto/{productoId}` | `infrastructure/controller/StockController.java:53` (`getStockByProducto`) |

</details>

<details><summary><code>TurnoCajaController</code> — 3 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| POST | `/api/v1/turnos-caja` | `infrastructure/controller/TurnoCajaController.java:24` (`abrir`) |
| POST | `/api/v1/turnos-caja/{id}/cierre` | `infrastructure/controller/TurnoCajaController.java:30` (`cerrar`) |
| GET | `/api/v1/turnos-caja/{id}/resumen` | `infrastructure/controller/TurnoCajaController.java:29` (`resumen`) |

</details>

<details><summary><code>UserController</code> — 5 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/users` | `infrastructure/controller/UserController.java:42` (`listarUsuarios`) |
| POST | `/api/v1/users` | `infrastructure/controller/UserController.java:36` (`crearUsuario`) |
| DELETE | `/api/v1/users/{id}` | `infrastructure/controller/UserController.java:62` (`eliminarUsuario`) |
| GET | `/api/v1/users/{id}` | `infrastructure/controller/UserController.java:47` (`buscarUsuarioPorId`) |
| PUT | `/api/v1/users/{id}` | `infrastructure/controller/UserController.java:54` (`actualizarUsuario`) |

</details>

<details><summary><code>VentaController</code> — 4 endpoints</summary>

| Método | Endpoint | Ubicación |
|---|---|---|
| GET | `/api/v1/ventas` | `infrastructure/controller/VentaController.java:62` (`buscar`) |
| POST | `/api/v1/ventas` | `infrastructure/controller/VentaController.java:43` (`registrar`) |
| GET | `/api/v1/ventas/{id}` | `infrastructure/controller/VentaController.java:55` (`obtener`) |
| POST | `/api/v1/ventas/{id}/anulacion` | `infrastructure/controller/VentaController.java:80` (`anular`) |

</details>

## 4. Services / Impls sin uso

| Servicio | Ubicación | Detalle |
|---|---|---|
| `TotpConfigService` | `application/service/TotpConfigService.java` | Interfaz sin implementación ni referencias: código muerto |

`AuditService`, `CorrelativoService` y `FileStorageService` no los usa ningún controller, pero son infraestructura usada por
otros servicios (correcto). Todos los demás servicios tienen controller.

## 5. DTOs huérfanos (sin referencias en `src/main`)

| DTO | Ubicación |
|---|---|
| `AsistenciaFilterDTO` | `application/dto/request/AsistenciaFilterDTO.java` |
| `DetalleCotizacionRequestDTO` | `application/dto/request/DetalleCotizacionRequestDTO.java` |
| `OrdenDespachoRequestDTO` | `application/dto/request/OrdenDespachoRequestDTO.java` |

## 6. Entidades sin uso

Ninguna: las 49 entidades tienen repositorio y se usan en servicios. Nota: `Cliente.ventas` tiene el mismo patrón que el
bug 1.4 (`@Builder` + `= new ArrayList<>()` sin `@Builder.Default`); hoy no falla porque nadie agrega ventas a esa lista.

## 7. Métodos públicos sin uso (25)

Métodos de repositorios, servicios, mappers y util que nada en `src/main` llama (búsqueda por nombre; se descartaron 11
falsos positivos que MapStruct usa implícitamente o dentro de `expression = "java(...)"`).

| Tipo | Clase | Método | Ubicación |
|---|---|---|---|
| mapper | `KardexMovimientoMapper` | `toResponseDTO` | `application/mapper/KardexMovimientoMapper.java` |
| servicio | `ClienteService` | `countClientesByDateRange` | `application/service/ClienteService.java` |
| servicio | `ClienteService` | `countClientesByTipoDocumento` | `application/service/ClienteService.java` |
| servicio | `FileStorageService` | `existe` | `application/service/FileStorageService.java` |
| repositorio | `AlmacenRepository` | `countBySede_Id` | `infrastructure/repository/AlmacenRepository.java` |
| repositorio | `AlmacenRepository` | `existsBySede_IdAndNombreIgnoreCase` | `infrastructure/repository/AlmacenRepository.java` |
| repositorio | `CatalogoFraccionRepository` | `countByProducto_Id` | `infrastructure/repository/CatalogoFraccionRepository.java` |
| repositorio | `CategoriaRepository` | `existsBySlugIgnoreCase` | `infrastructure/repository/CategoriaRepository.java` |
| repositorio | `ClienteRepository` | `findByDireccionContainingIgnoreCase` | `infrastructure/repository/ClienteRepository.java` |
| repositorio | `ClienteRepository` | `findByIdWithVentas` | `infrastructure/repository/ClienteRepository.java` |
| repositorio | `ClienteRepository` | `findByNumeroDocumentoIgnoreCase` | `infrastructure/repository/ClienteRepository.java` |
| repositorio | `ClienteRepository` | `findClientesByTipoAndNombre` | `infrastructure/repository/ClienteRepository.java` |
| repositorio | `CompraAdjuntoRepository` | `countByCompra_Id` | `infrastructure/repository/CompraAdjuntoRepository.java` |
| repositorio | `CompraRepository` | `findByEstadoCompraOrderByCreatedAtDesc` | `infrastructure/repository/CompraRepository.java` |
| repositorio | `PagoCuentaPorPagarRepository` | `findByCuentaPorPagar_IdOrderByFechaPagoAsc` | `infrastructure/repository/PagoCuentaPorPagarRepository.java` |
| repositorio | `ProductoRepository` | `existsByCodigoSkuIgnoreCase` | `infrastructure/repository/ProductoRepository.java` |
| repositorio | `ProveedorRepository` | `existsByTipoDocumentoAndNumeroDocumento` | `infrastructure/repository/ProveedorRepository.java` |
| repositorio | `RrhhAsistenciaRepository` | `countByEmpleadoIdAndFechaHoraAfter` | `infrastructure/repository/RrhhAsistenciaRepository.java` |
| repositorio | `RrhhAsistenciaRepository` | `findUltimaSalidaDelDia` | `infrastructure/repository/RrhhAsistenciaRepository.java` |
| repositorio | `StockRepository` | `countProductosBajoStockByAlmacen` | `infrastructure/repository/StockRepository.java` |
| repositorio | `StockRepository` | `findByAlmacen_Id` | `infrastructure/repository/StockRepository.java` |
| repositorio | `UserRepository` | `findByEmailAndRolAndActivoTrue` | `infrastructure/repository/UserRepository.java` |
| repositorio | `UserRepository` | `findByEmailWithActivoCheck` | `infrastructure/repository/UserRepository.java` |
| util | `HorarioUtil` | `aDayOfWeek` | `util/HorarioUtil.java` |
| util | `SecretGenerator` | `generateSecureSecret` | `util/SecretGenerator.java` |

## 8. TODO / FIXME

| Ubicación | Texto |
|---|---|
| `application/service/serviceImpl/RetiroSocioServiceImpl.java:136` | // TODO(Módulo 5.1): cuando haya múltiples socios, forzar adminId = usuarioActual.getId() |

No hay `FIXME`, `XXX` ni `HACK`. Los pendientes conocidos de M12 (puntualidad de proveedores, devoluciones por vendedor)
están documentados en javadoc y en `docs/DEUDA-TECNICA.md`.

## 9. Cómo se obtuvo

- **Permisos:** `@PreAuthorize` de clase y de método de los 36 controllers + llamadas a `exigirAdmin`/sede/dueño en los
  servicios; cruzado 1 a 1 con `/v3/api-docs` (273/273).
- **Sin uso:** búsqueda de referencias por nombre en `src/main/java` (sin comentarios); los métodos usados solo en tests
  cuentan como sin uso.
- **Bugs:** aplicación real contra PostgreSQL 16; smoke test de todos los GET y flujo completo con la colección Postman.
