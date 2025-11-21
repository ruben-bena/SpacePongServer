package com.spacepong.server.services;

import java.util.ArrayList;
import java.util.List;
import com.spacepong.server.model.Player;

public class GameManager implements Runnable {
    private volatile boolean running = false;
    private Thread gameThread;
    private List<GameSession> currentGames = new ArrayList<>();
    private final PlayerRegistry playerRegistry;
    private int gameCounter = 1;

    GameManager(PlayerRegistry playerRegistry) {
        this.playerRegistry = playerRegistry;
    }
    
    public void start() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
        com.spacepong.server.bbdd.DatabaseLogger.getInstance().logServerEvent("GAME_MANAGER_START", "GameManager iniciado");
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
        // Intentar obtener dos jugadores disponibles desde el registry
        Player[] players = playerRegistry.getFirstTwoAvaliblePlayersAndChangeTheirStatus();
        if (players == null || players.length < 2 || players[0] == null || players[1] == null) {
            return;
        }
        int gameId = gameCounter++;
        GameSession gs = new GameSession(players[0], players[1], gameId);
        currentGames.add(gs);

        String p1Id = null, p2Id = null;
        try {
            // We cannot access sockets here; use player names as identifier for logs
            p1Id = players[0].getName();
            p2Id = players[1].getName();
        } catch (Exception ignored) {}
        com.spacepong.server.bbdd.DatabaseLogger.getInstance().logGameEvent("GAME_CREATED", p1Id, p2Id, "Partida " + gameId + " creada");
    }
    private void updateCurrentGames() {
        // Small heartbeat log for game manager activity (lightweight)
        if (!currentGames.isEmpty()) {
            com.spacepong.server.bbdd.DatabaseLogger.getInstance().logServerEvent("GAME_MANAGER_UPDATE", "currentGames=" + currentGames.size());
        }
    }
}
