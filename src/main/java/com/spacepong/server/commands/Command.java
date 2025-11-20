package com.spacepong.server.commands;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

public interface Command {
    void execute(WebSocket socket, JSONObject json);
}
