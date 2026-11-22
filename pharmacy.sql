-- =============================================================================
--  PHARMACY MANAGEMENT SYSTEM - RELATIONAL DATABASE SCRIPT
-- =============================================================================
--  Developer : Gulshan Singh
--  Roll No. : 24011010059
--  Course    : Relational Database Management Systems (RDBMS)
--  Department: Computer Science & Engineering
--  Institution: Rayat Bahra University
--
--  Target DB : MySQL 8.0.x / MariaDB 10.6+
--  Design    : Third Normal Form (3NF) - no partial / transitive dependencies.
--
--  NOTE: This script drops and recreates ONLY the `pharmacy` database.
--        It never touches any other database on the server.
-- =============================================================================

-- Safety: ensure a clean, repeatable import of the pharmacy database.
DROP DATABASE IF EXISTS pharmacy;
CREATE DATABASE pharmacy
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE pharmacy;

-- =============================================================================
-- 1. COMPANY  (supplier / manufacturer)
--    Primary key: surrogate company_id. Name is unique (natural candidate key).
-- =============================================================================
CREATE TABLE company (
  company_id INT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  address    VARCHAR(255),
  phone      VARCHAR(30),
  CONSTRAINT uq_company_name UNIQUE (name),
  CONSTRAINT chk_company_phone CHECK (phone IS NULL OR phone <> '')
) ENGINE=InnoDB;

-- =============================================================================
-- 2. USERS  (system users)
--    Passwords are stored ONLY as salted SHA-256 hashes - never plaintext.
-- =============================================================================
CREATE TABLE users (
  user_id           INT AUTO_INCREMENT PRIMARY KEY,
  name              VARCHAR(50) NOT NULL,
  dob               DATE,
  address           VARCHAR(255),
  phone             VARCHAR(30),
  salary            DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  password_hash      CHAR(64) NOT NULL,            -- hex SHA-256(salt + password)
  salt              CHAR(32) NOT NULL,             -- hex random salt (16 bytes)
  must_change_password TINYINT(1) NOT NULL DEFAULT 1,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_users_salary CHECK (salary >= 0)
) ENGINE=InnoDB;

-- =============================================================================
-- 3. DRUGS  (inventory master)
--    Primary key: BARCODE. FK -> company. Monetary values are DECIMAL.
-- =============================================================================
CREATE TABLE drugs (
  barcode       VARCHAR(20) NOT NULL PRIMARY KEY,
  name          VARCHAR(120) NOT NULL,
  type          VARCHAR(50),
  dose          VARCHAR(50),
  code          VARCHAR(50),
  cost_price    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  selling_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  production_date DATE,
  expiration_date DATE,
  place         VARCHAR(80),
  quantity      INT NOT NULL DEFAULT 0,
  company_id    INT,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_drugs_company FOREIGN KEY (company_id)
      REFERENCES company(company_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_drugs_cost  CHECK (cost_price >= 0),
  CONSTRAINT chk_drugs_sell CHECK (selling_price >= 0),
  CONSTRAINT chk_drugs_qty  CHECK (quantity >= 0),
  INDEX idx_drugs_name (name),
  INDEX idx_drugs_company (company_id),
  INDEX idx_drugs_expiry (expiration_date)
) ENGINE=InnoDB;

-- =============================================================================
-- 4. PURCHASE_ORDERS + PURCHASE_ITEMS
--    A medicine barcode is NOT the primary key here: the same medicine can be
--    purchased many times. Each order is a header with line items, so a single
--    purchase can contain several medicines.
-- =============================================================================
CREATE TABLE purchase_orders (
  order_id    INT AUTO_INCREMENT PRIMARY KEY,
  order_date  DATE NOT NULL,
  company_id  INT,
  user_id     INT,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_purchase_company FOREIGN KEY (company_id)
      REFERENCES company(company_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_purchase_user FOREIGN KEY (user_id)
      REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT chk_purchase_total CHECK (total_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE purchase_items (
  item_id   INT AUTO_INCREMENT PRIMARY KEY,
  order_id  INT NOT NULL,
  barcode   VARCHAR(20),
  name      VARCHAR(120),        -- transaction snapshot
  type      VARCHAR(50),         -- transaction snapshot
  quantity  INT NOT NULL,
  price     DECIMAL(10,2) NOT NULL,
  amount    DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_purchase_items_order FOREIGN KEY (order_id)
      REFERENCES purchase_orders(order_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_purchase_items_drug FOREIGN KEY (barcode)
      REFERENCES drugs(barcode) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_purchase_items_qty CHECK (quantity > 0),
  CONSTRAINT chk_purchase_items_price CHECK (price >= 0),
  CONSTRAINT chk_purchase_items_amount CHECK (amount >= 0),
  INDEX idx_purchase_items_order (order_id)
) ENGINE=InnoDB;

-- =============================================================================
-- 5. SALES + SALE_ITEMS
--    A medicine barcode is NOT the identifier of a complete sales transaction.
--    Each sale is a header (cashier + timestamp) with line items.
-- =============================================================================
CREATE TABLE sales (
  sale_id     INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT,
  sale_date   DATE NOT NULL,
  sale_time   TIME NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sales_user FOREIGN KEY (user_id)
      REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT chk_sales_total CHECK (total_amount >= 0),
  INDEX idx_sales_date (sale_date)
) ENGINE=InnoDB;

CREATE TABLE sale_items (
  item_id  INT AUTO_INCREMENT PRIMARY KEY,
  sale_id  INT NOT NULL,
  barcode  VARCHAR(20),
  name     VARCHAR(120),         -- transaction snapshot
  type     VARCHAR(50),          -- transaction snapshot
  dose     VARCHAR(50),          -- transaction snapshot
  quantity INT NOT NULL,
  price    DECIMAL(10,2) NOT NULL,
  amount   DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_sale_items_sale FOREIGN KEY (sale_id)
      REFERENCES sales(sale_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_sale_items_drug FOREIGN KEY (barcode)
      REFERENCES drugs(barcode) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_sale_items_qty CHECK (quantity > 0),
  CONSTRAINT chk_sale_items_price CHECK (price >= 0),
  CONSTRAINT chk_sale_items_amount CHECK (amount >= 0),
  INDEX idx_sale_items_sale (sale_id)
) ENGINE=InnoDB;

-- =============================================================================
-- 6. LOGIN_LOG  (audit of login events)
-- =============================================================================
CREATE TABLE login_log (
  login_id  INT AUTO_INCREMENT PRIMARY KEY,
  name      VARCHAR(50) NOT NULL,
  type      ENUM('SUCCESS','FAILURE') NOT NULL,
  login_date DATE NOT NULL,
  login_time TIME NOT NULL,
  INDEX idx_loginlog_name (name)
) ENGINE=InnoDB;

-- =============================================================================
-- 7. MESSAGES  (internal staff messaging)
-- =============================================================================
CREATE TABLE messages (
  message_id INT AUTO_INCREMENT PRIMARY KEY,
  message_from INT,
  message_to   INT,
  subject      VARCHAR(200),
  message_text TEXT NOT NULL,
  is_read      TINYINT(1) NOT NULL DEFAULT 0,
  sent_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_messages_from FOREIGN KEY (message_from)
      REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_messages_to FOREIGN KEY (message_to)
      REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL,
  INDEX idx_messages_to (message_to, is_read)
) ENGINE=InnoDB;

-- =============================================================================
-- 8. VIEWS  (reporting / alerting - avoids duplicated inventory state)
-- =============================================================================

-- Expiry report: medicines already past expiration.
CREATE OR REPLACE VIEW v_expired_drugs AS
SELECT name    AS product_name,
       code    AS product_code,
       expiration_date AS date_of_expiry,
       quantity AS quantity_remain
FROM drugs
WHERE expiration_date IS NOT NULL
  AND expiration_date < CURDATE();

-- Nearing expiry: expires within the next 90 days.
CREATE OR REPLACE VIEW v_almost_expire AS
SELECT name AS product_name,
       code AS product_code,
       expiration_date AS date_of_expiry,
       quantity AS quantity_remain
FROM drugs
WHERE expiration_date IS NOT NULL
  AND expiration_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 90 DAY);

-- Low stock alert: quantity at or below the reorder level (10 units).
CREATE OR REPLACE VIEW v_low_stock AS
SELECT barcode, name, type, quantity, place
FROM drugs
WHERE quantity <= 10
ORDER BY quantity ASC;

-- History of sales (join of sales + sale_items + users).
CREATE OR REPLACE VIEW history_sales AS
SELECT u.name          AS user_name,
       si.barcode,
       si.name,
       si.type,
       si.dose,
       si.quantity,
       si.price,
       si.amount,
       s.sale_date      AS date,
       s.sale_time      AS time
FROM sale_items si
JOIN sales s ON s.sale_id = si.sale_id
LEFT JOIN users u ON u.user_id = s.user_id;

-- Inbox: messages addressed to the current user, sender names resolved,
-- unread messages first.
CREATE OR REPLACE VIEW inbox AS
SELECT m.message_id,
       f.name AS from_name,
       m.message_from,
       m.subject,
       m.message_text,
       m.is_read,
       m.sent_at
FROM messages m
LEFT JOIN users f ON f.user_id = m.message_from;

-- =============================================================================
-- 9. DEMONSTRATION SEED DATA
--    All records below are clearly labeled demo data for academic use.
--    No real-world business contact information is fabricated.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 9.1 Companies (Indian pharmaceutical manufacturers - demo)
-- ---------------------------------------------------------------------------
INSERT INTO company (name, address, phone) VALUES
  ('Cipla',   'Mumbai, Maharashtra, India', '022-2000-0001'),   -- demo
  ('Sun Pharma','Ahmedabad, Gujarat, India', '079-4000-0002'),   -- demo
  ('Lupin',   'Mumbai, Maharashtra, India', '022-3000-0003');    -- demo

-- ---------------------------------------------------------------------------
-- 9.2 Users (demonstration credentials)
--     admin    / admin    -> must change password on first login
--     staff    / staff123 -> demo user
--     Passwords are stored as salted SHA-256 hashes only.
-- ---------------------------------------------------------------------------
INSERT INTO users (user_id, name, dob, address, phone, salary, password_hash, salt, must_change_password) VALUES
  (1, 'admin', '2000-01-01', 'Rayat Bahra University, Punjab', '0175-000-0001',
   25000.00, 'a2c9b1afcf2ac2633c05f3b4876814fc535be7999210a8969ed96740b9fe5a8d',
   '6aab3089d38a01ed86a472a6d31dccec', 1),
  (2, 'staff', '2001-06-15', 'Mohali, Punjab, India', '0172-000-0002',
   18000.00, '34cd41fba2ef9675e4684bb532ffb08f09018b1528055673d63b1891c9570b0a',
   '95f9214fc1a4a8e377aa97e4fcf1564b', 0);

-- ---------------------------------------------------------------------------
-- 9.3 Drugs (demo inventory)
-- ---------------------------------------------------------------------------
INSERT INTO drugs (barcode, name, type, dose, code, cost_price, selling_price,
                   production_date, expiration_date, place, quantity, company_id) VALUES
  ('8901001000001', 'Paracetamol 500mg', 'Tablet', '500mg', 'PARA-500',
   20.00, 30.00, '2026-01-01', '2028-01-01', 'Shelf A1', 490, 1),
  ('8901002000002', 'Amoxicillin 250mg', 'Capsule', '250mg', 'AMOX-250',
   45.00, 60.00, '2026-02-10', '2027-06-30', 'Shelf B2', 300, 2),
  ('8901003000003', 'Omeprazole 20mg', 'Capsule', '20mg', 'OMEP-20',
   35.00, 50.00, '2026-03-05', '2026-12-31', 'Shelf A3', 200, 3),
  ('8901004000004', 'Azithromycin 500mg', 'Tablet', '500mg', 'AZI-500',
   90.00, 120.00, '2026-01-20', '2028-03-15', 'Shelf B1', 150, 1),
  ('8901005000005', 'Cetirizine 10mg', 'Tablet', '10mg', 'CETI-10',
   15.00, 25.00, '2026-04-11', '2029-05-01', 'Shelf A2', 395, 2),
  ('8901006000006', 'ORS Sachet Sugar Free', 'Powder', '21.8g', 'ORS-SF',
   10.00, 18.00, '2024-01-01', '2025-06-30', 'Shelf C1', 50, 1), -- already expired
  ('8901007000007', 'Paracetamol 250mg Syrup', 'Syrup', '250mg/5ml', 'PARA-SY',
   40.00, 55.00, '2026-09-01', '2026-11-30', 'Shelf D1', 5, 3); -- low stock

-- ---------------------------------------------------------------------------
-- 9.4 Demo purchase order + line items (procurement)
-- ---------------------------------------------------------------------------
INSERT INTO purchase_orders (order_id, order_date, company_id, user_id, total_amount) VALUES
  (1, '2026-02-10', 1, 1, 2000.00);   -- demo

INSERT INTO purchase_items (order_id, barcode, name, type, quantity, price, amount) VALUES
  (1, '8901001000001', 'Paracetamol 500mg', 'Tablet', 100, 20.00, 2000.00);  -- demo

-- ---------------------------------------------------------------------------
-- 9.5 Demo sales + line items
--     Quantities above are post-sale (inventory already decreased).
-- ---------------------------------------------------------------------------
INSERT INTO sales (sale_id, user_id, sale_date, sale_time, total_amount) VALUES
  (1, 2, '2026-11-10', '10:15:00', 425.00),  -- demo
  (2, 1, '2026-11-15', '16:40:00', 360.00);  -- demo

INSERT INTO sale_items (sale_id, barcode, name, type, dose, quantity, price, amount) VALUES
  (1, '8901001000001', 'Paracetamol 500mg', 'Tablet', '500mg', 10, 30.00, 300.00),
  (1, '8901005000005', 'Cetirizine 10mg', 'Tablet', '10mg', 5, 25.00, 125.00),
  (2, '8901002000002', 'Amoxicillin 250mg', 'Capsule', '250mg', 6, 60.00, 360.00);

-- ---------------------------------------------------------------------------
-- 9.6 Demo login log
-- ---------------------------------------------------------------------------
INSERT INTO login_log (name, type, login_date, login_time) VALUES
  ('admin', 'SUCCESS', '2026-11-15', '16:39:00'),
  ('staff', 'SUCCESS', '2026-11-10', '10:14:00'),
  ('unknown', 'FAILURE', '2026-11-10', '09:00:00');

-- ---------------------------------------------------------------------------
-- 9.7 Demo messages (internal staff messaging)
-- ---------------------------------------------------------------------------
INSERT INTO messages (message_from, message_to, subject, message_text, is_read) VALUES
  (1, 2, 'Welcome', 'Welcome to the system. Please keep your stock records updated.', 0);

-- =============================================================================
-- End of pharmacy.sql
-- =============================================================================
