package com.spacepong.server.commands;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import com.spacepong.server.Server;
import com.spacepong.server.enums.MessageType;
import com.spacepong.server.model.Player;
import com.spacepong.server.services.GameSession;
import com.spacepong.server.services.PlayerRegistry;

public class MoveAPPCommand implements Command {
    private final Server server;
    private final PlayerRegistry playerRegistry;

    public MoveAPPCommand(Server server, PlayerRegistry playerRegistry) {
        this.server = server;
        this.playerRegistry = playerRegistry;
    }

    @Override
    public void execute(WebSocket socket, JSONObject json) {
        Player player = playerRegistry.playerBySocket(socket);
        GameSession gameSession = server.getGameManager().getPlayerCurrentSession(player);
        if (gameSession != null) {
            if (player == gameSession.getPlayer1()) {
                gameSession.getGame().setPaddleP1Y(json.getFloat(MessageType.Y));
            } else if (player == gameSession.getPlayer2()) {
                gameSession.getGame().setPaddleP2Y(json.getFloat(MessageType.Y));
            }
        }
    }
}
