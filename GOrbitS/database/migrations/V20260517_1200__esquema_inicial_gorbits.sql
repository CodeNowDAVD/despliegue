-- GOrbitS — esquema inicial (MySQL 8.x / MariaDB 10.3+)
-- Idempotente: seguro en bases gorbits ya existentes (solo crea lo que falte).

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(128) NOT NULL,
    applied_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_schema_migrations PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL,
    password_hash LONGTEXT NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BIT NOT NULL,
    CONSTRAINT uk_username UNIQUE (username),
    CONSTRAINT pk_users PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS book_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uk_book_category_name UNIQUE (name),
    CONSTRAINT pk_book_categories PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS books (
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

CREATE TABLE IF NOT EXISTS campaigns (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    CONSTRAINT uk_campaign_name UNIQUE (name),
    CONSTRAINT pk_campaigns PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_zones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uk_zone_name UNIQUE (name),
    CONSTRAINT pk_sales_zones PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS clients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(40),
    email VARCHAR(120),
    address_note VARCHAR(500),
    CONSTRAINT pk_clients PRIMARY KEY (id),
    CONSTRAINT fk_clients_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS provider_profiles (
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

CREATE TABLE IF NOT EXISTS sales_guides (
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

CREATE TABLE IF NOT EXISTS sales_contract_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    CONSTRAINT uk_sales_contract_tag_owner_name UNIQUE (owner_id, name),
    CONSTRAINT pk_sales_contract_tags PRIMARY KEY (id),
    CONSTRAINT fk_sct_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_guide_tags (
    guide_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    CONSTRAINT pk_sales_guide_tags PRIMARY KEY (guide_id, tag_id),
    CONSTRAINT fk_sgt_guide FOREIGN KEY (guide_id) REFERENCES sales_guides (id),
    CONSTRAINT fk_sgt_tag FOREIGN KEY (tag_id) REFERENCES sales_contract_tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_guide_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guide_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_sales_guide_lines PRIMARY KEY (id),
    CONSTRAINT fk_sgl_guide FOREIGN KEY (guide_id) REFERENCES sales_guides (id),
    CONSTRAINT fk_sgl_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS installments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guide_id BIGINT NOT NULL,
    seq INT NOT NULL,
    due_date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    CONSTRAINT uk_installment_guide_seq UNIQUE (guide_id, seq),
    CONSTRAINT pk_installments PRIMARY KEY (id),
    CONSTRAINT fk_installments_guide FOREIGN KEY (guide_id) REFERENCES sales_guides (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS installment_reschedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    installment_id BIGINT NOT NULL,
    previous_due_date DATE NOT NULL,
    new_due_date DATE NOT NULL,
    rescheduled_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_installment_reschedules PRIMARY KEY (id),
    CONSTRAINT fk_ir_installment FOREIGN KEY (installment_id) REFERENCES installments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS installment_payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    installment_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    paid_on DATE NOT NULL,
    note VARCHAR(500),
    CONSTRAINT pk_installment_payments PRIMARY KEY (id),
    CONSTRAINT fk_ip_installment FOREIGN KEY (installment_id) REFERENCES installments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_withdrawals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    note VARCHAR(500),
    CONSTRAINT pk_stock_withdrawals PRIMARY KEY (id),
    CONSTRAINT fk_sw_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_withdrawal_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    withdrawal_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT pk_stock_withdrawal_lines PRIMARY KEY (id),
    CONSTRAINT fk_swl_withdrawal FOREIGN KEY (withdrawal_id) REFERENCES stock_withdrawals (id),
    CONSTRAINT fk_swl_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS library_supply_invoices (
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

CREATE TABLE IF NOT EXISTS library_supply_invoice_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(14,2) NOT NULL,
    CONSTRAINT pk_library_supply_invoice_lines PRIMARY KEY (id),
    CONSTRAINT fk_lsline_invoice FOREIGN KEY (invoice_id) REFERENCES library_supply_invoices (id),
    CONSTRAINT fk_lsline_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS library_payments (
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

CREATE TABLE IF NOT EXISTS library_stock_returns (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    campaign_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    note VARCHAR(500),
    CONSTRAINT pk_library_stock_returns PRIMARY KEY (id),
    CONSTRAINT fk_lsr_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_lsr_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS library_stock_return_lines (
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

CREATE TABLE IF NOT EXISTS inventory_movements (
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

CREATE TABLE IF NOT EXISTS warehouse_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT uk_warehouse_stock_book UNIQUE (book_id),
    CONSTRAINT pk_warehouse_stock PRIMARY KEY (id),
    CONSTRAINT fk_ws_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS provider_field_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT uk_provider_field_stock_owner_book UNIQUE (owner_id, book_id),
    CONSTRAINT pk_provider_field_stock PRIMARY KEY (id),
    CONSTRAINT fk_pfs_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_pfs_book FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
