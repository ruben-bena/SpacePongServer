package com.spacepong.server.services;

import java.util.ArrayList;
import java.util.List;

import com.spacepong.server.model.Player;

public class GameManager implements Runnable {
    private volatile boolean running = false;
    private Thread gameThread;
    private List<GameSession> currentGames = new ArrayList<>();
    private final PlayerRegistry playerRegistry;
    private int nextGameId = 1;

    GameManager(PlayerRegistry playerRegistry) {
        this.playerRegistry = playerRegistry;
    }
    
    public void start() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();
            
            if (playerRegistry.isAtLeastTwoPlayersAvalible()) {
                createGameSession();
            }
            updateCurrentGames();
            
            long frameTime = System.currentTimeMillis() - startTime;
            long sleepTime = Math.max(0, 16 - frameTime); // ≈60 FPS
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void createGameSession() {
        Player[] newGamePlayers = playerRegistry.getFirstTwoAvaliblePlayersAndChangeTheirStatus();
        GameSession newGameSession = new GameSession(newGamePlayers[0], newGamePlayers[1], nextGameId, playerRegistry);
        currentGames.add(newGameSession);
        updateNextGameId();
    }

    private void updateCurrentGames() {

    }

    private void updateNextGameId() {
        nextGameId++;
        if (nextGameId <= 0) { nextGameId = 1; } // To manage int overflow
    }
}
