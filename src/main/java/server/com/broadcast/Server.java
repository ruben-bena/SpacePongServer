package main.java.server.com.broadcast;

import main.java.server.com.broadcast.config.ServerConfig;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static ServerSocket serverSocket;
    private static boolean isRunning = false;
    
    public static void main(String[] args) {
        startServer();
    }
    
    public static void startServer() {
        int port = ServerConfig.getPort();
        String host = ServerConfig.getHost();
        
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));
            isRunning = true;
            
            System.out.println("🚀 Servidor Broadcast iniciado");
            System.out.println("📍 Host: " + host);
            System.out.println("🔌 Puerto: " + port);
            System.out.println("⏳ Esperando clientes...");
            
            // Hilo para aceptar conexiones
            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler clientHandler = new ClientHandler(clientSocket);
                        clients.add(clientHandler);
                        new Thread(clientHandler).start();
                        
                        System.out.println("✅ Nuevo cliente conectado: " + 
                            clientSocket.getInetAddress().getHostAddress() + ":" + 
                            clientSocket.getPort());
                        System.out.println("👥 Clientes conectados: " + clients.size());
                        
                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("Error aceptando conexión: " + e.getMessage());
                        }
                    }
                }
            }).start();
            
            // Consola para enviar broadcasts
            startConsoleInput();
            
        } catch (IOException e) {
            System.err.println("❌ Error iniciando servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void startConsoleInput() {
        new Thread(() -> {
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            try {
                String input;
                while (isRunning && (input = consoleReader.readLine()) != null) {
                    if ("shutdown".equalsIgnoreCase(input)) {
                        stopServer();
                        break;
                    } else if (!input.trim().isEmpty()) {
                        broadcast("[SERVER] " + input);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error leyendo consola: " + e.getMessage());
            }
        }).start();
    }
    
    public static void broadcast(String message) {
        if (clients.isEmpty()) {
            System.out.println("⚠️  No hay clientes conectados para enviar broadcast");
            return;
        }
        
        System.out.println("📢 Enviando broadcast: " + message);
        System.out.println("🎯 Destinatarios: " + clients.size() + " clientes");
        
        Iterator<ClientHandler> iterator = clients.iterator();
        int sentCount = 0;
        
        while (iterator.hasNext()) {
            ClientHandler client = iterator.next();
            if (client.isConnected()) {
                client.sendMessage(message);
                sentCount++;
            } else {
                iterator.remove();
                System.out.println("🧹 Cliente removido: " + client.getClientId());
            }
        }
        
        System.out.println("✅ Broadcast enviado a " + sentCount + " clientes");
    }
    
    public static void removeClient(ClientHandler client) {
        if (clients.remove(client)) {
            System.out.println("❌ Cliente desconectado: " + client.getClientId());
            System.out.println("👥 Clientes restantes: " + clients.size());
        }
    }
    
    public static void stopServer() {
        isRunning = false;
        System.out.println("🛑 Apagando servidor...");
        
        // Cerrar todas las conexiones de clientes
        for (ClientHandler client : clients) {
            try {
                client.sendMessage("[SERVER] Servidor apagándose...");
            } catch (Exception e) {
                // Ignorar errores en clientes desconectados
            }
        }
        clients.clear();
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error cerrando server socket: " + e.getMessage());
        }
        
        System.out.println("👋 Servidor apagado correctamente");
        System.exit(0);
    }
    
    public static int getConnectedClientsCount() {
        return clients.size();
    }
}