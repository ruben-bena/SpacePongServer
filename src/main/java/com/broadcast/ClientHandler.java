package com.broadcast;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientIP;
    private boolean connected = false;
    private String groupName;
    
    public ClientHandler(Socket socket, String groupName) {
        this.socket = socket;
        this.clientIP = socket.getInetAddress().getHostAddress();
        this.groupName = groupName;
    }
    
    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            
            System.out.println("🔌 Cliente conectado desde: " + clientIP);
            
            // ENVIAR CONFIGURACIÓN INMEDIATAMENTE al cliente
            sendGroupConfiguration();
            
            String inputLine;
            while ((inputLine = in.readLine()) != null && connected) {
                System.out.println("📨 [" + clientIP + "] Mensaje: " + inputLine);
                
                // Si el cliente pide configuración, reenviar
                if (inputLine.contains("\"type\"") && inputLine.contains("\"getConfig\"")) {
                    sendGroupConfiguration();
                }
            }
            
        } catch (IOException e) {
            System.out.println("❌ Error con cliente " + clientIP + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    private void sendGroupConfiguration() {
        // Enviar configuración del grupo en JSON
        String configMessage = String.format(
            "{\"type\":\"groupConfig\",\"groupName\":\"%s\",\"timestamp\":%d}",
            groupName, System.currentTimeMillis()
        );
        
        sendMessage(configMessage);
        System.out.println("📤 Configuración enviada a " + clientIP + ": " + groupName);
    }
    
    public void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
        }
    }
    
    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando conexión: " + e.getMessage());
        }
        System.out.println("🔌 Cliente desconectado: " + clientIP);
        Server.removeClient(this);
    }
}