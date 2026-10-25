-- Baja segura de clientes. No borra filas: agrega la marca de activo.
-- En el perfil dev, Hibernate también puede crear la columna (ddl-auto=update).
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS activo boolean DEFAULT true;
UPDATE clientes SET activo = true WHERE activo IS NULL;
