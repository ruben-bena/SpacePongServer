package com.spacepong.server;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(new InetSocketAddress(3000));
        server.start();
    }
}
