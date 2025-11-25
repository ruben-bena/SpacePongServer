package com.spacepong.server;

import java.net.InetSocketAddress;

import javax.xml.crypto.Data;

import com.spacepong.server.services.Logger;

import com.spacepong.server.bbdd.DatabaseLogger;

public class Main {
    public static void main(String[] args) {

        DatabaseLogger dbLogger = DatabaseLogger.getInstance();
        
        dbLogger.printServerStats();
        Logger.log("Arranco el server");
        Server server = new Server(new InetSocketAddress(3000));
        server.start();        

    }
}