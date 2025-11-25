package com.spacepong.server.states;

import org.json.JSONObject;

import com.spacepong.server.enums.MessageType;
import com.spacepong.server.enums.StateType;
import com.spacepong.server.services.Countdown;
import com.spacepong.server.services.GameSession;
import com.spacepong.server.services.Logger;

public class CountdownState implements GameState {
    private Countdown countdown;
    private boolean isRunning = false;
    
    @Override
    public void onEnter(GameSession session) {
        Logger.log("Cambio a CountdownState");
        // SQLiteLogger.log("La partida pasa al Countdown");
    }
    
    @Override
    public void update(GameSession session) {
        if (isRunning) {
            return;
        } else {
            isRunning = true;
        }

        JSONObject payloadStartCountdown = new JSONObject();
        payloadStartCountdown.put(MessageType.K_TYPE, MessageType.T_START_COUNTDOWN);
        session.sendToBothPlayers(payloadStartCountdown);
        Logger.log("He mandado el startCountdown");
        
        countdown = new Countdown(3);

        countdown.setOnTick((remaining) -> {
            JSONObject payloadRemainingCountdown = new JSONObject();
            payloadRemainingCountdown.put(MessageType.K_TYPE, MessageType.T_REMAINING_COUNTDOWN);
            payloadRemainingCountdown.put(MessageType.F_REMAINING_COUNTDOWN, remaining);
            session.sendToBothPlayers(payloadRemainingCountdown);
            Logger.log("He mandado un remainingCountdown con valor de " + remaining);
        });

        countdown.setOnFinished(() -> {
            JSONObject payloadEndCountdown = new JSONObject();
            payloadEndCountdown.put(MessageType.K_TYPE, MessageType.T_END_COUNTDOWN);
            session.sendToBothPlayers(payloadEndCountdown);
            Logger.log("He mandado el endCountdown");

            session.changeState(StateType.PLAYING);
        });

        countdown.startCountdown();
    }
    
    @Override
    public void onExit(GameSession session) {
        Logger.log("Salgo de CountdownState");
    }
    
    @Override
    public void handleMessage(GameSession session, JSONObject message) { }
}