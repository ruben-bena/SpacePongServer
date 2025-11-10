package main.java.server.com.broadcast.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerConfig {
    private static final Properties properties = new Properties();
    
    static {
        try (InputStream input = ServerConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                // Valores por defecto
                properties.setProperty("server.port", "8080");
                properties.setProperty("server.host", "0.0.0.0");
                properties.setProperty("server.maxClients", "100");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static int getPort() {
        return Integer.parseInt(properties.getProperty("server.port", "8080"));
    }
    
    public static String getHost() {
        return properties.getProperty("server.host", "0.0.0.0");
    }
    
    public static int getMaxClients() {
        return Integer.parseInt(properties.getProperty("server.maxClients", "100"));
    }
}