package com.broadcast;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());
    private static String groupName = "SpacePong Team"; // ← NOMBRE DEL GRUPO
    
    public static void main(String[] args) throws IOException {
        System.out.println("🚀 SpacePong Server - Puerto 3000");
        System.out.println("📍 Grupo: " + groupName);
        
        ServerSocket serverSocket = new ServerSocket(3000, 50, InetAddress.getByName("0.0.0.0"));
        System.out.println("✅ Servidor listo en puerto 3000");
        
        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket, groupName);
            clientHandlers.add(clientHandler);
            new Thread(clientHandler).start();
        }
    }
    
    public static void removeClient(ClientHandler handler) {
        clientHandlers.remove(handler);
        System.out.println("👋 Cliente removido. Total: " + clientHandlers.size());
    }
    
    public static String getGroupName() {
        return groupName;
    }
}