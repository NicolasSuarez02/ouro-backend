-- Slots cancelados por el terapeuta quedan como BLOCKED (restaurables) en vez de borrarse.
-- Correr ANTES de deployar el backend.
-- Verificar primero el tipo actual de la columna: SHOW COLUMNS FROM time_slot LIKE 'status';
-- Si es VARCHAR, esta migración no hace falta.
ALTER TABLE time_slot MODIFY status ENUM('FREE', 'RESERVED', 'BLOCKED') NOT NULL DEFAULT 'FREE';
