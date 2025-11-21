package com.spacepong.server.commands;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import com.spacepong.server.bbdd.DatabaseLogger;

public class MoveCommand implements Command {

    @Override
    public void execute(WebSocket socket, JSONObject json) {
        String playerId = null;
        try {
            if (socket != null && socket.getRemoteSocketAddress() != null) {
                playerId = socket.getRemoteSocketAddress().toString();
            } else if (socket != null) {
                playerId = "socket-" + System.identityHashCode(socket);
            }
        } catch (Exception ignored) {}

        // Log command received (no player name available here)
        DatabaseLogger.getInstance().logCommandReceived("MOVE", playerId, null);

        // TODO: implementar lógica de movimiento
        // Si hay error, registrar con logCommandError
    }

}
