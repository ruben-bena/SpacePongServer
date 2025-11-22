package com.spacepong.server.services;

import org.json.JSONObject;

import com.spacepong.server.enums.GameStatus;
import com.spacepong.server.enums.MessageType;
import com.spacepong.server.model.Game;
import com.spacepong.server.model.Player;

public class GameSession {
    private final Player p1, p2;
    private final int gameId;
    private final Game game;
    private final PlayerRegistry playerRegistry;
    private GameStatus status = GameStatus.WAITING;
    private GameState currentState;

    GameSession(Player p1, Player p2, int gameId, PlayerRegistry playerRegistry) {
        this.p1 = p1;
        this.p2 = p2;
        this.gameId = gameId;
        this.game = new Game();
        this.playerRegistry = playerRegistry;
        this.status = GameStatus.COUNTDOWN;
        Logger.log("He creado un objeto GameSession con gameId=" + gameId);
    }

    public void update() {
        switch (status) {
            case COUNTDOWN:
                handleCountdown();
                status = GameStatus.PLAYING;
                break;
            case PLAYING:
                // processReceivedInfo();
                // generateGameState();
                // sendGameStateToPlayer();
                // if (isGameFinished()) {
                //     status = GameStatus.FINISHED;
                //     sendGameOutcomeToPlayer();
                // }
                break;
            case FINISHED:
                break;
        }
    }

    private void handleCountdown() {
        Logger.log("Entro en handleCountdown");
        JSONObject payloadStartCountdown = new JSONObject();
        payloadStartCountdown.put(MessageType.K_TYPE, MessageType.T_START_COUNTDOWN);
        playerRegistry.socketByPlayer(p1).send(payloadStartCountdown.toString());
        playerRegistry.socketByPlayer(p2).send(payloadStartCountdown.toString());
        Logger.log("He mandado el startCountdown");
        Countdown countdown = new Countdown(3);
        countdown.setOnTick((remaining) -> {
            JSONObject payloadRemainingCountdown = new JSONObject();
            payloadRemainingCountdown.put(MessageType.K_TYPE, MessageType.T_REMAINING_COUNTDOWN);
            payloadRemainingCountdown.put(MessageType.F_REMAINING_COUNTDOWN, remaining);
            playerRegistry.socketByPlayer(p1).send(payloadRemainingCountdown.toString());
            playerRegistry.socketByPlayer(p2).send(payloadRemainingCountdown.toString());
            Logger.log("He mandado un remainingCountdown con valor de " + remaining);
        });
        countdown.setOnFinished(() -> {
            JSONObject payloadEndCountdown = new JSONObject();
            payloadEndCountdown.put(MessageType.K_TYPE, MessageType.T_END_COUNTDOWN);
            playerRegistry.socketByPlayer(p1).send(payloadEndCountdown.toString());
            playerRegistry.socketByPlayer(p2).send(payloadEndCountdown.toString());
            Logger.log("He mandado el endCountdown");
        });
        countdown.startCountdown();
    }
}