package com.spacepong.server;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        OldServer server = new OldServer(new InetSocketAddress(3000));
        server.start();
    }
}
