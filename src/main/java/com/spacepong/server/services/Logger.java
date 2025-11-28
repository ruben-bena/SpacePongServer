package com.spacepong.server.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final boolean isTurnedOn = true;
    public static void log(String message) {
        if (isTurnedOn) {
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("[yyyy-MM-dd HH:mm:ss]"));
            timestamp = "[" + timestamp + "]";
            System.out.println(timestamp + " " + message);
        }
    }
}
