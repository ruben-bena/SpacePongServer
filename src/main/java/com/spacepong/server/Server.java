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
import org.json.JSONArray;
import org.json.JSONObject;

public class Server extends WebSocketServer {
    private Set<WebSocket> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Set<String> playerNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private String groupName = "SpacePong";
    private int playerCounter = 1;
    
    public Server(InetSocketAddress address) {
        super(address);
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        
        // ✅ ASIGNAR NOMBRE TEMPORAL AL JUGADOR
        String playerName = "Jugador" + playerCounter++;
        playerNames.add(playerName);
        
        log("🔌 Cliente conectado: " + playerName + " desde " + clientIP);
        log("👥 Total de jugadores conectados: " + connections.size());
        
        // ✅ ENVIAR SALUDO INDIVIDUAL (manteniendo compatibilidad)
        conn.send("Hola " + clientIP);
        
        // ✅ ENVIAR CONFIRMACIÓN DE CONEXIÓN CON NOMBRE ASIGNADO
        JSONObject welcomeMsg = new JSONObject();
        welcomeMsg.put("type", "playerConnected");
        welcomeMsg.put("playerName", playerName);
        welcomeMsg.put("playerIndex", connections.size() - 1);
        welcomeMsg.put("totalPlayers", connections.size());
        conn.send(welcomeMsg.toString());
        
        // ✅ ENVIAR LISTA ACTUALIZADA DE JUGADORES A TODOS LOS CLIENTES
        broadcastPlayerList();
        
        // ✅ SI HAY 2 JUGADORES, INICIAR COUNTDOWN
        if (connections.size() >= 2) {
            startGameCountdown();
        }
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        log("🔌 Cliente desconectado: " + clientIP);
        log("👥 Jugadores restantes: " + connections.size());
        
        // ✅ ACTUALIZAR LISTA DE JUGADORES PARA LOS QUE QUEDAN
        broadcastPlayerList();
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        log("📨 [" + clientIP + "] Mensaje: " + message);
        
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");
            
            switch (type) {
                case "requestConfiguration":
                    log("⚙️ Solicitud de configuración de " + clientIP);
                    sendGroupConfiguration(conn);
                    break;
                    
                case "join":
                    // ✅ MANEJAR MENSAJE DE UNIÓN CON NOMBRE PERSONALIZADO
                    String playerName = json.optString("playerName", "Jugador");
                    handlePlayerJoin(conn, playerName, clientIP);
                    break;
                    
                case "playerReady":
                    // ✅ MANEJAR JUGADOR LISTO
                    handlePlayerReady(conn, clientIP);
                    break;
                    
                default:
                    log("'type' no controlado: " + type);
            }
        } catch (Exception e) {
            log("❌ Error procesando mensaje: " + e.getMessage());
            // ✅ ENVIAR MENSAJE DE ERROR AL CLIENTE
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("type", "error");
            errorMsg.put("message", "Error procesando mensaje: " + e.getMessage());
            conn.send(errorMsg.toString());
        }
    }
    
    // ✅ NUEVO MÉTODO: MANEJAR UNIÓN DE JUGADOR CON NOMBRE PERSONALIZADO
    private void handlePlayerJoin(WebSocket conn, String playerName, String clientIP) {
        log("🎮 Jugador se une: " + playerName + " (" + clientIP + ")");
        
        // ✅ ACTUALIZAR NOMBRE DEL JUGADOR
        playerNames.add(playerName);
        
        // ✅ ENVIAR CONFIRMACIÓN AL JUGADOR
        JSONObject welcomeMsg = new JSONObject();
        welcomeMsg.put("type", "welcome");
        welcomeMsg.put("message", "Bienvenido " + playerName);
        welcomeMsg.put("playerName", playerName);
        conn.send(welcomeMsg.toString());
        
        // ✅ NOTIFICAR A TODOS SOBRE EL NUEVO JUGADOR
        JSONObject playerJoinedMsg = new JSONObject();
        playerJoinedMsg.put("type", "playerJoined");
        playerJoinedMsg.put("playerName", playerName);
        playerJoinedMsg.put("playerIndex", connections.size() - 1);
        broadcastToAll(playerJoinedMsg.toString());
        
        // ✅ ACTUALIZAR LISTA COMPLETA
        broadcastPlayerList();
    }
    
    // ✅ NUEVO MÉTODO: MANEJAR JUGADOR LISTO
    private void handlePlayerReady(WebSocket conn, String clientIP) {
        log("✅ Jugador listo: " + clientIP);
        
        // ✅ ENVIAR CONFIRMACIÓN
        JSONObject readyMsg = new JSONObject();
        readyMsg.put("type", "playerReadyConfirmed");
        readyMsg.put("message", "Jugador marcado como listo");
        conn.send(readyMsg.toString());
    }
    
    // ✅ NUEVO MÉTODO: ENVIAR LISTA DE JUGADORES A TODOS LOS CLIENTES
    private void broadcastPlayerList() {
        JSONObject playersMsg = new JSONObject();
        playersMsg.put("type", "playersUpdate");
        
        JSONArray playersArray = new JSONArray();
        int index = 0;
        
        // ✅ CREAR ARRAY CON TODOS LOS JUGADORES CONECTADOS
        for (String playerName : playerNames) {
            JSONObject playerObj = new JSONObject();
            playerObj.put("index", index);
            playerObj.put("name", playerName);
            playerObj.put("connected", true);
            playerObj.put("ready", false); // Por defecto no listo
            playersArray.put(playerObj);
            index++;
            
            if (index >= 2) break; // Máximo 2 jugadores para SpacePong
        }
        
        playersMsg.put("players", playersArray);
        playersMsg.put("totalPlayers", connections.size());
        playersMsg.put("maxPlayers", 2); // SpacePong es para 2 jugadores
        
        String messageStr = playersMsg.toString();
        broadcastToAll(messageStr);
        
        log("📢 Lista de jugadores enviada: " + connections.size() + " jugadores conectados");
    }
    
    // ✅ NUEVO MÉTODO: INICIAR COUNTDOWN DEL JUEGO
    private void startGameCountdown() {
        log("⏱️ Iniciando countdown para 2 jugadores...");
        
        // ✅ COUNTDOWN DE 5 SEGUNDOS
        for (int i = 5; i >= 0; i--) {
            final int count = i;
            try {
                Thread.sleep(1000); // Esperar 1 segundo entre cada número
                
                JSONObject countdownMsg = new JSONObject();
                countdownMsg.put("type", "countdown");
                countdownMsg.put("value", count);
                countdownMsg.put("message", count == 0 ? "¡GO!" : "Iniciando en " + count);
                
                broadcastToAll(countdownMsg.toString());
                
                log("⏱️ Countdown: " + count);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // ✅ INICIAR JUEGO
        JSONObject gameStartMsg = new JSONObject();
        gameStartMsg.put("type", "gameStart");
        gameStartMsg.put("message", "¡El juego ha comenzado!");
        broadcastToAll(gameStartMsg.toString());
        
        log("🎯 ¡JUEGO INICIADO!");
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
        log("✅ Servidor listo para múltiples jugadores (máximo 2)");
    }
    
    private void sendGroupConfiguration(WebSocket conn) {
        String configMessage = getGroupNameFromJson();
        JSONObject payload = new JSONObject();
        payload.put("type", "configuration");
        payload.put("configMessage", configMessage);
        payload.put("maxPlayers", 2);
        payload.put("gameName", "SpacePong");
        conn.send(payload.toString());
        
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        log("📤 Configuración enviada a " + clientIP);
    }

    private String getGroupNameFromJson() {
        try (JsonReader jsonReader = Json.createReader(new FileReader("config/groups.json"))) {
            JsonObject jsonObject = jsonReader.readObject();
            String groupName = jsonObject.getString("name");
            return groupName;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "SpacePong"; // Valor por defecto
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
            log("📢 Mensaje enviado a " + sentCount + " clientes (excluyendo remitente)");
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
            log("📢 Mensaje enviado a " + sentCount + " clientes");
        }
    }
    
    public String getGroupName() {
        return groupName;
    }

    private void log(String text) {
        System.out.println(text);
    }
    
    public static void main(String[] args) throws Exception {
        Server server = new Server(new InetSocketAddress(3000));
        server.start();
        System.out.println("🛑 Servidor SpacePong ejecutándose en puerto 3000");
        System.out.println("📍 Máximo 2 jugadores");
        System.out.println("⏹️  Presiona Ctrl+C para detener");
    }
}