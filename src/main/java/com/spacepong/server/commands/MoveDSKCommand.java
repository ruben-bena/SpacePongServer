package com.spacepong.server.commands;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import com.spacepong.server.Server;
import com.spacepong.server.enums.MessageType;
import com.spacepong.server.model.Player;
import com.spacepong.server.services.GameSession;
import com.spacepong.server.services.PlayerRegistry;


public class MoveDSKCommand implements Command {

    private final Server server;
    private final PlayerRegistry playerRegistry;

    public MoveDSKCommand(Server server, PlayerRegistry playerRegistry) {
        this.server = server;
        this.playerRegistry = playerRegistry;
    }

    @Override
    public void execute(WebSocket socket, JSONObject json) {

        Player player = playerRegistry.playerBySocket(socket);
        GameSession gameSession = server.getGameManager().getPlayerCurrentSession(player);
        if (gameSession == null) return;
        boolean isP1 = (player == gameSession.getPlayer1());
        if (json.has("direction")) {
            String dir = json.optString("direction", "stop");
            float speed = 0.02f;

            if (isP1) {
                float y = gameSession.getGame().getPaddleP1Y();
                if (dir.equals("up"))   y += speed;
                if (dir.equals("down")) y -= speed;
                y = Math.max(0f, Math.min(1f, y));
                gameSession.getGame().setPaddleP1Y(y);
            } else {
                float y = gameSession.getGame().getPaddleP2Y();
                if (dir.equals("up"))   y += speed;
                if (dir.equals("down")) y -= speed;
                y = Math.max(0f, Math.min(1f, y));
                gameSession.getGame().setPaddleP2Y(y);
            }
        }
    }
}