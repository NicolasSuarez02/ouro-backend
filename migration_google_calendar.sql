-- Migración: sincronización con Google Calendar (terapeuta y cliente, ambos opcionales)
-- Ejecutar en producción y desarrollo

ALTER TABLE user
  ADD COLUMN google_access_token TEXT DEFAULT NULL
    COMMENT 'Access token OAuth de Google Calendar del usuario (cifrado AES-256)',
  ADD COLUMN google_refresh_token TEXT DEFAULT NULL
    COMMENT 'Refresh token OAuth de Google Calendar del usuario (cifrado AES-256)',
  ADD COLUMN google_token_expiry DATETIME DEFAULT NULL
    COMMENT 'Expiración del access_token actual, en UTC';

ALTER TABLE appointment
  ADD COLUMN google_event_id_therapist VARCHAR(255) DEFAULT NULL
    COMMENT 'ID del evento creado en el Google Calendar del terapeuta (si conectó su cuenta)',
  ADD COLUMN google_event_id_client VARCHAR(255) DEFAULT NULL
    COMMENT 'ID del evento creado en el Google Calendar del cliente (si conectó su cuenta)';
