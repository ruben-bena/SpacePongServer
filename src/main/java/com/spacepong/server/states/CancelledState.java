package com.spacepong.server.states;

import org.json.JSONObject;

import com.spacepong.server.enums.MessageType;
import com.spacepong.server.services.GameSession;

public class CancelledState implements GameState {
    @Override
    public void onEnter(GameSession session) {
        JSONObject payloadCancelledGame = new JSONObject();
        payloadCancelledGame.put(MessageType.K_TYPE, MessageType.T_CANCELLED_GAME);
        session.sendToBothPlayers(payloadCancelledGame);
    }

    @Override
    public void update(GameSession session) {}

    @Override
    public void onExit(GameSession session) {}

    @Override
    public void handleMessage(GameSession session, JSONObject message) {}
}
