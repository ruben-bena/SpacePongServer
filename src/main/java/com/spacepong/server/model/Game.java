package com.spacepong.server.model;

public class Game {
    /*
    * All float parameters are expressed as a percentage that goes from 0 to 1
    * With this, we achieve being client-agnostic for displaying the game on a screen
    * */
    private float ballX, ballY;
    private float paddleP1Y, paddleP2Y;
    private int scoreP1, scoreP2;
    private final float ballRadius;
    private boolean gameFinished;

    public Game() {
        this.ballX = 0.5f;
        this.ballY = 0.5f;
        this.paddleP1Y = 0.5f;
        this.paddleP2Y = 0.5f;
        this.scoreP1 = 0;
        this.scoreP2 = 0;
        this.ballRadius = 0.1f;
        this.gameFinished = false;
    }

    // Getters
    public float getBallX() { return ballX; }
    public float getBallY() { return ballY; }
    public float getPaddleP1Y() { return paddleP1Y; }
    public float getPaddleP2Y() { return paddleP2Y; }
    public float getBallRadius() { return ballRadius; }
    public int getScoreP1() { return scoreP1; }
    public int getScoreP2() { return scoreP2; }

    // Setters
    public void setBallX(float newBallX) { this.ballX = newBallX; }
    public void setBallY(float newBallY) { this.ballY = newBallY; }
    public void setPaddleP1Y(float newPaddleP1Y) { this.paddleP1Y = newPaddleP1Y; }
    public void setPaddleP2Y(float newPaddleP2Y) { this.paddleP2Y = newPaddleP2Y; }
    public void setScoreP1(int newScore) { this.scoreP1 = newScore; }
    public void setScoreP2(int newScore) { this.scoreP2 = newScore; }

    public boolean isGameFinished() { return gameFinished; }
}
