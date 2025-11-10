package main.java.server.com.broadcast;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientId;
    
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        } catch (IOException e) {
            System.err.println("Error inicializando ClientHandler: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Mensaje de " + clientId + ": " + inputLine);
                // Opcional: reenviar a todos los clientes
                Server.broadcast("Cliente " + clientId + ": " + inputLine);
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + clientId);
        } finally {
            disconnect();
        }
    }
    
    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }
    
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
    
    public String getClientId() {
        return clientId;
    }
    
    private void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando conexión: " + e.getMessage());
        }
        Server.removeClient(this);
    }
}