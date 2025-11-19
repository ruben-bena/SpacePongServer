package com.spacepong.server;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import com.spacepong.utils.DatabaseLogger;
import com.spacepong.server.MessageType;

public class Server extends WebSocketServer {
    private Set<WebSocket> allConnections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // ✅ SISTEMA MEJORADO DE GESTIÓN DE JUGADORES
    private Map<WebSocket, Player> players = new ConcurrentHashMap<>();
    private Map<String, GameSession> activeGames = new ConcurrentHashMap<>();
    private Queue<Player> availablePlayers = new LinkedList<>();
    
    private String groupName = "SpacePong";
    private int playerCounter = 1;
    private int gameCounter = 1;
    
    // ✅ CLASE PLAYER PARA MEJOR GESTIÓN
    private class Player {
        String id;
        String name;
        WebSocket connection;
        PlayerStatus status;
        String currentGameId;
        
        Player(String name, WebSocket connection) {
            this.id = "player_" + System.currentTimeMillis() + "_" + playerCounter++;
            this.name = name;
            this.connection = connection;
            this.status = PlayerStatus.AVAILABLE;
            this.currentGameId = null;
        }
    }
    
    // ✅ CLASE GAME SESSION
    private class GameSession {
        String gameId;
        Player player1;
        Player player2;
        GameStatus status;
        Date startTime;
        
        GameSession(Player p1, Player p2) {
            this.gameId = "game_" + gameCounter++;
            this.player1 = p1;
            this.player2 = p2;
            this.status = GameStatus.WAITING;
            this.startTime = new Date();
            
            // Marcar jugadores como en juego
            p1.status = PlayerStatus.IN_GAME;
            p1.currentGameId = gameId;
            p2.status = PlayerStatus.IN_GAME;
            p2.currentGameId = gameId;
        }
    }
    
    // ✅ ENUMS PARA ESTADOS
    private enum PlayerStatus {
        AVAILABLE,      // Conectado y disponible para jugar
        IN_GAME,        // Actualmente en una partida
        OFFLINE         // Desconectado
    }
    
    private enum GameStatus {
        WAITING,        // Esperando para empezar
        COUNTDOWN,      // En countdown
        PLAYING,        // Partida en curso
        FINISHED        // Partida terminada
    }
    
    public Server(InetSocketAddress address) {
        super(address);
    }

    
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        allConnections.add(conn);
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        
        // ✅ CREAR JUGADOR CON NOMBRE TEMPORAL
        String playerName = "Jugador" + playerCounter;
        Player newPlayer = new Player(playerName, conn);
        players.put(conn, newPlayer);
        
        log("🔌 Nuevo jugador: " + playerName + " [" + newPlayer.id + "] desde " + clientIP);
        
        // ✅ REGISTRAR EN LOGS
        DatabaseLogger.logEvent("PLAYER_CONNECT", newPlayer.id, playerName, 
            null, "Nuevo jugador conectado desde " + clientIP, clientIP);
            
        // ✅ ENVIAR SALUDO INDIVIDUAL
        conn.send("Hola " + clientIP);
        
        // ✅ ENVIAR INFORMACIÓN DEL JUGADOR CREADO
        JSONObject playerInfo = new JSONObject();
        playerInfo.put(MessageType.K_TYPE, "playerCreated");
        playerInfo.put("playerId", newPlayer.id);
        playerInfo.put("playerName", playerName);
        playerInfo.put("playerIndex", players.size() - 1);
        conn.send(playerInfo.toString());
        
        // ✅ AGREGAR A JUGADORES DISPONIBLES
        addToAvailablePlayers(newPlayer);
        
        // ✅ ENVIAR ESTADO ACTUAL A TODOS
        broadcastGameState();
        
        // ✅ INTENTAR CREAR PARTIDA SI HAY SUFICIENTES JUGADORES
        tryCreateGame();
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Player player = players.get(conn);
        if (player != null) {
            log("🔌 Jugador desconectado: " + player.name + " [" + player.id + "]");
            
            // ✅ SI ESTABA EN JUEGO, TERMINAR LA PARTIDA
            if (player.status == PlayerStatus.IN_GAME && player.currentGameId != null) {
                endGame(player.currentGameId, "Jugador desconectado");
            }

            DatabaseLogger.logEvent("PLAYER_DISCONNECT", player.id, player.name, 
                player.currentGameId, "Desconexión - Código: " + code + ", Razón: " + reason, null);
            
            // ✅ REMOVER DE DISPONIBLES
            availablePlayers.remove(player);
            players.remove(conn);
            player.status = PlayerStatus.OFFLINE;
        }
        
        allConnections.remove(conn);
        log("👥 Jugadores restantes: " + players.size() + " | Disponibles: " + getAvailablePlayersCount());
        
        // ✅ ACTUALIZAR ESTADO
        broadcastGameState();
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        Player player = players.get(conn);
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        
        if (player == null) {
            log("❌ Mensaje de conexión no registrada: " + clientIP);
            return;
        }
        
        log("📨 [" + player.name + "] Mensaje: " + message);

            DatabaseLogger.logEvent("MESSAGE_RECEIVED", player.id, player.name, 
            player.currentGameId, "Tipo: " + messageType + " | Contenido: " + truncateMessage(message), clientIP);

        
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString(MessageType.K_TYPE);
            
            switch (type) {
                case MessageType.T_REQUEST_CONFIGURATION:
                    DatabaseLogger.logEvent("CONFIG_REQUEST", player.id, player.name, 
                        null, "Solicitud de configuración del grupo", clientIP);
                    sendGroupConfiguration(conn);
                    break;
                    
                case "join":
                    String playerName = json.optString("playerName", player.name);
                    DatabaseLogger.logEvent("PLAYER_JOIN", player.id, playerName, 
                        null, "Jugador se une/cambia nombre", clientIP);
                    handlePlayerJoin(player, playerName);
                    break;
                    
                case "playerReady":
                    DatabaseLogger.logEvent("PLAYER_READY", player.id, player.name, 
                        null, "Jugador marcado como listo para partida", clientIP);
                    handlePlayerReady(player);
                    break;
                    
                case "leaveGame":
                    DatabaseLogger.logEvent("PLAYER_LEAVE", player.id, player.name, 
                        player.currentGameId, "Jugador abandona partida/cola", clientIP);
                    handleLeaveGame(player);
                    break;
                    
                case "findGame":
                    DatabaseLogger.logEvent("FIND_GAME", player.id, player.name, 
                        null, "Jugador busca partida", clientIP);
                    handleFindGame(player);
                    break;

                case MessageType.T_REGISTER:
                    String clientName = json.getString("clientName");
                    DatabaseLogger.logEvent("PLAYER_REGISTER", player.id, clientName, 
                        null, "Registro de jugador con nombre: " + clientName, clientIP);
                    handleRegister(conn, player, clientName);
                    break;

                case MessageType.T_MOVE:
                    DatabaseLogger.logEvent("PLAYER_MOVE", player.id, player.name, 
                        player.currentGameId, "Movimiento en juego detectado", clientIP);
                    break;

                case MessageType.T_EXIT:
                    DatabaseLogger.logEvent("PLAYER_EXIT", player.id, player.name, 
                        player.currentGameId, "Jugador solicita salir del juego", clientIP);
                    break;
                    
                default:
                    DatabaseLogger.logEvent("UNKNOWN_MESSAGE_TYPE", player.id, player.name, 
                        player.currentGameId, "Tipo no controlado: " + type, clientIP);
                    log("'type' no controlado: " + type);
            }
        } catch (Exception e) {
            DatabaseLogger.logEvent("MESSAGE_PROCESSING_ERROR", player.id, player.name, 
                player.currentGameId, "Error: " + e.getMessage() + " | Mensaje: " + message, clientIP);
            log("❌ Error procesando mensaje: " + e.getMessage());
            sendError(conn, "Error procesando mensaje: " + e.getMessage());
        }
    }
    
    // ✅ MANEJAR UNIÓN DE JUGADOR CON NOMBRE PERSONALIZADO
    private void handlePlayerJoin(Player player, String newName) {
        log("🎮 Jugador cambia nombre: " + player.name + " → " + newName);
        player.name = newName;
        
        // ✅ ENVIAR CONFIRMACIÓN
        JSONObject welcomeMsg = new JSONObject();
        welcomeMsg.put(MessageType.K_TYPE, "welcome");
        welcomeMsg.put("message", "Bienvenido " + newName);
        welcomeMsg.put("playerName", newName);
        player.connection.send(welcomeMsg.toString());
        
        // ✅ NOTIFICAR A TODOS SOBRE EL CAMBIO
        broadcastGameState();
    }
    
    // ✅ MANEJAR JUGADOR LISTO
    private void handlePlayerReady(Player player) {
        log("✅ Jugador listo: " + player.name);
        
        // ✅ SI YA ESTÁ EN JUEGO, NO HACER NADA
        if (player.status == PlayerStatus.IN_GAME) {
            sendError(player.connection, "Ya estás en una partida");
            return;
        }
        
        // ✅ AGREGAR/MOVER A DISPONIBLES
        addToAvailablePlayers(player);
        
        // ✅ ENVIAR CONFIRMACIÓN
        JSONObject readyMsg = new JSONObject();
        readyMsg.put(MessageType.K_TYPE, "playerReadyConfirmed");
        readyMsg.put("message", "Estás en la cola de espera");
        player.connection.send(readyMsg.toString());
        
        // ✅ INTENTAR CREAR PARTIDA
        tryCreateGame();
    }
    
    // ✅ MANEJAR BUSCAR PARTIDA
    private void handleFindGame(Player player) {
        log("🎯 Jugador busca partida: " + player.name);
        handlePlayerReady(player); // Misma lógica que ready
    }
    
    // ✅ MANEJAR ABANDONAR PARTIDA
    private void handleLeaveGame(Player player) {
        if (player.status == PlayerStatus.IN_GAME && player.currentGameId != null) {
            log("🚪 Jugador abandona partida: " + player.name);
            endGame(player.currentGameId, player.name + " abandonó la partida");
        } else {
            log("ℹ️ Jugador sale de la cola: " + player.name);
            availablePlayers.remove(player);
            broadcastGameState();
        }
    }

    private void handleRegister(WebSocket conn, Player player, String clientName) {
        JSONObject json = new JSONObject();
        if (isNameAlreadyRegistered(clientName)) {
            json.put(MessageType.K_TYPE, MessageType.T_DENY_REGISTER);
            json.put(MessageType.F_REASON, "name is already in use by someone else");
        } else {
            json.put(MessageType.K_TYPE, MessageType.T_ACCEPT_REGISTER);
            player.name = clientName;
            broadcastGameState();
        }
        conn.send(json.toString());
    }

    private boolean isNameAlreadyRegistered(String newName) {
        for (Player player : players.values()) {
            if (player.name.equals(newName)) {
                return true;
            }
        }    
        return false;
    }
    
    // ✅ AGREGAR JUGADOR A DISPONIBLES
    private void addToAvailablePlayers(Player player) {
        if (player.status != PlayerStatus.IN_GAME && !availablePlayers.contains(player)) {
            availablePlayers.offer(player);
            player.status = PlayerStatus.AVAILABLE;
            log("➕ Jugador disponible: " + player.name + " | En cola: " + availablePlayers.size());
        }
    }
    
    // ✅ INTENTAR CREAR PARTIDA
    private void tryCreateGame() {
        while (availablePlayers.size() >= 2) {
            Player player1 = availablePlayers.poll();
            Player player2 = availablePlayers.poll();
            
            if (player1 != null && player2 != null && 
                player1.connection.isOpen() && player2.connection.isOpen()) {
                
                createGame(player1, player2);
            }
        }
    }
    
    // ✅ MODIFICA EL MÉTODO createGame PARA DAR TIEMPO A MOSTRAR INFORMACIÓN
    private void createGame(Player player1, Player player2) {
        GameSession game = new GameSession(player1, player2);
        activeGames.put(game.gameId, game);
        
        log("🎮 NUEVA PARTIDA CREADA: " + game.gameId);
        log("   👤 Jugador 1: " + player1.name);
        log("   👤 Jugador 2: " + player2.name);

            DatabaseLogger.logEvent("GAME_CREATED", null, null, game.gameId, 
            "Partida entre " + player1.name + " y " + player2.name, null);
        
        // ✅ NOTIFICAR A LOS JUGADORES
        notifyPlayersGameFound(player1, player2, game.gameId);
        
        // ✅ ESPERAR 2 SEGUNDOS ANTES DEL COUNTDOWN PARA QUE LOS CLIENTES MUESTREN LA INFO
        new Thread(() -> {
            try {
                log("⏳ Esperando 2 segundos antes del countdown...");
                Thread.sleep(2000); // Dar tiempo a los clientes para mostrar la información
                
                // ✅ INICIAR COUNTDOWN DE LA PARTIDA
                startGameCountdown(game);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                endGame(game.gameId, "Error al iniciar countdown");
            }
        }).start();
    }
    
    // ✅ NOTIFICAR JUGADORES QUE ENCONTRARON PARTIDA
    private void notifyPlayersGameFound(Player player1, Player player2, String gameId) {
        JSONObject gameFoundMsg = new JSONObject();
        gameFoundMsg.put(MessageType.K_TYPE, "gameFound");
        gameFoundMsg.put("gameId", gameId);
        gameFoundMsg.put("opponentName", player2.name);
        gameFoundMsg.put("playerIndex", 0);
        player1.connection.send(gameFoundMsg.toString());
        
        gameFoundMsg.put("opponentName", player1.name);
        gameFoundMsg.put("playerIndex", 1);
        player2.connection.send(gameFoundMsg.toString());
    }
    
    // ✅ INICIAR COUNTDOWN DE PARTIDA
    private void startGameCountdown(GameSession game) {
        game.status = GameStatus.COUNTDOWN;

            DatabaseLogger.logEvent("GAME_COUNTDOWN", null, null, game.gameId, 
            "Countdown iniciado para partida", null);

        // 📢 Enviar inicio de countdown
        JSONObject startCountdownMsg = new JSONObject();
        startCountdownMsg.put(MessageType.K_TYPE, MessageType.T_START_COUNTDOWN);
        if (game.player1.connection.isOpen()) {
            game.player1.connection.send(startCountdownMsg.toString());
        }
        if (game.player2.connection.isOpen()) {
            game.player2.connection.send(startCountdownMsg.toString());
        }
        
        new Thread(() -> {
            try {
                // ✅ COUNTDOWN DE 3 SEGUNDOS
                for (int i = 3; i >= 0; i--) {
                    final int count = i;
                    
                    JSONObject countdownMsg = new JSONObject();
                    countdownMsg.put(MessageType.K_TYPE, "countdown");
                    countdownMsg.put("value", count);
                    countdownMsg.put("gameId", game.gameId);
                    countdownMsg.put("message", count == 0 ? "¡GO!" : "Iniciando en " + count);
                    
                    // ✅ ENVIAR A AMBOS JUGADORES
                    if (game.player1.connection.isOpen()) {
                        game.player1.connection.send(countdownMsg.toString());
                    }
                    if (game.player2.connection.isOpen()) {
                        game.player2.connection.send(countdownMsg.toString());
                    }
                    
                    log("⏱️ Countdown [" + game.gameId + "]: " + count);
                    Thread.sleep(1000);
                    
                    // ✅ VERIFICAR SI ALGÚN JUGADOR SE DESCONECTÓ
                    if (!game.player1.connection.isOpen() || !game.player2.connection.isOpen()) {
                        endGame(game.gameId, "Jugador desconectado durante countdown");
                        return;
                    }
                }
                
                // ✅ INICIAR JUEGO
                game.status = GameStatus.PLAYING;

                // 📢 Enviar fin del countdown
                JSONObject endCountdownMsg = new JSONObject();
                endCountdownMsg.put(MessageType.K_TYPE, MessageType.T_END_COUNTDOWN);
                if (game.player1.connection.isOpen()) {
                    game.player1.connection.send(endCountdownMsg.toString());
                }
                if (game.player2.connection.isOpen()) {
                    game.player2.connection.send(endCountdownMsg.toString());
                }

                JSONObject gameStartMsg = new JSONObject();
                gameStartMsg.put(MessageType.K_TYPE, MessageType.T_START_GAME);
                gameStartMsg.put("gameId", game.gameId);
                gameStartMsg.put("message", "¡La partida ha comenzado!");
                
                if (game.player1.connection.isOpen()) {
                    game.player1.connection.send(gameStartMsg.toString());
                }
                if (game.player2.connection.isOpen()) {
                    game.player2.connection.send(gameStartMsg.toString());
                }
                
                log("🎯 PARTIDA INICIADA: " + game.gameId);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                endGame(game.gameId, "Countdown interrumpido");
            }
        }).start();
    }
    
    // ✅ TERMINAR PARTIDA
    private void endGame(String gameId, String reason) {
        GameSession game = activeGames.remove(gameId);
        if (game != null) {
            log("🏁 Partida terminada: " + gameId + " - Razón: " + reason);
            game.status = GameStatus.FINISHED;
            
            // ✅ LIBERAR JUGADORES
            if (game.player1 != null) {
                game.player1.status = PlayerStatus.AVAILABLE;
                game.player1.currentGameId = null;
                addToAvailablePlayers(game.player1);
            }
            if (game.player2 != null) {
                game.player2.status = PlayerStatus.AVAILABLE;
                game.player2.currentGameId = null;
                addToAvailablePlayers(game.player2);
            }
            
            // ✅ NOTIFICAR FIN DE PARTIDA
            JSONObject gameEndMsg = new JSONObject();
            gameEndMsg.put(MessageType.K_TYPE, "gameEnd");
            gameEndMsg.put("gameId", gameId);
            gameEndMsg.put(MessageType.F_REASON, reason);
            
            if (game.player1 != null && game.player1.connection.isOpen()) {
                game.player1.connection.send(gameEndMsg.toString());
            }
            if (game.player2 != null && game.player2.connection.isOpen()) {
                game.player2.connection.send(gameEndMsg.toString());
            }
            
            // ✅ ACTUALIZAR ESTADO GLOBAL
            broadcastGameState();
            
            // ✅ INTENTAR CREAR NUEVAS PARTIDAS
            tryCreateGame();
        }
    }
    
    // ✅ BROADCAST ESTADO ACTUAL DEL JUEGO
    private void broadcastGameState() {
        JSONObject stateMsg = new JSONObject();
        stateMsg.put(MessageType.K_TYPE, MessageType.T_GAME_STATE);
        
        // ✅ JUGADORES DISPONIBLES
        JSONArray availableArray = new JSONArray();
        for (Player player : players.values()) {
            if (player.status == PlayerStatus.AVAILABLE) {
                JSONObject playerObj = new JSONObject();
                playerObj.put("id", player.id);
                playerObj.put("name", player.name);
                playerObj.put("status", "available");
                availableArray.put(playerObj);
            }
        }
        stateMsg.put("availablePlayers", availableArray);
        
        // ✅ PARTIDAS ACTIVAS
        JSONArray gamesArray = new JSONArray();
        for (GameSession game : activeGames.values()) {
            JSONObject gameObj = new JSONObject();
            gameObj.put("gameId", game.gameId);
            gameObj.put("player1", game.player1.name);
            gameObj.put("player2", game.player2.name);
            gameObj.put("status", game.status.toString());
            gamesArray.put(gameObj);
        }
        stateMsg.put("activeGames", gamesArray);
        
        // ✅ ESTADÍSTICAS
        stateMsg.put("totalPlayers", players.size());
        stateMsg.put("availablePlayersCount", getAvailablePlayersCount());
        stateMsg.put("activeGamesCount", activeGames.size());
        stateMsg.put("playersInGame", getPlayersInGameCount());
        
        broadcastToAll(stateMsg.toString());
    }
    
    // ✅ MÉTODOS UTILITARIOS
    private int getAvailablePlayersCount() {
        return availablePlayers.size();
    }
    
    private int getPlayersInGameCount() {
        return (int) players.values().stream()
                .filter(p -> p.status == PlayerStatus.IN_GAME)
                .count();
    }
    
    private void sendError(WebSocket conn, String message) {
        JSONObject errorMsg = new JSONObject();
        errorMsg.put(MessageType.K_TYPE, "error");
        errorMsg.put("message", message);
        conn.send(errorMsg.toString());
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
        // ✅ INICIALIZAR BASE DE DATOS DE LOGS
        DatabaseLogger.initialize();
        
        log("🚀 SpacePong Server - Sistema de Gestión de Partidas");
        log("📍 Grupo: " + groupName);
        log("✅ Servidor listo en puerto 3000");
        log("🗄️  Logs almacenados en: spacepong_logs.db");
    }
    
    private void sendGroupConfiguration(WebSocket conn) {
        String configMessage = getGroupNameFromJson();
        JSONObject payload = new JSONObject();
        payload.put(MessageType.K_TYPE, MessageType.T_CONFIGURATION);
        payload.put(MessageType.F_CONFIG_MESSAGE, configMessage);
        payload.put("maxPlayers", 2);
        payload.put("gameName", "SpacePong");
        conn.send(payload.toString());
    }

    private String getGroupNameFromJson() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("groups.json");
        try (JsonReader jsonReader = Json.createReader(inputStream)) {
            JsonObject jsonObject = jsonReader.readObject();
            return jsonObject.getString("name");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    private void broadcastToAll(String message) {
        synchronized (allConnections) {
            for (WebSocket client : allConnections) {
                if (client.isOpen()) {
                    client.send(message);
                }
            }
        }
    }
    
    private void log(String text) {
        System.out.println(text);
    }
    
    public static void main(String[] args) throws Exception {
        Server server = new Server(new InetSocketAddress(3000));
        server.start();
        System.out.println("🛑 Servidor SpacePong ejecutándose en puerto 3000");
        System.out.println("🎮 Sistema de gestión de partidas activo");
        System.out.println("⏹️  Presiona Ctrl+C para detener");
    }
}