-- Migración: token de Mercado Pago por terapeuta
-- Ejecutar en producción y desarrollo

ALTER TABLE therapist
  ADD COLUMN mp_access_token VARCHAR(512) DEFAULT NULL
    COMMENT 'Access token personal de Mercado Pago del terapeuta (APP_USR-...)';
