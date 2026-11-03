# AVANCE — Auditoría QA POLICALUM

Fecha: 3 de noviembre de 2026.  
Alcance ejecutado: suite backend existente, pruebas nuevas de los hallazgos, suite unitaria del ERP Angular y revisión de código de los flujos de stock, CMS, sesión y portal público.

## 1. Resumen Ejecutivo

**Con Observaciones.**

El ecosistema no está listo para producción como “cero bugs”. La lógica de negocio que ya tenía pruebas unitarias quedó verde, y en esta pasada se cerraron cinco defectos reales. No existe una batería MockMvc de cada endpoint, ni pruebas de componente para los módulos del ERP, ni una corrida E2E del portal contra la API (el backend no estaba levantado en la revisión visual del sitio).

Cifras de esta corrida:

- Backend, suite completa: **837 pruebas, 0 fallos, 1 error, 3 omitidas**. El error era el cierre del archivo temporal en Windows (`FileStorageServiceImplTest`).
- Backend, reejecución de lo tocado (`FileStorage`, `User`, `Cliente`, `Kardex`, `CmsPublicador`): **exit 0**. Las 3 pruebas que estaban desactivadas por bugs conocidos quedaron habilitadas y pasaron.
- ERP Angular: **6 archivos, 7 pruebas, todas pasaron** (cascarón de layout, no los módulos de negocio).
- Sitio público: sin pruebas automáticas. El layout de tres columnas se vio en el navegador; el catálogo no pudo llenarse porque `localhost:8080` no respondía.

## 2. Matriz de Cobertura Backend

Leyenda: 🟢 las pruebas unitarias existentes pasaron en la suite. 🟡 cubierto solo en parte, o corregido aquí y reprobado en las clases afectadas. 🔴 fallo abierto. “HTTP” indica si hay MockMvc de ese módulo. No lo hay en ninguno: la columna HTTP es revisión de código más el `ControllerAdvice`, no una prueba de cada verbo.

| Módulo | Datos / consultas | Transaccionalidad | Errores y HTTP | Estado |
| --- | --- | --- | --- | --- |
| Kardex y stock | `FOR UPDATE` y rechazo de salida sin stock, cubiertos por `KardexServiceImplTest` y `StockOperacionServiceImplTest` | `@Transactional(rollbackFor = Exception.class)` en el movimiento | `IllegalArgumentException` → 400 JSON en `GlobalExceptionHandler` | 🟡 |
| Ventas y caja | `VentaServiceImplTest`, `TurnoCajaServiceImplTest`. Integración compra → recepción → venta → caja en `FlujoComercialIT` (no figuró entre los fallos) | La venta y el kardex comparten la transacción | Cantidad de línea `@DecimalMin("0.0001")`. Pagos deben cuadrar el total | 🟢 |
| Compras | `CompraServiceImplTest` | Recepción mueve kardex; rollback declarado en el servicio | Sin MockMvc | 🟢 |
| Cotizaciones | `CotizacionServiceImplTest`. El portal recalcula el precio en servidor | `rollbackFor` en el servicio | RUC de 11 dígitos y teléfono validados en el DTO público | 🟡 |
| Devoluciones, preformas, reembolsos, despacho, cuentas por pagar | Pruebas de servicio existentes y verdes en la suite | `rollbackFor` en las escrituras multi-tabla | Sin MockMvc | 🟢 |
| Clientes, proveedores, productos, categorías, almacenes, sedes | Pruebas de servicio verdes | Alta y edición en transacción | Email nulo de cliente corregido en esta pasada | 🟡 |
| Seguridad y usuarios | `UserImplTest`. JWT inválido o de otra versión de sesión responde 401 JSON | Cambio de clave sube `sesionVersion` y revoca refresh tokens | Un token vencido no entra al JSON 401 propio: queda anónimo y lo rechaza la cadena de seguridad | 🟡 |
| CMS y portal | `CmsPublicadorTest` nuevo. No hay prueba del `UPDATE` de ofertas contra PostgreSQL | El aviso al sitio ahora espera el commit | `/api/v1/public/**` es público. El DTO de catálogo no lleva costo ni proveedor | 🟡 |
| Archivos | `FileStorageServiceImplTest` verde tras cerrar el stream y rehabilitar el caso de path traversal | No aplica | `eliminar` y `cargar` exigen que la ruta quede dentro de la base | 🟡 |
| Finanzas, RRHH, reportes, correlativos | Pruebas de servicio en la suite, sin error | Según cada servicio | `RendimientoController` y `RrhhAsistenciaController` consultan repositorios directo | 🟡 |

La prueba de PostgreSQL efímero (`FlujoComercialIT`, Testcontainers, Postgres 16) cubre compra en borrador, recepción, stock 10, venta de 2, stock 8, dos movimientos de kardex y caja de 100 a 120. No cubre el portal ni el motor de ofertas.

## 3. Matriz de Cobertura Frontend (ERP y sitio)

| Superficie | Reactividad | Formularios | Responsividad | Estado |
| --- | --- | --- | --- | --- |
| ERP: header, sidebar, navbar, footer, asistencia | 7 pruebas de creación pasaron con Vitest | No ejercitan `submit` | No se midieron viewports | 🟡 |
| ERP: módulos de negocio (ventas, caja, productos, cotizaciones, CMS) | Sin specs. Los listados usan `takeUntil(destroy$)` donde se revisó | Validadores en los DTO y en formularios reactivos; no hay prueba de cada control | MarketNest con `clamp` en tokens; no se recorrió cada modal | 🟡 |
| ERP: sede y caja | `SedeFiltroService` y `CajaSessionService` guardan en `sessionStorage`, que el navegador aísla por pestaña | No hay prueba de dos pestañas | — | 🟡 |
| Sitio público | `CmsStore` aplica variables y escucha SSE. Sin specs | Cotización y contacto validan en cliente y el servidor vuelve a validar | Layout de tres columnas visto en escritorio ancho. Bajo 1180 px la vitrina derecha se oculta y aparece la barra inferior | 🟡 |
| Tipografía e identidad del sitio | Inter en la tienda clara pedida en la última referencia visual | — | No es la piel oscura con JetBrains Mono ni un bento de tarjetas negras. Esa piel se reemplazó a propósito por la tienda clara | 🟡 |

No se corrió Lighthouse. No hay medición de los 300 ms del CMS: el paquete público sale de caché Caffeine (2 minutos) y el sitio lo vuelve a pedir al recibir el evento SSE.

## 4. Registro de Bugs Resueltos

1. **Salida de kardex con cantidad negativa.** `stockAnterior.compareTo(cantidadNegativa) < 0` no se cumple si hay stock, y `subtract` de un negativo suma unidades. `KardexServiceImpl.registrarMovimiento` ahora rechaza tipo nulo y cantidad nula o no positiva antes de tocar la fila. Prueba: `registrarMovimiento_rechazaCantidadNegativaQueInflariaElStock`.
2. **CMS avisaba antes del commit.** `CmsPublicador.publicar` limpiaba la caché y empujaba SSE dentro de la transacción. Si el commit fallaba, el sitio ya había leído el valor viejo y podía dejarlo cacheado. Ahora, si hay transacción activa, la limpieza y el SSE ocurren en `afterCommit`.
3. **Suite rota en Windows al leer un adjunto.** `cargar_existente_devuelveRecursoLegible` dejaba abierto el `InputStream` del `UrlResource` y JUnit no podía borrar el `@TempDir` (`Failed to close extension context`). El stream se cierra con try-with-resources.
4. **Prueba de path traversal desactivada con el arreglo ya presente.** `eliminar` ya descarta rutas que salen de la carpeta base. Se rehabilitó `eliminar_rutaConTraversal_noBorraFueraDelAlmacenamiento` y pasó.
5. **Editar un usuario sin `activo` lo dejaba en null.** No podía volver a entrar y un `!usuario.getActivo()` podía lanzar NPE. `UserMapper.updateEntityFromDto` ignora propiedades nulas. Prueba: `updateUser_sinCampoActivo_conservaElEstadoActual`.
6. **Cliente sin correo no aceptaba un correo nuevo.** `existingCliente.getEmail().equals(...)` explotaba si el email era null y la API respondía 500. Se compara con `Objects.equals`. Prueba: `update_clienteSinEmailAgregaEmail_guarda`.

Hallazgos que siguen abiertos, sin parche en esta pasada:

- El `UPDATE` de ofertas de 90 días (`NOT EXISTS` sobre `KardexMovimiento`) no se ejecutó contra PostgreSQL.
- Un JWT vencido no usa el JSON 401 de `JwtAuthenticationFilter`; solo la denegación y el desajuste de `sesionVersion` lo hacen. La operación ya confirmada no se revierte: el vencimiento afecta la petición siguiente.
- No hay regla que impida cotizar con el RUC de la propia empresa. Un precio negativo enviado por el cliente no se usa: el servidor recalcula con el precio de venta.
- `application.properties` y `docker-compose.yml` traen la clave local `postgre` como valor por defecto.

## 5. Flujos E2E Validados

| Flujo | Resultado |
| --- | --- |
| Compra borrador → recepción → entrada de kardex y stock → venta POS → salida de kardex → efectivo de caja | Cubierto por `FlujoComercialIT`. En la suite de 837 no apareció como fallo ni como prueba omitida. |
| Venta sin stock | Cubierto en unidad: salida mayor al saldo no guarda movimiento y el saldo queda igual. |
| Cantidad negativa en kardex | Cubierto en unidad tras el arreglo. |
| Web lead → cotización `PENDIENTE` / canal `WEB` → bandeja del ERP | Implementado (`CotizacionWebRequestDTO`, `crearDesdeWeb`, notificación). **No se ejecutó** de punta a punta: la API en el puerto 8080 no respondía. |
| Formulario de contacto → Mensajes web | Mismo límite: código presente, sin corrida viva. |
| Cambio de color, aviso u oferta en el CMS → sitio en menos de 300 ms | El mecanismo (caché + SSE después del commit) está escrito. **No se midió** el tiempo. |
| Dos pestañas, dos sedes, cierre de caja | Por código, sede y turno viven en `sessionStorage` (`erp-sede`, `erp-turno-caja`), así que una pestaña no pisa el almacenamiento de la otra. No hay prueba de navegador con dos pestañas. El cierre en servidor sí afecta a quien tenga el mismo id de turno. |

## 6. Siguientes Pasos

1. **CI.** No hay workflow de GitHub. Hace falta un job que ejecute `backend/mvnw test` (con Docker, para `FlujoComercialIT`) y `npx ng test --watch=false` en `frontend`. El sitio no tiene specs.
2. **Secretos.** Producción ya lee `DB_PASSWORD`, `JWT_SECRET` y `TOTP_PUBLIC_SECRET` (`application-prod.properties`, `ddl-auto=validate`, Swagger apagado). Hay que sacar la clave por defecto `postgre` del compose y de `application.properties` antes de publicar el repo o el servidor.
3. **Contrato HTTP.** Añadir MockMvc al menos para `/api/v1/public/cotizaciones`, `/api/v1/public/contacto`, `/api/v1/cms/**` y el 401 del JWT vencido.
4. **Ofertas.** Correr `marcarOfertasEstancadas` / `quitarOfertasConRotacion` dentro de `FlujoComercialIT` para confirmar que Hibernate genera SQL válido en PostgreSQL.
5. **Portal.** Levantar API y base, y recorrer cotización, contacto y la grilla con productos reales. Medir el GET `/api/v1/public/cms` con caché caliente.
6. **ERP.** Specs de los formularios de venta, caja y productos. Hoy solo está cubierto el cascarón visual.
