package com.spacepong.server.services;

import java.util.ArrayList;
import java.util.List;

import com.spacepong.server.model.Player;

public class GameManager implements Runnable {
    private volatile boolean running = false;
    private Thread gameThread;
    private final List<GameSession> currentGames = new ArrayList<>();
    private final PlayerRegistry playerRegistry;
    private int nextGameId = 1;

    public GameManager(PlayerRegistry playerRegistry) {
        this.playerRegistry = playerRegistry;
        Logger.log("Creo el objeto GameManager");
    }
    
    public void start() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
        Logger.log("llamo al método start() del GameManager");
    }

    @Override
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();
            
            if (playerRegistry.isAtLeastTwoPlayersAvalible()) {
                Logger.log("Hay al menos 2 jugadores disponibles. Creo un GameSession");
                createGameSession();
            }
            updateCurrentGames();
            
            long frameTime = System.currentTimeMillis() - startTime;
            long sleepTime = Math.max(0, 16 - frameTime); // ≈60 FPS
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                playerRegistry.broadcastToAll(e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                playerRegistry.broadcastToAll(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void createGameSession() {
        Player[] newGamePlayers = playerRegistry.getFirstTwoAvaliblePlayersAndChangeTheirStatus();
        Logger.log("Los jugadores del nuevo GameSession son: " + newGamePlayers[0].getName() + ", " + newGamePlayers[1].getName());
        GameSession newGameSession = new GameSession(newGamePlayers[0], newGamePlayers[1], nextGameId, playerRegistry);
        currentGames.add(newGameSession);
        updateNextGameId();
    }

    private void updateCurrentGames() {
        for (GameSession gameSession : currentGames) {
            gameSession.update();
        }

        try {
            // Usar copia para evitar problemas de concurrencia
            List<GameSession> gamesCopy = new ArrayList<>(currentGames);
            for (GameSession gameSession : gamesCopy) {
                try {
                    gameSession.update();
                } catch (Exception e) {
                    System.err.println("Error en GameSession.update(): " + e.getMessage());
                    playerRegistry.broadcastToAll(e.getMessage());
                    e.printStackTrace();
                    // Remover juego problemático
                    currentGames.remove(gameSession);
                }
            }
        } catch (Exception e) {
            System.err.println("Error crítico en updateCurrentGames: " + e.getMessage());
            playerRegistry.broadcastToAll(e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateNextGameId() {
        nextGameId++;
        if (nextGameId <= 0) { nextGameId = 1; } // To manage int overflow
    }
}
