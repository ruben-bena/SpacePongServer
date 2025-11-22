package com.spacepong.server.services;

import org.json.JSONObject;

import com.spacepong.server.enums.GameStatus;
import com.spacepong.server.enums.MessageType;
import com.spacepong.server.model.Player;

public class GameSession {
    private final Player p1, p2;
    private final int gameId;
    private final PlayerRegistry playerRegistry;
    private GameStatus status = GameStatus.WAITING;

    GameSession(Player p1, Player p2, int gameId, PlayerRegistry playerRegistry) {
        this.p1 = p1;
        this.p2 = p2;
        this.gameId = gameId;
        this.playerRegistry = playerRegistry;
        this.status = GameStatus.COUNTDOWN;
    }

    public void update() {
        if (status == GameStatus.COUNTDOWN) {
            handleCountdown();
            status = GameStatus.WAITING;
        }
    }

    private void handleCountdown() {
        JSONObject payloadStartCountdown = new JSONObject();
        payloadStartCountdown.put(MessageType.K_TYPE, MessageType.T_START_COUNTDOWN);
        playerRegistry.socketByPlayer(p1).send(payloadStartCountdown.toString());
        playerRegistry.socketByPlayer(p2).send(payloadStartCountdown.toString());
        Countdown countdown = new Countdown(3);
        countdown.setOnTick((remaining) -> {
            JSONObject payloadRemainingCountdown = new JSONObject();
            payloadRemainingCountdown.put(MessageType.K_TYPE, MessageType.T_REMAINING_COUNTDOWN);
            payloadRemainingCountdown.put(MessageType.F_REMAINING_COUNTDOWN, remaining);
            playerRegistry.socketByPlayer(p1).send(payloadRemainingCountdown.toString());
            playerRegistry.socketByPlayer(p2).send(payloadRemainingCountdown.toString());
        });
        countdown.setOnFinished(() -> {
            JSONObject payloadEndCountdown = new JSONObject();
            payloadEndCountdown.put(MessageType.K_TYPE, MessageType.T_END_COUNTDOWN);
            playerRegistry.socketByPlayer(p1).send(payloadEndCountdown.toString());
            playerRegistry.socketByPlayer(p2).send(payloadEndCountdown.toString());
        });
        countdown.startCountdown();
    }
}