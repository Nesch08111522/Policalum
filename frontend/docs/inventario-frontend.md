# Inventario frontend ErpPoliCalum

Documento de la fase 1 y de los huecos que quedaron después de reemplazar los wrappers por vistas reales. Los tipos `UserResponseDTO.id` y `CatalogoFraccionResponseDTO.productoId/productoNombre` se completaron a mano: **requiere regeneración del generador OpenAPI**.

## Inventario por módulo

| Módulo | Endpoints backend (principales) | Catálogo / API service | Vista actual | Vista esperada |
| --- | --- | --- | --- | --- |
| Clientes | GET/POST `/api/v1/clientes`, GET/PUT/DELETE `/{id}` | `ClientesApiService` | CRUD reactivo, filtros y paginación | CRUD maestro |
| Productos | GET/POST `/api/v1/productos`, GET/PUT/DELETE `/{id}` | `ProductosApiService` | CRUD con categoría, proveedor, tipo y precios | CRUD con relaciones |
| Servicios | GET/POST `/api/v1/servicios`, GET/PUT/DELETE `/{id}` | `ServiciosApiService` | CRUD activo y precio | CRUD simple |
| Categorías | GET/POST `/api/v1/categorias`, raíces, GET/PUT/DELETE `/{id}` | `CategoriasApiService` | CRUD con padre y slug | CRUD con relación |
| Fracciones | GET/POST `/api/v1/catalogo-fracciones`, PUT/DELETE `/{id}` | `CatalogoFraccionesApiService` | CRUD ligado a producto | CRUD con relación |
| Sedes | GET `/api/v1/sedes`, GET `/estado`, POST, PUT/DELETE `/{id}` | `SedesApiService` | CRUD admin, filtro de texto en cliente | CRUD simple, solo admin |
| Almacenes | GET/POST `/api/v1/almacenes`, PUT/DELETE `/{id}` | `AlmacenesApiService` | CRUD con sede `{id}` | CRUD con sede |
| Stock | consolidado, bajo stock, ajustes, transferencias, dashboard | `StockApiService` | Pestañas, ajuste y transferencia | Operación de inventario |
| Kardex | movimientos, saldos, valorización, mermas, POST movimiento | `KardexApiService` | Pestañas; costos solo admin | Movimientos y saldos |
| Compras | listado, alta, edición, recepción, anulación, delete, dashboard | `ComprasApiService` | Listado, formulario con líneas, recibir y anular | Orden de compra |
| Proveedores | GET/POST `/api/v1/proveedores`, PUT/DELETE `/{id}` | `ProveedoresApiService` | CRUD con datos bancarios | CRUD simple |
| Ventas | GET/POST `/api/v1/ventas`, GET `/{id}`, POST anulación | `VentasApiService` | Listado, alta y anulación. POS en `/ventas/pos` | Ventas + POS intacto |
| Cotizaciones | listado, vencidas, alta, edición, aceptar, rechazar, convertir, delete | `CotizacionesApiService` | Pestañas y conversión a venta | Cotización comercial |
| Despachos | listado, alta, dashboard, asignar repartidor, cambiar estado | `DespachosApiService` | CRUD operativo sin evidencia de archivo | Entregas |
| Movilidades | listado, dashboard, alta, edición, asignar conductor | `MovilidadesApiService` | CRUD y conductor | CRUD simple |
| Caja | POST apertura, GET `/{id}/resumen`, POST `/{id}/cierre` | `TurnosCajaApiService` | Apertura, resumen y cierre. El id se guarda en el navegador | Turno de caja |
| Cuentas por pagar | listado, dashboard, POST `/{id}/pago` | `CuentasPorPagarApiService` | Listado y pago. No se crean a mano | Cobro a proveedores |
| Finanzas | cuentas, saldos, KPIs, patrimonio, evolución, utilidad, retiros | `FinanzasApiService` | Resumen, cuentas y retiros. Solo admin | Dashboard financiero |
| RRHH | legajos, asistencias, turnos, nóminas, comisiones | `RrhhApiService` | Cinco pestañas con sus acciones | Personas |
| Reportes | ventas, resumen, rentabilidad, estrella, clientes, proveedores, arqueos | `ReportesApiService` | Consultas con filtros. Sin descarga de archivo | Reportes |
| Auditoría | logs paginados, detalle, dashboard | `AuditoriaApiService` | Tabla, filtros y detalle. Solo admin | Trazabilidad |
| Configuración | empresa, parámetros, series | `ConfiguracionApiService` | Tres pestañas. Solo admin | Parámetros de empresa |
| Usuarios | GET/POST `/api/v1/users`, PUT/DELETE `/{id}` | `UsersApiService` | Lista en cliente y CRUD. Solo admin | Usuarios |
| Roles | GET/POST `/api/v1/roles`, PUT/DELETE `/{id}` | `RolesApiService` | CRUD de permisos JSON. Solo admin | Roles |
| Panel | agrega stock, CxP, ventas, compras y, si es admin, finanzas | varios | `/inicio` | Dashboard |

## Mapa de relaciones

- Producto depende de categoría y proveedor. El formulario pide ambos selects. La fracción depende del producto.
- Almacén depende de sede. Stock, kardex, compras y ventas dependen de almacén y producto.
- Compra depende de proveedor, sede, producto y almacén. La recepción genera stock y la cuenta por pagar nace de la compra, no de un alta manual.
- Venta depende de sede, almacén, turno de caja, líneas de producto o servicio y pagos. Cliente y cotización son opcionales.
- Cotización depende de sede y de líneas de producto o servicio. Convertirla exige almacén, turno de caja y un pago.
- Despacho depende de una venta. El repartidor es un usuario y la movilidad es opcional.
- Usuario depende de rol y sede. Legajo, turno, asistencia, nómina y comisión dependen del usuario.
- Cuenta financiera y retiro de socio pueden asociarse a una sede. Serie de comprobante exige sede.
- El panel junta ventas del día, stock crítico, cuentas por pagar vencidas y, para administración, utilidad y patrimonio.

## Endpoints faltantes o mejorables

- Turnos de caja no tiene listado. La pantalla recuerda el último turno abierto en `sessionStorage` (`erp-turno-caja`).
- Las exportaciones (`reportes/*/exportar`, `auditoria/logs/exportar`) están tipadas como `string` en el cliente generado. Una descarga real necesita `responseType: 'blob'`. No se invocan.
- Adjuntos de compra, evidencia de despacho y documentos de legajo son cargas de archivo. El cliente generado no arma `FormData`. Quedan fuera de la vista.
- Usuarios, roles y el listado de sedes no paginan en el servidor. El filtro de sedes es local porque el GET no acepta `searchTerm`.
- `UserResponseDTO` no traía `id` y `CatalogoFraccionResponseDTO` no traía el producto. Se añadieron en Java y en `openapi.types.ts`. Hay que regenerar el cliente OpenAPI para no perderlos.

## Checklist de vistas

La compilación de producción (`ng build`) termina bien y cada módulo sale en un chunk perezoso. Sin el backend en `localhost:8080` la verificación de datos reales queda en el estado de error y reintento. El POS (`/ventas/pos`) y la asistencia QR no se reescribieron.

| Vista | Carga | Datos | Filtro | Página | Crear | Editar | Eliminar | Errores | Responsive | Teclado |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Clientes, servicios, proveedores, sedes, almacenes, categorías, productos, fracciones, usuarios, roles, movilidades | Compila y usa el API service | Requiere API | Sí | Sí | Sí | Sí | Sí | Tabla con reintento y toast | Tabla con scroll y modal a pantalla completa en móvil | Escape cierra el modal |
| Stock, kardex, compras, ventas, cotizaciones, despachos, CxP | Igual | Requiere API | Sí | Sí | Según el caso de negocio | Según estado | Según estado | Igual | Igual | Igual |
| Caja | Apertura, resumen y cierre | Requiere API | No hay listado | No aplica | Abre turno | No | Cierra turno | Alerta de error | Formularios en grilla | Igual |
| Finanzas, RRHH, reportes, auditoría, configuración, panel | Pestañas o KPIs | Requiere API | Sí | Donde el backend pagina | Según pestaña | Según pestaña | Según pestaña | Alerta y reintento | Grillas a una columna bajo 720px | Igual |
