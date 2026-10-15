
-- ================================================================
-- MÓDULO 01: SEDES Y ALMACENES 
-- ================================================================
CREATE TABLE sedes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          VARCHAR(100) NOT NULL,
    pais            VARCHAR(100) DEFAULT 'Perú',
    ciudad          VARCHAR(100),
    direccion       VARCHAR(255),
    celular_sede    VARCHAR(20),
    moneda          VARCHAR(10) DEFAULT 'PEN',
    activa          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE almacenes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id     UUID NOT NULL REFERENCES sedes(id) ON DELETE CASCADE,
    nombre      VARCHAR(100) NOT NULL,
    activo      BOOLEAN DEFAULT TRUE,
    descripcion TEXT
);


-- ================================================================
-- MÓDULO 02: SEGURIDAD, ROLES Y USUARIOS
-- ================================================================
CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          VARCHAR(50) NOT NULL UNIQUE,
    permisos_json   JSONB,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_rol_nombre CHECK (nombre IN ('ROLE_ADMIN', 'ROLE_MULTIFUNCIONAL'))
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id         UUID NOT NULL REFERENCES roles(id),
    sede_id         UUID NOT NULL REFERENCES sedes(id),
    nombre_usuario  VARCHAR(50) UNIQUE NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    apellido        VARCHAR(100) NOT NULL,
    email           VARCHAR(150) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    celular         VARCHAR(15),
    activo          BOOLEAN DEFAULT TRUE,
    ultimo_login    TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
-- MÓDULO 03: RECURSOS HUMANOS — LEGAJO, ASISTENCIA Y TURNOS
-- ================================================================
CREATE TABLE rrhh_legajo_digital (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tipo_contrato           VARCHAR(50),            -- PLANILLA, RECIBO_HONORARIOS, CAS
    fecha_ingreso           DATE NOT NULL,
    sueldo_base             DECIMAL(12,2) NOT NULL DEFAULT 0,
    porcentaje_comision     DECIMAL(5,2) DEFAULT 0, -- Ej: 2.50 = 2.50% por venta
    documentos_json         JSONB,                  -- Rutas de contratos, CVs en S3/GCS
    activo                  BOOLEAN DEFAULT TRUE,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- TURNOS DE TRABAJO (horarios asignados al personal)
CREATE TABLE rrhh_turnos_trabajo (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    sede_id         UUID NOT NULL REFERENCES sedes(id),
    nombre_turno    VARCHAR(50) NOT NULL,       -- Ej: MAÑANA, TARDE, NOCHE
    hora_inicio     TIME NOT NULL,
    hora_fin        TIME NOT NULL,
    dias_semana     JSONB NOT NULL,             -- ["LUNES","MARTES","MIERCOLES",...]
    activo          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rrhh_asistencia (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    turno_trabajo_id    UUID REFERENCES rrhh_turnos_trabajo(id), -- Horario asignado (referencia)
    -- turno_caja_id se añade como FK circular resuelta al final del archivo
    turno_caja_id       UUID UNIQUE,            -- FK a turnos_caja (1 asistencia : 1 turno_caja)
    fecha               DATE NOT NULL DEFAULT CURRENT_DATE,
    hora_entrada        TIMESTAMP,              -- Se llena al abrir el turno_caja
    hora_salida         TIMESTAMP,              -- Se llena al cerrar el turno_caja
    horas_trabajadas    DECIMAL(5,2),           -- Calculado al cerrar: EXTRACT(EPOCH FROM (hora_salida - hora_entrada)) / 3600
    estado              VARCHAR(20) DEFAULT 'PENDIENTE', -- PENDIENTE, PRESENTE, TARDANZA, AUSENTE
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_asistencia_estado CHECK (estado IN ('PENDIENTE','PRESENTE','TARDANZA','AUSENTE'))
);

CREATE TABLE rrhh_nominas (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id),
    periodo                 VARCHAR(20) NOT NULL,            -- Ej: '2026-05'
    sueldo_base_calculado   DECIMAL(12,2) NOT NULL,
    total_comisiones        DECIMAL(12,2) DEFAULT 0,
    descuentos              DECIMAL(12,2) DEFAULT 0,
    total_neto              DECIMAL(12,2) NOT NULL,
    estado                  VARCHAR(30) DEFAULT 'PENDIENTE', -- PENDIENTE, APROBADA, PAGADA
    aprobado_por            UUID REFERENCES users(id),       -- Solo ROLE_ADMIN puede aprobar
    fecha_aprobacion        TIMESTAMP,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
-- MÓDULO 04: CATÁLOGO, PRODUCTOS Y FRACCIONES
-- ================================================================
CREATE TABLE categorias (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID REFERENCES categorias(id),
    nombre      VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) UNIQUE NOT NULL,
    activa      BOOLEAN DEFAULT TRUE
);

CREATE TABLE proveedores (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_razon      VARCHAR(255) NOT NULL,
    tipo_documento    VARCHAR(10) NOT NULL DEFAULT 'RUC', -- DNI, RUC, CE
    numero_documento  VARCHAR(15) NOT NULL,
    celular           VARCHAR(15),
    email             VARCHAR(150),
    activo            BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_documento_proveedor UNIQUE (tipo_documento, numero_documento)
);

CREATE TABLE productos (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    categoria_id     UUID REFERENCES categorias(id),
    proveedor_id     UUID REFERENCES proveedores(id),
    codigo_sku       VARCHAR(50),
    nombre           VARCHAR(255) NOT NULL,
    descripcion      TEXT,
    -- FISICO:   producto con stock físico (planchas, pernos, pegamentos, marcos, etc.)
    -- SERVICIO: mano de obra / servicio ejecutado (cortes, estructuras, instalaciones)
    tipo             VARCHAR(20) NOT NULL DEFAULT 'FISICO',
    precio_compra    DECIMAL(12,2) NOT NULL,
    -- precio_venta_base: precio de la unidad completa de venta.
    --   Para productos fraccionables (planchas): es el precio de la plancha entera.
    --     Las fracciones (3/4, 1/2, 1/4) se calculan a partir de este valor
    --     y se almacenan en catalogo_fracciones.
    --   Para productos NO fraccionables (pernos, pegamentos, marcos, etc.):
    --     es simplemente el precio por unidad/caja/rollo según su unidad de medida.
    precio_venta_base DECIMAL(12,2) NOT NULL,
    -- unidad: unidad de medida de venta (UNID=unidad, CAJ=caja, KG=kilogramo, etc.)
    unidad           VARCHAR(20) DEFAULT 'UNID',
    -- es_fraccionable: TRUE solo para productos que se comercializan en fracciones.
    --   Cuando es TRUE, el backend exige que existan filas en catalogo_fracciones.
    --   Cuando es FALSE, se vende siempre en cantidad entera; catalogo_fracciones
    --   no aplica y el POS no mostrará la opción de fraccionar.
    es_fraccionable  BOOLEAN NOT NULL DEFAULT FALSE,
    activo           BOOLEAN DEFAULT TRUE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tipo_producto CHECK (tipo IN ('FISICO', 'SERVICIO'))
);

-- catalogo_fracciones: solo existe para productos con es_fraccionable = TRUE.
-- El backend debe validar esta condición al insertar filas aquí.
-- factor_descuento_kardex define cuánto se descuenta del stock en equivalentes
-- de unidad completa: 1.0000=entera, 0.7500=3/4, 0.5000=1/2, 0.2500=1/4.
-- Ejemplo de lógica de equivalencia: 4 ventas de factor 0.25 = 1 unidad completa descontada.
CREATE TABLE catalogo_fracciones (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id             UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    nombre_fraccion         VARCHAR(50) NOT NULL,    -- Ej: 'Plancha Completa', 'Media Plancha'
    factor_descuento_kardex DECIMAL(5,4) NOT NULL,
    precio_venta            DECIMAL(12,2) NOT NULL,  -- precio_venta_base * factor * margen_config
    activo                  BOOLEAN DEFAULT TRUE,
    CONSTRAINT chk_factor           CHECK (factor_descuento_kardex > 0 AND factor_descuento_kardex <= 1),
    CONSTRAINT uq_producto_fraccion UNIQUE (producto_id, factor_descuento_kardex)
);

CREATE TABLE servicios (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio      DECIMAL(12,2) NOT NULL,
    activo      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
-- MÓDULO 05: INVENTARIO Y KARDEX (STOCK DECIMAL PARA FRACCIONES)
-- ================================================================
CREATE TABLE stock (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id     UUID NOT NULL REFERENCES productos(id),
    almacen_id      UUID NOT NULL REFERENCES almacenes(id),
    cantidad        DECIMAL(12,4) DEFAULT 0,  -- Soporta fracciones: 0.2500, 0.5000, etc.
    stock_minimo    DECIMAL(12,4) DEFAULT 0,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(producto_id, almacen_id)
);

-- tipo_referencia incluye DEVOLUCION para restaurar stock automáticamente
CREATE TABLE kardex_movimientos (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id         UUID NOT NULL REFERENCES productos(id),
    almacen_id          UUID NOT NULL REFERENCES almacenes(id),
    referencia_id       UUID,                  -- ID de Venta, Compra, Devolución u Ajuste
    tipo_referencia     VARCHAR(20) NOT NULL,  -- VENTA, COMPRA, AJUSTE, MERMA, TRANSFERENCIA, DEVOLUCION
    tipo_movimiento     VARCHAR(10) NOT NULL,  -- ENTRADA, SALIDA
    cantidad_movida     DECIMAL(12,4) NOT NULL,
    stock_resultante    DECIMAL(12,4) NOT NULL,
    user_id             UUID NOT NULL REFERENCES users(id),
    observacion         TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tipo_mov CHECK (tipo_movimiento IN ('ENTRADA', 'SALIDA')),
    CONSTRAINT chk_tipo_ref CHECK (tipo_referencia IN (
        'VENTA', 'COMPRA', 'AJUSTE', 'MERMA', 'TRANSFERENCIA', 'DEVOLUCION'
    ))
);

-- ================================================================
-- MÓDULO 06: COMPRAS Y CUENTAS POR PAGAR (CxP)
-- ================================================================
CREATE TABLE compras (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proveedor_id                UUID NOT NULL REFERENCES proveedores(id),
    sede_id                     UUID NOT NULL REFERENCES sedes(id),
    user_id                     UUID NOT NULL REFERENCES users(id),
    numero_factura_proveedor    VARCHAR(50),
    total                       DECIMAL(12,2) DEFAULT 0,
    estado_recepcion            VARCHAR(30) DEFAULT 'PENDIENTE', -- PENDIENTE, RECIBIDO_PARCIAL, RECIBIDO
    estado_pago                 VARCHAR(30) DEFAULT 'PENDIENTE', -- PENDIENTE, PAGADO_PARCIAL, PAGADO
    observaciones               TEXT,
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE detalle_compra (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    compra_id       UUID NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
    producto_id     UUID NOT NULL REFERENCES productos(id),
    almacen_id      UUID NOT NULL REFERENCES almacenes(id),
    cantidad        DECIMAL(12,4) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL
);

CREATE TABLE finanzas_cuentas_por_pagar (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id             UUID NOT NULL REFERENCES sedes(id),
    referencia_id       UUID,                  -- ID de la compra, nómina u otro gasto
    tipo_referencia     VARCHAR(20) NOT NULL,  -- COMPRA, PLANILLA, SERVICIO, OTRO
    descripcion         TEXT NOT NULL,
    monto_total         DECIMAL(12,2) NOT NULL,
    monto_pagado        DECIMAL(12,2) DEFAULT 0,
    estado              VARCHAR(30) DEFAULT 'PENDIENTE', -- PENDIENTE, PAGADO_PARCIAL, PAGADO
    fecha_vencimiento   DATE,
    fecha_pago          TIMESTAMP,
    metodo_pago         VARCHAR(30),           -- EFECTIVO, TRANSFERENCIA, YAPE, PLIN, CHEQUE
    pagado_por          UUID REFERENCES users(id),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cxp_tipo_ref CHECK (tipo_referencia IN ('COMPRA', 'PLANILLA', 'SERVICIO', 'OTRO')),
    CONSTRAINT chk_cxp_monto    CHECK (monto_pagado <= monto_total)
);

-- ================================================================
-- MÓDULO 07: CRM — CLIENTES Y COTIZACIONES
-- ================================================================
CREATE TABLE clientes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_documento      VARCHAR(10) DEFAULT 'DNI', -- DNI, RUC, CE, PASAPORTE
    numero_documento    VARCHAR(20) UNIQUE,
    nombre              VARCHAR(150) NOT NULL,
    email               VARCHAR(150),
    celular             VARCHAR(15),
    direccion           VARCHAR(255),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Flujo: Cotización → (aceptada) → Venta
CREATE TABLE cotizaciones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id         UUID NOT NULL REFERENCES sedes(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    cliente_id      UUID REFERENCES clientes(id),
    estado          VARCHAR(30) DEFAULT 'EMITIDA', -- EMITIDA, ACEPTADA, RECHAZADA, VENCIDA, CONVERTIDA
    total           DECIMAL(12,2) DEFAULT 0,
    fecha_vigencia  DATE NOT NULL,
    observaciones   TEXT,
    venta_id        UUID,                          -- Se llena al convertir (FK circular resuelta abajo)
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cotizacion_estado CHECK (estado IN (
        'EMITIDA','ACEPTADA','RECHAZADA','VENCIDA','CONVERTIDA'
    )),
    CONSTRAINT fk_cotizacion_venta
    FOREIGN KEY (venta_id) REFERENCES ventas(id)
);

CREATE TABLE detalle_cotizacion (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cotizacion_id   UUID NOT NULL REFERENCES cotizaciones(id) ON DELETE CASCADE,
    producto_id     UUID REFERENCES productos(id),
    fraccion_id     UUID REFERENCES catalogo_fracciones(id),
    servicio_id     UUID REFERENCES servicios(id),
    cantidad        DECIMAL(12,4) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_item_cotizacion CHECK (
        producto_id IS NOT NULL OR fraccion_id IS NOT NULL OR servicio_id IS NOT NULL
    )
);

-- ================================================================
-- MÓDULO 08: VENTAS
-- ================================================================
CREATE TABLE ventas (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id                     UUID NOT NULL REFERENCES sedes(id),
    user_id                     UUID NOT NULL REFERENCES users(id),
    cliente_id                  UUID REFERENCES clientes(id),
    cotizacion_id               UUID REFERENCES cotizaciones(id),
    preforma_id                 UUID,                               -- FK a preformas_venta (circular resuelta abajo)
    turno_caja_id               UUID,                               -- FK a turnos_caja (circular resuelta abajo)
    tipo_comprobante            VARCHAR(20) NOT NULL DEFAULT 'BOLETA', -- BOLETA, FACTURA, TICKET
    serie_correlativo           VARCHAR(50) UNIQUE,                 -- Ej: B001-00001
    -- PREFORMA: reservado, sin movimiento de stock ni comprobante
    -- COMPLETADA: stock descontado, comprobante emitido
    -- ANULADA: se revierte por devolución aprobada (ver tabla devoluciones)
    estado                      VARCHAR(30) DEFAULT 'COMPLETADA',
    total                       DECIMAL(12,2) DEFAULT 0,
    canal_venta                 VARCHAR(10) DEFAULT 'LOCAL',         -- LOCAL, ONLINE
    estado_sunat                VARCHAR(30) DEFAULT 'PENDIENTE',     -- PENDIENTE, ACEPTADO, RECHAZADO, ANULADO
    hash_cpe                    VARCHAR(255),
    mensaje_sunat               TEXT,
    referencia_pedido_online    VARCHAR(100) UNIQUE,
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_estado_venta    CHECK (estado IN ('COMPLETADA','ANULADA')),
    CONSTRAINT chk_canal_venta     CHECK (canal_venta IN ('LOCAL', 'ONLINE')),
    CONSTRAINT chk_tipo_comprobante CHECK (tipo_comprobante IN ('BOLETA', 'FACTURA', 'TICKET'));
    CONSTRAINT fk_venta_preforma FOREIGN KEY (preforma_id) REFERENCES preformas_venta(id)
);

CREATE TABLE detalle_venta (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id        UUID NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    producto_id     UUID REFERENCES productos(id),
    fraccion_id     UUID REFERENCES catalogo_fracciones(id),
    servicio_id     UUID REFERENCES servicios(id),
    cantidad        DECIMAL(12,4) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_item_venta CHECK (
        ((producto_id IS NOT NULL)::INT) +
        ((fraccion_id IS NOT NULL)::INT) +
        ((servicio_id IS NOT NULL)::INT) = 1
    )
);

CREATE TABLE pagos_venta (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id        UUID NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    metodo_pago     VARCHAR(30) NOT NULL,
    monto           DECIMAL(12,2) NOT NULL,
    referencia      VARCHAR(100),               -- Código de operación Yape/Plin/Niubiz
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_metodo_pago CHECK (metodo_pago IN (
        'EFECTIVO','YAPE','PLIN', 'SIP/AGORA','TARJETA','TRANSFERENCIA'
    ))
);


-- ================================================================
-- MÓDULO 08-B: PREFORMAS DE VENTA
-- ================================================================
-- Una preforma es un borrador de venta que el cliente puede revisar antes de confirmar.
-- Al confirmarla se convierte en una venta real (stock se descuenta en ese momento).
-- Casos de uso:
--   - Cliente indeciso que quiere ver el total antes de pagar
--   - Venta que requiere aprobación de precio o descuento
--   - Reserva sin pago inmediato (Ej: cliente viene a recoger mañana)
--
-- La preforma NO descuenta stock. Solo la conversión a venta lo hace.
-- El ROLE_ADMIN puede configurar si las preformas tienen tiempo de expiración.
CREATE TABLE preformas_venta (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id         UUID NOT NULL REFERENCES sedes(id),
    user_id         UUID NOT NULL REFERENCES users(id),     -- Quien la creó
    cliente_id      UUID REFERENCES clientes(id),
    cotizacion_id   UUID REFERENCES cotizaciones(id),       -- Si proviene de una cotización
    turno_caja_id   UUID,                                   -- FK circular resuelta abajo
    total_estimado  DECIMAL(12,2) DEFAULT 0,
    -- ABIERTA:    creada, pendiente de confirmación del cliente
    -- CONFIRMADA: el cliente aceptó → se convirtió en venta
    -- CANCELADA:  el cliente rechazó o cambió de productos (no afecta inventario)
    -- EXPIRADA:   superó la fecha límite sin confirmar
    estado          VARCHAR(20) DEFAULT 'ABIERTA',
    fecha_expiracion TIMESTAMP,                             -- Opcional: cuándo vence la reserva
    observaciones   TEXT,
    venta_id        UUID REFERENCES ventas(id),             -- Se llena al confirmar
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_preforma_estado CHECK (estado IN (
        'ABIERTA','CONFIRMADA','CANCELADA','EXPIRADA'
    ))
);

CREATE TABLE detalle_preforma (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    preforma_id     UUID NOT NULL REFERENCES preformas_venta(id) ON DELETE CASCADE,
    producto_id     UUID REFERENCES productos(id),
    fraccion_id     UUID REFERENCES catalogo_fracciones(id),
    servicio_id     UUID REFERENCES servicios(id),
    cantidad        DECIMAL(12,4) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    -- El cliente puede cambiar producto antes de confirmar (campo libre para nota)
    nota_cambio     TEXT,
    CONSTRAINT chk_item_preforma CHECK (
        ((producto_id IS NOT NULL)::INT) +
        ((fraccion_id IS NOT NULL)::INT) +
        ((servicio_id IS NOT NULL)::INT) = 1
    )
);


-- ================================================================
-- MÓDULO 08-C: DEVOLUCIONES Y REEMBOLSOS
-- ================================================================
-- Política de negocio POLICALUM:
--   1. Solo se acepta devolución si el producto está en buen estado (BUENO).
--   2. El ROLE_ADMIN es quien aprueba o rechaza la solicitud.
--   3. Al aprobar: se anula la venta, se genera movimiento ENTRADA en kardex
--      y se registra un reembolso (dinero devuelto al cliente).
--   4. No se pueden devolver servicios (cortes ya ejecutados).
--   5. El reembolso puede ser total o parcial según lo devuelto.

CREATE TABLE devoluciones (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id            UUID NOT NULL REFERENCES ventas(id),
    sede_id             UUID NOT NULL REFERENCES sedes(id),
    solicitado_por      UUID NOT NULL REFERENCES users(id),     -- Quien registró la solicitud (MULTIFUNCIONAL)
    aprobado_por        UUID REFERENCES users(id),              -- Solo ROLE_ADMIN
    cliente_id          UUID REFERENCES clientes(id),
    motivo              TEXT NOT NULL,                          -- Descripción del motivo de devolución
    -- PENDIENTE:  solicitud registrada, esperando revisión del ADMIN
    -- APROBADA:   ADMIN aprobó → se dispara anulación de venta + kardex + reembolso
    -- RECHAZADA:  ADMIN rechazó (producto dañado, plazo vencido, etc.)
    estado              VARCHAR(20) DEFAULT 'PENDIENTE',
    fecha_solicitud     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion    TIMESTAMP,
    observacion_admin   TEXT,                                   -- Nota del ADMIN al resolver
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_devolucion_estado CHECK (estado IN (
        'PENDIENTE','APROBADA','RECHAZADA'
    ))
);

-- Ítems específicos que el cliente quiere devolver (puede ser parcial)
CREATE TABLE detalle_devolucion (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    devolucion_id           UUID NOT NULL REFERENCES devoluciones(id) ON DELETE CASCADE,
    detalle_venta_id        UUID NOT NULL REFERENCES detalle_venta(id), -- Ítem original vendido
    producto_id             UUID REFERENCES productos(id),
    fraccion_id             UUID REFERENCES catalogo_fracciones(id),
    cantidad_devuelta       DECIMAL(12,4) NOT NULL,
    precio_unitario_ref     DECIMAL(12,2) NOT NULL,             -- Precio al que se vendió (referencia)
    subtotal_a_reembolsar   DECIMAL(12,2) NOT NULL,
    -- Estado físico del producto al momento de la devolución
    -- Solo los productos en estado BUENO regresan al stock
    -- DAÑADO/USADO → no vuelven al stock, se registra como MERMA en kardex
    estado_producto         VARCHAR(20) NOT NULL DEFAULT 'BUENO',
    observacion             TEXT,
    CONSTRAINT chk_estado_producto CHECK (estado_producto IN ('BUENO','DAÑADO','USADO'))
);

-- Registro del dinero devuelto al cliente tras devolución aprobada
CREATE TABLE reembolsos (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    devolucion_id       UUID NOT NULL REFERENCES devoluciones(id),
    venta_id            UUID NOT NULL REFERENCES ventas(id),
    cliente_id          UUID REFERENCES clientes(id),
    monto_total_reembolso DECIMAL(12,2) NOT NULL CHECK (monto_total_reembolso > 0),
    -- El reembolso se realiza idealmente por el mismo medio que el pago original
    metodo_reembolso    VARCHAR(30) NOT NULL,
    referencia          VARCHAR(100),                           -- Código de operación si aplica
    procesado_por       UUID NOT NULL REFERENCES users(id),     -- ROLE_ADMIN que ejecutó el reembolso
    -- PENDIENTE:  aprobada la devolución, pero aún no se entregó el dinero
    -- PROCESADO:  dinero devuelto al cliente
    estado              VARCHAR(20) DEFAULT 'PENDIENTE',
    fecha_procesado     TIMESTAMP,
    observaciones       TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_metodo_reembolso CHECK (metodo_reembolso IN (
        'EFECTIVO','YAPE','PLIN','TARJETA','TRANSFERENCIA'
    )),
    CONSTRAINT chk_reembolso_estado CHECK (estado IN ('PENDIENTE','PROCESADO'))
);

-- ================================================================
-- MÓDULO 09: TESORERÍA Y CAJA
-- ================================================================
-- El cuadre real se obtiene dinámicamente:
--   SELECT metodo_pago, SUM(monto)
--   FROM pagos_venta
--   WHERE venta_id IN (SELECT id FROM ventas WHERE turno_caja_id = :turno_id AND estado = 'COMPLETADA')
--   GROUP BY metodo_pago;
CREATE TABLE turnos_caja (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id                 UUID NOT NULL REFERENCES sedes(id),
    user_id                 UUID NOT NULL REFERENCES users(id),
    monto_inicial           DECIMAL(12,2) NOT NULL,
    monto_final_declarado   DECIMAL(12,2),
    fecha_apertura          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre            TIMESTAMP,
    estado                  VARCHAR(20) DEFAULT 'ABIERTO',      -- ABIERTO, CERRADO
    observaciones           TEXT,
    CONSTRAINT chk_turno_estado CHECK (estado IN ('ABIERTO', 'CERRADO'))
);

-- FK circulares resueltas: ventas y preformas → turnos_caja
ALTER TABLE ventas ADD CONSTRAINT fk_venta_turno_caja
    FOREIGN KEY (turno_caja_id) REFERENCES turnos_caja(id);

ALTER TABLE preformas_venta ADD CONSTRAINT fk_preforma_turno_caja
    FOREIGN KEY (turno_caja_id) REFERENCES turnos_caja(id);

-- FK circular: rrhh_asistencia → turnos_caja
-- Al abrir el turno_caja, el backend crea/actualiza el registro de asistencia automáticamente.
-- No hay intervención manual del empleado.
ALTER TABLE rrhh_asistencia ADD CONSTRAINT fk_asistencia_turno_caja
    FOREIGN KEY (turno_caja_id) REFERENCES turnos_caja(id);

-- Solo el ROLE_ADMIN accede a este módulo (Flujo de Patrimonio, no OPEX)
CREATE TABLE finanzas_retiros_socio (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id        UUID NOT NULL REFERENCES users(id),
    sede_id         UUID NOT NULL REFERENCES sedes(id),
    turno_caja_id   UUID REFERENCES turnos_caja(id),
    monto_retirado  DECIMAL(12,2) NOT NULL CHECK (monto_retirado > 0),
    origen_fondos   VARCHAR(30) NOT NULL,                       -- CAJA_FISICA, CUENTA_BANCARIA
    descripcion     TEXT,
    fecha_retiro    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_origen_fondos CHECK (origen_fondos IN ('CAJA_FISICA', 'CUENTA_BANCARIA'))
);

-- ================================================================
-- MÓDULO 10: LOGÍSTICA — DELIVERY Y MOVILIDADES
-- ================================================================
CREATE TABLE movilidades (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id         UUID NOT NULL REFERENCES sedes(id),
    placa           VARCHAR(10) UNIQUE NOT NULL,
    tipo            VARCHAR(30) NOT NULL,   -- MOTO, CAMIONETA, CAMION, AUTO
    marca           VARCHAR(50),
    modelo          VARCHAR(50),
    conductor_id    UUID REFERENCES users(id),
    activo          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ordenes_despacho (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id                UUID NOT NULL REFERENCES ventas(id),
    sede_id                 UUID NOT NULL REFERENCES sedes(id),
    responsable_id          UUID REFERENCES users(id),
    movilidad_id            UUID REFERENCES movilidades(id),
    direccion_entrega       VARCHAR(255) NOT NULL,
    referencia_entrega      VARCHAR(255),
    estado                  VARCHAR(30) DEFAULT 'PENDIENTE',    -- PENDIENTE, EN_CAMINO, ENTREGADO, FALLIDO
    fecha_programada        DATE,
    fecha_entrega           TIMESTAMP,
    observaciones           TEXT,
    url_evidencia_entrega   VARCHAR(255),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_estado_despacho CHECK (estado IN (
        'PENDIENTE','EN_CAMINO','ENTREGADO','FALLIDO'
    ))
);

-- ================================================================
-- MÓDULO 11: AUDITORÍA DE ACCIONES CRÍTICAS
-- ================================================================
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id),
    tabla           VARCHAR(100) NOT NULL,
    registro_id     UUID NOT NULL,
    accion          VARCHAR(20) NOT NULL,   -- INSERT, UPDATE, DELETE, LOGIN, LOGOUT, ACCESS
    datos_json      JSONB,                  -- Snapshot del estado anterior y nuevo
    user_agent      TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_audit_accion CHECK (accion IN (
        'INSERT','UPDATE','DELETE','LOGIN','LOGOUT','ACCESS'
    ))
);

-- ================================================================
-- ÍNDICES DE RENDIMIENTO
-- ================================================================
-- Seguridad / Usuarios
CREATE INDEX idx_users_role         ON users(role_id);
CREATE INDEX idx_users_sede         ON users(sede_id);
CREATE INDEX idx_users_activo       ON users(activo);

-- Productos / Catálogo
CREATE INDEX idx_productos_sku      ON productos(codigo_sku);
CREATE INDEX idx_productos_tipo     ON productos(tipo);

-- Stock
CREATE INDEX idx_stock_prod_almacen ON stock(producto_id, almacen_id);
CREATE INDEX idx_stock_almacen      ON stock(almacen_id);

-- Ventas
CREATE INDEX idx_ventas_canal       ON ventas(canal_venta);
CREATE INDEX idx_ventas_fecha       ON ventas(created_at);
CREATE INDEX idx_ventas_sede        ON ventas(sede_id);
CREATE INDEX idx_ventas_turno       ON ventas(turno_caja_id);
CREATE INDEX idx_ventas_cotizacion  ON ventas(cotizacion_id);
CREATE INDEX idx_ventas_estado      ON ventas(estado);

-- Preformas
CREATE INDEX idx_preforma_estado    ON preformas_venta(estado);
CREATE INDEX idx_preforma_sede      ON preformas_venta(sede_id);
CREATE INDEX idx_preforma_turno     ON preformas_venta(turno_caja_id);

-- Devoluciones
CREATE INDEX idx_devolucion_venta   ON devoluciones(venta_id);
CREATE INDEX idx_devolucion_estado  ON devoluciones(estado);
CREATE INDEX idx_devolucion_sede    ON devoluciones(sede_id);

-- Reembolsos
CREATE INDEX idx_reembolso_devolucion ON reembolsos(devolucion_id);
CREATE INDEX idx_reembolso_estado     ON reembolsos(estado);

-- Pagos
CREATE INDEX idx_pagos_venta        ON pagos_venta(venta_id);
CREATE INDEX idx_pagos_metodo       ON pagos_venta(metodo_pago);

-- Kardex
CREATE INDEX idx_kardex_producto    ON kardex_movimientos(producto_id);
CREATE INDEX idx_kardex_fecha       ON kardex_movimientos(created_at);
CREATE INDEX idx_kardex_referencia  ON kardex_movimientos(referencia_id);
CREATE INDEX idx_kardex_tipo_ref    ON kardex_movimientos(tipo_referencia);

-- Compras
CREATE INDEX idx_compras_proveedor  ON compras(proveedor_id);
CREATE INDEX idx_compras_estado     ON compras(estado_recepcion, estado_pago);

-- CxP
CREATE INDEX idx_cxp_estado         ON finanzas_cuentas_por_pagar(estado);
CREATE INDEX idx_cxp_vencimiento    ON finanzas_cuentas_por_pagar(fecha_vencimiento);

-- RRHH
CREATE INDEX idx_asistencia_fecha   ON rrhh_asistencia(fecha);
CREATE INDEX idx_asistencia_user    ON rrhh_asistencia(user_id);
CREATE INDEX idx_nomina_periodo     ON rrhh_nominas(periodo);

-- Cotizaciones
CREATE INDEX idx_cotizacion_estado  ON cotizaciones(estado);
CREATE INDEX idx_cotizacion_vigencia ON cotizaciones(fecha_vigencia);

-- Despachos
CREATE INDEX idx_despacho_estado    ON ordenes_despacho(estado);
CREATE INDEX idx_despacho_venta     ON ordenes_despacho(venta_id);

-- Auditoría
CREATE INDEX idx_audit_tabla        ON audit_log(tabla);
CREATE INDEX idx_audit_fecha        ON audit_log(created_at);
CREATE INDEX idx_audit_user         ON audit_log(user_id);

-- ================================================================
-- DATOS SEMILLA: ROLES BASE DEL SISTEMA
-- ================================================================
INSERT INTO roles (nombre, permisos_json) VALUES
(
    'ROLE_ADMIN',
    '{
        "modulos": [
            "SEGURIDAD","SEDES","CATALOGO","INVENTARIO","COMPRAS",
            "VENTAS","PREFORMAS","DEVOLUCIONES","REEMBOLSOS",
            "CAJA","RRHH","NOMINA","REPORTES","RETIROS"
        ],
        "puede_aprobar_nomina":      true,
        "puede_retirar_fondos":      true,
        "puede_anular_ventas":       true,
        "puede_aprobar_devolucion":  true,
        "puede_procesar_reembolso":  true
    }'
),
(
    'ROLE_MULTIFUNCIONAL',
    '{
        "modulos": [
            "VENTAS","PREFORMAS","CAJA","CRM",
            "INVENTARIO_BASICO","DELIVERY","DEVOLUCIONES_SOLICITUD"
        ],
        "puede_aprobar_nomina":      false,
        "puede_retirar_fondos":      false,
        "puede_anular_ventas":       false,
        "puede_aprobar_devolucion":  false,
        "puede_procesar_reembolso":  false
    }'
);

-- ================================================================
-- SEMILLA: FRACCIONES ESTÁNDAR
-- Solo aplica a productos con es_fraccionable = TRUE.
-- El backend debe verificar esta condición antes de insertar.
-- Productos NO fraccionables (pernos, pegamentos, marcos, perfiles, etc.)
-- NO tienen filas en esta tabla y se venden siempre en cantidad entera.
--
-- Ejemplo para una plancha de policarbonato o aluminio:
--   INSERT INTO catalogo_fracciones
--       (producto_id, nombre_fraccion, factor_descuento_kardex, precio_venta)
--   VALUES
--     (:pid, 'Plancha Completa',        1.0000, :precio_venta_base * 1.0000 * :margen),
--     (:pid, 'Tres Cuartos (3/4)',       0.7500, :precio_venta_base * 0.7500 * :margen),
--     (:pid, 'Media Plancha (1/2)',      0.5000, :precio_venta_base * 0.5000 * :margen),
--     (:pid, 'Cuarto de Plancha (1/4)', 0.2500, :precio_venta_base * 0.2500 * :margen);
--
-- Lógica de equivalencia en kardex (solo para fraccionables):
--   4 ventas de factor 0.25 = 1 unidad completa descontada del stock
--   2 ventas de factor 0.50 = 1 unidad completa descontada del stock
--   Las alertas de stock_minimo trabajan siempre en unidades completas.
--
-- Para productos NO fraccionables el POS vende directamente usando
-- precio_venta_base y descuenta cantidad entera del stock.
-- ================================================================

-- ================================================================
-- RESUMEN DE TABLAS (33 en total)
-- ================================================================
-- Módulo 01 - Sedes y Almacenes:        sedes, almacenes
-- Módulo 02 - Seguridad:                roles, users
-- Módulo 03 - RRHH:                     rrhh_legajo_digital, rrhh_turnos_trabajo,
--                                        rrhh_asistencia, rrhh_nominas
-- Módulo 04 - Catálogo:                 categorias, proveedores, productos,
--                                        catalogo_fracciones, servicios
-- Módulo 05 - Inventario:               stock, kardex_movimientos
-- Módulo 06 - Compras:                  compras, detalle_compra,
--                                        finanzas_cuentas_por_pagar
-- Módulo 07 - CRM:                      clientes, cotizaciones, detalle_cotizacion
-- Módulo 08 - Ventas:                   ventas, detalle_venta, pagos_venta
-- Módulo 08B - Preformas:               preformas_venta, detalle_preforma
-- Módulo 08C - Devoluciones:            devoluciones, detalle_devolucion, reembolsos
-- Módulo 09 - Tesorería:                turnos_caja, finanzas_retiros_socio
-- Módulo 10 - Logística:                movilidades, ordenes_despacho
-- Módulo 11 - Auditoría:                audit_log
-- ================================================================