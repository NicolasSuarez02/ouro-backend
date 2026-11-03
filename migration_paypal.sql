-- Pagos con PayPal (USD, directo a la cuenta PayPal de cada terapeuta).
-- Correr en prod ANTES de deployar el backend. Todas las columnas nuevas aceptan NULL:
-- los datos actuales no se tocan y Mercado Pago sigue igual.

ALTER TABLE therapist_specialty
  ADD COLUMN price_usd_cents INT NULL;

ALTER TABLE therapist
  ADD COLUMN paypal_email VARCHAR(255) NULL,
  ADD COLUMN paypal_connected_at DATETIME NULL;

ALTER TABLE appointment
  ADD COLUMN payment_provider VARCHAR(16) NULL,
  ADD COLUMN paypal_order_id VARCHAR(64) NULL,
  ADD COLUMN paypal_capture_id VARCHAR(64) NULL,
  ADD UNIQUE KEY uk_appointment_paypal_order (paypal_order_id);

-- Una fila por cada orden de PayPal creada. Guarda qué se intentó reservar.
-- Sin FK a time_slot a propósito: la regeneración de disponibilidad borra slots FREE.
CREATE TABLE paypal_checkout (
  id INT AUTO_INCREMENT PRIMARY KEY,
  paypal_order_id VARCHAR(64) NULL UNIQUE,
  time_slot_id INT NOT NULL,
  user_id INT NOT NULL,
  therapist_id INT NOT NULL,
  specialty_name VARCHAR(255) NULL,
  amount_usd_cents INT NOT NULL,
  payee_email VARCHAR(255) NOT NULL,
  notes TEXT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'CREATED', -- CREATED | CAPTURED | PENDING | SLOT_TAKEN | FAILED | DENIED
  paypal_capture_id VARCHAR(64) NULL,
  appointment_id INT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_paypal_checkout_slot (time_slot_id),
  CONSTRAINT fk_ppc_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
  CONSTRAINT fk_ppc_therapist FOREIGN KEY (therapist_id) REFERENCES therapist (id) ON DELETE CASCADE,
  CONSTRAINT fk_ppc_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id) ON DELETE SET NULL
);
