package com.spacepong.server;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

public class Server extends WebSocketServer {
    // TODO Usar ClientRegistry en lugar de esto para aprovechar trabajo ya hecho
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
        log("🔌 Cliente conectado desde: " + clientIP + ". Mandando saludo a todos los clientes...");
        broadcastToAll("Hola " + clientIP); 
        // TODO Debería mandar un json no un string, y los clientes deberían saber cómo manejar ese json
        // TODO Actualizar documentación API cuando se haga ese cambio en el json
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
        JSONObject json = new JSONObject(message);
        String type = json.getString("type");
        switch (type) {
            case "configuration":
                sendGroupConfiguration(conn);
                break;
            default:
                log("'type' no controlado");
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
        String configMessage = getGroupNameFromJson();
        JSONObject payload = new JSONObject();
        payload.put("type", "configuration");
        payload.put("configMessage", configMessage);
        conn.send(configMessage);
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        log("📤 Configuración enviada a " + clientIP + ": " + groupName);
    }

    private String getGroupNameFromJson() {
        try (JsonReader jsonReader = Json.createReader(new FileReader("config/groups.json"))) {
            JsonObject jsonObject = jsonReader.readObject();
            System.out.println(jsonObject);
            String groupName = jsonObject.getString("name");
            return groupName;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // TODO Esto es una chapuza
    }
    
    private void broadcastToAllExceptSender(String message, WebSocket excludeSender) {
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

    private void broadcastToAll(String message) {
        synchronized (connections) {
            int sentCount = 0;
            for (WebSocket client : connections) {
                if (client.isOpen()) {
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