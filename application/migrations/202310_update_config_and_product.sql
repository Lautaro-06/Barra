
-- Agregar columnas para los datos del dueño en la tabla configuracion
ALTER TABLE configuracion
ADD COLUMN nombre_dueno TEXT,
ADD COLUMN email_dueno TEXT;

-- Agregar columna umbral_stock en la tabla producto
ALTER TABLE producto
ADD COLUMN umbral_stock INTEGER DEFAULT NULL;
