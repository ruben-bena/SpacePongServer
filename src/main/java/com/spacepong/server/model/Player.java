package com.spacepong.server.model;

import java.time.LocalDateTime;

import com.spacepong.server.enums.PlayerStatus;

public class Player {
    private final String name;
    private PlayerStatus status;
    private int currentGameId;
    private LocalDateTime dateAvalible;

    public Player(String name) {
        this.name = name;
        this.status = PlayerStatus.AVAILABLE;
    }

    public String getName() { return name; }
    public PlayerStatus getStatus() { return status; }
    public void setStatus(PlayerStatus status) { this.status = status; }
    public void setCurrentGameId(int currentGameId) { this.currentGameId = currentGameId; }
    public int getCurrentGameId() { return currentGameId; }
    public LocalDateTime getDateAvalible() {return dateAvalible; }
    public void updateDateAvalible() {
        this.dateAvalible = LocalDateTime.now();
    }
}