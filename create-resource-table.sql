-- Tabla para archivos de Biblioteca de Alejandría y Formaciones
-- Ejecutar este script antes de iniciar la aplicación

USE ouro;

CREATE TABLE IF NOT EXISTS resource (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(255)    NOT NULL,
    description     TEXT,
    category        ENUM('BIBLIOTECA', 'FORMACIONES') NOT NULL,
    original_file_name  VARCHAR(255)    NOT NULL,
    stored_file_name    VARCHAR(255)    NOT NULL UNIQUE,
    file_path       VARCHAR(512)    NOT NULL,
    file_size       BIGINT          NOT NULL,
    mime_type       VARCHAR(100)    NOT NULL,
    uploaded_by     INT             NOT NULL,
    approved_by     INT,
    approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,

    CONSTRAINT fk_resource_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES user(id),
    CONSTRAINT fk_resource_approved_by FOREIGN KEY (approved_by) REFERENCES user(id)
);

CREATE INDEX IF NOT EXISTS idx_resource_category_status ON resource(category, approval_status);
CREATE INDEX IF NOT EXISTS idx_resource_uploaded_by ON resource(uploaded_by);
