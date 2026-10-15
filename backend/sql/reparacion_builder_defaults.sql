-- =====================================================================================
-- Reparación del bug "@Builder ignora los inicializadores de campo"
-- PostgreSQL. Idempotente: solo toca filas con NULL. Ejecutar primero el DIAGNÓSTICO.
-- (kardex_movimientos.created_at y audit_log.created_at ya se repararon en el M10/M13.)
-- =====================================================================================

-- ---------- 1) DIAGNÓSTICO: cuántas filas tienen NULL ----------
SELECT 'pagos_venta.created_at'              AS campo, COUNT(*) FROM pagos_venta                WHERE created_at IS NULL
UNION ALL SELECT 'productos.created_at',              COUNT(*) FROM productos                  WHERE created_at IS NULL
UNION ALL SELECT 'productos.unidad',                  COUNT(*) FROM productos                  WHERE unidad IS NULL
UNION ALL SELECT 'productos.incluye_igv',             COUNT(*) FROM productos                  WHERE incluye_igv IS NULL
UNION ALL SELECT 'productos.tipo_afectacion',         COUNT(*) FROM productos                  WHERE tipo_afectacion IS NULL
UNION ALL SELECT 'servicios.created_at',              COUNT(*) FROM servicios                  WHERE created_at IS NULL
UNION ALL SELECT 'movilidades.created_at',            COUNT(*) FROM movilidades                WHERE created_at IS NULL
UNION ALL SELECT 'clientes.tipo_documento',           COUNT(*) FROM clientes                   WHERE tipo_documento IS NULL
UNION ALL SELECT 'stock.cantidad',                    COUNT(*) FROM stock                      WHERE cantidad IS NULL
UNION ALL SELECT 'stock.stock_minimo',                COUNT(*) FROM stock                      WHERE stock_minimo IS NULL
UNION ALL SELECT 'stock.updated_at',                  COUNT(*) FROM stock                      WHERE updated_at IS NULL
UNION ALL SELECT 'users.totp_enabled',                COUNT(*) FROM users                      WHERE totp_enabled IS NULL
UNION ALL SELECT 'finanzas_cuentas_por_pagar.monto_pagado', COUNT(*) FROM finanzas_cuentas_por_pagar WHERE monto_pagado IS NULL
UNION ALL SELECT 'detalle_venta.porcentaje_igv',      COUNT(*) FROM detalle_venta              WHERE porcentaje_igv IS NULL
UNION ALL SELECT 'detalle_cotizacion.porcentaje_igv', COUNT(*) FROM detalle_cotizacion         WHERE porcentaje_igv IS NULL
UNION ALL SELECT 'detalle_compra.porcentaje_igv',     COUNT(*) FROM detalle_compra             WHERE porcentaje_igv IS NULL
UNION ALL SELECT 'sedes.pais',                        COUNT(*) FROM sedes                      WHERE pais IS NULL
UNION ALL SELECT 'sedes.moneda',                      COUNT(*) FROM sedes                      WHERE moneda IS NULL
UNION ALL SELECT 'ventas.canal_venta',                COUNT(*) FROM ventas                     WHERE canal_venta IS NULL
UNION ALL SELECT 'ventas.estado_sunat',               COUNT(*) FROM ventas                     WHERE estado_sunat IS NULL
-- Solo informativos (NO se reparan automáticamente, revisar a mano si salen > 0):
UNION ALL SELECT '[revisar] detalle_compra.cantidad_recibida', COUNT(*) FROM detalle_compra    WHERE cantidad_recibida IS NULL
UNION ALL SELECT '[revisar] cotizaciones.total',      COUNT(*) FROM cotizaciones               WHERE total IS NULL;

-- ---------- 2) REPARACIÓN ----------
BEGIN;

-- Fechas: se reconstruyen con el mejor dato disponible
UPDATE pagos_venta p SET created_at = v.created_at
  FROM ventas v WHERE p.venta_id = v.id AND p.created_at IS NULL AND v.created_at IS NOT NULL;

UPDATE productos p SET created_at = COALESCE(
    (SELECT MIN(k.created_at) FROM kardex_movimientos k WHERE k.producto_id = p.id), NOW())
  WHERE p.created_at IS NULL;

UPDATE servicios s SET created_at = COALESCE(
    (SELECT MIN(v.created_at) FROM detalle_venta d JOIN ventas v ON v.id = d.venta_id WHERE d.servicio_id = s.id), NOW())
  WHERE s.created_at IS NULL;

UPDATE movilidades SET created_at = NOW() WHERE created_at IS NULL;
UPDATE stock       SET updated_at = NOW() WHERE updated_at IS NULL;

-- Valores por defecto de la entidad
UPDATE productos SET unidad = 'UNID'        WHERE unidad IS NULL;
UPDATE productos SET incluye_igv = TRUE     WHERE incluye_igv IS NULL;
UPDATE productos SET tipo_afectacion = '10' WHERE tipo_afectacion IS NULL;
UPDATE clientes  SET tipo_documento = CASE WHEN numero_documento ~ '^[0-9]{11}$' THEN 'RUC' ELSE 'DNI' END
  WHERE tipo_documento IS NULL;
UPDATE stock SET cantidad = 0     WHERE cantidad IS NULL;
UPDATE stock SET stock_minimo = 0 WHERE stock_minimo IS NULL;
UPDATE users SET totp_enabled = FALSE WHERE totp_enabled IS NULL;
UPDATE finanzas_cuentas_por_pagar SET monto_pagado = 0 WHERE monto_pagado IS NULL;
UPDATE detalle_venta      SET porcentaje_igv = 18.00 WHERE porcentaje_igv IS NULL;
UPDATE detalle_cotizacion SET porcentaje_igv = 18.00 WHERE porcentaje_igv IS NULL;
UPDATE detalle_compra     SET porcentaje_igv = 18.00 WHERE porcentaje_igv IS NULL;
UPDATE sedes  SET pais = 'Perú'  WHERE pais IS NULL;
UPDATE sedes  SET moneda = 'PEN' WHERE moneda IS NULL;
UPDATE ventas SET canal_venta = 'LOCAL'      WHERE canal_venta IS NULL;
UPDATE ventas SET estado_sunat = 'PENDIENTE' WHERE estado_sunat IS NULL;

COMMIT;
-- Volver a correr el DIAGNÓSTICO: todo debe quedar en 0 salvo los [revisar].
