package main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connect - centralized JDBC connection manager.
 *
 * <p>Development defaults target the local MySQL server:
 * {@code jdbc:mysql://localhost:3306/pharmacy} with {@code root}/{@code empty}
 * password. These are local development defaults only and can be overridden by
 * the environment variables {@code PHARMACY_DB_URL}, {@code PHARMACY_DB_USER}
 * and {@code PHARMACY_DB_PASSWORD}.
 *
 * @author Gulshan Singh
 */
public final class Connect {

    /** Default development JDBC URL. */
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/pharmacy";
    /** Default development username. */
    private static final String DEFAULT_USER = "root";
    /** Default development password (empty for local root). */
    private static final String DEFAULT_PASSWORD = "";

    private static volatile Connection connection;

    private Connect() {
        // utility class - not meant to be instantiated
    }

    /**
     * Returns a shared {@link Connection}, creating it on first use.
     *
     * @return an open connection to the pharmacy database
     * @throws SQLException if the driver is missing or the server is unreachable
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = env("PHARMACY_DB_URL", DEFAULT_URL);
            String user = env("PHARMACY_DB_USER", DEFAULT_USER);
            String password = env("PHARMACY_DB_PASSWORD", DEFAULT_PASSWORD);

            loadDriver();

            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    /**
     * Closes the shared connection, if one is open. Safe to call any time.
     */
    public static void close() {
        Connection current = connection;
        connection = null;
        if (current != null) {
            try {
                current.close();
            } catch (SQLException ignore) {
                // closing an already-closed connection is a non-event
            }
        }
    }

    /**
     * Loads a MySQL JDBC driver. Supports both Connector/J 5.x
     * ({@code com.mysql.jdbc.Driver}) and 8.x ({@code com.mysql.cj.jdbc.Driver}).
     * If no driver class is found we still proceed - modern drivers register
     * themselves via the JDBC service loader and DriverManager will report a
     * clear "no suitable driver" error if the JAR is genuinely absent.
     */
    private static void loadDriver() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ignore) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ignoreAgain) {
                // rely on driver auto-registration
            }
        }
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isEmpty() ? fallback : value;
    }
}
