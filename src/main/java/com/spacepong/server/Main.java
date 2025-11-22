package com.spacepong.server;

import java.net.InetSocketAddress;

import com.spacepong.server.services.Logger;

public class Main {
    public static void main(String[] args) {
        Logger.log("Arranco el server");
        Server server = new Server(new InetSocketAddress(3000));
        server.start();
    }
}