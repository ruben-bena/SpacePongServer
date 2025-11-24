package com.spacepong.server.states;

import org.json.JSONObject;

import com.spacepong.server.services.GameSession;

public class FinishedState implements GameState {

    @Override
    public void onEnter(GameSession session) {
    }

    @Override
    public void update(GameSession session) {
    }

    @Override
    public void onExit(GameSession session) {
    }

    @Override
    public void handleMessage(GameSession session, JSONObject message) {
    }

}
