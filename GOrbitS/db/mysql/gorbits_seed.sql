-- =============================================================================
-- GOrbitS — esquema + datos demo (MySQL 8.x / MariaDB 10.3+)
-- Importar:
--   mysql -u USER -p NOMBRE_BD < db/mysql/gorbits_seed.sql
--
-- Credenciales demo: admin / admin123 — proveedor / proveedor123
-- Contraseñas: BCrypt (Spring Security BCryptPasswordEncoder)
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- Limpieza (bases nuevas pueden omitirse si no existen las tablas)
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS sales_guide_tags;
DROP TABLE IF EXISTS sales_contract_tags;
DROP TABLE IF EXISTS installment_reschedules;
DROP TABLE IF EXISTS inventory_movements;
DROP TABLE IF EXISTS installment_payments;
DROP TABLE IF EXISTS installments;
DROP TABLE IF EXISTS sales_guide_lines;
DROP TABLE IF EXISTS sales_guides;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS provider_profiles;
DROP TABLE IF EXISTS library_supply_invoice_lines;
DROP TABLE IF EXISTS library_supply_invoices;
DROP TABLE IF EXISTS library_stock_return_lines;
DROP TABLE IF EXISTS library_stock_returns;
DROP TABLE IF EXISTS library_payments;
DROP TABLE IF EXISTS stock_withdrawal_lines;
DROP TABLE IF EXISTS stock_withdrawals;
DROP TABLE IF EXISTS provider_field_stock;
DROP TABLE IF EXISTS warehouse_stock;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS book_categories;
DROP TABLE IF EXISTS campaigns;
DROP TABLE IF EXISTS sales_zones;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- Tablas (equivalentes al modelo JPA actual)
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL,
    password_hash LONGTEXT NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BIT NOT NULL,
    CONSTRAINT uk_username UNIQUE (username),
    CONSTRAINT pk_users PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE book_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uk_book_category_name UNIQUE (name),
    CONSTRAINT pk_book_categories PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    book_type VARCHAR(20) NOT NULL,
    package_note VARCHAR(500),
    companion_book_id BIGINT,
    companion_line_price DECIMAL(12,2),
    CONSTRAINT pk_books PRIMARY KEY (id),
    CONSTRAINT fk_book_category FOREIGN KEY (category_id) REFERENCES book_categories (id),
    CONSTRAINT fk_book_companion FOREIGN KEY (companion_book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE campaigns (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    CONSTRAINT uk_campaign_name UNIQUE (name),
    CONSTRAINT pk_campaigns PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sales_zones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uk_zone_name UNIQUE (name),
    CONSTRAINT pk_sales_zones PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE clients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(40),
    email VARCHAR(120),
    address_note VARCHAR(500),
    CONSTRAINT pk_clients PRIMARY KEY (id),
    CONSTRAINT fk_clients_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE provider_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT NOT NULL,
    zone_id BIGINT,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    dni VARCHAR(20),
    phone VARCHAR(40),
    email VARCHAR(120),
    career VARCHAR(120),
    CONSTRAINT uk_provider_user UNIQUE (user_account_id),
    CONSTRAINT uk_provider_dni UNIQUE (dni),
    CONSTRAINT pk_provider_profiles PRIMARY KEY (id),
    CONSTRAINT fk_provider_user FOREIGN KEY (user_account_id) REFERENCES users (id),
    CONSTRAINT fk_provider_zone FOREIGN KEY (zone_id) REFERENCES sales_zones (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sales_guides (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    contract_number VARCHAR(6) NOT NULL,
    order_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    note VARCHAR(500),
    client_return_at DATETIME(6),
    client_return_reason VARCHAR(500),
    client_return_hidden BIT NOT NULL,
    CONSTRAINT pk_sales_guides PRIMARY KEY (id),
    CONSTRAINT uk_sales_guide_owner_contract UNIQUE (owner_id, contract_number),
    CONSTRAINT fk_guides_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_guides_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id),
    CONSTRAINT fk_guides_client FOREIGN KEY (client_id) REFERENCES clients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sales_contract_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    CONSTRAINT uk_sales_contract_tag_owner_name UNIQUE (owner_id, name),
    CONSTRAINT pk_sales_contract_tags PRIMARY KEY (id),
    CONSTRAINT fk_sct_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sales_guide_tags (
    guide_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    CONSTRAINT pk_sales_guide_tags PRIMARY KEY (guide_id, tag_id),
    CONSTRAINT fk_sgt_guide FOREIGN KEY (guide_id) REFERENCES sales_guides (id),
    CONSTRAINT fk_sgt_tag FOREIGN KEY (tag_id) REFERENCES sales_contract_tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sales_guide_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guide_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_sales_guide_lines PRIMARY KEY (id),
    CONSTRAINT fk_sgl_guide FOREIGN KEY (guide_id) REFERENCES sales_guides (id),
    CONSTRAINT fk_sgl_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE installments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guide_id BIGINT NOT NULL,
    seq INT NOT NULL,
    due_date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    CONSTRAINT uk_installment_guide_seq UNIQUE (guide_id, seq),
    CONSTRAINT pk_installments PRIMARY KEY (id),
    CONSTRAINT fk_installments_guide FOREIGN KEY (guide_id) REFERENCES sales_guides (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE installment_reschedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    installment_id BIGINT NOT NULL,
    previous_due_date DATE NOT NULL,
    new_due_date DATE NOT NULL,
    rescheduled_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_installment_reschedules PRIMARY KEY (id),
    CONSTRAINT fk_ir_installment FOREIGN KEY (installment_id) REFERENCES installments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE installment_payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    installment_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    paid_on DATE NOT NULL,
    note VARCHAR(500),
    CONSTRAINT pk_installment_payments PRIMARY KEY (id),
    CONSTRAINT fk_ip_installment FOREIGN KEY (installment_id) REFERENCES installments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_withdrawals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    note VARCHAR(500),
    CONSTRAINT pk_stock_withdrawals PRIMARY KEY (id),
    CONSTRAINT fk_sw_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stock_withdrawal_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    withdrawal_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT pk_stock_withdrawal_lines PRIMARY KEY (id),
    CONSTRAINT fk_swl_withdrawal FOREIGN KEY (withdrawal_id) REFERENCES stock_withdrawals (id),
    CONSTRAINT fk_swl_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE library_supply_invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    invoice_number VARCHAR(80) NOT NULL,
    issued_on DATE NOT NULL,
    note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_library_supply_invoice_owner_number UNIQUE (owner_id, invoice_number),
    CONSTRAINT pk_library_supply_invoices PRIMARY KEY (id),
    CONSTRAINT fk_lsi_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE library_supply_invoice_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(14,2) NOT NULL,
    CONSTRAINT pk_library_supply_invoice_lines PRIMARY KEY (id),
    CONSTRAINT fk_lsline_invoice FOREIGN KEY (invoice_id) REFERENCES library_supply_invoices (id),
    CONSTRAINT fk_lsline_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE library_payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    campaign_id BIGINT,
    amount DECIMAL(14,2) NOT NULL,
    paid_on DATE NOT NULL,
    note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_library_payments PRIMARY KEY (id),
    CONSTRAINT fk_lp_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_lp_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE library_stock_returns (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    campaign_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    note VARCHAR(500),
    CONSTRAINT pk_library_stock_returns PRIMARY KEY (id),
    CONSTRAINT fk_lsr_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_lsr_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE library_stock_return_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    library_stock_return_id BIGINT NOT NULL,
    invoice_line_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT pk_library_stock_return_lines PRIMARY KEY (id),
    CONSTRAINT fk_lsrl_return FOREIGN KEY (library_stock_return_id) REFERENCES library_stock_returns (id),
    CONSTRAINT fk_lsrl_invoice_line FOREIGN KEY (invoice_line_id) REFERENCES library_supply_invoice_lines (id),
    CONSTRAINT fk_lsrl_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inventory_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    movement_type VARCHAR(40) NOT NULL,
    quantity_delta INT NOT NULL,
    warehouse_delta INT NOT NULL,
    field_delta INT NOT NULL,
    reference_type VARCHAR(60),
    reference_id BIGINT,
    note VARCHAR(500),
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_inventory_movements PRIMARY KEY (id),
    CONSTRAINT fk_im_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_im_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE warehouse_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT uk_warehouse_stock_book UNIQUE (book_id),
    CONSTRAINT pk_warehouse_stock PRIMARY KEY (id),
    CONSTRAINT fk_ws_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE provider_field_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT uk_provider_field_stock_owner_book UNIQUE (owner_id, book_id),
    CONSTRAINT pk_provider_field_stock PRIMARY KEY (id),
    CONSTRAINT fk_pfs_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_pfs_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Datos demo (= seeds dev/test del proyecto)
-- ---------------------------------------------------------------------------

INSERT INTO users (id, username, password_hash, role, enabled)
VALUES
 (1, 'admin', '$2a$10$iwuR9sMpOXwJ0EAALNEHP.tPEM6XXifojyDO4xNhz4EDHsHlnZnN2', 'ADMIN', 1),
 (2, 'proveedor', '$2a$10$i5XMDtTdZFz0xhJ5lxlwi.ZKwrQNPYFtnMWIsFmSWw7.VRIDosBsW', 'PROVEEDOR', 1);

INSERT INTO book_categories (id, name)
VALUES (1, 'Salud'), (2, 'Familia');

INSERT INTO books (id, category_id, title, price, book_type, package_note, companion_book_id, companion_line_price)
VALUES
 (1, 1, 'El maravilloso mundo de la Biblia', 18.00, 'UNITARIO', NULL, NULL, NULL),
 (2, 1, 'Bebidas saludables nutritivas y deliciosas', 24.00, 'PAQUETE', 'Manual librería **: suele facturarse también «Vivir con Esperanza».', 4, 2.50),
 (3, 2, 'Educación en el hogar', 15.00, 'UNITARIO', NULL, NULL, NULL),
 (4, 1, 'Vivir con Esperanza', 2.50, 'UNITARIO', NULL, NULL, NULL),
 (5, 1, 'Saludablemente', 20.00, 'UNITARIO', NULL, NULL, NULL);

INSERT INTO sales_zones (id, name)
VALUES (1, 'Campo A'), (2, 'Campo B');

INSERT INTO campaigns (id, name, starts_on, ends_on)
VALUES (1, 'Campaña 2026', '2026-01-01', '2026-12-31');

INSERT INTO provider_profiles (id, user_account_id, zone_id, first_name, last_name, dni, phone, email, career)
VALUES (1, 2, 1, 'María', 'González', '12345678', '70000001', 'maria.gonzalez@ejemplo.com', 'Administración de empresas');

INSERT INTO warehouse_stock (book_id, quantity)
VALUES (1, 500), (2, 500), (3, 500), (4, 500), (5, 500);

ALTER TABLE users AUTO_INCREMENT = 3;
ALTER TABLE book_categories AUTO_INCREMENT = 3;
ALTER TABLE books AUTO_INCREMENT = 5;
ALTER TABLE campaigns AUTO_INCREMENT = 2;
ALTER TABLE sales_zones AUTO_INCREMENT = 3;
ALTER TABLE provider_profiles AUTO_INCREMENT = 2;
