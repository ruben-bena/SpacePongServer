package com.spacepong.server.commands;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import com.spacepong.server.Server;
import com.spacepong.server.bbdd.DatabaseLogger;
import com.spacepong.server.enums.MessageType;
import com.spacepong.server.model.Player;
import com.spacepong.server.services.PlayerRegistry;

public class RegisterCommand implements Command {

    private final PlayerRegistry playerRegistry;
    private final Server server;

    public RegisterCommand(PlayerRegistry playerRegistry, Server server) {
        this.playerRegistry = playerRegistry;
        this.server = server;
    }

    @Override
    public void execute(WebSocket socket, JSONObject receivedJson) {
        JSONObject payload = new JSONObject();
        String name = receivedJson.getString(MessageType.F_CLIENT_NAME);
        String playerId = null;
        try {
            if (socket != null && socket.getRemoteSocketAddress() != null) {
                playerId = socket.getRemoteSocketAddress().toString();
            } else if (socket != null) {
                playerId = "socket-" + System.identityHashCode(socket);
            }
        } catch (Exception ignored) {
            playerId = socket != null ? "socket-" + System.identityHashCode(socket) : null;
        }

        // Registrar recepción del comando
        DatabaseLogger.getInstance().logCommandReceived("REGISTER", playerId, name);
        if (playerRegistry.isNameAlreadyRegistered(name)) {
            payload.put(MessageType.K_TYPE, MessageType.T_DENY_REGISTER);
            payload.put(MessageType.F_REASON, "name is already in use by someone else");
            DatabaseLogger.getInstance().logCommandError("REGISTER", playerId, name, "name already in use");
        } else {
            payload.put(MessageType.K_TYPE, MessageType.T_ACCEPT_REGISTER);
            playerRegistry.add(
                socket,
                new Player(name)
            );
            // Log registro de jugador
            DatabaseLogger.getInstance().logPlayerRegistered(playerId, name);
            server.broadcastToAll("¡Nuevo jugador registrado! Hola " + name);
        }
        socket.send(payload.toString());
    }

}
