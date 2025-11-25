package com.spacepong.server.bbdd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Gestor de logs para el servidor SpacePong
 * Solo almacena información esencial para debugging
 */
public class DatabaseLogger {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseLogger.class);
    private static DatabaseLogger instance;
    private Connection connection;
    
    private static final String DB_FILE = "spacepong_logs.db";
    
    private DatabaseLogger() {
        initialize();
    }
    
    public static DatabaseLogger getInstance() {
        if (instance == null) {
            instance = new DatabaseLogger();
        }
        return instance;
    }
    
    private void initialize() {
        try {
            String url = "jdbc:sqlite:" + DB_FILE;
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(false);
            
            createLogsTable();
            createIndexes();
            
            logger.info("✅ DatabaseLogger inicializado: {}", DB_FILE);
            
            logServerEvent("SERVER_START", "Servidor iniciado");
            
        } catch (SQLException e) {
            // Log via SLF4J (if binding present). Also print stacktrace to stderr to ensure visibility when SLF4J binding missing.
            logger.error("❌ Error inicializando DatabaseLogger: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createLogsTable() throws SQLException {
        String sql = 
            "CREATE TABLE IF NOT EXISTS server_logs (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "details TEXT" +
            ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }
    
    private void createIndexes() throws SQLException {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_timestamp ON server_logs(timestamp)",
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String index : indexes) {
                stmt.execute(index);
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }
    
    // ==================== MÉTODOS ESENCIALES DE LOGGING ====================
    
    /**
     * Log de eventos del servidor
     */
    public void logServerEvent(String eventType, String details) {
        logEvent(eventType, null, null, null, details);
    }
    
    /**
     * Log de nueva conexión
     */
    public void logConnectionOpened() {
        logEvent("CONNECTION_OPENED", null, null, null, "Nueva conexión WebSocket");
    }
    
    /**
     * Log de conexión cerrada
     */
    public void logConnectionClosed(String playerId, String playerName, int code, String reason) {
        String details = "Conexión cerrada - Código: " + code;
        if (playerId != null) {
            details += " - Jugador: " + (playerName != null ? playerName : playerId);
        }
        
        logEvent("CONNECTION_CLOSED", playerId, playerName, null, details);
    }
    
    /**
     * Log de registro exitoso de jugador
     */
    public void logPlayerRegistered(String playerId, String playerName) {
        logEvent("PLAYER_REGISTERED", playerId, playerName, null, 
                "Jugador registrado: " + playerName);
    }
    
    /**
     * Log de comando recibido
     */
    public void logCommandReceived(String commandType, String playerId, String playerName) {
        logEvent("COMMAND_RECEIVED", playerId, playerName, commandType, 
                "Comando procesado");
    }
    
    /**
     * Log de error en comando
     */
    public void logCommandError(String commandType, String playerId, String playerName, String error) {
        logEvent("COMMAND_ERROR", playerId, playerName, commandType, 
                "Error: " + error);
    }
    
    /**
     * Log de evento de juego
     */
    public void logGameEvent(String eventType, String playerId, String playerName, String details) {
        logEvent(eventType, playerId, playerName, null, details);
    }
    
    // ==================== MÉTODO PRIVADO PRINCIPAL ====================
    
    private void logEvent(String eventType, String playerId, String playerName, 
                        String commandType, String details) {
        String sql = "INSERT INTO server_logs (details) VALUES (?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            setNullableString(pstmt, 1, truncateDetails(details));
            pstmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            rollbackAndLogError("registrando evento", e);
        }
    }

    
    // ==================== MÉTODOS DE CONSULTA ====================
    
    /**
     * Obtiene estadísticas básicas del servidor
     */
    public void printServerStats() {
        try (Statement stmt = connection.createStatement()) {
            System.out.println("\n📊 ESTADÍSTICAS DEL SERVIDOR");
            System.out.println("============================");
            
            // Total de logs
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM server_logs")) {
                if (rs.next()) System.out.println("📨 Total logs: " + rs.getInt(1));
            }
            
            // Comandos recientes (última hora)
            try (ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM server_logs WHERE timestamp > datetime('now', '-1 hour')")) {
                if (rs.next()) System.out.println("🔄 Comandos (1h): " + rs.getInt(1));
            }
            
            // Conexiones recientes
            try (ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM server_logs  WHERE timestamp > datetime('now', '-5 minutes')")) {
                if (rs.next()) System.out.println("🔗 Conexiones (5m): " + rs.getInt(1));
            }
            
        } catch (SQLException e) {
            logger.error("❌ Error obteniendo estadísticas: {}", e.getMessage());
        }
    }
    
    /**
     * Muestra los logs más recientes
     */
    public void printRecentLogs(int limit) {
        String sql = "SELECT timestamp, details " +
                    "FROM server_logs ORDER BY timestamp DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n📋 ÚLTIMOS " + limit + " EVENTOS");
                System.out.println("=======================");
                
                while (rs.next()) {
                    System.out.printf("🕒 %s | %s%n",
                        rs.getString("timestamp").substring(11, 19),
                        rs.getString("details")
                    );
                }
            }
            
        } catch (SQLException e) {
            logger.error("❌ Error obteniendo logs: {}", e.getMessage());
        }
    }
    
    // ==================== MÉTODOS DE MANTENIMIENTO ====================
    
    /**
     * Limpia logs antiguos
     */
    public void cleanupOldLogs(int daysToKeep) {
        String sql = "DELETE FROM server_logs WHERE timestamp < datetime('now', '-" + daysToKeep + " days')";
        
        try (Statement stmt = connection.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            connection.commit();
            
            if (deleted > 0) {
                logger.info("🧹 Limpiados {} logs antiguos (más de {} días)", deleted, daysToKeep);
            }
            
        } catch (SQLException e) {
            rollbackAndLogError("limpiando logs", e);
        }
    }
    
    /**
     * Cierra la conexión
     */
    public void close() {
        if (connection != null) {
            try {
                logServerEvent("SERVER_SHUTDOWN", "Servidor detenido");
                connection.close();
                logger.info("🔌 DatabaseLogger cerrado");
            } catch (SQLException e) {
                logger.error("❌ Error cerrando DatabaseLogger: {}", e.getMessage());
            }
        }
    }
    
    // ==================== MÉTODOS PRIVADOS AUXILIARES ====================
    
    private void setNullableString(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value != null && !value.trim().isEmpty()) {
            pstmt.setString(index, value);
        } else {
            pstmt.setNull(index, Types.VARCHAR);
        }
    }
    
    private String truncateDetails(String details) {
        if (details == null) return null;
        return details.length() > 500 ? details.substring(0, 500) + "..." : details;
    }
    
    private void rollbackAndLogError(String operation, SQLException e) {
        try {
            connection.rollback();
        } catch (SQLException rollbackEx) {
            logger.error("❌ Error en rollback durante {}: {}", operation, rollbackEx.getMessage());
        }
        logger.error("❌ Error {}: {}", operation, e.getMessage());
    }
}
