package com.server;

import java.util.function.Consumer;

import javafx.animation.AnimationTimer;

public class CanvasTimer extends AnimationTimer {

    public double fps;
    
    private long lastNanoTime;
    private int frameCount;
    private double elapsedTime;
    private double updateInterval = 0.25;
    private Double frameTime;

    private Consumer<Double> runFunction;
    private Runnable drawFunction;

    public CanvasTimer(Consumer<Double> runFunction, 
    // Runnable drawFunction, 
    double targetFPS) {
        this.runFunction = runFunction;
        // this.drawFunction = drawFunction;
        if (targetFPS > 0) {
            this.frameTime = 1.0 / targetFPS;
        } else {
            this.frameTime = null; // Without FPS limit
        }
        lastNanoTime = System.nanoTime();
    }

    @Override
    public void handle(long now) {
        long nanoDelta = now - lastNanoTime;
        double delta = nanoDelta / 1_000_000_000.0;

        if (frameTime == null || delta >= frameTime) {
            elapsedTime += delta;
            frameCount++;

            if (elapsedTime >= updateInterval) {
                fps = frameCount / elapsedTime;
                elapsedTime = 0;
                frameCount = 0;
            }

            runFunction.accept(fps);
            // drawFunction.run();

            lastNanoTime = now;
        }
    }

  
    
}
