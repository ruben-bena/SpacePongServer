package com.spacepong.server.services;

import java.util.ArrayList;
import java.util.List;

public class GameManager implements Runnable {
    private volatile boolean running = false;
    private Thread gameThread;
    private List<GameSession> currentGames = new ArrayList<>();
    private final PlayerRegistry playerRegistry;

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
            
            while (playerRegistry.isAtLeastTwoPlayersAvalible()) {
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

    }

    private void updateCurrentGames() {

    }
}
