package com.spacepong.server.commands;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    private final Map<String, Command> commands = new HashMap<>();

    public void register(String type, Command command) {
        commands.put(type, command);
    }

    public Command get(String type) {
        return commands.get(type);
    }
}
