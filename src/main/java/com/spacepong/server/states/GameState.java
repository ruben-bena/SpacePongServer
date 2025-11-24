package com.spacepong.server.states;

import org.json.JSONObject;

import com.spacepong.server.services.GameSession;

public interface GameState {
    void onEnter(GameSession session);
    void update(GameSession session);
    void onExit(GameSession session);
    void handleMessage(GameSession session, JSONObject message);
}