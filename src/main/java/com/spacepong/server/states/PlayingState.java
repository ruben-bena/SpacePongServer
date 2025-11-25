package com.spacepong.server.states;

import java.time.Instant;

import org.json.JSONObject;

import com.spacepong.server.enums.MessageType;
import com.spacepong.server.enums.StateType;
import com.spacepong.server.model.Game;
import com.spacepong.server.services.GameSession;
import com.spacepong.server.services.Logger;

public class PlayingState implements GameState {

    @Override
    public void onEnter(GameSession session) {
        Logger.log("Cambio a PlayingState. Aviso a jugadores con un json startGame");
        JSONObject payloadStartGame = new JSONObject();
        payloadStartGame.put(MessageType.K_TYPE, MessageType.T_START_GAME);
        payloadStartGame.put(MessageType.F_PLAYER_1, session.getPlayer1().getName());
        payloadStartGame.put(MessageType.F_PLAYER_2, session.getPlayer2().getName());
        session.sendToBothPlayers(payloadStartGame);
    }

    @Override
    public void update(GameSession session) {
        JSONObject payloadGameState = generateGameStateJson(session);
        session.sendToBothPlayers(payloadGameState);
        if (session.getGame().isGameFinished()) {
            session.changeState(StateType.FINISHED);
        }
    }

    @Override
    public void onExit(GameSession session) {
        Logger.log("Salgo de CountdownState");
    }

    @Override
    public void handleMessage(GameSession session, JSONObject message) {}

    public JSONObject generateGameStateJson(GameSession session) {
        /*
        * JSON example:
        *
        *  {
        *      "type": "gameState",
        *      "ball": { "x": 0.50, "y": 0.82 },
        *      "paddles": {
        *          "p1": { "y": 0.30 },
        *          "p2": { "y": 0.66 }
        *      },
        *      "score": { "p1": 3, "p2": 1 },
        *      "timestamp": "2025-11-07T21:23:02.123Z"
        *  }
        * 
        */
        Game game = session.getGame();
        JSONObject payloadGameState = new JSONObject();
        payloadGameState.put(MessageType.K_TYPE, MessageType.T_GAME_STATE);

        JSONObject ball = new JSONObject();
        ball.put(MessageType.X, game.getBallX());
        ball.put(MessageType.Y, game.getBallY());
        payloadGameState.put(MessageType.F_BALL, ball);

        JSONObject paddles = new JSONObject();
        paddles.put(MessageType.P1,
            new JSONObject().put(MessageType.Y, game.getPaddleP1Y()));
        paddles.put(MessageType.P2,
            new JSONObject().put(MessageType.Y, game.getPaddleP2Y()));
        payloadGameState.put(MessageType.F_PADDLES, paddles);

        JSONObject score = new JSONObject();
        score.put(MessageType.P1, game.getScoreP1());
        score.put(MessageType.P2, game.getScoreP2());
        payloadGameState.put(MessageType.F_SCORE, score);

        payloadGameState.put(MessageType.F_TIMESTAMP, Instant.now().toString());

        return payloadGameState;
    }
}
