package com.spacepong.server.ddbb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public final class SQLiteLogger {

    private static final String DB_PATH = "spacepong_logs.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    private static Connection connection = null;

    // Ejecutado automáticamente al cargar la clase
    static {
        try {
            connection = DriverManager.getConnection(URL);
            createTableIfNotExists();
            System.out.println("[SQLiteLogger] Base de datos lista: " + DB_PATH);
        } catch (SQLException e) {
            System.err.println("[SQLiteLogger] ERROR inicializando SQLite: " + e.getMessage());
        }
    }

    private SQLiteLogger() {} // Evita instanciación

    private static void createTableIfNotExists() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                message TEXT NOT NULL,
                timestamp TEXT NOT NULL
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Guarda un log en SQLite.
     * @param msg El texto del log.
     */
    public static void log(String msg) {
        if (connection == null) return;

        String sql = "INSERT INTO logs(message, timestamp) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, msg);
            pstmt.setString(2, LocalDateTime.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SQLiteLogger] ERROR al insertar log: " + e.getMessage());
        }
    }

    /**
     * Cierra manualmente la conexión si quieres cerrar el server limpiamente.
     */
    public static void close() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[SQLiteLogger] Conexión cerrada.");
            } catch (SQLException e) {
                System.err.println("[SQLiteLogger] ERROR al cerrar: " + e.getMessage());
            }
        }
    }
}
