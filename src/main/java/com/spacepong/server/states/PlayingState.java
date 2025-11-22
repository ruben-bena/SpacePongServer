package com.spacepong.server.states;

import org.json.JSONObject;

import com.spacepong.server.services.GameSession;

public class PlayingState implements GameState {

    @Override
    public void onEnter(GameSession session) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onEnter'");
    }

    @Override
    public void update(GameSession session) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public void onExit(GameSession session) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onExit'");
    }

    @Override
    public void handleMessage(GameSession session, JSONObject message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleMessage'");
    }

}
