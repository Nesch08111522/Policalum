# POLICALUM ERP

Sistema de gestión para una operación comercial con varias sedes en el Perú. Cubre la venta de mostrador, el inventario, las compras, la caja, las finanzas internas, las personas y la auditoría. El administrador ve la operación en tiempo real. Un mismo usuario puede tener la sesión abierta en dos sedes a la vez, cada una con su caja, sin que el cierre de una tumbe a la otra.

Este documento describe lo que el sistema hace hoy, cómo está armado y qué queda fuera hasta contratar dominio, SUNAT, pasarela y correo.

## Cómo está armado

- Backend: Spring Boot 4, Java 17, Spring Security con JWT, JPA y PostgreSQL. El perfil de desarrollo actualiza el esquema al arrancar.
- Frontend: Angular 22, pantallas en español, tema claro y oscuro.
- API: `/api/v1`, autenticada salvo el login, la marca pública y el QR de asistencia.
- Archivos: fotos y adjuntos en disco local (`./uploads`), no dentro de la base.
- Caché: catálogos de sedes, categorías y la configuración pública duran 10 minutos. La validez de la sesión se relee como máximo cada 20 segundos, y se invalida al cerrar las demás sesiones.
- Reportes: Excel con Apache POI y PDF con OpenPDF, generados en segundo plano, con tope de 2000 filas.

## Seguridad y varias sesiones

El acceso es stateless. La contraseña no viaja de vuelta en ningún JSON. El token de acceso dura 8 horas y el de refresco 14 días. El refresco es opaco: en la base solo se guarda su hash, se rota en cada uso y queda invalidado si la versión de sesión cambia.

RBAC con dos roles de trabajo:

- `ROLE_ADMIN` ve configuración, usuarios, roles, sedes, auditoría y finanzas.
- `ROLE_MULTIFUNCIONAL` opera ventas, inventario, compras y su asistencia. Finanzas y gastos empresariales responden 403.

Un administrador puede iniciar sesión en la sede 1 y en la sede 2 al mismo tiempo. Cada pestaña guarda la sede elegida en `sessionStorage`, así que dos pestañas no se pisan. Cerrar sesión en un navegador revoca solo el token de acceso y el refresco de ese navegador. El otro sigue consultando y puede renovar su refresco. «Cerrar otras sesiones», en el perfil, sube la versión de sesión, corta los accesos restantes y deja un token nuevo en el navegador que lo pidió.

Cabeceras de respuesta: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` y política de referrer estricta. CSRF está apagado porque la API no usa cookies de sesión.

La lista de tokens de acceso revocados vive en memoria del proceso. Al reiniciar el servidor, un token de acceso ya emitido vuelve a ser válido hasta que venza; el refresco revocado en la base no vuelve. Cuando haya más de un servidor hará falta una lista compartida.

## Sedes, caja y mapa

Hoy la base de demostración tiene la Sede Principal en Lima. El módulo admite las que se registren: la segunda sede, y las siguientes, se crean en Configuración → Sedes, con ciudad, dirección, latitud, longitud y foto (JPG, PNG o WEBP de hasta 5 MB).

Si no se escriben coordenadas y la ciudad es una de las principales del país (Lima, Callao, Arequipa, Trujillo, Cusco, Piura, Chiclayo, Iquitos, Huancayo, Tacna, Chimbote, Pucallpa, Juliaca, Ica, Cajamarca, Puno, Ayacucho, Huánuco), el servidor las completa con un gazetter local. No llama a internet y no tiene cupo. El mapa del módulo usa Leaflet y teselas libres de OpenStreetMap, centrado en el Perú, con un punto por sede ubicada.

Eso no reemplaza un geocodificador de calles. Cuando haya servidor propio, el paso siguiente es Photon o Nominatim en Docker con el mapa de Perú (el extracto pesa menos de 100 MB), o Gisgraphy sobre PostGIS. Esas piezas siguen sin instalarse a propósito: no se descargó el mapa del país en esta entrega.

La caja es una por usuario y por sede. El administrador puede tener la caja de la sede 1 y la de la sede 2 abiertas a la vez. Si elige una sede sin turno abierto, el sistema lo lleva a Caja antes de operar. Un error de red no lo deja atrapado. El resumen del turno muestra montos con etiqueta (inicial, ventas en efectivo, retiros, reembolsos, esperado, declarado y diferencia). La caja chica (transporte, suministros, oficina u otro) ya entra en el efectivo esperado.

## Módulos

### Inicio

- Panel: indicadores del día, productos estrella y ranking de vendedores.
- Notificaciones: stock mínimo y stock crítico. Se marcan leídas, se archivan y a los 30 días se purgan. La campana solo muestra las nuevas.

### Ventas

- Punto de venta y listado de ventas, con comprobante boleta, factura o ticket.
- Clientes, con búsqueda, validación de documento y correo, e importación ya existente de productos en el catálogo.
- Cotizaciones: emitir, aceptar, rechazar, vencer, marcar «no compró» (no mueve stock) y convertir a venta si está aceptada y hay caja abierta. PDF y Excel del documento.
- Preformas abiertas y vencidas.
- Devoluciones y reembolsos, atados al turno de caja.
- Caja: apertura, cierre, resumen y caja chica.

### Inventario

- Productos, con costo promedio ponderado, imagen por archivo o URL, e importación Excel.
- Servicios, con plantilla e importación Excel/CSV.
- Categorías y fracciones de venta.
- Stock por almacén, ajustes, transferencias, bajo stock y consolidado.
- Kardex valorizado: cada movimiento guarda costo y precio de venta del momento, margen y ganancia estimada.
- Almacenes por sede.

### Compras

- Compras con adjuntos, proveedores, cuentas por pagar, despachos y movilidades.

### Finanzas (solo administrador)

- Flujo de caja realizado, devengado y proyección.
- Patrimonio, KPIs y evolución mensual.
- Utilidad operativa: utilidad bruta menos nóminas y gastos empresariales (alquiler, agua, luz, personal u otro).
- Retiros de socios, con efecto en el efectivo esperado de la caja.
- Cuentas financieras.

### Facturación SUNAT

La pantalla existe para el panel de comprobantes electrónicos. No hay conexión certificada con SUNAT, ni certificado digital, ni envío de XML/CDR. Hasta contratar ese servicio los comprobantes del ERP son internos (boleta, factura y ticket).

### Personas

- Ficha, legajo, foto y sede asignada.
- Asistencia con QR de kiosco y marcación del día.
- Turnos de trabajo, comisiones y nómina con boleta.
- El perfil permite cambiar contraseña, activar un segundo factor con código temporal y cerrar las otras sesiones. No envía la contraseña por correo.

### Análisis

- Reportes de ventas, rentabilidad, clientes frecuentes, ranking de proveedores y arqueos de caja, en pantalla, Excel y PDF.
- Auditoría de eventos. El detalle se lee campo por campo.

### Configuración

- Usuarios y roles. El administrador edita la matriz de permisos del rol.
- Empresa: razón social, RUC, series y logo.
- Sedes, con foto y mapa.

Exportación corporativa desde clientes, productos, stock, kardex, ventas, compras, gastos y recursos humanos.

## Lo que se probó

Contra el backend en marcha, con el administrador de demostración:

- 121 consultas GET de los módulos responden 200.
- Cuatro consultas responden 404 porque no hay dato, y ese es el contrato: logo de empresa aún no cargado, legajo del administrador aún no creado, foto de perfil aún no subida, y un RUC de proveedor que no existe.
- El rol multifuncional recibe 403 en gastos empresariales.
- Dos logins seguidos del mismo administrador producen tokens distintos. Cerrar el primero deja el segundo operativo, incluido su refresco. Reusar un refresco ya rotado falla.
- Excel y PDF de clientes se generan.
- El mapa de sedes muestra el Perú, el punto de Lima y la ficha de la sede, en escritorio y en un ancho de teléfono. El resumen de caja y el detalle de auditoría no vuelcan JSON.
- Cabeceras `nosniff` y `DENY` presentes.

La colección está en `postman/POLICALUM.postman_collection.json` con el entorno `postman/POLICALUM.postman_environment.json`. En VS Code se importa con la extensión de Postman (entorno «POLICALUM local» y después la colección). El mismo barrido se puede repetir con `python3 postman/barrido.py` mientras el backend escucha en el puerto 8080. Ese script también regenera la colección.

Las pruebas de interfaz abrieron y cerraron un turno en cero para poder entrar a Sedes. Quedó cerrado, con observación de verificación. No se borró el historial.

## Lo que falta para una operación nacional contratada

Estas piezas se dejan explícitamente pendientes. No están simuladas.

1. Dominio y HTTPS del ERP, con el frontend y la API publicados. Hoy el sistema corre en el servidor local (puertos 4200 y 8080) y el CORS ya admite localhost, la red de la tienda y túneles ngrok de prueba.
2. API SUNAT: certificado, usuario SOL, envío, consulta de CDR, bajas y resumen diario. La pantalla de facturación no sustituye ese contrato.
3. Pasarela de pagos (tarjetas y billeteras). Los cobros del punto de venta son registro interno, con efectivo separado para la caja.
4. Envío de contraseñas y de avisos por correo. El alta y el cambio de clave los hace un administrador o el propio usuario ya autenticado. No hay SMTP ni recuperación por bandeja.
5. Geocodificador de calles y mapa propio. Hoy: gazetter de ciudades más teselas de OpenStreetMap. Mañana, en el mismo servidor: Photon, Nominatim o Gisgraphy, sin cupo y sin costo de licencia. Las teselas públicas de OpenStreetMap no deben usarse como mapa de producción con mucho tráfico; en ese momento se autoaloja el estilo o se contrata un proveedor.
6. Lista compartida de tokens revocados si el ERP pasa a más de una instancia.
7. La segunda sede, y las que sigan, se registran en el módulo. El mapa las irá sumando sin otro cambio de código.

## Puesta en marcha local

Backend, desde `backend/`, con perfil `dev` y la clave de PostgreSQL en `DB_PASSWORD`:

`bash mvnw -B spring-boot:run`

Frontend, desde `frontend/`:

`npx ng serve --host 0.0.0.0 --port 4200`

Al primer arranque se crean los roles, la Sede Principal y los usuarios de demostración. Las contraseñas están en el entorno de Postman y en el cargador de datos; no se repiten aquí.
