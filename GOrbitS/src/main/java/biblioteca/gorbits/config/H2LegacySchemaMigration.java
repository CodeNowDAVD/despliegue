package biblioteca.gorbits.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Parchea bases H2 creadas antes de contract_number / order_date / invoice_line_id.
 * Debe ejecutarse antes de que Hibernate valide o amplíe el esquema.
 */
public final class H2LegacySchemaMigration {

    private static final Logger log = LoggerFactory.getLogger(H2LegacySchemaMigration.class);

    private final DataSource dataSource;
    private final Environment environment;

    public H2LegacySchemaMigration(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    public void apply() {
        String url = environment.getProperty("spring.datasource.url", "");
        if (!url.toLowerCase().contains("jdbc:h2:")) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            if (!isH2(connection)) {
                return;
            }
            patchSalesGuides(connection);
            patchLibraryStockReturnLines(connection);
            log.info("Migración H2 legacy aplicada (contract_number, order_date, invoice_line_id)");
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo migrar el esquema H2 legacy", ex);
        }
    }

    private static boolean isH2(Connection connection) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName();
        return product != null && product.toLowerCase().contains("h2");
    }

    private void patchSalesGuides(Connection connection) throws SQLException {
        if (!tableExists(connection, "SALES_GUIDES")) {
            return;
        }
        if (!columnExists(connection, "SALES_GUIDES", "CONTRACT_NUMBER")) {
            exec(connection, "ALTER TABLE sales_guides ADD COLUMN contract_number VARCHAR(6)");
        }
        if (!columnExists(connection, "SALES_GUIDES", "ORDER_DATE")) {
            exec(connection, "ALTER TABLE sales_guides ADD COLUMN order_date DATE");
        }
        exec(
                connection,
                """
                UPDATE sales_guides
                SET contract_number = LPAD(CAST(id AS VARCHAR), 6, '0')
                WHERE contract_number IS NULL
                """);
        exec(
                connection,
                """
                UPDATE sales_guides
                SET order_date = CAST(created_at AS DATE)
                WHERE order_date IS NULL
                """);
        exec(connection, "UPDATE sales_guides SET order_date = CURRENT_DATE WHERE order_date IS NULL");
    }

    private void patchLibraryStockReturnLines(Connection connection) throws SQLException {
        if (!tableExists(connection, "LIBRARY_STOCK_RETURN_LINES")) {
            return;
        }
        if (!columnExists(connection, "LIBRARY_STOCK_RETURN_LINES", "INVOICE_LINE_ID")) {
            exec(connection, "ALTER TABLE library_stock_return_lines ADD COLUMN invoice_line_id BIGINT");
        }
        exec(
                connection,
                """
                UPDATE library_stock_return_lines rl
                SET invoice_line_id = (
                    SELECT MIN(lsil.id)
                    FROM library_supply_invoice_lines lsil
                    INNER JOIN library_supply_invoices lsi ON lsi.id = lsil.invoice_id
                    INNER JOIN library_stock_returns lsr ON lsr.id = rl.library_stock_return_id
                    WHERE lsil.book_id = rl.book_id AND lsi.owner_id = lsr.owner_id
                )
                WHERE invoice_line_id IS NULL
                """);
        exec(connection, "DELETE FROM library_stock_return_lines WHERE invoice_line_id IS NULL");
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (var ps =
                connection.prepareStatement(
                        """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                        WHERE UPPER(TABLE_NAME) = ?
                        """)) {
            ps.setString(1, tableName.toUpperCase());
            try (var rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName)
            throws SQLException {
        try (var ps =
                connection.prepareStatement(
                        """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE UPPER(TABLE_NAME) = ? AND UPPER(COLUMN_NAME) = ?
                        """)) {
            ps.setString(1, tableName.toUpperCase());
            ps.setString(2, columnName.toUpperCase());
            try (var rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static void exec(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
