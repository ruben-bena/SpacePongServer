package com.spacepong.server.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;

import com.spacepong.server.enums.PlayerStatus;
import com.spacepong.server.model.Player;
import com.spacepong.server.bbdd.DatabaseLogger;

public class PlayerRegistry {
    private final Map<WebSocket, Player> bySocket = new ConcurrentHashMap<>();

    public void add(WebSocket socket, Player player) {
        bySocket.put(socket, player);
        try {
            String playerId = socket != null && socket.getRemoteSocketAddress() != null
                    ? socket.getRemoteSocketAddress().toString()
                    : (socket != null ? "socket-" + System.identityHashCode(socket) : null);
            DatabaseLogger.getInstance().logPlayerRegistered(playerId, player.getName());
        } catch (Exception ignored) {}
    }

    public boolean remove(WebSocket socket) {
        if (!bySocket.containsKey(socket)) { return false; }
        Player p = bySocket.remove(socket);
        try {
            String playerId = socket != null && socket.getRemoteSocketAddress() != null
                    ? socket.getRemoteSocketAddress().toString()
                    : (socket != null ? "socket-" + System.identityHashCode(socket) : null);
            String playerName = p != null ? p.getName() : null;
            DatabaseLogger.getInstance().logConnectionClosed(playerId, playerName, 0, "removed from registry");
        } catch (Exception ignored) {}
        return true;
    }

    public Player playerBySocket(WebSocket socket) {
        return bySocket.get(socket);
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
            if (counter >= 2) {
                return true;
            }
        }
        return false;
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
}
