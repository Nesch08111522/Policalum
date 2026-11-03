# ERP POLICALUM

Revisión del 3 de noviembre de 2026. El inventario de clases salió de `tree /f` sobre `backend/src/main/java`, `frontend/src/app` y `sitio_web/src/app`. El árbol completo está al final.

## Cómo leer la calificación

| Nota | Significado |
| --- | --- |
| Sólido | El módulo tiene pantalla, API y regla de acceso. La suite unitaria del backend pasó. |
| Con observación | Funciona el camino local, y queda un hueco concreto. |
| No recorrido en el navegador | En esta pasada no se inició sesión de administrador para pulsar cada botón de esa pantalla. |

La suite `mvnw test` dejó **847 pruebas ejecutadas, 0 fallos y 2 omitidas**. Las dos omitidas son `FlujoComercialIT` (`compraRecepcionVentaYCaja` y `productoSinSalidaEn90DiasQuedaEnOferta`) porque en esta máquina no hay Docker. No se afirma que ese flujo de punta a punta haya corrido.

Tampoco se recorrió, con un usuario administrador, cada clic de cada pantalla del ERP. La calificación de frontend dice si la página y su cliente HTTP existen y compilan en la revisión anterior del panel, más lo que se corrigió ahora.

## Seguridad

Spring Security ya estaba activo: sesión stateless, JWT, BCrypt, `@EnableMethodSecurity` y entrada 401 en JSON.

En esta pasada se ajustó:

- CORS solo acepta los orígenes de `app.cors.allowed-origins`. El valor de fábrica es `http://localhost:4200`, `http://127.0.0.1:4200`, `http://localhost:4201` y `http://127.0.0.1:4201`. Se quitaron los comodines de red local y de ngrok. Otro equipo se agrega con la variable `APP_CORS_ORIGINS`.
- Las respuestas llevan `X-Content-Type-Options`, `X-Frame-Options: DENY`, referrer `strict-origin-when-cross-origin` y una política que niega cámara, micrófono y geolocalización.
- Las APIs de operación (ventas, compras, productos, stock, kardex, caja, cotizaciones, sedes y el resto del listado de controladores de negocio) exigen `ROLE_ADMIN` o `ROLE_MULTIFUNCIONAL`. Finanzas, usuarios, roles, nómina y auditoría siguen solo en `ROLE_ADMIN`.
- El sitio público sigue en `/api/v1/public/**`. El login y la marca pública siguen abiertos. El QR de kiosco sigue en su GET público.
- Sitio web y Portafolio web del panel ahora usan `RoleGuard` y solo entran con `ROLE_ADMIN`. Si la ruta no declara roles, el guardia niega el acceso.

Sigue abierto, a propósito o porque falta entorno:

- El secreto JWT y el secreto TOTP de desarrollo siguen en `application.properties` como valor de fábrica. En producción hay que reemplazarlos por variables.
- Swagger está encendido en el perfil de desarrollo y apagado en `application-prod.properties`.
- El servidor escucha en `0.0.0.0`.
- `ddl-auto` sigue en `update`.
- La emisión SUNAT responde 501: el cliente existe y la API real no está conectada.

## El ERP y el sitio usan los mismos datos

| Lo que se ve en el sitio | Dónde se edita en el ERP | Registro |
| --- | --- | --- |
| Nombre, RUC, razón social, teléfono, correo, dirección | Sitio web, y también Configuración de empresa | `configuracion_empresa` |
| Sedes y direcciones | Sedes | `sedes` |
| Productos, precio, stock, oferta, imagen | Productos, Stock, Kardex | catálogo e inventario |
| Categorías del menú | Categorías | categorías |
| Servicios | Servicios | servicios |
| Obras del blog y de la portada | Portafolio web | portafolio |
| Utilidad, movilidad por distrito y otros cargos | Configuración → Cotización | tarifas de cotización, no una copia en el CMS |
| Franja, pie, horarios, redes, textos legales, antetítulo, título y texto de la portada | Sitio web | `cms_ajustes` |
| Mensajes del chat y del formulario | Mensajes web | bandeja web |
| Cotización enviada | Cotizaciones, con aviso | `cotizaciones` |

Si el RUC, la razón social, la dirección o el ubigeo están vacíos, Sitio web guarda el texto del portal y deja el registro de empresa como está, para no borrar datos a medias. El teléfono y la dirección que hoy muestra el sitio salen de la sede activa cuando la empresa no tiene los suyos.

La portada usa el texto guardado en Sitio web. Si ese texto está vacío, el sitio muestra la frase de fábrica. Esa columna nueva entra cuando el servidor arranca con este código: Hibernate agrega `hero_kicker`, `hero_titulo` y `hero_texto`.

## Módulos

| Módulo | Backend | Frontend | Nota |
| --- | --- | --- | --- |
| Acceso y perfil | `AuthController`, `PerfilController`, filtro JWT | Login, recuperar redirige al login, Mi perfil | Sólido. Token vencido responde 401 JSON. |
| Panel | datos de inicio en los servicios de cada área | `/inicio` | Sólido en código. No recorrido en el navegador. |
| Notificaciones | `NotificacionController` | `/modules/notificaciones` | Sólido. La cotización web crea el aviso «Tienes una nueva cotización para visualizar». |
| Ventas y punto de venta | `VentaController` | `/modules/ventas`, `/ventas/pos` | Sólido en código. El POS exige turno de caja abierto. El flujo completo con Docker no corrió. |
| Clientes | `ClienteController` | `/modules/clientes` | Sólido. |
| Cotizaciones | `CotizacionController`, formato y cargos | `/modules/cotizaciones` | Sólido. El sitio y el ERP usan el mismo cálculo. Una cotización de prueba quedó en la base y se puede rechazar. |
| Mensajes web | `CanalWebController` | `/modules/mensajes-web` | Sólido. El chat del sitio entra como mensaje, no como conversación en vivo. |
| Preformas | `PreformaController` | `/modules/preformas` | Sólido en código. |
| Devoluciones y reembolsos | controladores propios, rol de operación | pantallas del menú Ventas | Sólido en código. |
| Caja | `TurnoCajaController` | `/modules/turnos-caja` | Sólido en código. Sin turno abierto la venta responde 409. |
| Productos | `ProductoController` | `/modules/productos` | Sólido. El catálogo público lee el mismo inventario. |
| Servicios | `ServicioController` e importación | `/modules/servicios` | Sólido. |
| Categorías y fracciones | controladores de catálogo | dos pantallas | Sólido. |
| Stock, kardex y almacenes | tres controladores | tres pantallas | Sólido. Los reportes de valorización y mermas del kardex quedan en administrador. |
| Compras y proveedores | `CompraController`, `ProveedorController` | dos pantallas | Sólido en código. |
| Despachos y movilidades | controladores con rol de operación | dos pantallas | Sólido en código. La movilidad de la cotización web es otra tabla: la tarifa por distrito, en Configuración. |
| Cuentas por pagar | `CuentaPorPagarController` | pantalla de finanzas operativas | Sólido en código. |
| Facturación SUNAT | panel local y cliente SUNAT | `/modules/facturacion-electronica` | Con observación. Emitir y consultar validez responden 501 hasta conectar la API de SUNAT. |
| Finanzas, gastos, cuentas, retiros y utilidad | solo `ROLE_ADMIN` | `/modules/finanzas` | Sólido en código. |
| Personas, legajo, turnos, nómina, comisiones y asistencia | RRHH; nómina y legajo solo administrador | `/modules/rrhh`, `/asistencias` | Sólido en código. El QR público de marcación sigue abierto a propósito. |
| Reportes y rendimiento | `ReporteController`, `RendimientoController` | `/modules/reportes` | Sólido en código. |
| Auditoría | solo administrador | `/modules/auditoria` | Sólido. |
| Configuración de empresa, series y cotización | `ConfiguracionController` | `/modules/configuracion` | Sólido. La cotización del sitio sale de aquí. |
| Sedes | alta solo administrador; consulta también para operación | `/modules/sedes`, guardia de administrador | Sólido. El sitio público lista las sedes activas. |
| Usuarios y roles | solo administrador | dos pantallas con guardia | Sólido. Hay dos roles: administrador y multifuncional. |
| Portafolio web | `PortafolioController` | `/modules/portafolio-web`, ahora solo administrador | Sólido. Si no hay obras, el sitio lo dice y no inventa proyectos. |
| Sitio web | `CmsController` | `/modules/gestor-web`, ahora solo administrador | Sólido en el enlace. Falta guardar la portada una vez con el servidor nuevo para que el título deje de ser el de fábrica. |

## Lo que falta para llamarlo cerrado de punta a punta

1. Recorrer cada pantalla con un administrador y un multifuncional. Esta revisión no lo hizo.
2. Correr `FlujoComercialIT` en una máquina con Docker.
3. Conectar SUNAT. Hoy el panel existe y la emisión responde que no está implementada.
4. Poner secretos reales de JWT y TOTP fuera del archivo de propiedades antes de publicar el servidor.
5. Reiniciar el backend que ya estaba en marcha para que tome el CORS nuevo, los roles de los controladores y las columnas de la portada.
6. Completar en Sitio web el RUC, la razón social, la dirección, el ubigeo y el correo si se quieren ver en el pie. Hoy el teléfono y la dirección visibles salen de la sede.

## Árbol `tree /f`

Los tres listados siguientes son la salida del comando, sin `target`, sin `node_modules` y sin `dist`.

### Backend
```text

Listado de rutas de carpetas para el volumen Windows-SSD
El número de serie del volumen es 4A6B-F158
C:\USERS\NANCY SANDOVAL\DOWNLOADS\ERP\POLICALUM\BACKEND\SRC\MAIN\JAVA
\---com
    \---policalum
        \---erp
            |   ErpApplication.java
            |   
            +---application
            |   +---dto
            |   |   +---request
            |   |   |       AjusteCargoCotizacionRequestDTO.java
            |   |   |       AjusteStockRequestDTO.java
            |   |   |       AlmacenRequestDTO.java
            |   |   |       AnularCompraRequestDTO.java
            |   |   |       AnularRetiroRequestDTO.java
            |   |   |       AnularVentaRequestDTO.java
            |   |   |       AperturaCajaRequestDTO.java
            |   |   |       AprobarDevolucionRequestDTO.java
            |   |   |       AsignarConductorRequestDTO.java
            |   |   |       AsignarRepartidorRequestDTO.java
            |   |   |       AsignarTurnoRequestDTO.java
            |   |   |       AsistenciaFilterDTO.java
            |   |   |       AsistenciaQrDTO.java
            |   |   |       AuditLogRequestDTO.java
            |   |   |       CajaChicaRequestDTO.java
            |   |   |       CalcularComisionesRequestDTO.java
            |   |   |       CambiarEstadoDespachoRequestDTO.java
            |   |   |       CancelarPreformaRequestDTO.java
            |   |   |       CatalogoFraccionRequestDTO.java
            |   |   |       CategoriaRequestDTO.java
            |   |   |       CierreCajaRequestDTO.java
            |   |   |       ClienteRequestDTO.java
            |   |   |       CmsAjusteRequestDTO.java
            |   |   |       CompraRequestDTO.java
            |   |   |       ConfirmarPreformaRequestDTO.java
            |   |   |       ContactoWebRequestDTO.java
            |   |   |       ConvertirCotizacionRequestDTO.java
            |   |   |       CotizacionRequestDTO.java
            |   |   |       CotizacionWebRequestDTO.java
            |   |   |       CrearDespachoRequestDTO.java
            |   |   |       CuentaFinancieraRequestDTO.java
            |   |   |       DetalleCompraCommandDTO.java
            |   |   |       DetalleCompraRequestDTO.java
            |   |   |       DetalleCotizacionCommandDTO.java
            |   |   |       DetalleCotizacionRequestDTO.java
            |   |   |       DetalleDevolucionRequestDTO.java
            |   |   |       DetallePreformaRequestDTO.java
            |   |   |       DetalleVentaRequestDTO.java
            |   |   |       DevolucionRequestDTO.java
            |   |   |       EmpresaRequestDTO.java
            |   |   |       EvidenciaEntregaRequestDTO.java
            |   |   |       FiltroVentasReporteDTO.java
            |   |   |       FinanzasCuentaPorPagarRequestDTO.java
            |   |   |       FinanzasRetiroSocioRequestDTO.java
            |   |   |       FormatoCotizacionRequestDTO.java
            |   |   |       GastoEmpresarialRequestDTO.java
            |   |   |       GenerarNominaRequestDTO.java
            |   |   |       GestionarMensajeRequestDTO.java
            |   |   |       HorarioOperativoRequestDTO.java
            |   |   |       KardexMovimientoRequestDTO.java
            |   |   |       LegajoRequestDTO.java
            |   |   |       LineaVentaCommandDTO.java
            |   |   |       LoginRequestDTO.java
            |   |   |       LogoutRequestDTO.java
            |   |   |       MarcaRequestDTO.java
            |   |   |       MovilidadRequestDTO.java
            |   |   |       OfertaManualRequestDTO.java
            |   |   |       OrdenDespachoRequestDTO.java
            |   |   |       PagarNominaRequestDTO.java
            |   |   |       PagoCxPRequestDTO.java
            |   |   |       PagoVentaCommandDTO.java
            |   |   |       PagoVentaRequestDTO.java
            |   |   |       ParametrosFinanzasRequestDTO.java
            |   |   |       ParametrosInventarioRequestDTO.java
            |   |   |       ParametrosReportesRequestDTO.java
            |   |   |       ParametrosRequestDTO.java
            |   |   |       ParametrosVentasRequestDTO.java
            |   |   |       PerfilActualizarRequestDTO.java
            |   |   |       PerfilContrasenaRequestDTO.java
            |   |   |       PerfilTotpRequestDTO.java
            |   |   |       PreformaVentaRequestDTO.java
            |   |   |       ProcesarReembolsoRequestDTO.java
            |   |   |       ProductoImportacionFilaDTO.java
            |   |   |       ProductoImportacionRequestDTO.java
            |   |   |       ProductoRequestDTO.java
            |   |   |       ProveedorRequestDTO.java
            |   |   |       ProyectoPortafolioRequestDTO.java
            |   |   |       RecepcionCompraRequestDTO.java
            |   |   |       RechazarCotizacionRequestDTO.java
            |   |   |       RechazarDevolucionRequestDTO.java
            |   |   |       ReembolsoRequestDTO.java
            |   |   |       RefreshRequestDTO.java
            |   |   |       RegistrarSaldoCuentaRequestDTO.java
            |   |   |       RegistrarVentaRequestDTO.java
            |   |   |       RetiroSocioRequestDTO.java
            |   |   |       RolRequestDTO.java
            |   |   |       RrhhAsistenciaRequestDTO.java
            |   |   |       RrhhLegajoDigitalRequestDTO.java
            |   |   |       RrhhNominaRequestDTO.java
            |   |   |       RrhhTurnoTrabajoRequestDTO.java
            |   |   |       SedeRequestDTO.java
            |   |   |       SerieComprobanteRequestDTO.java
            |   |   |       SeriesRequestDTO.java
            |   |   |       ServicioColaboradorRequestDTO.java
            |   |   |       ServicioRequestDTO.java
            |   |   |       StockRequestDTO.java
            |   |   |       TransferenciaRequestDTO.java
            |   |   |       TransferenciaStockRequestDTO.java
            |   |   |       TurnoCajaRequestDTO.java
            |   |   |       TurnoTrabajoRequestDTO.java
            |   |   |       UserRequestDTO.java
            |   |   |       VentaRequestDTO.java
            |   |   |       VistaCotizacionRequestDTO.java
            |   |   |       
            |   |   \---response
            |   |           ActividadTablaDTO.java
            |   |           ActividadUsuarioDTO.java
            |   |           AjusteStockResponseDTO.java
            |   |           AlmacenResponseDTO.java
            |   |           AlmacenResumenDTO.java
            |   |           ArchivoDescargaDTO.java
            |   |           ArqueosReporteDTO.java
            |   |           ArqueoTurnoDTO.java
            |   |           AsignacionTurnoResponseDTO.java
            |   |           AsistenciaEstadoDTO.java
            |   |           AuditLogResponseDTO.java
            |   |           AuditoriaDashboardDTO.java
            |   |           BoletaConceptoDTO.java
            |   |           BoletaPagoDTO.java
            |   |           CajaChicaResponseDTO.java
            |   |           CalculoComisionesResponseDTO.java
            |   |           CargosCotizacionDTO.java
            |   |           CatalogoFraccionResponseDTO.java
            |   |           CategoriaPublicaDTO.java
            |   |           CategoriaResponseDTO.java
            |   |           CategoriaSummaryDTO.java
            |   |           ClienteFrecuenteDTO.java
            |   |           ClienteResponseDTO.java
            |   |           ClientesFrecuentesDTO.java
            |   |           CmsPublicoDTO.java
            |   |           ComisionResponseDTO.java
            |   |           CompraAdjuntoResponseDTO.java
            |   |           CompraListadoDTO.java
            |   |           CompraResponseDTO.java
            |   |           ConfiguracionPublicaDTO.java
            |   |           ConfirmacionPreformaDTO.java
            |   |           ConteoDiarioDTO.java
            |   |           CotizacionListadoDTO.java
            |   |           CotizacionResponseDTO.java
            |   |           CuentaFinancieraResponseDTO.java
            |   |           CuentaPorPagarResponseDTO.java
            |   |           DashboardAsistenciaDTO.java
            |   |           DespachoDashboardDTO.java
            |   |           DetalleCompraResponseDTO.java
            |   |           DetalleCotizacionResponseDTO.java
            |   |           DetalleDevolucionResponseDTO.java
            |   |           DetallePreformaResponseDTO.java
            |   |           DetalleVentaResponseDTO.java
            |   |           DevolucionDashboardDTO.java
            |   |           DevolucionListadoDTO.java
            |   |           DevolucionResponseDTO.java
            |   |           DisponibleCalculadoDTO.java
            |   |           EmpresaPublicaDTO.java
            |   |           EmpresaResponseDTO.java
            |   |           EvolucionMensualResponseDTO.java
            |   |           FacturacionElectronicaPanelDTO.java
            |   |           FinanzasCuentaPorPagarResponseDTO.java
            |   |           FinanzasRetiroSocioResponseDTO.java
            |   |           FlujoCajaRealResponseDTO.java
            |   |           FlujoCajaResumenDTO.java
            |   |           FlujoCajaSedeDTO.java
            |   |           FlujoDevengadoResponseDTO.java
            |   |           FormatoCotizacionDTO.java
            |   |           FraccionPublicaDTO.java
            |   |           GastoEmpresarialResponseDTO.java
            |   |           GeneracionNominaResponseDTO.java
            |   |           HistorialProductoDTO.java
            |   |           KardexMovimientoDetalleDTO.java
            |   |           KardexMovimientoResponseDTO.java
            |   |           KpisFinancierosDTO.java
            |   |           KpisFinancierosResponseDTO.java
            |   |           KpisSedeDTO.java
            |   |           LegajoDashboardDTO.java
            |   |           LegajoDocumentoResponseDTO.java
            |   |           LegajoListadoDTO.java
            |   |           LegajoResponseDTO.java
            |   |           LineaVentaResponseDTO.java
            |   |           LogAuditoriaResponseDTO.java
            |   |           LoginResponseDTO.java
            |   |           MensajeContactoResponseDTO.java
            |   |           MermaKardexDTO.java
            |   |           MermaPorProductoDTO.java
            |   |           MesEvolucionDTO.java
            |   |           MesProyectadoDTO.java
            |   |           MetodoPagoMontoDTO.java
            |   |           MontoPorMedioDTO.java
            |   |           MontoProyectadoDTO.java
            |   |           MovilidadDashboardDTO.java
            |   |           MovilidadListadoDTO.java
            |   |           MovilidadResponseDTO.java
            |   |           NominaDashboardDTO.java
            |   |           NominaListadoDTO.java
            |   |           NominaResponseDTO.java
            |   |           NotificacionResponseDTO.java
            |   |           OrdenDespachoResponseDTO.java
            |   |           PaginaPublicaDTO.java
            |   |           PagoVentaDetalleResponseDTO.java
            |   |           PagoVentaResponseDTO.java
            |   |           ParametrosFinanzasDTO.java
            |   |           ParametrosInventarioDTO.java
            |   |           ParametrosReportesDTO.java
            |   |           ParametrosResponseDTO.java
            |   |           ParametrosVentasDTO.java
            |   |           PatrimonioResponseDTO.java
            |   |           PatrimonioSedeDTO.java
            |   |           PerfilResponseDTO.java
            |   |           PeriodoVentasDTO.java
            |   |           PreformaDashboardDTO.java
            |   |           PreformaListadoDTO.java
            |   |           PreformaVentaResponseDTO.java
            |   |           ProductoImportacionResultadoDTO.java
            |   |           ProductoPublicoDTO.java
            |   |           ProductoResponseDTO.java
            |   |           ProductosEstrellaDTO.java
            |   |           ProveedoresRankingDTO.java
            |   |           ProveedorRankingDTO.java
            |   |           ProveedorResponseDTO.java
            |   |           ProveedorSummaryDTO.java
            |   |           ProyeccionFlujoResponseDTO.java
            |   |           ProyectoPortafolioResponseDTO.java
            |   |           QrPublicoDTO.java
            |   |           ReembolsoResponseDTO.java
            |   |           RentabilidadItemDTO.java
            |   |           RentabilidadReporteDTO.java
            |   |           ReporteMermasDTO.java
            |   |           ReporteUsoMovilidadDTO.java
            |   |           ResumenArqueosDTO.java
            |   |           ResumenCierreCajaDTO.java
            |   |           ResumenCxPDashboardDTO.java
            |   |           RetiroPorAdminDTO.java
            |   |           RetiroSocioDashboardDTO.java
            |   |           RetiroSocioListadoDTO.java
            |   |           RetiroSocioResponseDTO.java
            |   |           RetiroSocioResumenDTO.java
            |   |           RolResponseDTO.java
            |   |           RrhhAsistenciaResponseDTO.java
            |   |           RrhhLegajoDigitalResponseDTO.java
            |   |           RrhhNominaResponseDTO.java
            |   |           RrhhTurnoTrabajoResponseDTO.java
            |   |           SaldoCuentaEstimadoDTO.java
            |   |           SaldoCuentaResponseDTO.java
            |   |           SaldoKardexDTO.java
            |   |           SaldosProductoDTO.java
            |   |           SedeMapaDTO.java
            |   |           SedePublicaDTO.java
            |   |           SedeResponseDTO.java
            |   |           SeparacionPatrimonialDTO.java
            |   |           SerieComprobanteResponseDTO.java
            |   |           ServicioColaboradorResponseDTO.java
            |   |           ServicioPublicoDTO.java
            |   |           ServicioResponseDTO.java
            |   |           StockConsolidadoDTO.java
            |   |           StockDashboardDTO.java
            |   |           StockResponseDTO.java
            |   |           TotpConfigDTO.java
            |   |           TransferenciaStockResponseDTO.java
            |   |           TurnoAbiertoResponseDTO.java
            |   |           TurnoCajaResponseDTO.java
            |   |           TurnoDashboardDTO.java
            |   |           TurnoDelDiaDTO.java
            |   |           TurnoTrabajoResponseDTO.java
            |   |           UserInfoDTO.java
            |   |           UserResponseDTO.java
            |   |           UsoMovilidadDTO.java
            |   |           UtilidadNetaResponseDTO.java
            |   |           UtilidadSedeDTO.java
            |   |           ValorizacionKardexDTO.java
            |   |           ValorizacionProductoDTO.java
            |   |           VendedorRendimientoDTO.java
            |   |           VentaDetalleResponseDTO.java
            |   |           VentaListadoResponseDTO.java
            |   |           VentaProcesadaResponseDTO.java
            |   |           VentaReporteDTO.java
            |   |           VentaResponseDTO.java
            |   |           VentasResumenDTO.java
            |   |           VistaCotizacionDTO.java
            |   |           
            |   +---mapper
            |   |       AlmacenMapper.java
            |   |       AuditLogMapper.java
            |   |       CatalogoFraccionMapper.java
            |   |       CategoriaMapper.java
            |   |       ClienteMapper.java
            |   |       ComisionMapper.java
            |   |       CompraMapper.java
            |   |       ConfiguracionMapper.java
            |   |       CotizacionMapper.java
            |   |       CuentaFinancieraMapper.java
            |   |       DetalleCompraMapper.java
            |   |       DetalleCotizacionMapper.java
            |   |       DetalleDevolucionMapper.java
            |   |       DetallePreformaMapper.java
            |   |       DetalleVentaMapper.java
            |   |       DevolucionMapper.java
            |   |       FinanzasCuentaPorPagarMapper.java
            |   |       FinanzasRetiroSocioMapper.java
            |   |       KardexConsultaMapper.java
            |   |       KardexMovimientoMapper.java
            |   |       LegajoMapper.java
            |   |       LogAuditoriaMapper.java
            |   |       MovilidadMapper.java
            |   |       NominaMapper.java
            |   |       OrdenDespachoMapper.java
            |   |       PagoVentaMapper.java
            |   |       PreformaVentaMapper.java
            |   |       ProductoMapper.java
            |   |       ProveedorMapper.java
            |   |       ReembolsoMapper.java
            |   |       ReporteMapper.java
            |   |       RetiroSocioMapper.java
            |   |       RolMapper.java
            |   |       RrhhAsistenciaMapper.java
            |   |       RrhhLegajoDigitalMapper.java
            |   |       RrhhNominaMapper.java
            |   |       RrhhTurnoTrabajoMapper.java
            |   |       SaldoCuentaMapper.java
            |   |       SedeMapper.java
            |   |       SerieComprobanteMapper.java
            |   |       ServicioMapper.java
            |   |       StockMapper.java
            |   |       StockOperacionMapper.java
            |   |       TurnoCajaMapper.java
            |   |       TurnoTrabajoMapper.java
            |   |       UserMapper.java
            |   |       VentaMapper.java
            |   |       
            |   \---service
            |       |   AlmacenService.java
            |       |   AuditoriaService.java
            |       |   AuditService.java
            |       |   AuthService.java
            |       |   CatalogoFraccionService.java
            |       |   CategoriaService.java
            |       |   ClienteService.java
            |       |   ComisionService.java
            |       |   CompraAdjuntoService.java
            |       |   CompraService.java
            |       |   ConfiguracionService.java
            |       |   CorrelativoService.java
            |       |   CotizacionService.java
            |       |   CuentaFinancieraService.java
            |       |   CuentaPorPagarService.java
            |       |   DashboardFinancieroService.java
            |       |   DevolucionService.java
            |       |   FacturacionElectronicaService.java
            |       |   FileStorageService.java
            |       |   FlujoCajaService.java
            |       |   KardexService.java
            |       |   LegajoService.java
            |       |   MovilidadService.java
            |       |   NominaService.java
            |       |   OrdenDespachoService.java
            |       |   PreformaService.java
            |       |   ProductoImportacionService.java
            |       |   ProductoService.java
            |       |   ProveedorService.java
            |       |   ReembolsoService.java
            |       |   ReporteService.java
            |       |   RetiroSocioService.java
            |       |   RolService.java
            |       |   RrhhAsistenciaService.java
            |       |   SedeService.java
            |       |   ServicioService.java
            |       |   StockOperacionService.java
            |       |   StockService.java
            |       |   SunatFacturacionClient.java
            |       |   TotpConfigService.java
            |       |   TotpService.java
            |       |   TurnoCajaService.java
            |       |   TurnoTrabajoService.java
            |       |   UserService.java
            |       |   UtilidadNetaService.java
            |       |   VentaService.java
            |       |   
            |       +---serviceImpl
            |       |       AlmacenServiceImpl.java
            |       |       AuditoriaServiceImpl.java
            |       |       AuditServiceImpl.java
            |       |       CajaChicaServiceImpl.java
            |       |       CanalWebNotificador.java
            |       |       CatalogoFraccionServiceImpl.java
            |       |       CategoriaServiceImpl.java
            |       |       ClienteServiceImpl.java
            |       |       CmsPublicador.java
            |       |       CmsService.java
            |       |       ComisionServiceImpl.java
            |       |       CompraAdjuntoServiceImpl.java
            |       |       CompraServiceImpl.java
            |       |       ConfiguracionServiceImpl.java
            |       |       CorrelativoServiceImpl.java
            |       |       CotizacionDocumentoService.java
            |       |       CotizacionServiceImpl.java
            |       |       CuentaFinancieraServiceImpl.java
            |       |       CuentaPorPagarServiceImpl.java
            |       |       DashboardFinancieroServiceImpl.java
            |       |       DevolucionServiceImpl.java
            |       |       ExportacionServiceImpl.java
            |       |       FacturacionElectronicaServiceImpl.java
            |       |       FileStorageServiceImpl.java
            |       |       FlujoCajaServiceImpl.java
            |       |       FormatoCotizacionService.java
            |       |       GastoEmpresarialServiceImpl.java
            |       |       KardexServiceImpl.java
            |       |       LegajoServiceImpl.java
            |       |       LimitePublico.java
            |       |       MensajeWebService.java
            |       |       MovilidadServiceImpl.java
            |       |       MovimientosCaja.java
            |       |       MovimientosCajaCalculator.java
            |       |       NominaServiceImpl.java
            |       |       NotificacionServiceImpl.java
            |       |       OrdenDespachoServiceImpl.java
            |       |       PerfilServiceImpl.java
            |       |       PortafolioService.java
            |       |       PortalPublicoService.java
            |       |       PreformaServiceImpl.java
            |       |       ProductoImagenService.java
            |       |       ProductoImportacionServiceImpl.java
            |       |       ProductoServiceImpl.java
            |       |       ProveedorServiceImpl.java
            |       |       RangoFechas.java
            |       |       ReembolsoServiceImpl.java
            |       |       ReporteCsv.java
            |       |       ReporteServiceImpl.java
            |       |       RetiroSocioServiceImpl.java
            |       |       RolImpl.java
            |       |       RrhhAsistenciaImpl.java
            |       |       SedeServiceImpl.java
            |       |       ServicioImportacionServiceImpl.java
            |       |       ServicioServiceImpl.java
            |       |       StockOperacionServiceImpl.java
            |       |       StockServiceImpl.java
            |       |       SunatFacturacionClientPendiente.java
            |       |       TurnoCajaServiceImpl.java
            |       |       TurnoTrabajoServiceImpl.java
            |       |       UserImpl.java
            |       |       UtilidadNetaServiceImpl.java
            |       |       VentaServiceImpl.java
            |       |       
            |       \---support
            |               CalculoCotizacion.java
            |               RangosConsulta.java
            |               
            +---domain
            |   +---enums
            |   |       AccionAudit.java
            |   |       AgrupacionReporte.java
            |   |       CanalCotizacion.java
            |   |       CanalVenta.java
            |   |       ClienteTipoDocumentoEnum.java
            |   |       ConceptoCajaChica.java
            |   |       CriterioRankingCliente.java
            |   |       CriterioRankingProducto.java
            |   |       CriterioRankingProveedor.java
            |   |       EstadoAsistencia.java
            |   |       EstadoComision.java
            |   |       EstadoCompra.java
            |   |       EstadoCotizacion.java
            |   |       EstadoCxP.java
            |   |       EstadoDespacho.java
            |   |       EstadoDevolucion.java
            |   |       EstadoMensajeWeb.java
            |   |       EstadoNomina.java
            |   |       EstadoPago.java
            |   |       EstadoPreforma.java
            |   |       EstadoProducto.java
            |   |       EstadoRecepcion.java
            |   |       EstadoReembolso.java
            |   |       EstadoRetiro.java
            |   |       EstadoSunat.java
            |   |       EstadoTurno.java
            |   |       EstadoVenta.java
            |   |       MetodoPago.java
            |   |       MetodoReembolso.java
            |   |       Moneda.java
            |   |       OrigenFondos.java
            |   |       ResultadoArqueo.java
            |   |       RolNombre.java
            |   |       SistemaPension.java
            |   |       TipoCajaChica.java
            |   |       TipoCargoCotizacion.java
            |   |       TipoComprobante.java
            |   |       TipoCuentaFinanciera.java
            |   |       TipoDocumentoLegajo.java
            |   |       TipoGastoEmpresarial.java
            |   |       TipoMarcacion.java
            |   |       TipoMovilidad.java
            |   |       TipoMovimiento.java
            |   |       TipoProducto.java
            |   |       TipoReferencia.java
            |   |       TipoReferenciaCxP.java
            |   |       
            |   \---model
            |           AjusteStock.java
            |           Almacen.java
            |           AuditLog.java
            |           CatalogoFraccion.java
            |           Categoria.java
            |           Cliente.java
            |           CmsAjuste.java
            |           Compra.java
            |           CompraAdjunto.java
            |           ConceptoCotizacion.java
            |           ConfiguracionEmpresa.java
            |           ConfiguracionFinanzas.java
            |           ConfiguracionInventario.java
            |           ConfiguracionReportes.java
            |           ConfiguracionVentas.java
            |           Correlativo.java
            |           Cotizacion.java
            |           CuentaFinanciera.java
            |           DetalleCompra.java
            |           DetalleCotizacion.java
            |           DetalleDevolucion.java
            |           DetallePreforma.java
            |           DetalleVenta.java
            |           Devolucion.java
            |           FinanzasCuentaPorPagar.java
            |           FinanzasRetiroSocio.java
            |           GastoEmpresarial.java
            |           HorarioOperativo.java
            |           KardexMovimiento.java
            |           MensajeContacto.java
            |           Movilidad.java
            |           MovimientoCajaChica.java
            |           Notificacion.java
            |           OrdenDespacho.java
            |           PagoCuentaPorPagar.java
            |           PagoVenta.java
            |           ParametroCotizacion.java
            |           PreformaVenta.java
            |           Producto.java
            |           Proveedor.java
            |           ProyectoPortafolio.java
            |           Reembolso.java
            |           RefreshToken.java
            |           Rol.java
            |           RrhhAsistencia.java
            |           RrhhComision.java
            |           RrhhLegajoDigital.java
            |           RrhhLegajoDocumento.java
            |           RrhhNomina.java
            |           RrhhTurnoTrabajo.java
            |           SaldoCuentaFinanciera.java
            |           Sede.java
            |           SerieComprobante.java
            |           Servicio.java
            |           ServicioColaborador.java
            |           Stock.java
            |           TarifaMovilidad.java
            |           TransferenciaStock.java
            |           TurnoCaja.java
            |           User.java
            |           Venta.java
            |           
            +---exception
            |       BusinessException.java
            |       FileStorageException.java
            |       GlobalExceptionHandler.java
            |       ImportacionRechazadaException.java
            |       ResourceNotFoundException.java
            |       
            +---infrastructure
            |   +---config
            |   |       ApiTrailingSlashFilter.java
            |   |       FileStorageProperties.java
            |   |       HibernateJackson3FormatMapper.java
            |   |       OpenApiConfig.java
            |   |       PlataformaConfig.java
            |   |       PreformaExpiracionScheduler.java
            |   |       
            |   +---controller
            |   |       AlmacenController.java
            |   |       AuditoriaController.java
            |   |       AuthController.java
            |   |       CanalWebController.java
            |   |       CatalogoFraccionController.java
            |   |       CategoriaController.java
            |   |       ClienteController.java
            |   |       CmsController.java
            |   |       ComisionController.java
            |   |       CompraController.java
            |   |       ConfiguracionController.java
            |   |       CotizacionController.java
            |   |       CuentaFinancieraController.java
            |   |       CuentaPorPagarController.java
            |   |       DevolucionController.java
            |   |       ExportacionController.java
            |   |       FacturacionElectronicaController.java
            |   |       FinanzasController.java
            |   |       FinanzasRetiroSocioController.java
            |   |       GastoEmpresarialController.java
            |   |       KardexController.java
            |   |       LegajoController.java
            |   |       MovilidadController.java
            |   |       NominaController.java
            |   |       NotificacionController.java
            |   |       OrdenDespachoController.java
            |   |       PerfilController.java
            |   |       PortafolioController.java
            |   |       PortalPublicoController.java
            |   |       PortalRecepcionController.java
            |   |       PreformaController.java
            |   |       ProductoController.java
            |   |       ProveedorController.java
            |   |       ReembolsoController.java
            |   |       RendimientoController.java
            |   |       ReporteController.java
            |   |       RolController.java
            |   |       RrhhAsistenciaController.java
            |   |       SedeController.java
            |   |       ServicioController.java
            |   |       ServicioImportacionController.java
            |   |       StockController.java
            |   |       StockOperacionController.java
            |   |       TurnoCajaController.java
            |   |       TurnoTrabajoController.java
            |   |       UserController.java
            |   |       UtilidadNetaController.java
            |   |       VentaController.java
            |   |       
            |   +---export
            |   |       ExcelEstilos.java
            |   |       PdfCorporativo.java
            |   |       ReporteSolicitud.java
            |   |       
            |   +---geo
            |   |       PeruGeocoder.java
            |   |       
            |   +---loader
            |   |       CatalogoSeeder.java
            |   |       ConfiguracionInicializador.java
            |   |       CotizacionEsquemaInicializador.java
            |   |       DataLoader.java
            |   |       FormatoCotizacionInicializador.java
            |   |       
            |   +---repository
            |   |   |   AjusteStockRepository.java
            |   |   |   AlmacenRepository.java
            |   |   |   AuditLogRepository.java
            |   |   |   CatalogoFraccionRepository.java
            |   |   |   CategoriaRepository.java
            |   |   |   ClienteRepository.java
            |   |   |   CmsAjusteRepository.java
            |   |   |   CompraAdjuntoRepository.java
            |   |   |   CompraRepository.java
            |   |   |   ConceptoCotizacionRepository.java
            |   |   |   ConfiguracionEmpresaRepository.java
            |   |   |   ConfiguracionFinanzasRepository.java
            |   |   |   ConfiguracionInventarioRepository.java
            |   |   |   ConfiguracionReportesRepository.java
            |   |   |   ConfiguracionVentasRepository.java
            |   |   |   CorrelativoRepository.java
            |   |   |   CotizacionRepository.java
            |   |   |   CuentaFinancieraRepository.java
            |   |   |   DetalleCompraRepository.java
            |   |   |   DetalleCotizacionRepository.java
            |   |   |   DetalleDevolucionRepository.java
            |   |   |   DetallePreformaRepository.java
            |   |   |   DetalleVentaRepository.java
            |   |   |   DevolucionRepository.java
            |   |   |   FinanzasCuentaPorPagarRepository.java
            |   |   |   FinanzasRetiroSocioRepository.java
            |   |   |   GastoEmpresarialRepository.java
            |   |   |   HorarioOperativoRepository.java
            |   |   |   KardexRepository.java
            |   |   |   MensajeContactoRepository.java
            |   |   |   MovilidadRepository.java
            |   |   |   MovimientoCajaChicaRepository.java
            |   |   |   NotificacionRepository.java
            |   |   |   OrdenDespachoRepository.java
            |   |   |   PagoCuentaPorPagarRepository.java
            |   |   |   PagoVentaRepository.java
            |   |   |   ParametroCotizacionRepository.java
            |   |   |   PreformaVentaRepository.java
            |   |   |   ProductoRepository.java
            |   |   |   ProveedorRepository.java
            |   |   |   ProyectoPortafolioRepository.java
            |   |   |   ReembolsoRepository.java
            |   |   |   RefreshTokenRepository.java
            |   |   |   RolRepository.java
            |   |   |   RrhhAsistenciaRepository.java
            |   |   |   RrhhComisionRepository.java
            |   |   |   RrhhLegajoDigitalRepository.java
            |   |   |   RrhhLegajoDocumentoRepository.java
            |   |   |   RrhhNominaRepository.java
            |   |   |   RrhhTurnoTrabajoRepository.java
            |   |   |   SaldoCuentaFinancieraRepository.java
            |   |   |   SedeRepository.java
            |   |   |   SerieComprobanteRepository.java
            |   |   |   ServicioRepository.java
            |   |   |   StockRepository.java
            |   |   |   TarifaMovilidadRepository.java
            |   |   |   TransferenciaStockRepository.java
            |   |   |   TurnoCajaRepository.java
            |   |   |   UserRepository.java
            |   |   |   VentaRepository.java
            |   |   |   
            |   |   \---projection
            |   |           ActividadUsuarioProjection.java
            |   |           ComprasPorProveedorProjection.java
            |   |           ConteoPorAccionProjection.java
            |   |           ConteoPorFechaProjection.java
            |   |           ConteoPorTablaProjection.java
            |   |           ConteoPorTextoProjection.java
            |   |           CxpPendienteProjection.java
            |   |           DespachosPorEstadoProjection.java
            |   |           DespachosPorRepartidorProjection.java
            |   |           DevolucionesPorEstadoProjection.java
            |   |           DevueltoPorProductoProjection.java
            |   |           EfectivoPorTurnoProjection.java
            |   |           MermaProjection.java
            |   |           MontoCantidadPorIdProjection.java
            |   |           MontoCantidadProjection.java
            |   |           MontoPorMetodoProjection.java
            |   |           MontoPorSedeMetodoProjection.java
            |   |           MontoPorSedeProjection.java
            |   |           NominaTotalesPorEstadoProjection.java
            |   |           PagoCxpHistoricoProjection.java
            |   |           PagoCxpPorSedeProjection.java
            |   |           PagoPorVentaProjection.java
            |   |           PreformasPorEstadoProjection.java
            |   |           RecepcionCompraProjection.java
            |   |           ReembolsoPorSedeMetodoProjection.java
            |   |           RentabilidadProjection.java
            |   |           RetiroPorAdminProjection.java
            |   |           RetiroPorOrigenProjection.java
            |   |           RetiroPorSedeOrigenProjection.java
            |   |           SaldoProductoAlmacenProjection.java
            |   |           StockConsolidadoProjection.java
            |   |           TotalesFinancierosProjection.java
            |   |           TotalPorTipoMovimientoProjection.java
            |   |           UsoMovilidadProjection.java
            |   |           ValorizacionProductoProjection.java
            |   |           VentasPorClienteProjection.java
            |   |           VentasPorDiaProjection.java
            |   |           VentasPorVendedorProjection.java
            |   |           
            |   \---security
            |           JwtAuthenticationEntryPoint.java
            |           JwtAuthenticationFilter.java
            |           JwtUtil.java
            |           RespuestaNoAutorizado.java
            |           SecurityConfig.java
            |           SesionUsuarioCache.java
            |           TokenDenylist.java
            |           
            \---util
                    CsvUtil.java
                    HorarioUtil.java
                    SecretGenerator.java
                    
```
### Frontend
```text

Listado de rutas de carpetas para el volumen Windows-SSD
El número de serie del volumen es 4A6B-F158
C:\USERS\NANCY SANDOVAL\DOWNLOADS\ERP\POLICALUM\FRONTEND\SRC\APP
|   app.config.ts
|   app.css
|   app.html
|   app.routes.ts
|   app.spec.ts
|   app.ts
|   module.routes.ts
|   
+---components
|   +---asistencia
|   |       asistencia.component.html
|   |       asistencia.css
|   |       asistencia.spec.ts
|   |       asistencia.ts
|   |       listado-asistencias.component.css
|   |       listado-asistencias.component.html
|   |       listado-asistencias.component.ts
|   |       
|   +---login
|   |       login.component.css
|   |       login.component.html
|   |       login.component.ts
|   |       
|   \---qr-publico
|           qr-publico.component.css
|           qr-publico.component.html
|           qr-publico.component.ts
|           
+---core
|   |   erp-error.handler.ts
|   |   erp-title.strategy.ts
|   |   
|   +---api
|   |       api-endpoints.generated.ts
|   |       backend-api.service.ts
|   |       openapi.types.ts
|   |       
|   \---interceptors
|           api-error.interceptor.ts
|           auth.interceptor.ts
|           
+---features
|   +---almacenes
|   |   +---data-access
|   |   |       almacenes.api.ts
|   |   |       
|   |   \---pages
|   |           almacenes-page.component.css
|   |           almacenes-page.component.html
|   |           almacenes-page.component.ts
|   |           
|   +---auditoria
|   |   +---data-access
|   |   |       auditoria.api.ts
|   |   |       
|   |   \---pages
|   |           auditoria-page.component.css
|   |           auditoria-page.component.html
|   |           auditoria-page.component.ts
|   |           
|   +---catalogo-fracciones
|   |   +---data-access
|   |   |       catalogo-fracciones.api.ts
|   |   |       
|   |   \---pages
|   |           catalogo-fracciones-page.component.css
|   |           catalogo-fracciones-page.component.html
|   |           catalogo-fracciones-page.component.ts
|   |           
|   +---categorias
|   |   +---data-access
|   |   |       categorias.api.ts
|   |   |       
|   |   \---pages
|   |           categorias-page.component.css
|   |           categorias-page.component.html
|   |           categorias-page.component.ts
|   |           
|   +---clientes
|   |   +---data-access
|   |   |       clientes.api.ts
|   |   |       
|   |   \---pages
|   |           clientes-page.component.css
|   |           clientes-page.component.html
|   |           clientes-page.component.ts
|   |           
|   +---compras
|   |   +---data-access
|   |   |       compras.api.ts
|   |   |       
|   |   \---pages
|   |           compras-page.component.css
|   |           compras-page.component.html
|   |           compras-page.component.ts
|   |           
|   +---configuracion
|   |   +---data-access
|   |   |       configuracion.api.ts
|   |   |       
|   |   \---pages
|   |           configuracion-page.component.css
|   |           configuracion-page.component.html
|   |           configuracion-page.component.ts
|   |           formato-cotizacion.component.html
|   |           formato-cotizacion.component.ts
|   |           
|   +---cotizaciones
|   |   +---data-access
|   |   |       cotizaciones.api.ts
|   |   |       
|   |   \---pages
|   |           cotizaciones-page.component.css
|   |           cotizaciones-page.component.html
|   |           cotizaciones-page.component.ts
|   |           
|   +---cuentas-por-pagar
|   |   +---data-access
|   |   |       cuentas-por-pagar.api.ts
|   |   |       
|   |   \---pages
|   |           cuentas-por-pagar-page.component.css
|   |           cuentas-por-pagar-page.component.html
|   |           cuentas-por-pagar-page.component.ts
|   |           
|   +---dashboard
|   |   \---pages
|   |           dashboard-page.component.css
|   |           dashboard-page.component.html
|   |           dashboard-page.component.ts
|   |           
|   +---despachos
|   |   +---data-access
|   |   |       despacho-evidencia.api.ts
|   |   |       despachos.api.ts
|   |   |       
|   |   \---pages
|   |           despachos-page.component.css
|   |           despachos-page.component.html
|   |           despachos-page.component.ts
|   |           
|   +---devoluciones
|   |   +---data-access
|   |   |       devoluciones.api.ts
|   |   |       
|   |   \---pages
|   |           devoluciones-page.component.css
|   |           devoluciones-page.component.html
|   |           devoluciones-page.component.ts
|   |           
|   +---errores
|   |       error-page.component.css
|   |       error-page.component.html
|   |       error-page.component.ts
|   |       
|   +---facturacion-electronica
|   |   +---components
|   |   |       facturacion-sunat.component.css
|   |   |       facturacion-sunat.component.html
|   |   |       facturacion-sunat.component.ts
|   |   |       
|   |   +---data-access
|   |   |       facturacion-electronica.api.ts
|   |   |       
|   |   \---pages
|   |           facturacion-electronica-page.component.ts
|   |           
|   +---finanzas
|   |   +---data-access
|   |   |       finanzas.api.ts
|   |   |       
|   |   \---pages
|   |           finanzas-page.component.css
|   |           finanzas-page.component.html
|   |           finanzas-page.component.ts
|   |           
|   +---gestor-web
|   |       gestor-web-page.component.ts
|   |       
|   +---kardex
|   |   +---data-access
|   |   |       kardex.api.ts
|   |   |       
|   |   \---pages
|   |           kardex-page.component.css
|   |           kardex-page.component.html
|   |           kardex-page.component.ts
|   |           
|   +---mensajes-web
|   |       mensajes-web-page.component.css
|   |       mensajes-web-page.component.html
|   |       mensajes-web-page.component.ts
|   |       
|   +---movilidades
|   |   +---data-access
|   |   |       movilidades.api.ts
|   |   |       
|   |   \---pages
|   |           movilidades-page.component.css
|   |           movilidades-page.component.html
|   |           movilidades-page.component.ts
|   |           
|   +---notificaciones
|   |       notificaciones-page.component.css
|   |       notificaciones-page.component.html
|   |       notificaciones-page.component.ts
|   |       notificaciones.api.ts
|   |       
|   +---perfil
|   |   +---data-access
|   |   |       perfil.api.ts
|   |   |       
|   |   \---pages
|   |           perfil-page.component.css
|   |           perfil-page.component.html
|   |           perfil-page.component.ts
|   |           
|   +---portafolio-web
|   |       portafolio-web-page.component.css
|   |       portafolio-web-page.component.html
|   |       portafolio-web-page.component.ts
|   |       
|   +---preformas
|   |   +---data-access
|   |   |       preformas.api.ts
|   |   |       
|   |   \---pages
|   |           preformas-page.component.css
|   |           preformas-page.component.html
|   |           preformas-page.component.ts
|   |           
|   +---productos
|   |   +---data-access
|   |   |       productos-masivo.api.ts
|   |   |       productos.api.ts
|   |   |       
|   |   \---pages
|   |           productos-page.component.css
|   |           productos-page.component.html
|   |           productos-page.component.ts
|   |           
|   +---proveedores
|   |   +---data-access
|   |   |       proveedores.api.ts
|   |   |       
|   |   \---pages
|   |           proveedores-page.component.css
|   |           proveedores-page.component.html
|   |           proveedores-page.component.ts
|   |           
|   +---reembolsos
|   |   +---data-access
|   |   |       reembolsos.api.ts
|   |   |       
|   |   \---pages
|   |           reembolsos-page.component.css
|   |           reembolsos-page.component.html
|   |           reembolsos-page.component.ts
|   |           
|   +---reportes
|   |   +---data-access
|   |   |       reportes.api.ts
|   |   |       
|   |   \---pages
|   |           reportes-page.component.css
|   |           reportes-page.component.html
|   |           reportes-page.component.ts
|   |           
|   +---roles
|   |   +---data-access
|   |   |       roles.api.ts
|   |   |       
|   |   \---pages
|   |           roles-page.component.css
|   |           roles-page.component.html
|   |           roles-page.component.ts
|   |           
|   +---rrhh
|   |   +---data-access
|   |   |       rrhh.api.ts
|   |   |       
|   |   \---pages
|   |           rrhh-page.component.css
|   |           rrhh-page.component.html
|   |           rrhh-page.component.ts
|   |           
|   +---sedes
|   |   +---components
|   |   |       peru-mapa.component.ts
|   |   |       
|   |   +---data-access
|   |   |       peru-geocoder.ts
|   |   |       sede-ubicacion.api.ts
|   |   |       sedes.api.ts
|   |   |       
|   |   \---pages
|   |           sedes-page.component.css
|   |           sedes-page.component.html
|   |           sedes-page.component.ts
|   |           
|   +---servicios
|   |   +---data-access
|   |   |       servicios.api.ts
|   |   |       
|   |   \---pages
|   |           servicios-page.component.css
|   |           servicios-page.component.html
|   |           servicios-page.component.ts
|   |           
|   +---stock
|   |   +---data-access
|   |   |       stock.api.ts
|   |   |       
|   |   \---pages
|   |           stock-page.component.css
|   |           stock-page.component.html
|   |           stock-page.component.ts
|   |           
|   +---turnos-caja
|   |   +---data-access
|   |   |       turnos-caja.api.ts
|   |   |       
|   |   \---pages
|   |           turnos-caja-page.component.css
|   |           turnos-caja-page.component.html
|   |           turnos-caja-page.component.ts
|   |           
|   +---users
|   |   +---data-access
|   |   |       users.api.ts
|   |   |       
|   |   \---pages
|   |           users-page.component.css
|   |           users-page.component.html
|   |           users-page.component.ts
|   |           
|   \---ventas
|       |   pos.component.css
|       |   pos.component.html
|       |   pos.component.ts
|       |   ventas-api.service.ts
|       |   ventas.css
|       |   
|       +---data-access
|       |       ventas.api.ts
|       |       
|       \---pages
|               ventas-page.component.css
|               ventas-page.component.html
|               ventas-page.component.ts
|               
+---guards
|       auth.guard.ts
|       role.guard.ts
|       
+---layout
|   |   authenticated-layout.component.ts
|   |   navigation.ts
|   |   
|   \---components
|       +---footer
|       |       footer.css
|       |       footer.html
|       |       footer.spec.ts
|       |       footer.ts
|       |       
|       +---header
|       |       header.css
|       |       header.html
|       |       header.spec.ts
|       |       header.ts
|       |       
|       +---navbar
|       |       navbar.css
|       |       navbar.html
|       |       navbar.spec.ts
|       |       navbar.ts
|       |       
|       \---sidebar
|               sidebar.css
|               sidebar.html
|               sidebar.spec.ts
|               sidebar.ts
|               
+---models
|       asistencia.model.ts
|       user.model.ts
|       
+---services
|       asistencia.service.ts
|       auth.service.ts
|       token.service.ts
|       
\---shared
    +---components
    |   +---bar-chart
    |   |       bar-chart.component.ts
    |   |       
    |   +---breadcrumbs
    |   |       breadcrumbs.component.ts
    |   |       
    |   +---confirm-dialog
    |   |       confirm-dialog.component.ts
    |   |       
    |   +---data-table
    |   |       data-table.component.ts
    |   |       
    |   +---empty-state
    |   |       empty-state.component.ts
    |   |       
    |   +---export-buttons
    |   |       export-buttons.component.ts
    |   |       
    |   +---filter-bar
    |   |       filter-bar.component.ts
    |   |       
    |   +---form-field
    |   |       form-field.component.ts
    |   |       
    |   +---icon
    |   |       icon.component.ts
    |   |       
    |   +---loader
    |   |       loader.component.ts
    |   |       
    |   +---mn-button
    |   |       mn-button.component.ts
    |   |       
    |   +---mn-card
    |   |       mn-card.component.ts
    |   |       
    |   +---modal
    |   |       modal.component.ts
    |   |       
    |   +---page-header
    |   |       page-header.component.ts
    |   |       
    |   +---pagination
    |   |       pagination.component.ts
    |   |       
    |   +---select
    |   |       select.component.ts
    |   |       
    |   +---stat-card
    |   |       stat-card.component.ts
    |   |       
    |   +---tabs
    |   |       tabs.component.ts
    |   |       
    |   \---toast
    |           toast-host.component.ts
    |           
    +---i18n
    |       es.ts
    |       i18n.service.ts
    |       translate.pipe.ts
    |       
    +---models
    |       ui.models.ts
    |       
    +---pipes
    |       format.pipes.ts
    |       
    +---services
    |       avatar.service.ts
    |       caja-gate.service.ts
    |       caja-session.service.ts
    |       canal-web-avisos.service.ts
    |       inventario-sync.service.ts
    |       layout.service.ts
    |       lookup.service.ts
    |       marca.service.ts
    |       modal-stack.service.ts
    |       report.service.ts
    |       sede-filtro.service.ts
    |       theme.service.ts
    |       toast.service.ts
    |       
    \---utils
            fecha-local.ts
            feedback.ts
            page-state.ts
            turno.ts
            validators.ts
            
```
### Sitio
```text

Listado de rutas de carpetas para el volumen Windows-SSD
El número de serie del volumen es 4A6B-F158
C:\USERS\NANCY SANDOVAL\DOWNLOADS\ERP\POLICALUM\SITIO_WEB\SRC\APP
|   app.config.ts
|   app.html
|   app.routes.ts
|   app.spec.ts
|   app.ts
|   
+---components
|       asistente.component.ts
|       ficha-producto.component.ts
|       mn-button.component.ts
|       mn-card.component.ts
|       visor-producto.component.ts
|       
+---core
|       aviso.service.ts
|       carrito.service.ts
|       cms.store.ts
|       empresa.store.ts
|       error.interceptor.ts
|       marca.service.ts
|       models.ts
|       obras.ts
|       portal.api.ts
|       tema.service.ts
|       
\---pages
        carrito.component.html
        carrito.component.ts
        catalogo.component.html
        catalogo.component.ts
        contacto.component.html
        contacto.component.ts
        cotizar.component.html
        cotizar.component.ts
        inicio.component.html
        inicio.component.ts
        legal.component.ts
        portafolio.component.html
        portafolio.component.ts
        producto.component.html
        producto.component.ts
        servicios.component.ts
        
```

