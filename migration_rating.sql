-- Migración: tabla de calificaciones de terapeutas
-- Ejecutar en la base de datos MySQL de Railway

CREATE TABLE IF NOT EXISTS rating (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    therapist_id INT     NOT NULL,
    user_id      INT     NOT NULL,
    score        TINYINT NOT NULL CHECK (score >= 1 AND score <= 5),
    comment      TEXT,
    created_at   DATETIME NOT NULL,
    UNIQUE KEY unique_user_therapist (user_id, therapist_id),
    CONSTRAINT fk_rating_therapist FOREIGN KEY (therapist_id) REFERENCES therapist (id) ON DELETE CASCADE,
    CONSTRAINT fk_rating_user      FOREIGN KEY (user_id)      REFERENCES user (id)      ON DELETE CASCADE
);
