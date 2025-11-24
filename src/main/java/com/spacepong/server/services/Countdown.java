package com.spacepong.server.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Countdown {

    private final int startSeconds;
    private int remainingSeconds;

    private Consumer<Integer> onTick;  // callback each second
    private Runnable onFinished;       // callback when finish

    private ScheduledExecutorService scheduler;

    public Countdown(int startSeconds) {
        this.startSeconds = startSeconds;
    }

    public void startCountdown() {
        remainingSeconds = startSeconds;

        if (onTick != null) {
            onTick.accept(remainingSeconds);
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            remainingSeconds--;

            if (remainingSeconds > 0) {
                if (onTick != null) {
                    onTick.accept(remainingSeconds);
                }
            } else {
                if (onTick != null) {
                    onTick.accept(0);
                }
                if (onFinished != null) {
                    onFinished.run();
                }

                scheduler.shutdown();
            }

        }, 1, 1, TimeUnit.SECONDS); // waits 1s before every execution
    }

    public void setOnTick(Consumer<Integer> onTick) {
        this.onTick = onTick;
    }

    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
