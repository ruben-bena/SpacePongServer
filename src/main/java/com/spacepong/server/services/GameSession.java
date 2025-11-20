package com.spacepong.server.services;

import com.spacepong.server.model.Player;

public class GameSession {
    private final Player p1, p2;
    private final int gameId;

    GameSession(Player p1, Player p2, int gameId) {
        this.p1 = p1;
        this.p2 = p2;
        this.gameId = gameId;
    }
    // private final String gameId;
    // private final Player player1;
    // private final Player player2;
    // private GameStatus status;
    // private final Date startTime;

    // public GameSession(String gameId, Player player1, Player player2) {
    //     this.gameId = gameId;
    //     this.player1 = player1;
    //     this.player2 = player2;
    //     this.status = GameStatus.WAITING;
    //     this.startTime = new Date();
        
    //     // Marcar jugadores como en juego
    //     player1.setStatus(PlayerStatus.IN_GAME);
    //     player1.setCurrentGameId(gameId);
    //     player2.setStatus(PlayerStatus.IN_GAME);
    //     player2.setCurrentGameId(gameId);
    // }

    // // Getters
    // public String getGameId() { return gameId; }
    // public Player getPlayer1() { return player1; }
    // public Player getPlayer2() { return player2; }
    // public GameStatus getStatus() { return status; }
    // public void setStatus(GameStatus status) { this.status = status; }
    // public Date getStartTime() { return startTime; }
}