package com.broadcast;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static Set<PrintWriter> clientWriters = Collections.synchronizedSet(new HashSet<>());
    
    public static void main(String[] args) {
        System.out.println("🚀 Servidor Broadcast iniciado en puerto 3000");
        
        // Iniciar el broadcast automático cada 10 segundos
        startAutoBroadcast();
        
        try (ServerSocket serverSocket = new ServerSocket(3000)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ Nuevo cliente conectado: " + clientSocket.getInetAddress());
                
                // Manejar cliente en hilo separado
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Método para enviar broadcast automático cada 10 segundos
    private static void startAutoBroadcast() {
        Thread broadcastThread = new Thread(() -> {
            int messageCount = 0;
            try {
                while (true) {
                    Thread.sleep(10000); // 10 segundos
                    messageCount++;
                    broadcast("¡Hola a todos los clientes! Mensaje #" + messageCount + " desde el servidor");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        broadcastThread.setDaemon(true);
        broadcastThread.start();
        
        System.out.println("⏰ Broadcast automático activado (cada 10 segundos)");
    }
    
    // Método para enviar broadcast a todos los clientes
    public static void broadcast(String message) {
        synchronized (clientWriters) {
            if (clientWriters.isEmpty()) {
                System.out.println("⚠️  No hay clientes conectados para enviar: " + message);
                return;
            }
            
            System.out.println("📢 Enviando broadcast a " + clientWriters.size() + " clientes: " + message);
            
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }
    }
    
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // Agregar writer a la lista
                synchronized (clientWriters) {
                    clientWriters.add(out);
                    System.out.println("👥 Clientes conectados: " + clientWriters.size());
                }
                
                // Enviar mensaje de bienvenida
                out.println("Bienvenido al servidor broadcast!");
                
                // Leer mensajes del cliente
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("📨 Mensaje de cliente: " + inputLine);
                    // Opcional: reenviar a todos
                    broadcast("Cliente dice: " + inputLine);
                }
                
            } catch (IOException e) {
                System.out.println("❌ Cliente desconectado: " + socket.getInetAddress());
            } finally {
                // Remover writer al desconectar
                if (out != null) {
                    synchronized (clientWriters) {
                        clientWriters.remove(out);
                        System.out.println("👥 Clientes restantes: " + clientWriters.size());
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}