-- Fecha en que el terapeuta aceptó las pautas de convivencia de Ouro (NULL = todavía no aceptó).
-- Correr ANTES de deployar el backend.
ALTER TABLE therapist ADD COLUMN guidelines_accepted_at DATETIME NULL;
