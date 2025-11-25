package com.spacepong.server.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;

import com.spacepong.server.enums.PlayerStatus;
import com.spacepong.server.model.Player;

public class PlayerRegistry {
    private final Map<WebSocket, Player> bySocket = new ConcurrentHashMap<>();
    private final Map<Player, WebSocket> byPlayer = new ConcurrentHashMap<>();
    private WebSocket socketRPi;

    public void add(WebSocket socket, Player player) {
        bySocket.put(socket, player);
        byPlayer.put(player, socket);
        player.updateDateAvalible();
        Logger.log("Nuevo Player con name=" + player.getName());
        // SQLiteLogger.log("Nuevo Player con name=" + player.getName());
        broadcastToAll("Nº jugadores disponibles = " + countAvaliblePlayers());
    }

    public Player remove(WebSocket socket) {
        Player player = bySocket.remove(socket);
        if (player != null) {
            byPlayer.remove(player);
            Logger.log("Borrado player con name=" + player.getName());
            // SQLiteLogger.log("Borrado player con name=" + player.getName());
        }
        return player;
    }

    public Player playerBySocket(WebSocket socket) {
        return bySocket.get(socket);
    }

    public WebSocket socketByPlayer(Player player) {
        return byPlayer.get(player);
    }

    public Map<WebSocket, Player> snapshot() {
        return Map.copyOf(bySocket);
    }

    public boolean isNameAlreadyRegistered(String newName) {
        for (Player player : snapshot().values()) {
            if (player.getName().equals(newName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAtLeastTwoPlayersAvalible() {
        int counter = 0;
        for (Player player : snapshot().values()) {
            if (player.getStatus() == PlayerStatus.AVAILABLE) {
                counter++;
            }
        }
        return counter >= 2;
    }

    public Player[] getFirstTwoAvaliblePlayersAndChangeTheirStatus() {
        synchronized (this) {
            Player p1 = null;
            Player p2 = null;
            for (Player player : snapshot().values()) {
                boolean isPlayerAvalible = player.getStatus() == PlayerStatus.AVAILABLE;
                if (!isPlayerAvalible) {
                    continue;
                }
                if (p1 == null || player.getDateAvalible().isBefore(p1.getDateAvalible())) {
                    p2 = p1;
                    p1 = player;
                } else if (p2 == null || player.getDateAvalible().isBefore(p2.getDateAvalible())) {
                    p2 = player;
                }
            }
            setPlayerIngame(p1);
            setPlayerIngame(p2);
            return new Player[] {p1, p2};
        }
    }

    public void setPlayerAvalible(Player player) {
        if (snapshot().containsValue(player)) {
            player.setStatus(PlayerStatus.AVAILABLE);
            player.updateDateAvalible();
        }
    }

    private void setPlayerIngame(Player player) {
        if (snapshot().containsValue(player)) {
            player.setStatus(PlayerStatus.IN_GAME);
        }
    }

    public void broadcastToAll(String message) {
        for (WebSocket socket : snapshot().keySet()) {
            socket.send(message);
        }
    }

    private int countAvaliblePlayers() {
        int counter = 0;
        for (Player player : snapshot().values()) {
            if (player.getStatus() == PlayerStatus.AVAILABLE) {
                counter++;
            }
        }
        return counter;
    }

    public void setSocketRPi(WebSocket socket) { this.socketRPi = socket; }
    public WebSocket getSocketRPi() { return this.socketRPi; }
}
