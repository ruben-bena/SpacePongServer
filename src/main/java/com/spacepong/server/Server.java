package com.spacepong.server;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import com.spacepong.server.commands.Command;
import com.spacepong.server.commands.CommandRegistry;
import com.spacepong.server.commands.ExitCommand;
import com.spacepong.server.commands.MoveCommand;
import com.spacepong.server.commands.RegisterCommand;
import com.spacepong.server.commands.RequestConfigurationCommand;
import com.spacepong.server.enums.MessageType;
import com.spacepong.server.services.PlayerRegistry;

import com.spacepong.server.bbdd.DatabaseLogger;

public class Server extends WebSocketServer {

    private final PlayerRegistry playerRegistry = new PlayerRegistry();
    private final CommandRegistry commandRegistry = new CommandRegistry();

    private final DatabaseLogger dbLogger = DatabaseLogger.getInstance();

    public Server(InetSocketAddress address) {
        super(address);
        registerCommands();
    }

    @Override
    /* 
    * onOpen does nothing because registers must be validated for every new connection
    */
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        dbLogger.logConnectionOpened();
     }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String playerId = null;
        String playerName = null;
        try {
            if (conn != null && conn.getRemoteSocketAddress() != null) {
                playerId = conn.getRemoteSocketAddress().toString();
            } else if (conn != null) {
                playerId = "socket-" + System.identityHashCode(conn);
            }
            if (playerRegistry.playerBySocket(conn) != null) {
                playerName = playerRegistry.playerBySocket(conn).getName();
            }
        } catch (Exception ignored) {}

        dbLogger.logConnectionClosed(playerId, playerName, code, reason);
        playerRegistry.remove(conn); // clean up player data on disconnect

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString(MessageType.K_TYPE);
            // info of the player
            String playerId = null;
            String playerName = null;
            try {
                if (conn != null && conn.getRemoteSocketAddress() != null) {
                    playerId = conn.getRemoteSocketAddress().toString();
                } else if (conn != null) {
                    playerId = "socket-" + System.identityHashCode(conn);
                }
                if (playerRegistry.playerBySocket(conn) != null) {
                    playerName = playerRegistry.playerBySocket(conn).getName();
                }
            } catch (Exception ignored) {}

            // log
            dbLogger.logCommandReceived(type, playerId, playerName);

            Command command = commandRegistry.get(type);
            if (command == null) {
                dbLogger.logCommandError(type, playerId, playerName, "Comando desconocido");
                System.out.println("⚠️ Comando desconocido: " + type);
                return;
            }
            command.execute(conn, json);

        } catch (Exception e) {
            String playerId = null;
            String playerName = null;
            try {
                if (conn != null && conn.getRemoteSocketAddress() != null) {
                    playerId = conn.getRemoteSocketAddress().toString();
                } else if (conn != null) {
                    playerId = "socket-" + System.identityHashCode(conn);
                }
                if (playerRegistry.playerBySocket(conn) != null) {
                    playerName = playerRegistry.playerBySocket(conn).getName();
                }
            } catch (Exception ignored) {}
            dbLogger.logCommandError("UNKNOWN", playerId, playerName, e.getMessage());
            System.err.println("❌ Error procesando mensaje: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        String playerId = null;
        String playerName = null;
        try {
            if (conn != null && conn.getRemoteSocketAddress() != null) {
                playerId = conn.getRemoteSocketAddress().toString();
            } else if (conn != null) {
                playerId = "socket-" + System.identityHashCode(conn);
            }
            if (conn != null && playerRegistry.playerBySocket(conn) != null) {
                playerName = playerRegistry.playerBySocket(conn).getName();
            }
        } catch (Exception ignored) {}
        dbLogger.logGameEvent("SERVER_ERROR", playerId, playerName, ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() { 
        dbLogger.logServerEvent("SERVER_START", "Servidor iniciado");
    }


    private void registerCommands() {
        commandRegistry.register(
            MessageType.T_REGISTER,
            new RegisterCommand(playerRegistry, this)
        );
        commandRegistry.register(
            MessageType.T_MOVE,
            new MoveCommand()
        );
        commandRegistry.register(
            MessageType.T_EXIT,
            new ExitCommand()
        );
        commandRegistry.register(
            MessageType.T_REQUEST_CONFIGURATION,
            new RequestConfigurationCommand()
        );
    }

    public void broadcastToAll(String message) {
        for (WebSocket socket : playerRegistry.snapshot().keySet()) {
            socket.send(message);
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(new InetSocketAddress(3000));
        server.start();
    }

    // metodo de debbuging (TEMPORAL)
    public void printDebugInfo() {
        dbLogger.printServerStats();
        dbLogger.printRecentLogs(10);
    }

}
