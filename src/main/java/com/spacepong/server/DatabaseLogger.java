package com.spacepong.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Clase utilitaria para gestionar logs de eventos en SQLite
 */
public class DatabaseLogger {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseLogger.class);
    private static Connection connection;
    
    private static final String DB_FILE = "spacepong_logs.db";
    
    /**
     * Inicializa la base de datos y crea la tabla de logs
     */
    public static void initialize() {
        try {
            String url = "jdbc:sqlite:" + DB_FILE;
            connection = DriverManager.getConnection(url);
            createLogsTable();
            logger.info("✅ Base de datos de logs inicializada: {}", DB_FILE);
            
            // Registrar evento de inicio
            logEvent("SERVER_START", null, "Servidor SpacePong iniciado");
            
        } catch (SQLException e) {
            logger.error("❌ Error inicializando base de datos: {}", e.getMessage());
        }
    }
    
    /**
     * Crea la tabla de logs si no existe
     */
    private static void createLogsTable() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS server_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "event_type TEXT NOT NULL," +
                "player_id TEXT," +
                "player_name TEXT," +
                "game_id TEXT," +
                "details TEXT," +
                "ip_address TEXT" +
                ")";
        
/*         // Índices para búsquedas más rápidas
        String index1 = "CREATE INDEX IF NOT EXISTS idx_event_type ON server_logs(event_type)";
        String index2 = "CREATE INDEX IF NOT EXISTS idx_timestamp ON server_logs(timestamp)";
        String index3 = "CREATE INDEX IF NOT EXISTS idx_player_id ON server_logs(player_id)"; */
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            // stmt.execute(index1);
            // stmt.execute(index2);
            // stmt.execute(index3);
            logger.info("✅ Tabla de logs creada/verificada");
        }
    }
    
    /**
     * Registra un evento en la base de datos
     */
    public static void logEvent(String eventType, String playerId, String details) {
        logEvent(eventType, playerId, null, null, details, null);
    }
    
    /**
     * Registra un evento en la base de datos con todos los parámetros
     */
    public static void logEvent(String eventType, String playerId, String playerName, 
                               String gameId, String details, String ipAddress) {
        String sql = "INSERT INTO server_logs " +
                    "(event_type, player_id, player_name, game_id, details, ip_address) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eventType);
            pstmt.setString(2, playerId);
            pstmt.setString(3, playerName);
            pstmt.setString(4, gameId);
            pstmt.setString(5, details);
            pstmt.setString(6, ipAddress);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("❌ Error registrando evento en BD: {}", e.getMessage());
        }
    }
    
    /**
     * Cierra la conexión a la base de datos
     */
    public static void close() {
        if (connection != null) {
            try {
                // Registrar evento de cierre
                logEvent("SERVER_SHUTDOWN", null, "Servidor SpacePong detenido");
                
                connection.close();
                logger.info("🔌 Conexión a base de datos de logs cerrada");
            } catch (SQLException e) {
                logger.error("❌ Error cerrando conexión: {}", e.getMessage());
            }
        }
    }
}