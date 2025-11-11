package com.spacepong.server;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class Server extends WebSocketServer {
    private Set<WebSocket> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private String groupName = "SpacePong";
    
    public Server(int port) {
        super(new InetSocketAddress("0.0.0.0", port));
        setReuseAddr(true);
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        log("🔌 Cliente conectado desde: " + clientIP);
        
        // Enviar configuración inmediatamente al cliente
        sendGroupConfiguration(conn);
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        log("🔌 Cliente desconectado: " + clientIP);
        log("👋 Clientes restantes: " + connections.size());
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        log("📨 [" + clientIP + "] Mensaje: " + message);

        
        
        // Si el cliente pide configuración, reenviar
        if (message.contains("\"type\"") && message.contains("\"getConfig\"")) {
            sendGroupConfiguration(conn);
        } else {
            // REENVIAR MENSAJE A TODOS LOS DEMÁS CLIENTES (BROADCAST)
            broadcastToAll(message, conn);
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("❌ Error WebSocket: " + ex.getMessage());
        if (conn != null) {
            conn.close();
        }
    }
    
    @Override
    public void onStart() {
        log("🚀 SpacePong Server WebSocket - Puerto 3000");
        log("📍 Grupo: " + groupName);
        log("✅ Servidor WebSocket listo en puerto 3000");
    }
    
    private void sendGroupConfiguration(WebSocket conn) {
        // Enviar configuración del grupo en JSON
        String configMessage = String.format(
            "{\"type\":\"groupConfig\",\"groupName\":\"%s\",\"timestamp\":%d}",
            groupName, System.currentTimeMillis()
        );
        
        conn.send(configMessage);
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        log("📤 Configuración enviada a " + clientIP + ": " + groupName);
    }
    
    public void broadcastToAll(String message, WebSocket excludeSender) {
        synchronized (connections) {
            int sentCount = 0;
            for (WebSocket client : connections) {
                if (client != excludeSender && client.isOpen()) {
                    client.send(message);
                    sentCount++;
                }
            }
            log("📢 Broadcast enviado a " + sentCount + " clientes: " + message);
        }
    }
    
    public String getGroupName() {
        return groupName;
    }

    private void log(String text) {
        System.out.println(text);
    }
    
    public static void main(String[] args) throws Exception {
        Server server = new Server(3000);
        server.start();
        System.out.println("🛑 Servidor WebSocket ejecutándose. Presiona Ctrl+C para detener.");
    }
}