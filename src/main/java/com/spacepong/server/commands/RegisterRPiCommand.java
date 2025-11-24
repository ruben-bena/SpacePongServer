package com.spacepong.server.commands;

import com.spacepong.server.services.PlayerRegistry;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

public class RegisterRPiCommand implements Command {
    private final PlayerRegistry playerRegistry;

    public RegisterRPiCommand(PlayerRegistry playerRegistry) {
        this.playerRegistry = playerRegistry;
    }

    @Override
    public void execute(WebSocket socket, JSONObject receivedJson) {
        playerRegistry.setSocketRPi(socket);
        socket.send("Has sido registrado correctamente como socketRPi");
    }
}
