package com.spacepong.server.commands;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import com.spacepong.server.enums.MessageType;
import com.spacepong.server.model.Player;
import com.spacepong.server.services.PlayerRegistry;

public class RegisterCommand implements Command {

    private final PlayerRegistry playerRegistry;

    public RegisterCommand(PlayerRegistry playerRegistry) {
        this.playerRegistry = playerRegistry;
    }

    @Override
    public void execute(WebSocket socket, JSONObject receivedJson) {
        JSONObject payload = new JSONObject();
        String name = receivedJson.getString(MessageType.F_CLIENT_NAME);
        if (playerRegistry.isNameAlreadyRegistered(name)) {
            payload.put(MessageType.K_TYPE, MessageType.T_DENY_REGISTER);
            payload.put(MessageType.F_REASON, "name is already in use by someone else");
        } else {
            payload.put(MessageType.K_TYPE, MessageType.T_ACCEPT_REGISTER);
            playerRegistry.add(
                socket,
                new Player(name)
            );
            playerRegistry.broadcastToAll("¡Nuevo jugador registrado! Hola " + name);
        }
        socket.send(payload.toString());
    }

}
