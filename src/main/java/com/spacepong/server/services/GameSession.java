package com.spacepong.server.services;

import org.json.JSONObject;

import com.spacepong.server.enums.StateType;
import com.spacepong.server.model.Game;
import com.spacepong.server.model.Player;
import com.spacepong.server.states.CountdownState;
import com.spacepong.server.states.FinishedState;
import com.spacepong.server.states.GameState;
import com.spacepong.server.states.PlayingState;

public class GameSession {
    private final Player p1, p2;
    private final int gameId;
    private final Game game;
    private final PlayerRegistry playerRegistry;
    private GameState currentState;

    private final CountdownState countdownState;
    private final PlayingState playingState;
    private final FinishedState finishedState;

    GameSession(Player p1, Player p2, int gameId, PlayerRegistry playerRegistry) {
        this.p1 = p1;
        this.p2 = p2;
        this.gameId = gameId;
        this.game = new Game();
        this.playerRegistry = playerRegistry;

        this.countdownState = new CountdownState();
        this.playingState = new PlayingState();
        this.finishedState = new FinishedState();

        this.currentState = countdownState;

        Logger.log("He creado un objeto GameSession con gameId=" + gameId);
    }

    public Player getPlayer1() { return p1; }
    public Player getPlayer2() { return p2; }
    public PlayerRegistry getPlayerRegistry() { return playerRegistry; }
    public Game getGame() { return game; }

    public void update() {
        currentState.update(this);
    }

    public void handleMessage(JSONObject message) {
        currentState.handleMessage(this, message);
    }

    public void changeState(StateType newState) {
        this.currentState.onExit(this);
        
        switch (newState) {
            case COUNTDOWN:
                this.currentState = countdownState;
                break;
            case PLAYING:
                this.currentState = playingState;
                break;
            case FINISHED:
                this.currentState = finishedState;
                break;
        }
        
        this.currentState.onEnter(this);
    }

    public void sendToBothPlayers(JSONObject message) {
        playerRegistry.socketByPlayer(p1).send(message.toString());
        playerRegistry.socketByPlayer(p2).send(message.toString());
    }
}