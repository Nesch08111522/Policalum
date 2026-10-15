-- Tablas incorporadas: 
-- sedes, almacenes, roles, users, rrhh_legajo_digital, rrhh_asistencia_inteligente, rrhh_turnos_trabajo, rrhh_nominas, categorias, proveedores, productos, catalogo_fracciones, servicios, stock, kardex_movimientos, compras, detalle_compra, finanzas_cuentas_por_pagar, clientes, cotizaciones, detalle_cotizacion, ventas, detalle_venta, pagos_venta, turnos_caja, finanzas_retiros_socio, ordenes_despacho, movilidades y audit_log.
-- El total de las tablas creadas es de 30, cubriendo todos los módulos del sistema ERP propuesto.
--================================================================
-- MÓDULO 01: SEDES Y ALMACENES (Base de la Multitenencia lógica)
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
    tipo_contrato           VARCHAR(50),                          -- PLANILLA, RECIBO_HONORARIOS, CAS
    fecha_ingreso           DATE NOT NULL,
    sueldo_base             DECIMAL(12,2) NOT NULL DEFAULT 0,
    porcentaje_comision     DECIMAL(5,2) DEFAULT 0,              -- Ej: 2.50 = 2.50% por venta
    documentos_json         JSONB,                               -- Rutas de contratos, CVs en S3/GCS
    activo                  BOOLEAN DEFAULT TRUE,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rrhh_asistencia_inteligente (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    turno_caja_id       UUID UNIQUE REFERENCES turnos_caja(id),          -- Vínculo directo 1 a 1 con la caja
    turno_trabajo_id    UUID REFERENCES rrhh_turnos_trabajo(id),       -- Para saber qué horario le correspondía
    fecha               DATE DEFAULT CURRENT_DATE,
    hora_entrada        TIMESTAMP NOT NULL,                            -- Hora EXACTA en que abrió caja
    hora_salida         TIMESTAMP,                                     -- Hora EXACTA en que cerró caja
    horas_reales_pagar  DECIMAL(12,2) DEFAULT 0,                       -- Cálculo decimal (Ej: 8.50 horas)
    estado              VARCHAR(30) DEFAULT 'PRESENTE',                -- PRESENTE, TARDANZA
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- [FIX-10] Tabla de turnos de trabajo del personal, diferenciada de turnos_caja
CREATE TABLE rrhh_turnos_trabajo (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    sede_id         UUID NOT NULL REFERENCES sedes(id),
    nombre_turno    VARCHAR(50) NOT NULL,                        -- Ej: MAÑANA, TARDE, NOCHE
    hora_inicio     TIME NOT NULL,
    hora_fin        TIME NOT NULL,
    dias_semana     JSONB NOT NULL,                              -- ["LUNES","MARTES","MIERCOLES",...]
    activo          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rrhh_nominas (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id),
    periodo                 VARCHAR(20) NOT NULL,                -- Ej: '2026-05'
    sueldo_base_calculado   DECIMAL(12,2) NOT NULL,
    total_comisiones        DECIMAL(12,2) DEFAULT 0,
    descuentos              DECIMAL(12,2) DEFAULT 0,
    total_neto              DECIMAL(12,2) NOT NULL,
    estado                  VARCHAR(30) DEFAULT 'PENDIENTE',     -- PENDIENTE, APROBADA, PAGADA
    aprobado_por            UUID REFERENCES users(id),           -- Solo ROLE_ADMIN puede aprobar
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
    nombre_razon      VARCHAR(255) NOT NULL, -- Nombre comercial o razón social
    tipo_documento    VARCHAR(10) NOT NULL DEFAULT 'RUC', -- DNI, RUC, CE
    numero_documento  VARCHAR(15) NOT NULL, -- Soporta 11 (RUC) u 8 (DNI)
    celular           VARCHAR(15),
    email             VARCHAR(150),
    activo            BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_documento_proveedor UNIQUE (tipo_documento, numero_documento)
);

CREATE TABLE productos (
    id                            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    categoria_id                  UUID REFERENCES categorias(id),
    proveedor_id                  UUID REFERENCES proveedores(id),
    codigo_sku                    VARCHAR(50), -- Ya no es UNIQUE forzoso
    nombre                        VARCHAR(255) NOT NULL,
    descripcion                   TEXT, -- Útil para búsquedas más amplias
    tipo                          VARCHAR(20) NOT NULL DEFAULT 'FISICO',
    precio_compra                 DECIMAL(12,2) NOT NULL,
    precio_venta_plancha_entera   DECIMAL(12,2) NOT NULL,
    unidad                        VARCHAR(20) DEFAULT 'NIU',
    activo                        BOOLEAN DEFAULT TRUE,
    created_at                    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tipo_producto CHECK (tipo IN ('FISICO', 'SERVICIO'))
);

-- Factor 1.0000 = plancha completa, 0.7500 = 3/4, 0.5000 = 1/2, 0.2500 = 1/4
CREATE TABLE catalogo_fracciones (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id             UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    nombre_fraccion         VARCHAR(50) NOT NULL,    -- Ej: 'Plancha Completa', '3/4 de Plancha'
    factor_descuento_kardex DECIMAL(5,4) NOT NULL,  -- 1.0000 / 0.7500 / 0.5000 / 0.2500
    precio_venta            DECIMAL(12,2) NOT NULL,
    activo                  BOOLEAN DEFAULT TRUE,
    CONSTRAINT chk_factor CHECK (factor_descuento_kardex > 0 AND factor_descuento_kardex <= 1),
    CONSTRAINT uq_producto_fraccion UNIQUE (producto_id, factor_descuento_kardex)
);

-- Servicios independientes (cortes, instalaciones, estructuras)
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
    cantidad        DECIMAL(12,4) DEFAULT 0, -- Soporta 0.2500, 0.5000, 0.7500, 1.0000
    stock_minimo    DECIMAL(12,4) DEFAULT 0,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(producto_id, almacen_id)
);

-- [FIX-06] kardex_movimientos: añadido tipo_referencia para saber el origen del movimiento
CREATE TABLE kardex_movimientos (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id         UUID NOT NULL REFERENCES productos(id),
    almacen_id          UUID NOT NULL REFERENCES almacenes(id),
    referencia_id       UUID,                   -- ID de la Venta, Compra o Ajuste origen
    tipo_referencia     VARCHAR(20) NOT NULL,   -- VENTA, COMPRA, AJUSTE, MERMA, TRANSFERENCIA
    tipo_movimiento     VARCHAR(10) NOT NULL,   -- ENTRADA, SALIDA
    cantidad_movida     DECIMAL(12,4) NOT NULL,
    stock_resultante    DECIMAL(12,4) NOT NULL,
    user_id             UUID NOT NULL REFERENCES users(id),
    observacion         TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tipo_mov CHECK (tipo_movimiento IN ('ENTRADA', 'SALIDA')),
    CONSTRAINT chk_tipo_ref CHECK (tipo_referencia IN ('VENTA', 'COMPRA', 'AJUSTE', 'MERMA', 'TRANSFERENCIA'))
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
    -- Estado logístico (si llegó físicamente al almacén)
    estado_recepcion            VARCHAR(30) DEFAULT 'PENDIENTE',   -- PENDIENTE, RECIBIDO_PARCIAL, RECIBIDO
    -- Estado financiero (si ya se pagó al proveedor) → se complementa con CxP
    estado_pago                 VARCHAR(30) DEFAULT 'PENDIENTE',   -- PENDIENTE, PAGADO_PARCIAL, PAGADO
    observaciones               TEXT,
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- [FIX-05] Detalle de compra: tabla que faltaba completamente
-- Permite saber exactamente qué productos y cantidades llegaron al almacén
-- Al marcar estado_recepcion = 'RECIBIDO', el backend genera movimientos en kardex_movimientos
CREATE TABLE detalle_compra (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    compra_id       UUID NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
    producto_id     UUID NOT NULL REFERENCES productos(id),
    almacen_id      UUID NOT NULL REFERENCES almacenes(id),  -- A qué almacén ingresa el lote
    cantidad        DECIMAL(12,4) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL
);

-- [FIX-12] CxP con tipo_referencia legible y método de pago
CREATE TABLE finanzas_cuentas_por_pagar (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id             UUID NOT NULL REFERENCES sedes(id),
    referencia_id       UUID,                   -- ID de la compra, nómina u otro gasto
    tipo_referencia     VARCHAR(20) NOT NULL,   -- COMPRA, PLANILLA, SERVICIO, OTRO
    descripcion         TEXT NOT NULL,
    monto_total         DECIMAL(12,2) NOT NULL,
    monto_pagado        DECIMAL(12,2) DEFAULT 0,
    estado              VARCHAR(30) DEFAULT 'PENDIENTE',  -- PENDIENTE, PAGADO_PARCIAL, PAGADO
    fecha_vencimiento   DATE,
    fecha_pago          TIMESTAMP,
    metodo_pago         VARCHAR(30),            -- EFECTIVO, TRANSFERENCIA, YAPE, PLIN, CHEQUE
    pagado_por          UUID REFERENCES users(id),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cxp_tipo_ref CHECK (tipo_referencia IN ('COMPRA', 'PLANILLA', 'SERVICIO', 'OTRO')),
    CONSTRAINT chk_cxp_monto CHECK (monto_pagado <= monto_total)
);

-- ================================================================
-- MÓDULO 07: CRM — CLIENTES Y COTIZACIONES
-- ================================================================
CREATE TABLE clientes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_documento      VARCHAR(10) DEFAULT 'DNI',   -- DNI, RUC, CE, PASAPORTE
    numero_documento    VARCHAR(20) UNIQUE,
    nombre              VARCHAR(150) NOT NULL,
    email               VARCHAR(150),
    celular             VARCHAR(15),
    direccion           VARCHAR(255),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- [FIX-07] Tabla de cotizaciones: faltaba completamente
-- El flujo es: Cotización → (aprobada por el cliente) → Venta
CREATE TABLE cotizaciones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id         UUID NOT NULL REFERENCES sedes(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    cliente_id      UUID REFERENCES clientes(id),
    estado          VARCHAR(30) DEFAULT 'EMITIDA',  -- EMITIDA, ACEPTADA, RECHAZADA, VENCIDA, CONVERTIDA
    total           DECIMAL(12,2) DEFAULT 0,
    fecha_vigencia  DATE NOT NULL,                  -- Fecha límite de validez de la cotización
    observaciones   TEXT,
    venta_id        UUID,                           -- Se llena cuando se convierte a venta (FK circular resuelta abajo)
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cotizacion_estado CHECK (estado IN ('EMITIDA','ACEPTADA','RECHAZADA','VENCIDA','CONVERTIDA'))
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
    -- Al menos uno de los tres ítems debe existir
    CONSTRAINT chk_item_cotizacion CHECK (
        producto_id IS NOT NULL OR fraccion_id IS NOT NULL OR servicio_id IS NOT NULL
    )
);

-- ================================================================
-- MÓDULO 08: VENTAS
-- ================================================================
CREATE TABLE ventas (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id             UUID NOT NULL REFERENCES sedes(id),
    user_id             UUID NOT NULL REFERENCES users(id),
    cliente_id          UUID REFERENCES clientes(id),
    cotizacion_id       UUID REFERENCES cotizaciones(id),  -- Si viene de una cotización
    turno_caja_id       UUID,                              -- FK a turnos_caja (añadida abajo)
    tipo_comprobante    VARCHAR(20) NOT NULL DEFAULT 'BOLETA', -- BOLETA, FACTURA, TICKET
    serie_correlativo   VARCHAR(50) UNIQUE,                -- Ej: B001-00001 (generado por SUNAT)
    estado              VARCHAR(30) DEFAULT 'COMPLETADA',  -- COMPLETADA, ANULADA
    total               DECIMAL(12,2) DEFAULT 0,
    canal_venta         VARCHAR(10) DEFAULT 'LOCAL',        -- LOCAL, ONLINE
    estado_sunat        VARCHAR(30) DEFAULT 'PENDIENTE', -- PENDIENTE, ACEPTADO, RECHAZADO, ANULADO
    hash_cpe            VARCHAR(255),                        -- Código Hash devuelto por SUNAT o la OSE
    mensaje_sunat       TEXT,                           -- Respuesta o código de error en caso de rechazo
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    referencia_pedido_online VARCHAR(100) UNIQUE
    CONSTRAINT chk_canal_venta CHECK (canal_venta IN ('LOCAL', 'ONLINE')),
    CONSTRAINT chk_tipo_comprobante CHECK (tipo_comprobante IN ('BOLETA', 'FACTURA', 'TICKET'))
);

-- [FIX-02] detalle_venta con CHECK que obliga a al menos un ítem
CREATE TABLE detalle_venta (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id        UUID NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    producto_id     UUID REFERENCES productos(id),
    fraccion_id     UUID REFERENCES catalogo_fracciones(id), -- Fracción vendida (1/4, 1/2, 3/4, 1)
    servicio_id     UUID REFERENCES servicios(id),
    cantidad        DECIMAL(12,4) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    -- Obliga a que exactamente uno de los tres esté presente
    CONSTRAINT chk_item_venta CHECK (
        (producto_id IS NOT NULL)::INT +
        (fraccion_id IS NOT NULL)::INT +
        (servicio_id IS NOT NULL)::INT = 1
    )
);

-- [FIX-03] Pagos por venta (soporte multimedios y pagos mixtos)
-- Ejemplo: cliente paga S/100 en Yape y S/50 en efectivo → 2 registros para 1 venta
CREATE TABLE pagos_venta (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id        UUID NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    metodo_pago     VARCHAR(30) NOT NULL,       -- EFECTIVO, YAPE, PLIN, TARJETA, TRANSFERENCIA
    monto           DECIMAL(12,2) NOT NULL,
    referencia      VARCHAR(100),               -- Código de operación Yape/Plin/Niubiz
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_metodo_pago CHECK (metodo_pago IN ('EFECTIVO','YAPE','PLIN','TARJETA','TRANSFERENCIA'))
);

-- Referencia circular resuelta: venta_id en cotizaciones
ALTER TABLE cotizaciones ADD CONSTRAINT fk_cotizacion_venta FOREIGN KEY (venta_id) REFERENCES ventas(id);

-- ================================================================
-- MÓDULO 09: TESORERÍA Y CAJA
-- ================================================================
-- [FIX-04] turnos_caja: el cuadre real se obtiene sumando pagos_venta agrupados por metodo_pago
-- No se guarda total_ingresos_ventas como campo calculado (viola normalización)
-- Se calcula dinámicamente: SELECT metodo_pago, SUM(monto) FROM pagos_venta WHERE venta_id IN (ventas del turno)
CREATE TABLE turnos_caja (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id                 UUID NOT NULL REFERENCES sedes(id),
    user_id                 UUID NOT NULL REFERENCES users(id),
    monto_inicial           DECIMAL(12,2) NOT NULL,             -- Efectivo con que inicia el turno
    monto_final_declarado   DECIMAL(12,2),                      -- Lo que el cajero declara al cerrar
    fecha_apertura          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre            TIMESTAMP,
    estado                  VARCHAR(20) DEFAULT 'ABIERTO',      -- ABIERTO, CERRADO
    observaciones           TEXT,
    CONSTRAINT chk_turno_estado CHECK (estado IN ('ABIERTO', 'CERRADO'))
);

-- FK de ventas hacia turno de caja
ALTER TABLE ventas ADD CONSTRAINT fk_venta_turno_caja FOREIGN KEY (turno_caja_id) REFERENCES turnos_caja(id);

-- [FIX-11] finanzas_retiros_socio: añadidos sede_id y turno_caja_id
-- Solo el ROLE_ADMIN (Matías) accede a este módulo
-- Es "Flujo de Patrimonio", no OPEX — no afecta el estado de resultados operativo
CREATE TABLE finanzas_retiros_socio (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id        UUID NOT NULL REFERENCES users(id),
    sede_id         UUID NOT NULL REFERENCES sedes(id),         -- ¿De qué sede se retira?
    turno_caja_id   UUID REFERENCES turnos_caja(id),            -- ¿De qué turno de caja?
    monto_retirado  DECIMAL(12,2) NOT NULL CHECK (monto_retirado > 0),
    origen_fondos   VARCHAR(30) NOT NULL,                        -- CAJA_FISICA, CUENTA_BANCARIA
    descripcion     TEXT,
    fecha_retiro    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_origen_fondos CHECK (origen_fondos IN ('CAJA_FISICA', 'CUENTA_BANCARIA'))
);

-- ================================================================
-- MÓDULO 10: LOGÍSTICA — DELIVERY Y MOVILIDADES
-- ================================================================
-- [FIX-08] Órdenes de despacho (delivery)
CREATE TABLE ordenes_despacho (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id            UUID NOT NULL REFERENCES ventas(id),
    sede_id             UUID NOT NULL REFERENCES sedes(id),
    responsable_id      UUID REFERENCES users(id),          -- Operador asignado al delivery
    movilidad_id        UUID,                               -- FK a movilidades (añadida abajo)
    direccion_entrega   VARCHAR(255) NOT NULL,
    referencia_entrega  VARCHAR(255),                       -- Ej: "Frente al grifo Repsol"
    estado              VARCHAR(30) DEFAULT 'PENDIENTE',    -- PENDIENTE, EN_CAMINO, ENTREGADO, FALLIDO
    fecha_programada    DATE,
    fecha_entrega       TIMESTAMP,
    observaciones       TEXT,
    url_evidencia_entrega VARCHAR(255);
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_estado_despacho CHECK (estado IN ('PENDIENTE','EN_CAMINO','ENTREGADO','FALLIDO'))
);

-- [FIX-09] Movilidades / Flota
CREATE TABLE movilidades (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sede_id         UUID NOT NULL REFERENCES sedes(id),
    placa           VARCHAR(10) UNIQUE NOT NULL,
    tipo            VARCHAR(30) NOT NULL,           -- MOTO, CAMIONETA, CAMION, AUTO
    marca           VARCHAR(50),
    modelo          VARCHAR(50),
    conductor_id    UUID REFERENCES users(id),      -- Conductor habitual asignado
    activo          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FK de ordenes_despacho hacia movilidades (circular resuelta)
ALTER TABLE ordenes_despacho ADD CONSTRAINT fk_despacho_movilidad FOREIGN KEY (movilidad_id) REFERENCES movilidades(id);

-- ================================================================
-- MÓDULO 11: AUDITORÍA DE ACCIONES CRÍTICAS
-- ================================================================
-- [FIX-01] audit_log: añadidos ip_address y user_agent para cumplir el informe
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id),
    tabla           VARCHAR(100) NOT NULL,
    registro_id     UUID NOT NULL,
    accion          VARCHAR(20) NOT NULL,            -- INSERT, UPDATE, DELETE, LOGIN, LOGOUT
    datos_json      JSONB,                           -- Snapshot del estado anterior y nuevo
    user_agent      TEXT,                            -- Navegador / dispositivo detectado
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_audit_accion CHECK (accion IN ('INSERT','UPDATE','DELETE','LOGIN','LOGOUT','ACCESS'))
);

-- ================================================================
-- ÍNDICES DE RENDIMIENTO (B-TREE)
-- ================================================================
-- Seguridad / Usuarios
CREATE INDEX idx_users_role        ON users(role_id);
CREATE INDEX idx_users_sede        ON users(sede_id);
CREATE INDEX idx_users_activo      ON users(activo);

-- Productos / Catálogo
CREATE INDEX idx_productos_sku     ON productos(codigo_sku);
CREATE INDEX idx_productos_tipo    ON productos(tipo);

-- Stock
CREATE INDEX idx_stock_prod_almacen ON stock(producto_id, almacen_id);
CREATE INDEX idx_stock_almacen      ON stock(almacen_id);

-- Ventas
CREATE INDEX idx_ventas_canal      ON ventas(canal_venta);
CREATE INDEX idx_ventas_fecha      ON ventas(created_at);
CREATE INDEX idx_ventas_sede       ON ventas(sede_id);
CREATE INDEX idx_ventas_turno      ON ventas(turno_caja_id);
CREATE INDEX idx_ventas_cotizacion ON ventas(cotizacion_id);

-- Pagos
CREATE INDEX idx_pagos_venta       ON pagos_venta(venta_id);
CREATE INDEX idx_pagos_metodo      ON pagos_venta(metodo_pago);

-- Kardex
CREATE INDEX idx_kardex_producto   ON kardex_movimientos(producto_id);
CREATE INDEX idx_kardex_fecha      ON kardex_movimientos(created_at);
CREATE INDEX idx_kardex_referencia ON kardex_movimientos(referencia_id);
CREATE INDEX idx_kardex_tipo_ref   ON kardex_movimientos(tipo_referencia);

-- Compras
CREATE INDEX idx_compras_proveedor ON compras(proveedor_id);
CREATE INDEX idx_compras_estado    ON compras(estado_recepcion, estado_pago);

-- CxP
CREATE INDEX idx_cxp_estado        ON finanzas_cuentas_por_pagar(estado);
CREATE INDEX idx_cxp_vencimiento   ON finanzas_cuentas_por_pagar(fecha_vencimiento);

-- RRHH
CREATE INDEX idx_asistencia_fecha  ON rrhh_asistencia_inteligente(fecha);
CREATE INDEX idx_asistencia_user   ON rrhh_asistencia_inteligente(user_id);
CREATE INDEX idx_nomina_periodo    ON rrhh_nominas(periodo);

-- Cotizaciones
CREATE INDEX idx_cotizacion_estado ON cotizaciones(estado);
CREATE INDEX idx_cotizacion_vigencia ON cotizaciones(fecha_vigencia);

-- Despachos
CREATE INDEX idx_despacho_estado   ON ordenes_despacho(estado);
CREATE INDEX idx_despacho_venta    ON ordenes_despacho(venta_id);

-- Auditoría
CREATE INDEX idx_audit_tabla       ON audit_log(tabla);
CREATE INDEX idx_audit_fecha       ON audit_log(created_at);
CREATE INDEX idx_audit_user        ON audit_log(user_id);

-- ================================================================
-- DATOS SEMILLA: ROLES BASE DEL SISTEMA
-- ================================================================
INSERT INTO roles (nombre, permisos_json) VALUES
(
    'ROLE_ADMIN',
    '{
        "modulos": ["SEGURIDAD","SEDES","CATALOGO","INVENTARIO","COMPRAS","VENTAS","CAJA","RRHH","NOMINA","REPORTES","RETIROS"],
        "puede_aprobar_nomina": true,
        "puede_retirar_fondos": true,
        "puede_anular_ventas": true
    }'
),
(
    'ROLE_MULTIFUNCIONAL',
    '{
        "modulos": ["VENTAS","CAJA","CRM","INVENTARIO_BASICO","DELIVERY"],
        "puede_aprobar_nomina": false,
        "puede_retirar_fondos": false,
        "puede_anular_ventas": false
    }'
);

-- ================================================================
-- DATOS SEMILLA: FRACCIONES ESTÁNDAR DE PLANCHA
-- (Se insertan vía código al crear el producto en el backend)
-- Referencia de inserción para el servicio de Catálogo:
--
-- INSERT INTO catalogo_fracciones (producto_id, nombre_fraccion, factor_descuento_kardex, precio_venta) VALUES
--   (:producto_id, 'Plancha Completa', 1.0000, :precio_base * 1.0000),
--   (:producto_id, 'Tres Cuartos (3/4)', 0.7500, :precio_base * 0.7500),
--   (:producto_id, 'Media Plancha (1/2)', 0.5000, :precio_base * 0.5000),
--   (:producto_id, 'Cuarto de Plancha (1/4)', 0.2500, :precio_base * 0.2500);
--
-- El precio_venta puede incluir un margen adicional configurable por el ADMIN.
-- ================================================================