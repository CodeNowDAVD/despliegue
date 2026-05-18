-- Datos base idempotentes (no pisa filas existentes).
-- Contraseñas demo: admin / admin123 — proveedor / proveedor123

INSERT IGNORE INTO users (username, password_hash, role, enabled)
VALUES
 ('admin', '$2a$10$iwuR9sMpOXwJ0EAALNEHP.tPEM6XXifojyDO4xNhz4EDHsHlnZnN2', 'ADMIN', 1),
 ('proveedor', '$2a$10$i5XMDtTdZFz0xhJ5lxlwi.ZKwrQNPYFtnMWIsFmSWw7.VRIDosBsW', 'PROVEEDOR', 1);

INSERT IGNORE INTO book_categories (name)
VALUES ('Salud'), ('Familia');

INSERT INTO books (category_id, title, price, book_type, package_note, companion_book_id, companion_line_price)
SELECT c.id, 'El maravilloso mundo de la Biblia', 18.00, 'UNITARIO', NULL, NULL, NULL
FROM book_categories c
WHERE c.name = 'Salud'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'El maravilloso mundo de la Biblia');

INSERT INTO books (category_id, title, price, book_type, package_note, companion_book_id, companion_line_price)
SELECT c.id, 'Vivir con Esperanza', 2.50, 'UNITARIO', NULL, NULL, NULL
FROM book_categories c
WHERE c.name = 'Salud'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Vivir con Esperanza');

INSERT INTO books (category_id, title, price, book_type, package_note, companion_book_id, companion_line_price)
SELECT c.id, 'Bebidas saludables nutritivas y deliciosas', 24.00, 'PAQUETE',
       'Manual librería **: suele facturarse también «Vivir con Esperanza».',
       comp.id, 2.50
FROM book_categories c
JOIN books comp ON comp.title = 'Vivir con Esperanza'
WHERE c.name = 'Salud'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Bebidas saludables nutritivas y deliciosas');

INSERT INTO books (category_id, title, price, book_type, package_note, companion_book_id, companion_line_price)
SELECT c.id, 'Educación en el hogar', 15.00, 'UNITARIO', NULL, NULL, NULL
FROM book_categories c
WHERE c.name = 'Familia'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Educación en el hogar');

INSERT INTO books (category_id, title, price, book_type, package_note, companion_book_id, companion_line_price)
SELECT c.id, 'Saludablemente', 20.00, 'UNITARIO', NULL, NULL, NULL
FROM book_categories c
WHERE c.name = 'Salud'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Saludablemente');

INSERT IGNORE INTO sales_zones (name)
VALUES ('Campo A'), ('Campo B');

INSERT IGNORE INTO campaigns (name, starts_on, ends_on)
VALUES ('Campaña 2026', '2026-01-01', '2026-12-31');

INSERT IGNORE INTO provider_profiles (user_account_id, zone_id, first_name, last_name, dni, phone, email, career)
SELECT u.id, z.id, 'María', 'González', '12345678', '70000001', 'maria.gonzalez@ejemplo.com', 'Administración de empresas'
FROM users u
JOIN sales_zones z ON z.name = 'Campo A'
WHERE u.username = 'proveedor';

INSERT INTO warehouse_stock (book_id, quantity)
SELECT b.id, 500
FROM books b
WHERE NOT EXISTS (SELECT 1 FROM warehouse_stock w WHERE w.book_id = b.id);
