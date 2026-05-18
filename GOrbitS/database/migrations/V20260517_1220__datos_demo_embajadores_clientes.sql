-- Demo idempotente: embajadores + clientes del proveedor (solo inserta lo que falta).
-- Contraseña embajadores: embajador123

INSERT IGNORE INTO users (username, password_hash, role, enabled)
VALUES
 ('embajador01', '$2a$10$xH5.gt4b0HnZYJ8rIz1Ld.MUh3HLRWkOxXtT8sT519LImf1oA1c9a', 'PROVEEDOR', 1),
 ('embajador02', '$2a$10$xH5.gt4b0HnZYJ8rIz1Ld.MUh3HLRWkOxXtT8sT519LImf1oA1c9a', 'PROVEEDOR', 1),
 ('embajador03', '$2a$10$xH5.gt4b0HnZYJ8rIz1Ld.MUh3HLRWkOxXtT8sT519LImf1oA1c9a', 'PROVEEDOR', 1),
 ('embajador04', '$2a$10$xH5.gt4b0HnZYJ8rIz1Ld.MUh3HLRWkOxXtT8sT519LImf1oA1c9a', 'PROVEEDOR', 1),
 ('embajador05', '$2a$10$xH5.gt4b0HnZYJ8rIz1Ld.MUh3HLRWkOxXtT8sT519LImf1oA1c9a', 'PROVEEDOR', 1);

INSERT IGNORE INTO provider_profiles (user_account_id, zone_id, first_name, last_name, dni, phone, email, career)
SELECT u.id, z.id, 'Juan', 'Pérez', '20100001', '70010001', 'juan.perez@demo.ejemplo', 'Teología'
FROM users u JOIN sales_zones z ON z.name = 'Campo A' WHERE u.username = 'embajador01'
UNION ALL
SELECT u.id, z.id, 'Laura', 'Soto', '20100002', '70010002', 'laura.soto@demo.ejemplo', 'Educación'
FROM users u JOIN sales_zones z ON z.name = 'Campo B' WHERE u.username = 'embajador02'
UNION ALL
SELECT u.id, z.id, 'Miguel', 'Ríos', '20100003', '70010003', 'miguel.rios@demo.ejemplo', 'Comunicación'
FROM users u JOIN sales_zones z ON z.name = 'Campo A' WHERE u.username = 'embajador03'
UNION ALL
SELECT u.id, z.id, 'Carmen', 'Díaz', '20100004', '70010004', 'carmen.diaz@demo.ejemplo', 'Psicología'
FROM users u JOIN sales_zones z ON z.name = 'Campo B' WHERE u.username = 'embajador04'
UNION ALL
SELECT u.id, z.id, 'Diego', 'Luna', '20100005', '70010005', 'diego.luna@demo.ejemplo', 'Administración'
FROM users u JOIN sales_zones z ON z.name = 'Campo A' WHERE u.username = 'embajador05';

-- Facturas de librería
INSERT INTO library_supply_invoices (owner_id, invoice_number, issued_on, note, created_at)
SELECT u.id, 'FAC-DEMO-MAIN', '2026-02-01', 'Stock demo proveedor principal', '2026-02-01 09:00:00.000000'
FROM users u WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM library_supply_invoices i WHERE i.owner_id = u.id AND i.invoice_number = 'FAC-DEMO-MAIN');

INSERT INTO library_supply_invoice_lines (invoice_id, book_id, quantity, line_total)
SELECT i.id, b.id, 120, 2160.00
FROM library_supply_invoices i
JOIN users u ON i.owner_id = u.id AND u.username = 'proveedor'
JOIN books b ON b.title = 'El maravilloso mundo de la Biblia'
WHERE i.invoice_number = 'FAC-DEMO-MAIN'
  AND NOT EXISTS (SELECT 1 FROM library_supply_invoice_lines l WHERE l.invoice_id = i.id AND l.book_id = b.id);

INSERT INTO library_supply_invoice_lines (invoice_id, book_id, quantity, line_total)
SELECT i.id, b.id, 60, 1440.00
FROM library_supply_invoices i
JOIN users u ON i.owner_id = u.id AND u.username = 'proveedor'
JOIN books b ON b.title = 'Bebidas saludables nutritivas y deliciosas'
WHERE i.invoice_number = 'FAC-DEMO-MAIN'
  AND NOT EXISTS (SELECT 1 FROM library_supply_invoice_lines l WHERE l.invoice_id = i.id AND l.book_id = b.id);

INSERT INTO library_supply_invoice_lines (invoice_id, book_id, quantity, line_total)
SELECT i.id, b.id, 60, 150.00
FROM library_supply_invoices i
JOIN users u ON i.owner_id = u.id AND u.username = 'proveedor'
JOIN books b ON b.title = 'Vivir con Esperanza'
WHERE i.invoice_number = 'FAC-DEMO-MAIN'
  AND NOT EXISTS (SELECT 1 FROM library_supply_invoice_lines l WHERE l.invoice_id = i.id AND l.book_id = b.id);

INSERT INTO library_supply_invoice_lines (invoice_id, book_id, quantity, line_total)
SELECT i.id, b.id, 40, 600.00
FROM library_supply_invoices i
JOIN users u ON i.owner_id = u.id AND u.username = 'proveedor'
JOIN books b ON b.title = 'Educación en el hogar'
WHERE i.invoice_number = 'FAC-DEMO-MAIN'
  AND NOT EXISTS (SELECT 1 FROM library_supply_invoice_lines l WHERE l.invoice_id = i.id AND l.book_id = b.id);

INSERT INTO library_supply_invoices (owner_id, invoice_number, issued_on, note, created_at)
SELECT u.id, CONCAT('FAC-EMB-', LPAD(SUBSTRING(u.username, 10), 2, '0')), '2026-02-10', 'Stock demo embajador', '2026-02-10 09:00:00.000000'
FROM users u
WHERE u.username IN ('embajador01', 'embajador02', 'embajador03', 'embajador04', 'embajador05')
  AND NOT EXISTS (
    SELECT 1 FROM library_supply_invoices i
    WHERE i.owner_id = u.id AND i.invoice_number = CONCAT('FAC-EMB-', LPAD(SUBSTRING(u.username, 10), 2, '0')));

INSERT INTO library_supply_invoice_lines (invoice_id, book_id, quantity, line_total)
SELECT i.id, b.id, 40, 720.00
FROM library_supply_invoices i
JOIN users u ON i.owner_id = u.id
JOIN books b ON b.title = 'El maravilloso mundo de la Biblia'
WHERE u.username IN ('embajador01', 'embajador02', 'embajador03', 'embajador04', 'embajador05')
  AND i.invoice_number LIKE 'FAC-EMB-%'
  AND NOT EXISTS (SELECT 1 FROM library_supply_invoice_lines l WHERE l.invoice_id = i.id AND l.book_id = b.id);

INSERT INTO library_payments (owner_id, campaign_id, amount, paid_on, note, created_at)
SELECT u.id, c.id, 500.00, '2026-02-15', 'Depósito parcial demo', '2026-02-15 10:00:00.000000'
FROM users u
JOIN campaigns c ON c.name = 'Campaña 2026'
WHERE u.username = 'proveedor'
  AND NOT EXISTS (
    SELECT 1 FROM library_payments p
    WHERE p.owner_id = u.id AND p.paid_on = '2026-02-15' AND p.note = 'Depósito parcial demo');

INSERT INTO library_payments (owner_id, campaign_id, amount, paid_on, note, created_at)
SELECT u.id, camp.id, 200.00, '2026-02-20', 'Depósito demo', '2026-02-20 10:00:00.000000'
FROM users u
JOIN campaigns camp ON camp.name = 'Campaña 2026'
WHERE u.username IN ('embajador01', 'embajador02')
  AND NOT EXISTS (
    SELECT 1 FROM library_payments p
    WHERE p.owner_id = u.id AND p.paid_on = '2026-02-20' AND p.note = 'Depósito demo');

-- Clientes proveedor principal
INSERT INTO clients (owner_id, full_name, phone, email, address_note)
SELECT u.id, 'Ana López', '999111001', 'ana@demo.ejemplo', 'Zona norte'
FROM users u WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM clients c WHERE c.owner_id = u.id AND c.full_name = 'Ana López');

INSERT INTO clients (owner_id, full_name, phone, email, address_note)
SELECT u.id, 'Carlos Ruiz', '999111002', NULL, NULL
FROM users u WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM clients c WHERE c.owner_id = u.id AND c.full_name = 'Carlos Ruiz');

INSERT INTO clients (owner_id, full_name, phone, email, address_note)
SELECT u.id, 'Rosa Méndez', '999111003', NULL, 'Devolución visible'
FROM users u WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM clients c WHERE c.owner_id = u.id AND c.full_name = 'Rosa Méndez');

INSERT INTO clients (owner_id, full_name, phone, email, address_note)
SELECT u.id, 'Pedro Vargas', '999111004', NULL, 'Cobranza vencida'
FROM users u WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM clients c WHERE c.owner_id = u.id AND c.full_name = 'Pedro Vargas');

INSERT INTO clients (owner_id, full_name, phone, email, address_note)
SELECT u.id, 'Lucía Paz', '999111005', 'lucia@demo.ejemplo', NULL
FROM users u WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM clients c WHERE c.owner_id = u.id AND c.full_name = 'Lucía Paz');

-- Clientes embajadores
INSERT INTO clients (owner_id, full_name, phone, email, address_note)
SELECT u.id, CONCAT(p.first_name, ' ', p.last_name, ' (cliente)'), p.phone, p.email, CONCAT('Cliente demo ', u.username)
FROM users u
JOIN provider_profiles p ON p.user_account_id = u.id
WHERE u.username IN ('embajador01', 'embajador02', 'embajador03', 'embajador04', 'embajador05')
  AND NOT EXISTS (
    SELECT 1 FROM clients c
    WHERE c.owner_id = u.id AND c.full_name = CONCAT(p.first_name, ' ', p.last_name, ' (cliente)'));

-- Guías proveedor
INSERT INTO sales_guides (owner_id, campaign_id, client_id, status, contract_number, order_date, created_at, note, client_return_at, client_return_reason, client_return_hidden)
SELECT u.id, camp.id, cli.id, 'ACTIVA', '000101', '2026-03-01', '2026-03-01 10:00:00.000000', 'Contrato activo con cuotas', NULL, NULL, 0
FROM users u
JOIN campaigns camp ON camp.name = 'Campaña 2026'
JOIN clients cli ON cli.owner_id = u.id AND cli.full_name = 'Ana López'
WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM sales_guides g WHERE g.owner_id = u.id AND g.contract_number = '000101');

INSERT INTO sales_guides (owner_id, campaign_id, client_id, status, contract_number, order_date, created_at, note, client_return_at, client_return_reason, client_return_hidden)
SELECT u.id, camp.id, cli.id, 'CERRADA', '000102', '2026-02-10', '2026-02-10 11:00:00.000000', 'Contrato cerrado', NULL, NULL, 0
FROM users u
JOIN campaigns camp ON camp.name = 'Campaña 2026'
JOIN clients cli ON cli.owner_id = u.id AND cli.full_name = 'Carlos Ruiz'
WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM sales_guides g WHERE g.owner_id = u.id AND g.contract_number = '000102');

INSERT INTO sales_guides (owner_id, campaign_id, client_id, status, contract_number, order_date, created_at, note, client_return_at, client_return_reason, client_return_hidden)
SELECT u.id, camp.id, cli.id, 'DEVUELTA', '000103', '2026-03-05', '2026-03-05 12:00:00.000000', NULL, '2026-03-20 15:00:00.000000', 'Cliente devolvió material completo', 0
FROM users u
JOIN campaigns camp ON camp.name = 'Campaña 2026'
JOIN clients cli ON cli.owner_id = u.id AND cli.full_name = 'Rosa Méndez'
WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM sales_guides g WHERE g.owner_id = u.id AND g.contract_number = '000103');

INSERT INTO sales_guides (owner_id, campaign_id, client_id, status, contract_number, order_date, created_at, note, client_return_at, client_return_reason, client_return_hidden)
SELECT u.id, camp.id, cli.id, 'ACTIVA', '000104', '2026-01-05', '2026-01-05 13:00:00.000000', NULL, NULL, NULL, 0
FROM users u
JOIN campaigns camp ON camp.name = 'Campaña 2026'
JOIN clients cli ON cli.owner_id = u.id AND cli.full_name = 'Pedro Vargas'
WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM sales_guides g WHERE g.owner_id = u.id AND g.contract_number = '000104');

INSERT INTO sales_guides (owner_id, campaign_id, client_id, status, contract_number, order_date, created_at, note, client_return_at, client_return_reason, client_return_hidden)
SELECT u.id, camp.id, cli.id, 'ACTIVA', '000105', '2026-03-12', '2026-03-12 14:00:00.000000', 'Paquete con libro incluido', NULL, NULL, 0
FROM users u
JOIN campaigns camp ON camp.name = 'Campaña 2026'
JOIN clients cli ON cli.owner_id = u.id AND cli.full_name = 'Lucía Paz'
WHERE u.username = 'proveedor'
  AND NOT EXISTS (SELECT 1 FROM sales_guides g WHERE g.owner_id = u.id AND g.contract_number = '000105');

-- Guías embajadores (contrato 0005xx según número de embajador)
INSERT INTO sales_guides (owner_id, campaign_id, client_id, status, contract_number, order_date, created_at, note, client_return_at, client_return_reason, client_return_hidden)
SELECT u.id, camp.id, cli.id, 'ACTIVA', CONCAT('0005', LPAD(SUBSTRING(u.username, 10), 2, '0')), '2026-03-15', '2026-03-15 10:00:00.000000', 'Venta demo embajador', NULL, NULL, 0
FROM users u
JOIN campaigns camp ON camp.name = 'Campaña 2026'
JOIN provider_profiles p ON p.user_account_id = u.id
JOIN clients cli ON cli.owner_id = u.id AND cli.full_name = CONCAT(p.first_name, ' ', p.last_name, ' (cliente)')
WHERE u.username IN ('embajador01', 'embajador02', 'embajador03', 'embajador04', 'embajador05')
  AND NOT EXISTS (
    SELECT 1 FROM sales_guides g
    WHERE g.owner_id = u.id AND g.contract_number = CONCAT('0005', LPAD(SUBSTRING(u.username, 10), 2, '0')));

-- Líneas de guía
INSERT INTO sales_guide_lines (guide_id, book_id, quantity, unit_price)
SELECT g.id, b.id, 2, 18.00
FROM sales_guides g JOIN users u ON g.owner_id = u.id JOIN books b ON b.title = 'El maravilloso mundo de la Biblia'
WHERE u.username = 'proveedor' AND g.contract_number = '000101'
  AND NOT EXISTS (SELECT 1 FROM sales_guide_lines l WHERE l.guide_id = g.id AND l.book_id = b.id);

INSERT INTO sales_guide_lines (guide_id, book_id, quantity, unit_price)
SELECT g.id, b.id, 1, 15.00
FROM sales_guides g JOIN users u ON g.owner_id = u.id JOIN books b ON b.title = 'Educación en el hogar'
WHERE u.username = 'proveedor' AND g.contract_number = '000102'
  AND NOT EXISTS (SELECT 1 FROM sales_guide_lines l WHERE l.guide_id = g.id AND l.book_id = b.id);

INSERT INTO sales_guide_lines (guide_id, book_id, quantity, unit_price)
SELECT g.id, b.id, 2, 11.00
FROM sales_guides g JOIN users u ON g.owner_id = u.id JOIN books b ON b.title = 'El maravilloso mundo de la Biblia'
WHERE u.username = 'proveedor' AND g.contract_number = '000103'
  AND NOT EXISTS (SELECT 1 FROM sales_guide_lines l WHERE l.guide_id = g.id AND l.book_id = b.id);

INSERT INTO sales_guide_lines (guide_id, book_id, quantity, unit_price)
SELECT g.id, b.id, 1, 18.00
FROM sales_guides g JOIN users u ON g.owner_id = u.id JOIN books b ON b.title = 'El maravilloso mundo de la Biblia'
WHERE u.username = 'proveedor' AND g.contract_number = '000104'
  AND NOT EXISTS (SELECT 1 FROM sales_guide_lines l WHERE l.guide_id = g.id AND l.book_id = b.id);

INSERT INTO sales_guide_lines (guide_id, book_id, quantity, unit_price)
SELECT g.id, b.id, 1, 24.00
FROM sales_guides g JOIN users u ON g.owner_id = u.id JOIN books b ON b.title = 'Bebidas saludables nutritivas y deliciosas'
WHERE u.username = 'proveedor' AND g.contract_number = '000105'
  AND NOT EXISTS (SELECT 1 FROM sales_guide_lines l WHERE l.guide_id = g.id AND l.book_id = b.id);

INSERT INTO sales_guide_lines (guide_id, book_id, quantity, unit_price)
SELECT g.id, b.id, 1, 2.50
FROM sales_guides g JOIN users u ON g.owner_id = u.id JOIN books b ON b.title = 'Vivir con Esperanza'
WHERE u.username = 'proveedor' AND g.contract_number = '000105'
  AND NOT EXISTS (SELECT 1 FROM sales_guide_lines l WHERE l.guide_id = g.id AND l.book_id = b.id);

INSERT INTO sales_guide_lines (guide_id, book_id, quantity, unit_price)
SELECT g.id, b.id, 1, 18.00
FROM sales_guides g
JOIN users u ON g.owner_id = u.id
JOIN books b ON b.title = 'El maravilloso mundo de la Biblia'
WHERE u.username IN ('embajador01', 'embajador02', 'embajador03', 'embajador04', 'embajador05')
  AND g.contract_number LIKE '0005__'
  AND NOT EXISTS (SELECT 1 FROM sales_guide_lines l WHERE l.guide_id = g.id AND l.book_id = b.id);

-- Cuotas
INSERT INTO installments (guide_id, seq, due_date, amount)
SELECT g.id, 1, '2026-04-01', 20.00
FROM sales_guides g JOIN users u ON g.owner_id = u.id
WHERE u.username = 'proveedor' AND g.contract_number = '000101'
  AND NOT EXISTS (SELECT 1 FROM installments i WHERE i.guide_id = g.id AND i.seq = 1);

INSERT INTO installments (guide_id, seq, due_date, amount)
SELECT g.id, 2, '2026-05-01', 16.00
FROM sales_guides g JOIN users u ON g.owner_id = u.id
WHERE u.username = 'proveedor' AND g.contract_number = '000101'
  AND NOT EXISTS (SELECT 1 FROM installments i WHERE i.guide_id = g.id AND i.seq = 2);

INSERT INTO installments (guide_id, seq, due_date, amount)
SELECT g.id, 1, '2026-02-28', 15.00
FROM sales_guides g JOIN users u ON g.owner_id = u.id
WHERE u.username = 'proveedor' AND g.contract_number = '000102'
  AND NOT EXISTS (SELECT 1 FROM installments i WHERE i.guide_id = g.id AND i.seq = 1);

INSERT INTO installments (guide_id, seq, due_date, amount)
SELECT g.id, 1, '2026-03-20', 22.00
FROM sales_guides g JOIN users u ON g.owner_id = u.id
WHERE u.username = 'proveedor' AND g.contract_number = '000103'
  AND NOT EXISTS (SELECT 1 FROM installments i WHERE i.guide_id = g.id AND i.seq = 1);

INSERT INTO installments (guide_id, seq, due_date, amount)
SELECT g.id, 1, '2026-01-15', 18.00
FROM sales_guides g JOIN users u ON g.owner_id = u.id
WHERE u.username = 'proveedor' AND g.contract_number = '000104'
  AND NOT EXISTS (SELECT 1 FROM installments i WHERE i.guide_id = g.id AND i.seq = 1);

INSERT INTO installments (guide_id, seq, due_date, amount)
SELECT g.id, 1, '2026-06-01', 26.50
FROM sales_guides g JOIN users u ON g.owner_id = u.id
WHERE u.username = 'proveedor' AND g.contract_number = '000105'
  AND NOT EXISTS (SELECT 1 FROM installments i WHERE i.guide_id = g.id AND i.seq = 1);

INSERT INTO installment_payments (installment_id, amount, paid_on, note)
SELECT i.id, 20.00, '2026-04-01', 'Pago demo'
FROM installments i
JOIN sales_guides g ON i.guide_id = g.id
JOIN users u ON g.owner_id = u.id
WHERE u.username = 'proveedor' AND g.contract_number = '000101' AND i.seq = 1
  AND NOT EXISTS (SELECT 1 FROM installment_payments p WHERE p.installment_id = i.id);

INSERT INTO installment_payments (installment_id, amount, paid_on, note)
SELECT i.id, 15.00, '2026-02-28', NULL
FROM installments i
JOIN sales_guides g ON i.guide_id = g.id
JOIN users u ON g.owner_id = u.id
WHERE u.username = 'proveedor' AND g.contract_number = '000102' AND i.seq = 1
  AND NOT EXISTS (SELECT 1 FROM installment_payments p WHERE p.installment_id = i.id);

INSERT INTO installment_reschedules (installment_id, previous_due_date, new_due_date, rescheduled_at)
SELECT i.id, '2026-01-01', '2026-01-15', '2026-01-10 09:00:00.000000'
FROM installments i
JOIN sales_guides g ON i.guide_id = g.id
JOIN users u ON g.owner_id = u.id
WHERE u.username = 'proveedor' AND g.contract_number = '000104' AND i.seq = 1
  AND NOT EXISTS (SELECT 1 FROM installment_reschedules r WHERE r.installment_id = i.id);
