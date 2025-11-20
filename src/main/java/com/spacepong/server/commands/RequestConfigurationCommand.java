package com.spacepong.server.commands;

import java.io.InputStream;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import com.spacepong.server.enums.MessageType;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public class RequestConfigurationCommand implements Command {

    @Override
    public void execute(WebSocket socket, JSONObject json) {
        String configMessage = getGroupNameFromJson();
        JSONObject payload = new JSONObject();
        payload.put(MessageType.K_TYPE, MessageType.T_CONFIGURATION);
        payload.put(MessageType.F_CONFIG_MESSAGE, configMessage);
        socket.send(payload.toString());
    }

    private String getGroupNameFromJson() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("groups.json");
        try (JsonReader jsonReader = Json.createReader(inputStream)) {
            JsonObject jsonObject = jsonReader.readObject();
            return jsonObject.getString("name");
        } catch (Exception e) {
            return null;
        }
    }
}
