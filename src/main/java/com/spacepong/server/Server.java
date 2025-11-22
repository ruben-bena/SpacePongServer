package com.spacepong.server;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import com.spacepong.server.commands.Command;
import com.spacepong.server.commands.CommandRegistry;
import com.spacepong.server.commands.ExitCommand;
import com.spacepong.server.commands.MoveCommand;
import com.spacepong.server.commands.RegisterCommand;
import com.spacepong.server.commands.RequestConfigurationCommand;
import com.spacepong.server.enums.MessageType;
import com.spacepong.server.services.GameManager;
import com.spacepong.server.services.PlayerRegistry;

public class Server extends WebSocketServer {

    private final PlayerRegistry playerRegistry = new PlayerRegistry();
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final GameManager gameManager;

    public Server(InetSocketAddress address) {
        super(address);
        registerCommands();
        this.gameManager = new GameManager(playerRegistry);
        gameManager.start();
    }

    @Override
    /* 
    * onOpen does nothing because registers must be validated for every new connection
    */
    public void onOpen(WebSocket conn, ClientHandshake handshake) { }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        playerRegistry.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString(MessageType.K_TYPE);
            Command command = commandRegistry.get(type);
            if (command == null) {
                System.out.println("⚠️ Comando desconocido: " + type);
                return;
            }
            command.execute(conn, json);

        } catch (Exception e) {
            System.err.println("❌ Error procesando mensaje: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() { }

    private void registerCommands() {
        commandRegistry.register(
            MessageType.T_REGISTER,
            new RegisterCommand(playerRegistry)
        );
        commandRegistry.register(
            MessageType.T_MOVE,
            new MoveCommand() // TODO
        );
        commandRegistry.register(
            MessageType.T_EXIT,
            new ExitCommand() // TODO
        );
        commandRegistry.register(
            MessageType.T_REQUEST_CONFIGURATION,
            new RequestConfigurationCommand()
        );
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(new InetSocketAddress(3000));
        server.start();
    }
}
